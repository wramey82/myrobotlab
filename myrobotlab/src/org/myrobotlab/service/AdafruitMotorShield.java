/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author GroG
 * 
 * References :
 * 	http://www.ladyada.net/make/mshield/use.html
 */

public class AdafruitMotorShield extends Service implements MotorController  {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// name of arduino service this shield is plugged into
	String arduinoName;
	
	// AF Shield controls these 2 servos
	// Servo servo9; - difficult idea - com port may not be initialized - which makes "attaching" impossible
	// Servo servo10;
	
	// vendor specific consts
	// Motor identifiers
	//final public int M1 = 0;
	//final public int FORWARD = 0;
	

	final public int BACKWARD = 0;
	final public int FORWARD = 0;
	
	// dc motors
	private Motor m1 = null;
	private Motor m2 = null;
	private Motor m3 = null;
	private Motor m4 = null;
	
	private Servo s1 = null;
	private Servo s2 = null;
	
	private Arduino myArduino = null;
	
	
	public final static Logger log = Logger.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public AdafruitMotorShield(String n) {
		super(n, AdafruitMotorShield.class.getCanonicalName());
		//servo9 = (Servo)Runtime.createAndStart("servo9", "Servo");
		//servo10 = (Servo)Runtime.createAndStart("servo10", "Servo");
		myArduino = new Arduino(String.format("%s_arduino", n));		
//		createDCMotors();
//		s1 = new Servo(String.format("%s_servo1", n));	
//		s2 = new Servo(String.format("%s_servo2", n));	
	}

	@Override
	public void loadDefaultConfiguration() {

	}
	
	public void startService() {
		super.startService();
		myArduino.startService();
//		startDCMotorServices();
//		s1.attach(myArduino.getName(), 9);
//		s2.attach(myArduino.getName(), 10);
	}
	
	// MOTOR SHIELD INTERFACE BEGIN ////////////
	// TODO - figure if the way framegrabber initilization with data stream properties is appropriate of 
	// Motors
	/*
	@Override
	public Motor createMotor(String data) {
	    Properties properties = new Properties();
	    try {
			properties.load(new StringReader(data));
			String name = properties.getProperty("name");
			String powerPin = properties.getProperty("powerPin");
			String directionPin = properties.getProperty("directionPin");
			
		} catch (IOException e) {
			Service.logException(e);
		}
		return null;
	}
	*/
	
	// TODO - 2 calls as the business logic is 1/2 - 2 motors or 1 stepper
	public void createDCMotors()
	{
		m1 = new Motor(String.format("%s_%s",getName(), "m1"));
		m2 = new Motor(String.format("%s_%s",getName(), "m2"));
		m3 = new Motor(String.format("%s_%s",getName(), "m3"));
		m4 = new Motor(String.format("%s_%s",getName(), "m4"));
	}
	
	public void startDCMotorServices()
	{
		m1.startService();
		m2.startService();
		m3.startService();
		m4.startService();
	}
	
	public void releaseMotor()
	{
		m1.releaseService();
		m2.releaseService();
		m3.releaseService();
		m4.releaseService();
	}
	
	// MOTOR SHIELD INTERFACE END ////////////
	
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
	// ----------- AFMotor API Begin --------------
	public void setSpeed(Integer speed)
	{
		
	}
	public void run(Integer command)
	{
		
	}
	// Stepper Motors
	public void step(int count, int direction, int type)
	{
		
	}
	// ----------- AFMotor API End --------------
	
	@Override
	public String getToolTip() {
		return "Adafruit Motor Shield Service";
	}
	
	// attachControllerBoard ???
	public boolean attach(String arduinoName) {
		
		if (Runtime.getServiceWrapper(arduinoName) != null)
		{
			this.arduinoName = arduinoName;
			broadcastState(); // state has changed let everyone know
		} 
		
		//servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		//servo10.attach(arduinoName, 10);
		
		//log.error(String.format("couldn't find %s", arduinoName));
		return false;
	}

	/*
	// Begin Motor Controller Interface
	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMove(String name, Integer amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void releaseMotor(String data) {
		// send dispose
		// release service
		
	}
	*/
	public static void main(String[] args)  {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		/*
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		SensorMonitor sensors = new SensorMonitor("sensors");
		sensors.startService();
		*/
		AdafruitMotorShield adafruit = new AdafruitMotorShield("adafruit");
		adafruit.startService();
		
		//adafruit.attach(arduino.getName());
		/*
		*/
		/*
		 * //Runtime.createAndStart("sensors", "SensorMonitor");
		 * 
		 * String code = FileIO.getResourceFile("Arduino/MRLComm.ino"); //String
		 * code = FileIO.fileToString(
		 * ".\\arduino\\libraries\\MyRobotLab\\examples\\MRLComm\\MRLComm.ino");
		 * 
		 * arduino.compile("MRLComm", code); arduino.setPort("COM7"); //- test
		 * re-entrant arduino.upload();
		 */
		// FIXME - I BELIEVE THIS LEAVES THE SERIAL PORT IN A CLOSED STATE !!!!

		// arduino.compileAndUploadSketch(".\\arduino\\libraries\\MyRobotLab\\examples\\MRLComm\\MRLComm.ino");
		// arduino.pinMode(44, Arduino.OUTPUT);
		// arduino.digitalWrite(44, Arduino.HIGH);

		Runtime.createAndStart("gui01", "GUIService");
		//Runtime.createAndStart("jython", "Jython");

	}


	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMove(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorDetach(String data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean motorAttach(MotorControl motor, Object... motorData) {
		// TODO Auto-generated method stub
		return false;
		
	}

	@Override
	public ArrayList<String> getMotorAttachData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getMotorValidAttachValues(String attachParameterName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		// TODO Auto-generated method stub
		return false;
	}
	
	// motor controller api



}
