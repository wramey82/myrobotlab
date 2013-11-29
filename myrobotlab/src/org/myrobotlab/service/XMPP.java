package org.myrobotlab.service;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.webgui.RESTProcessor;
import org.myrobotlab.webgui.RESTProcessor.RESTException;
import org.slf4j.Logger;

public class XMPP extends Service implements MessageListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(XMPP.class.getCanonicalName());
	static final int packetReplyTimeout = 500; // millis

	//
	String user;
	String password;
	String host;
	int port = 5222;
	String service = "gmail.com"; // defaulted :P

	transient ConnectionConfiguration config;
	transient XMPPConnection connection;
	transient ChatManager chatManager;

	transient Roster roster = null;

	transient HashMap<String, RosterEntry> idToEntry = new HashMap<String, RosterEntry>();

	/**
	 * auditors chat buddies who can see what commands are being processed and
	 * by who through the XMPP service TODO - audit full system ??? regardless
	 * of message origin?
	 */
	HashSet<String> auditors = new HashSet<String>();
	HashSet<String> responseRelays = new HashSet<String>();
	HashSet<String> allowCommandsFrom = new HashSet<String>();
	transient HashMap<String, Chat> chats = new HashMap<String, Chat>();

	public XMPP(String n) {
		super(n, XMPP.class.getCanonicalName());
	}

	@Override
	public void stopService() {
		super.stopService();
		disconnect();
	}

	@Override
	public String getDescription() {
		return "xmpp service to access the jabber network";
	}

	public boolean connect(String host) {
		this.host = host;
		return connect(host, port);
	}

	public boolean connect(String host, int port) {
		this.host = host;
		this.port = port;
		return connect(host, port, user, password);
	}

	public boolean connect(String host, int port, String user, String password) {
		return connect(host, port, user, password, service);
	}

	public boolean connect(String host, int port, String user, String password, String service) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.service = service;
		return connect();
	}

	public boolean connect() {

		try {

			if (config == null) {
				SASLAuthentication.supportSASLMechanism("PLAIN");
				// SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
				// SASLDigestMD5Mechanism.class);
				// SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);
				// WTF is a service name ?
				// ConnectionConfiguration config = new
				// ConnectionConfiguration(SERVER_HOST, SERVER_PORT);
				// ConnectionConfiguration config = new
				// ConnectionConfiguration("talk.google.com", 5222,
				// "gmail.com");
				// config.setTruststoreType("BKS");
				config = new ConnectionConfiguration(host, 5222, "gmail.com");
			}

			if (connection == null || !connection.isConnected()) {

				log.info(String.format("%s new connection to %s:%d", getName(), host, port));
				connection = new XMPPConnection(config);
				connection.connect();
				log.info(String.format("%s connected %s", getName(), connection.isConnected()));
				chatManager = connection.getChatManager();

				log.info(String.format("%s is connected - logging in", getName()));
				if (!login(user, password)) {
					disconnect();
				}

				getRoster();

			}

			return connection.isConnected();

		} catch (Exception e) {
			Logging.logException(e);
		}

		return false;
	}

	public void disconnect() {
		log.info(String.format("%s disconnecting from %s:%d", getName(), host, port));
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
			connection = null;
		}

		config = null;
		chatManager = null;
		chats.clear();
	}

	public boolean login(String username, String password) {
		log.info(String.format("login %s xxxxxxxx", username));
		if (connection != null && connection.isConnected()) {
			try {
				connection.login(username, password);
				// getRoster();
			} catch (Exception e) {
				Logging.logException(e);
				return false;
			}
			return true;
		} else {
			log.error("not connected !!!");
			return false;
		}
	}

	public void setStatus(boolean available, String status) {
		connect();
		if (connection != null && connection.isConnected()) {
			Presence.Type type = available ? Type.available : Type.unavailable;
			Presence presence = new Presence(type);
			presence.setStatus(status);
			connection.sendPacket(presence);
		} else {
			log.error("setStatus not connected");
		}
	}

	public Roster getRoster() {
		roster = connection.getRoster();
		for (RosterEntry entry : roster.getEntries()) {
			log.info(String.format("User: %s %s ", entry.getName(), entry.getUser()));
			idToEntry.put(entry.getName(), entry);
		}
		return roster;
	}

	RosterEntry getEntry(String userOrBuddyId) {
		RosterEntry entry = null;
		entry = roster.getEntry(userOrBuddyId);
		if (entry != null) {
			return entry;
		}

		if (idToEntry.containsKey(userOrBuddyId)) {
			return idToEntry.get(userOrBuddyId);
		}

		return null;

	}

	// TODO implement lower level messaging
	public void sendMyRobotLabJSONMessage(org.myrobotlab.framework.Message msg) {

	}

	public void sendMyRobotLabRESTMessage(org.myrobotlab.framework.Message msg) {

	}

	public org.myrobotlab.framework.Message processMyRobotLabRESTMessage(Message msg) {

		return null;
	}

	/**
	 * broadcast a chat message to all buddies in the relay
	 * 
	 * @param text
	 *            - text to broadcast
	 */
	public void broadcast(String text) {
		for (String buddy : responseRelays) {
			sendMessage(text, buddy);
		}
	}
	
	// FIXME - create Resistrar interface sendMRLMessage(Message msg, URI/String key)
	public void sendMRLMessage(org.myrobotlab.framework.Message msg, String id)
	{
		//Base64.enc
	}

	// FIXME synchronized not needed?
	synchronized public void sendMessage(String text, String id) {
		try {

			connect();

			RosterEntry entry = getEntry(id);
			if (entry == null) {
				error("could not send message to %s - entry does not exist", id);
				return;
			}

			String buddyJID = entry.getUser();

			// FIXME FIXME FIXME !!! - if
			// "just connected - ie just connected and this is the first chat of the connection then "create
			// chat" otherwise use existing chat !"
			Chat chat = null;
			if (chats.containsKey(buddyJID)) {
				chat = chats.get(buddyJID);
			} else {
				chat = chatManager.createChat(buddyJID, this);
				chats.put(buddyJID, chat);
			}

			if (text == null) {
				text = "null"; // dangerous converson?
			}
			log.info(String.format("sending %s (%s) %s", entry.getName(), buddyJID, text));
			chat.sendMessage(text);

		} catch (Exception e) {
			// currentChats.remove(buddyJID);
			Logging.logException(e);
		}
	}

	public void createEntry(String user, String name) throws Exception {
		log.info(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
		connect();
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}

	// FIXME move to codec package
	public Object processRESTChatMessage(Message msg) throws RESTException {
		String body = msg.getBody();
		log.info(String.format("processRESTChatMessage [%s]", body));

		if (auditors.size() > 0) {
			for (String auditor : auditors) {
				sendMessage(String.format("%s %s", msg.getFrom(), msg.getBody()), auditor);
			}
		}

		if (body == null || body.length() < 1) {
			log.info("invalid");
			return null;
		}

		// TODO - allow to be in middle of message
		// pre-processing begin --------
		int pos0 = body.indexOf('/');
		if (pos0 != 0) {
			log.info("command must start with /");
			return null;
		}

		int pos1 = body.indexOf("\n");
		if (pos1 == -1) {
			pos1 = body.length();
		}

		String uri = "";
		if (pos1 > 0) {
			uri = body.substring(pos0, pos1);
		}

		uri = uri.trim();

		log.info(String.format("[%s]", uri));

		// pre-processing end --------

		Object o = RESTProcessor.invoke(uri);

		// FIXME - encoding is that input uri before call ?
		// or config ?
		// FIXME - echo
		// FIXME - choose type of encoding based on input ? part of the URI init
		// call ?
		// e.g. /api/gson/runtime/getLocalIPAdddresses [/api/gson/ .. is assumed
		// (non-explicit) and pre-pended

		if (o != null) {
			broadcast(Encoder.gson.toJson(o, o.getClass()));
			// broadcast(o.toString());
		} else {
			broadcast(null);
		}

		return o;
	}

	// FIXME - should be in runtime
	public String listServices() {
		StringBuffer sb = new StringBuffer();
		List<ServiceWrapper> services = Runtime.getServices();
		for (int i = 0; i < services.size(); ++i) {
			ServiceWrapper sw = services.get(i);
			sb.append(String.format("/%s\n", sw.name));
		}
		return sb.toString();
	}

	// FIXME - Registrar interface
	// FIXME - get clear about different levels of authorization -
	// Security/Framework to handle at message/method level
	@Override
	public void processMessage(Chat chat, Message msg) {

		Message.Type type = msg.getType();
		String from = msg.getFrom();
		String body = msg.getBody();
		if (type.equals(Message.Type.error) || body == null || body.length() == 0) {
			log.error("{} processMessage returned error {}", from, body);
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Received %s message [%s] from [%s]", type, body, from));
		}
		
		
		if (body.startsWith(Encoder.SCHEME_BASE64))
		{
			org.myrobotlab.framework.Message inboundMsg = Encoder.base64ToMsg(body);
			// must add key for registration ???
			if (inboundMsg.method.equals("registerServices")){
				ServiceEnvironment se = (ServiceEnvironment)inboundMsg.data[0];
				String mrlURI = String.format("mrl:%s/xmpp://%s", getName(), from);
				try { // HMMM a vote for String vs URI here - since we need to catch syntax !!!
				se.accessURL =  new URI(mrlURI);
				} catch(Exception e) {
					Logging.logException(e);
				}
			}
			// ??? WTF not right
			Runtime.getInstance().registerServices(inboundMsg);
			getOutbox().add(inboundMsg);
			return;
		}

		// BinaryToken ???
		// Security.Authorization (buddyId -> Level ???)
		// Basic buddyId
		if (body.startsWith("{")) {
			try {
				// gson encoded MRL Message !
				org.myrobotlab.framework.Message remoteMsg = Encoder.gsonToMsg(body);
				Runtime.getInstance().registerServices(remoteMsg);
				//getOutbox().add(remoteMsg);
			} catch (Exception e) {
				Logging.logException(e);
			}
		}

		if (body.charAt(0) == '/') {
			// chat command - from chat client
			try {
				processRESTChatMessage(msg);
			} catch (Exception e) {
				broadcast(String.format("sorry sir, I do not understand your command %s", e.getMessage()));
				Logging.logException(e);
			}
		} else if (body != null && body.length() > 0 && body.charAt(0) != '/') {
			broadcast("sorry sir, I do not understand! I await your orders but,\n they must start with / for more information go to http://myrobotlab.org");
			broadcast("*HAIL BEPSL!*");
			broadcast(String.format("for a list of possible commands please type /%s/help", getName()));
			broadcast(String.format("current roster of active units is as follows\n\n %s", listServices()));
			broadcast(String.format("you may query any unit for help *HAIL BEPSL!*"));
			// sendMessage(String.format("<b>hello</b>"),
			// "supertick@gmail.com");
		}

		// FIXME - decide if its a publishing point
		// or do we directly invoke and expect a response type
		invoke("publishMessage", chat, msg);
	}

	public boolean addAuditor(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not add auditor %s", id);
			return false;
		}
		String buddyJID = entry.getUser();
		auditors.add(buddyJID);
		return true;
	}

	public boolean removeAuditor(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not remove auditor %s", id);
			return false;
		}
		String buddyJID = entry.getUser();
		auditors.remove(buddyJID);
		return true;
	}

	public boolean addRelay(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not add relay %s", id);
			return false;
		}
		String buddyJID = entry.getUser();
		responseRelays.add(buddyJID);
		return true;
	}

	public boolean removeRelay(String id) {
		RosterEntry entry = getEntry(id);
		if (entry == null) {
			error("can not remove relay %s", id);
			return false;
		}
		String buddyJID = entry.getUser();
		responseRelays.remove(buddyJID);
		return true;
	}

	/**
	 * publishing point for XMPP messages
	 * 
	 * @param message
	 * @return
	 */
	public Message publishMessage(Chat chat, Message msg) {
		log.info(String.format("%s sent msg %s", msg.getFrom(), msg.getBody()));
		return msg;
	}

	/*
	 * public String getStatus() { StringBuffer sb = new StringBuffer();
	 * sb.append(chatManager.get); }
	 */

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {

			int i = 1;
			// Runtime.main(new String[]{"-runtimeName", String.format("r%d",
			// i)});
			XMPP xmpp1 = (XMPP) Runtime.createAndStart(String.format("xmpp%d", i), "XMPP");
			Runtime.createAndStart(String.format("clock%d", i), "Clock");
			Runtime.createAndStart(String.format("gui%d", i), "GUIService");
			xmpp1.connect("talk.google.com", 5222, "incubator@myrobotlab.org", "hatchMe!");
			xmpp1.sendMessage("hello from incubator by name " + System.currentTimeMillis(), "Greg Perry");
			xmpp1.sendMessage("xmpp 2", "robot02 02");
			if (true) {
				return;
			}

			// ---------------------------THE
			// END--------------------------------------------

			XMPP xmpp = new XMPP("xmpp");
			xmpp.startService();

			// xmpp.connect("talk.google.com", 5222, "orbous@myrobotlab.org",
			// "mrlRocks!");
			xmpp.connect("talk.google.com", 5222, "incubator@myrobotlab.org", "hatchMe!");
			xmpp.sendMessage("hello from incubator xmpp name", "Greg Perry");

			// xmpp.getUserList();

			/*
			 * incubator Number of contacts: 2 User: Orbous Mundus
			 * 34duqo9xzvxh20rm34ihnf2cln@public.talk.google.com User: Greg
			 * Perry 23d3ufvoz10m30jfv4adl5daav@public.talk.google.com
			 */

			// Roster roster = xmpp.getRoster();
			xmpp.sendMessage("hello from incubator by user", "23d3ufvoz10m30jfv4adl5daav@public.talk.google.com");
			xmpp.addRelay("Greg Perry");
			xmpp.sendMessage("message from the REAL INCUBATOR !!!", "Orbous Mundus");
			xmpp.sendMessage("/runtime/getUptime", "Orbous Mundus");
			xmpp.sendMessage("/runtime/getUptime", "Orbous Mundus");
			// xmpp.sendMessage("/runtime/getUptime",
			// "34duqo9xzvxh20rm34ihnf2cln@public.talk.google.com");

			// RosterEntry user =
			// roster.getEntry("34duqo9xzvxh20rm34ihnf2cln@public.talk.google.com");
			// xmpp.connect("talk.google.com", 5222, "robot02@myrobotlab.org",
			// "mrlRocks!");

			// gets all users it can send messages to
			xmpp.getRoster();
			xmpp.setStatus(true, String.format("online all the time - %s", new Date()));
			xmpp.sendMessage("hello", "23d3ufvoz10m30jfv4adl5daav@public.talk.google.com");

			// TODO - autoRespond
			// TODO - auditCommand <-- to which protocol?
			// xmpp.addRelay("grasshopperrocket@gmail.com");
			// orbous -> grasshopperrocket
			// 389iq8ajgim8w2xm2rb4ho5l0c@public.talk.google.com
			// FIXME addMsgListener - default gson encoded return message only
			xmpp.addRelay("23d3ufvoz10m30jfv4adl5daav@public.talk.google.com");

			// incubator -> supertick
			// (23d3ufvoz10m30jfv4adl5daav@public.talk.google.com)

			xmpp.addRelay("supertick@gmail.com");

			// send a message
			xmpp.broadcast("reporting for duty *SIR* !");
			xmpp.sendMessage("hail bepsl", "supertick@gmail.com");
			log.info("here");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
