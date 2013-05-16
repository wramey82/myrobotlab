package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.leapmotion.LeapListener2;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class LeapMotion2 extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion2.class.getCanonicalName());
	
	//private Controller controller;
	//private Listener listener;
	
	private LeapListener2 listener;
	
	public LeapMotion2(String n) {
		super(n, LeapMotion2.class.getCanonicalName());	
		//controller = new Controller();
		//LeapListener listener = new LeapListener(this);
		//controller.addListener(listener);
		listener = new LeapListener2(this);
		/*
		 *  TODO .... do other initialization here to get leapListener working
		 */
	}
	
	// TODO
	// add other methods to control the listener here from the service..
	// that way other services can control it...
	// switch from integers to floats .. whatever ..
	// switch to publish only on different values .. etc..
	public void setFloatOutput()
	{
		//listener.setFloatOutput();
	}
	
	public void test()
	{
		listener.onFrame("new test!");
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public Float publishX(Float value){
		log.info("******** a new X has been published value {} ********", value);
		return value;
	}
	
	public Float publishY(Float value){
		log.info("******** a new Y has been published value {} ********", value);
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

		LeapMotion2 leapmotion = new LeapMotion2("leapmotion");
		leapmotion.startService();			
		
		leapmotion.test();
		
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("runtime", "Runtime");
		Runtime.createAndStart("python", "Python");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
