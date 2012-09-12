/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.service.interfaces.Motor;
import org.myrobotlab.service.interfaces.MotorController;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author greg
 * 
 */
public class AdafruitMotorShield extends Service implements MotorController {
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
	
	private HashMap<String, Motor> motors = new HashMap<String, Motor>();

	final public int BACKWARD = 0;
	final public int FORWARD = 0;
	
	
	public final static Logger log = Logger.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public AdafruitMotorShield(String n) {
		super(n, AdafruitMotorShield.class.getCanonicalName());
		//servo9 = (Servo)Runtime.createAndStart("servo9", "Servo");
		//servo10 = (Servo)Runtime.createAndStart("servo10", "Servo");
	}

	@Override
	public void loadDefaultConfiguration() {

	}
	
	// MOTOR SHIELD INTERFACE BEGIN ////////////
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
	
	public void releaseMotor()
	{
		
	}
	
	public void attachMotor(String motorName)
	{
		
	}
	// MOTOR SHIELD INTERFACE END ////////////
	
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
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
	// VENDOR SPECIFIC LIBRARY METHODS END /////
	
	@Override
	public String getToolTip() {
		return "Adafruit Motor Shield Service";
	}

	HashMap<String, Servo> servos = new HashMap<String, Servo>();
	
	// attachControllerBoard ???
	public boolean attach(String arduinoName) {
		
		if (Runtime.getService(arduinoName) != null)
		{
			this.arduinoName = arduinoName;
			broadcastState(); // state has changed let everyone know
		} 
		
		//servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		//servo10.attach(arduinoName, 10);
		
		//log.error(String.format("couldn't find %s", arduinoName));
		return false;
	}

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
	
	public static void main(String[] args)  {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		SensorMonitor sensors = new SensorMonitor("sensors");
		sensors.startService();
		/*
		AdafruitMotorShield adafruit = new AdafruitMotorShield("adafruit");
		adafruit.startService();
		
		adafruit.attach(arduino.getName());
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



}
