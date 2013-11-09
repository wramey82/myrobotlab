package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class Incubator extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Incubator.class);
	
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		
		peers.suggestAs("mouthControl.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headArduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");		
		peers.suggestAs("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");	
		peers.suggestAs("opencv", "opencv", "OpenCV", "shared head OpenCV");	
		
		peers.put("mouthControl", "MouthControl", "MouthControl");	
		peers.put("opencv", "OpenCV", "shared OpenCV instance");
		peers.put("headTracking", "Tracking", "Head tracking system");
		peers.put("eyesTracking", "Tracking", "Tracking for the eyes");
		peers.put("jaw", "Servo", "Jaw servo");
		peers.put("eyeX", "Servo", "Eyes pan servo");
		peers.put("eyeY", "Servo", "Eyes tilt servo");
		peers.put("headX", "Servo", "Head pan servo");
		peers.put("headY", "Servo", "Head tilt servo");
		peers.put("headArduino", "Arduino", "Arduino controller for this arm");
		
		
		
		// TODO better be whole dam tree ! - have to recurse based on Type !!!!
		/*
		peers.suggestAs("mouthControl.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		*/
				
		return peers;
	}
	
	
	public Incubator(String n) {
		super(n, Incubator.class.getCanonicalName());	
	}
	
	@Override
	public void startService() {
		super.startService();
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
