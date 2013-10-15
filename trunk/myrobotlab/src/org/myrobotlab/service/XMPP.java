package org.myrobotlab.service;

import java.util.ArrayList;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class XMPP extends Service implements MessageListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(XMPP.class.getCanonicalName());
	static final int packetReplyTimeout = 500; // millis

	// XMPPConnection connection = new XMPPConnection("gmail.com");
	String user;
	String password;
	String server;
	// Integer port;

	ConnectionConfiguration config;
	XMPPConnection connection;
	ChatManager chatManager;
	MessageListener messageListener;

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

	public void sendMessage(String message, String buddyJID) {
		try {
			log.info(String.format("Sending mesage '%1$s' to user %2$s", message, buddyJID));
			Chat chat = chatManager.createChat(buddyJID, messageListener);
			chat.sendMessage(message);
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void createEntry(String user, String name) throws Exception {
		log.info(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}

	@Override
	public void processMessage(Chat chat, Message message) {

		String from = message.getFrom();
		String body = message.getBody();
		log.info(String.format("Received message '%1$s' from %2$s", body, from));
		invoke("publishMessage", message);
	}
	
	/**
	 * publishing point for XMPP messages
	 * @param message
	 * @return
	 */
	public Message publishMessage(Message message)
	{
		return message;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		XMPP xmpp = new XMPP("xmpp");
		xmpp.startService();
		xmpp.connect("gmail.com");
		xmpp.login("robot01@myrobotlab.org", "password");
		
		// gets all users it can send messages to
		xmpp.getRoster();

		xmpp.setStatus(true, "online all the time");

		// send a message
		xmpp.sendMessage("hello this is robot01 - the current heatbed temperature is 40 degrees celcius", "supertick@gmail.com");
		log.info("ere");

		// Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
