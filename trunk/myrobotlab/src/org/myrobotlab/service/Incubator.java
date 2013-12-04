package org.myrobotlab.service;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;
import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.IndexNode;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class Incubator extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Incubator.class);
	
	transient public XMPP xmpp;
	transient public WebGUI webgui;
	Index<Object> cache = new Index<Object>();

	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		
		peers.put("xmpp", "XMPP", "XMPP service");
		peers.put("webgui", "WebGUI", "WebGUI service");
		
		// TODO better be whole dam tree ! - have to recurse based on Type !!!!
		/*
		peers.suggestAs("mouthControl.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		*/
				
		return peers;
	}
	
	
	public Incubator(String n) {
		super(n);	
		
		xmpp = (XMPP) createPeer("xmpp");
		webgui = (WebGUI) createPeer("webgui");
		webgui.httpPort = 4321;
		webgui.wsPort = 5432;
		
		subscribe(xmpp.getName(), "publishMessage");

	}
	
	@Override
	public void startService() {
		super.startService();
		xmpp.startService();
		webgui.startService();
	}
	
	public void publishMessage(Chat chat, Message msg)
	{
		// gson conversion?? - return from 
	}
	
	public IndexNode<Object> get(String robotName) {
		return cache.getNode(robotName);
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Incubator template = new Incubator("template");
		template.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
