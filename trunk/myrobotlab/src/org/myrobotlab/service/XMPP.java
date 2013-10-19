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
import org.slf4j.Logger;

public class XMPP extends Service implements MessageListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(XMPP.class.getCanonicalName());
	static final int packetReplyTimeout = 500; // millis

	String user;
	String password;
	String server;
	// Integer port;

	ConnectionConfiguration config;
	XMPPConnection connection;
	ChatManager chatManager;
	MessageListener messageListener;

	HashMap<String, Chat> currentChats = new HashMap<String, Chat>();
	HashSet<String> relays = new HashSet<String>();
	HashSet<String> allowCommandsFrom = new HashSet<String>();

	public XMPP(String n) {
		super(n, XMPP.class.getCanonicalName());
	}

	@Override
	public void stopService() {
		super.stopService();
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
		}
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public ArrayList<String> getRoster() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			/*
			 * // Connect connection.connect();
			 * 
			 * // Login with appropriate credentials connection.login(user,
			 * password);
			 */

			// Get the user's roster
			Roster roster = connection.getRoster();

			// Print the number of contacts
			log.info("Number of contacts: " + roster.getEntryCount());

			// Enumerate all contacts in the user's roster
			for (RosterEntry entry : roster.getEntries()) {
				log.info("User: " + entry.getUser());
				list.add(entry.getUser());
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		return list;
	}

	public void connect(String server) {

		try {
			log.info(String.format("Initializing connection to server %s", server));

			SASLAuthentication.supportSASLMechanism("PLAIN");
			// WTF is a service name ?
			ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
			connection = new XMPPConnection(config);

			/*
			 * SmackConfiguration.setPacketReplyTimeout(packetReplyTimeout); if
			 * (port == null) { port = 5269; }
			 */
			// config = new ConnectionConfiguration(server, port);

			// config = new ConnectionConfiguration("talk.google.com", 5222,
			// "gmail.com");
			// config.setSASLAuthenticationEnabled(false);
			// config.setSecurityMode(SecurityMode.disabled);
			// connection = new XMPPConnection(config);

			// this.server = server;
			// connection = new XMPPConnection(server);
			connection.connect();
			log.info("Connected: " + connection.isConnected());
			chatManager = connection.getChatManager();
			messageListener = this;
		} catch (Exception e) {
			Logging.logException(e);
		}

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
			return false;
		}
	}

	public void setStatus(boolean available, String status) {

		Presence.Type type = available ? Type.available : Type.unavailable;
		Presence presence = new Presence(type);
		presence.setStatus(status);
		connection.sendPacket(presence);

	}

	public void sendMyRobotLabJSONMessage(org.myrobotlab.framework.Message msg) {

	}

	public void sendMyRobotLabRESTMessage(org.myrobotlab.framework.Message msg) {

	}

	public org.myrobotlab.framework.Message processMyRobotLabRESTMessage(Message msg) {

		return null;
	}

	public void sendMessage(String text, String buddyJID) {
		try {
			Chat chat = chatManager.createChat(buddyJID, messageListener);
			chat.sendMessage(text);
			/*
			log.info(String.format("Sending mesage '%s' to user %s", text, buddyJID));
			if (currentChats.containsKey(buddyJID)) {
				currentChats.get(buddyJID).sendMessage(text);
			} else {
				Chat chat = chatManager.createChat(buddyJID, messageListener);
				chat.sendMessage(text);
				currentChats.put(buddyJID, chat);
			}
			*/
		} catch (Exception e) {
			//currentChats.remove(buddyJID);
			Logging.logException(e);
		}
	}

	public void createEntry(String user, String name) throws Exception {
		log.info(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}

	public Object processRESTChatMessage(Message msg) {
		String body = msg.getBody();
		log.debug(String.format("processRESTChatMessage [%s]", body));
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
		
		log.info(String.format("[%s]",uri));
		Object o = RESTProcessor.invoke(uri);
		
		// FIXME - encoding is that input uri before call ?
		// or config ?
		// FIXME - echo
		if (o != null){
			sendMessage(o.toString(), "supertick@gmail.com");
		} else {
			sendMessage(null, "supertick@gmail.com");
		}
		return o;
	}
	
	// FIXME - should be in 
	public String listCommands()
	{
		return null;
	}
	
	// FIXME - should be in Service
	public String listMethods()
	{
		return null;
	}
	
	public String listServices()
	{
		StringBuffer sb = new StringBuffer();
		List<ServiceWrapper> services = Runtime.getServices();
		for (int i = 0; i < services.size(); ++i)
		{
			ServiceWrapper sw = services.get(i);
			sb.append(String.format("/%s\n", sw.name));
		}
		return sb.toString();
	}

	@Override
	public void processMessage(Chat chat, Message msg) {

		Message.Type type =  msg.getType();
		String from = msg.getFrom();
		String body = msg.getBody();
		log.info(String.format("Received %s message [%s] from [%s]", type, body, from));
		if (body != null && body.length() > 0 && body.charAt(0) == '/') // FIXME && configuration && from on list
		{
			processRESTChatMessage(msg);
		} else if (body != null && body.length() > 0 && body.charAt(0) != '/') {
			sendMessage("sorry sir, I do not understand! I await your orders but,\n they must start with / for more information go to http://myrobotlab.org", "supertick@gmail.com");
			sendMessage("*HAIL BEPSL!*", "supertick@gmail.com");
			sendMessage(String.format("for a list of possible commands please type /%s/help", getName()), "supertick@gmail.com");
			sendMessage(String.format("current roster of active units is as follows\n\n %s", listServices()), "supertick@gmail.com");
			sendMessage(String.format("you may query any unit for help *HAIL BEPSL!*"), "supertick@gmail.com");
			//sendMessage(String.format("<b>hello</b>"), "supertick@gmail.com");
		}
		
		invoke("publishMessage", msg);
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

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		try {
			XMPP xmpp = new XMPP("xmpp");
			xmpp.startService();

			/*
			Message msg = new Message();
			msg.setBody("/runtime/getUptime");
			msg.setFrom("supertick@gmail.com");
			xmpp.processRESTChatMessage(msg);
			*/

			xmpp.connect("gmail.com");
			xmpp.login("robot02@myrobotlab.org", "mrlRocks!");

			// gets all users it can send messages to
			xmpp.getRoster();

			xmpp.setStatus(true, String.format("online all the time - %s", new Date()));

			// send a message
			xmpp.sendMessage("/name/method/params", "supertick@gmail.com");
			xmpp.sendMessage("/name/method/return", "supertick@gmail.com");
			log.info("ere");

			// Runtime.createAndStart("gui", "GUIService");
			/*
			 * GUIService gui = new GUIService("gui"); gui.startService();
			 * gui.display();
			 */

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
