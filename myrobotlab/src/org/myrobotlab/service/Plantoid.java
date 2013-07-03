package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class Plantoid extends Service {

	private static final long serialVersionUID = 1L;
	
	transient public Servo leftX, rightX, leftY, rightY;
	transient public Arduino arduino;
	transient public OpenCV nIRCamera, trueColorCamera;
	
	//:192.168.0.25 
	
	public String leftXName = "leftX";
	public String rightXName = "rightX";
	public String leftYName = "leftY";
	public String rightYName = "rightY";
	public String arduinoName = "arduino";
	public String nIRCameraName = "nIRCamera";
	public String trueColorCameraName = "trueColorCamera";

	public final static Logger log = LoggerFactory.getLogger(Plantoid.class.getCanonicalName());
	
	public Plantoid(String n) {
		super(n, Plantoid.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Plantoid template = new Plantoid("template");
		template.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
