package org.myrobotlab.service;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.leapmotion.LeapListener;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Gesture;

public class LeapMotion extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LeapMotion.class.getCanonicalName());
	//UNDO controller references when Leap Motion is ready to be released!
	//
	private Controller controller;
	private LeapListener listener;
	
	public LeapMotion(String n) {
		super(n, LeapMotion.class.getCanonicalName());	
		controller = new Controller();
		controller.enableGesture(Gesture.Type.TYPE_KEY_TAP);
		listener = new LeapListener(this);
		controller.addListener(listener);
		
	}

	@Override
	public String getToolTip() {
		return "Leap Motion service - publishes lots of finger position data";
	}
	
	// publish methods -------------------------
	public Float publishScreenX(Float value){
		return value;
	}
	
	public Float publishScreenY(Float value){
		return value;
	}
	
	public Float publishInvScreenX(Float value){
		return value;
	}
	
	public Float publishInvScreenY(Float value){
		return value;
	}
	
	public Float publishYaw1(Float value){
		return value;
	}
	
	public Float publishPitch1(Float value){
		return value;
	}
	public Float publishRoll1(Float value){
		return value;
	}
	
	public Float publishX1(Float value){
		return value;
	}
	public Float publishY1(Float value){
		return value;
	}
	
	public Float publishZ1(Float value){
		return value;
	}
	
	public Boolean keyTap(Boolean value){
		return value;
	}
    //end publish methods-----------------------

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

		LeapMotion leapmotion = new LeapMotion("leapmotion");
		leapmotion.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("runtime", "Runtime");
		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("java", "Java");
		Arduino arduino=new Arduino("arduino");
		arduino.startService();
		arduino.setSerialDevice("COM19",57600,8,1,0);
		//Runtime.createAndStart("servox","Servo");
		Servo servox=new Servo("servox");
		servox.startService();
		
		//Runtime.createAndStart("servoy","Servo");
		Servo servoy=new Servo("servoy");
		servoy.startService();
		
		
		arduino.servoAttach("servox",14);
		arduino.servoAttach("servoy",15);	

		OpenCV opencv=new OpenCV("opencv");
		opencv.startService();
		opencv.setCameraIndex(0);
		opencv.setInpurtSource(OpenCV.INPUT_SOURCE_CAMERA);
		//opencv.capture();
//		
		servox.subscribe("publishInvScreenX", leapmotion.getName(), "move", Float.class);
		servoy.subscribe("publishScreenY", leapmotion.getName(), "move", Float.class);
		
		
		Log log=new Log("log");
		log.startService();
		log.subscribe("keyTap",leapmotion.getName(),"log",Message.class);
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
