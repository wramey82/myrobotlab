package org.myrobotlab.service;

import java.util.ArrayList;
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
import org.myrobotlab.framework.Service;
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

	String user;
	String password;
	String host;
	int port = 5222;
	String service = "gmail.com"; // defaulted :P

	ConnectionConfiguration config;
	XMPPConnection connection;
	ChatManager chatManager;

	HashSet<String> responseRelays = new HashSet<String>();
	/**
	 * auditors chat buddies who can see what commands are being processed
	 * and by who through the XMPP service
	 * TODO - audit full system ??? regardless of message origin?
	 */
	HashSet<String> auditors = new HashSet<String>();
	
	HashSet<String> allowCommandsFrom = new HashSet<String>();
	HashMap<String,Chat> chats = new HashMap<String,Chat>();


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

	public ArrayList<String> getRoster() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			connect();

			// Get the user's roster
			Roster roster = connection.getRoster();
			

			// Print the number of contacts
			log.info("Number of contacts: " + roster.getEntryCount());

			
			
			// Enumerate all contacts in the user's roster
			for (RosterEntry entry : roster.getEntries()) {
				log.info("User: " + entry.getUser());
				/*
				if (!entry.getUser().equalsIgnoreCase("supertick@gmail.com"))
				{
					roster.removeEntry(entry);
				}
				*/
				list.add(entry.getUser());
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		return list;
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
	/**
	 * 
	 */
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


	synchronized public void sendMessage(String text, String buddyJID) {
		try {

			connect();
			// FIXME FIXME FIXME !!! - if
			// "just connected - ie just connected and this is the first chat of the connection then "create
			// chat" otherwise use existing chat !"
			Chat chat = null;
			if (chats.containsKey(buddyJID))
			{
				chat = chats.get(buddyJID);
			} else {
				chat = chatManager.createChat(buddyJID, this);
				chats.put(buddyJID, chat);
			}
			
			if (text == null)
			{
				text = "null"; // dangerous converson?
			}
			log.info(String.format("sending %s %s", buddyJID, text));
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
		
		if (auditors.size() > 0)
		{
			for (String auditor : auditors) {
				sendMessage(String.format("%s %s", msg.getFrom(), msg.getBody()), auditor);
			}
		}
		
		if (body == null || body.length() < 1) {
			log.info("invalid");
			return null;
		}

		// TODO - allow to be in middle of message
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
		Object o = RESTProcessor.invoke(uri);

		// FIXME - encoding is that input uri before call ?
		// or config ?
		// FIXME - echo

		if (o != null) {
			broadcast(o.toString());
		} else {
			broadcast(null);
		}

		return o;
	}

	// FIXME - should be in
	public String listCommands() {
		return null;
	}

	// FIXME - should be in Service
	public String listMethods() {
		return null;
	}

	public String listServices() {
		StringBuffer sb = new StringBuffer();
		List<ServiceWrapper> services = Runtime.getServices();
		for (int i = 0; i < services.size(); ++i) {
			ServiceWrapper sw = services.get(i);
			sb.append(String.format("/%s\n", sw.name));
		}
		return sb.toString();
	}	

	// FIXME - clean
	@Override
	public void processMessage(Chat chat, Message msg) {

		Message.Type type = msg.getType();
		String from = msg.getFrom();
		String body = msg.getBody();
		if (type.equals(Message.Type.error))
		{
			log.error("{} processMessage returned error {}", from, body);
			return;
		}
		log.info(String.format("Received %s message [%s] from [%s]", type, body, from));
		if (body != null && body.length() > 0 && body.charAt(0) == '/') {
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

		invoke("publishMessage", msg);
	}

	public boolean addXMPPListener(String buddyJID) {
		return responseRelays.add(buddyJID);
	}

	public boolean removeRelay(String buddyJID) {
		return responseRelays.remove(buddyJID);
	}

	/**
	 * publishing point for XMPP messages
	 * 
	 * @param message
	 * @return
	 */
	public Message publishMessage(Message message) {
		return message;
	}

	/*
	 * public String getStatus() { StringBuffer sb = new StringBuffer();
	 * sb.append(chatManager.get); }
	 */

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			XMPP xmpp = new XMPP("xmpp");
			xmpp.startService();

			xmpp.connect("talk.google.com", 5222, "robot01@myrobotlab.org", "mrlRocks!");

			// gets all users it can send messages to
			xmpp.getRoster();
			xmpp.setStatus(true, String.format("online all the time - %s", new Date()));
			
			// TODO - autoRespond
			// TODO - auditCommand <-- to which protocol?
			//xmpp.addRelay("grasshopperrocket@gmail.com");
			xmpp.addXMPPListener("389iq8ajgim8w2xm2rb4ho5l0c@public.talk.google.com");
			
			xmpp.addXMPPListener("supertick@gmail.com");

			// send a message
			xmpp.broadcast("reporting for duty *SIR* !");
			xmpp.sendMessage("hail bepsl", "supertick@gmail.com");
			log.info("here");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
