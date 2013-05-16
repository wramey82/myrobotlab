package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Listener;


public class LeapMotion extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion.class.getCanonicalName());
	
	private Controller controller;
	private Listener listener;
	
	public LeapMotion(String n) {
		super(n, LeapMotion.class.getCanonicalName());	
		controller = new Controller();
		LeapListener listener = new LeapListener(this);
		controller.addListener(listener);
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public Float publishX(Float value){
		return value;
	}
	
	public Float publishY(Float value){
		return value;
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
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		LeapMotion leapmotion = new LeapMotion("leapmotion");
		leapmotion.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("runtime", "Runtime");
		Runtime.createAndStart("python", "Python");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
