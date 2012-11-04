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
	
	public static final String ADAFRUIT_DEFINES = "\n\n #include <AFMotor.h>\n"
			+" \n"
			+" AF_DCMotor m1(1);\n"
			+" AF_DCMotor m2(2);\n"
			+" AF_DCMotor m3(3);\n"
			+" AF_DCMotor m4(4);\n"
			+" AF_DCMotor* motors[4];\n"
			+" \n";
	
	public boolean attach()
	{
		return attach(myArduino.getName(), (Object[])null);
	}
	
	
	// attachControllerBoard ???
	public boolean attach(String arduinoName, Object...data) { // FIXME <- other way around attach shield to the Arduino !!!
		
		if (Runtime.getServiceWrapper(arduinoName) != null)
		{
			//arduinoName;
			// TODO - check if local instance etc
			StringBuffer newProgram = new StringBuffer();
			newProgram.append(myArduino.getProgram());
			
			int insertPoint =  newProgram.indexOf(Arduino.VENDOR_DEFINES_BEGIN); // TODO public define ???
			
			if (insertPoint > 0)
			{
				newProgram.insert(Arduino.VENDOR_DEFINES_BEGIN.length() + insertPoint, ADAFRUIT_DEFINES);
			} else {
				log.error("could not find insert point in MRLComm.ino");
				// get info back to user
			}
			
			insertPoint =  newProgram.indexOf(Arduino.VENDOR_DEFINES_BEGIN); // TODO public define ???
			
			
			// modify program
			// set the program
			// broadcast the arduino state - ArduinoGUI should subscribe to setProgram
			myArduino.setProgram(newProgram.toString());
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
	

	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMove(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean motorDetach(String data) {
		return false;
		
	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	// motor controller api

	public static void main(String[] args)  {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
	
		AdafruitMotorShield fruity = (AdafruitMotorShield)Runtime.createAndStart("fruity", "AdafruitMotorShield");
		fruity.attach();
		
		Runtime.createAndStart("gui01", "GUIService");
		
		//Runtime.createAndStart("jython", "Jython");

	}


}
