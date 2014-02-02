package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.myrobotlab.framework.Index;
import org.myrobotlab.framework.IndexNode;
import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class Incubator extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Incubator.class);

	transient public XMPP xmpp;
	transient public WebGUI webgui;
	transient public Python python;

	Index<Object> cache = new Index<Object>();
	
	// TODO - take snapshot of threads - compare at
	// any other times - find the diff of threads - generated errors approprately


	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		peers.put("xmpp", "XMPP", "XMPP service");
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("python", "Python", "Python service");

		return peers;
	}

	public Incubator(String n) {
		super(n);

		xmpp = (XMPP) createPeer("xmpp");
		python = (Python) createPeer("python");
		webgui = (WebGUI) createPeer("webgui");
		webgui.port = 4321;

	}

	@Override
	public void startService() {
		super.startService();
		xmpp.startService();
		webgui.startService();

		xmpp.login("incubator@myrobotlab.org", "hatchMe!");

		xmpp.addAuditor("Greg Perry");
		python.startService();
	}

	static public class Error {
		public String name;
		public String type;
		public String description;

		public Error(String name, String type, Exception e) {
			this.name = name;
			this.type = type;
			this.description = Logging.stackToString(e);
		}

		public Error(String simpleType, String type) {
			this.name = simpleType;
			this.type = type;
		}
	}

	static public final ArrayList<Error> serializeTest() {
		
		String[] serviceTypeNames = Runtime.getInstance().getServiceSimpleNames();
		ArrayList<Error> badServices = new ArrayList<Error>();

		Runtime runtime = Runtime.getInstance();
		
		for (int i = 0; i < serviceTypeNames.length; ++i) {
			ServiceInterface s = null;
			ByteArrayOutputStream fos = null;
			ObjectOutputStream out = null;
			String simpleType = serviceTypeNames[i];
			
			try {
				if (simpleType.equals(Incubator.class.getSimpleName())){
					log.warn("skipping Incubator class");
					continue;
				}
				log.warn("starting " + simpleType);
				
				if (!runtime.isInstalled(String.format("org.myrobotlab.service.%s", simpleType)))
				{
					badServices.add(new Error(simpleType, "notInstalled"));
					continue;
				}
				
				s = Runtime.create(simpleType, simpleType);
				
				if (!Runtime.isHeadless() || (Runtime.isHeadless() && ! s.hasDisplay())){
					s.startService();					
				} else {
					log.warn(String.format("won't start %s - do not have a display", simpleType));
				}
				
			} catch (Exception e) {
				badServices.add(new Error(simpleType, "createAndStart", e));
			}

			try {
				log.warn("serializing " + simpleType);
				
				fos = new ByteArrayOutputStream();
				out = new ObjectOutputStream(fos);
				out.writeObject(s);
				fos.flush();
				out.close();

				log.info("releasing " + simpleType);

				if (s.hasPeers()){
					s.releasePeers();
				}
								
				s.releaseService();
				
				log.warn("released " + simpleType);
				log.warn("here");
			} catch (Exception e) {
				badServices.add(new Error(simpleType, "serailize", e));
			}

		}
		
		return badServices;
	}
	
	

	public void startTest() {
		ArrayList<Error> errors = serializeTest();
		log.info(String.format("found %d errors in serialization", errors.size()));
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
		
		Boolean b = Runtime.isHeadless();
		
		Incubator incubator = new Incubator("incubator");
		incubator.startService();
		incubator.startTest(); 

		
		//Runtime.createAndStart("gui", "GUIService");

	}

}
