/*
 * 
 *   AdafruitMotorShield
 *   
 *   TODO - test with Steppers & Motors - switches on board - interface accepts motor control
 *
 */

package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorController;

/**
 * AdaFruit Motor Shield Controller Service
 * 
 * @author GroG
 * 
 *         References : http://www.ladyada.net/make/mshield/use.html
 */

public class AdafruitMotorShield extends Service implements MotorController, ArduinoShield {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// AF Shield controls these 2 servos
	// Servo servo9; - difficult idea - com port may not be initialized - which
	// makes "attaching" impossible
	// Servo servo10;

	// vendor specific consts
	// Motor identifiers
	// final public int M1 = 0;
	// final public int FORWARD = 0;

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

	HashMap<String, Motor> motors = new HashMap<String, Motor>();

	public final static Logger log = Logger.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public AdafruitMotorShield(String n) {
		super(n, AdafruitMotorShield.class.getCanonicalName());
		// servo9 = (Servo)Runtime.createAndStart("servo9", "Servo");
		// servo10 = (Servo)Runtime.createAndStart("servo10", "Servo");
		myArduino = new Arduino(String.format("%s_arduino", n));
		// createDCMotors();
		// s1 = new Servo(String.format("%s_servo1", n));
		// s2 = new Servo(String.format("%s_servo2", n));
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	public void startService() {
		super.startService();
		myArduino.startService();
		createM1M2DCMotors();
		createM3M4DCMotors();
		// startDCMotorServices();
		// s1.attach(myArduino.getName(), 9);
		// s2.attach(myArduino.getName(), 10);
	}

	// MOTOR SHIELD INTERFACE BEGIN ////////////
	// TODO - figure if the way framegrabber initilization with data stream
	// properties is appropriate of
	// Motors
	/*
	 * @Override public Motor createMotor(String data) { Properties properties =
	 * new Properties(); try { properties.load(new StringReader(data)); String
	 * name = properties.getProperty("name"); String powerPin =
	 * properties.getProperty("powerPin"); String directionPin =
	 * properties.getProperty("directionPin");
	 * 
	 * } catch (IOException e) { Service.logException(e); } return null; }
	 */

	// TODO - 2 calls as the business logic is 1/2 - 2 motors or 1 stepper
	public void createM1M2DCMotors() {
		m1 = new Motor(String.format("%s_%s", getName(), "m1"));
		m2 = new Motor(String.format("%s_%s", getName(), "m2"));
		motors.put(m1.getName(), m1);
		motors.put(m2.getName(), m2);
		m1.startService();
		m2.startService();
	}

	public void createM3M4DCMotors() {
		m3 = new Motor(String.format("%s_%s", getName(), "m3"));
		m4 = new Motor(String.format("%s_%s", getName(), "m4"));
		motors.put(m3.getName(), m3);
		motors.put(m4.getName(), m4);
		m3.startService();
		m4.startService();
	}

	public void releaseM1M2Motor() {
		motors.remove(m1);
		motors.remove(m2);
		m1.releaseService();
		m2.releaseService();
	}
	public void releaseM3M4Motor() {
		motors.remove(m3);
		motors.remove(m4);
		m3.releaseService();
		m4.releaseService();
	}

	// MOTOR SHIELD INTERFACE END ////////////

	final int AF_DCMOTOR_SET_SPEED = 51;
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
	// ----------- AFMotor API Begin --------------
	public void setSpeed(String name, Integer speed) { // FIXME - sloppy
		Motor m = motors.get(name);
		if (m == m1)
		{
			setSpeed(0, speed);
		} else if (m == m2) {
			setSpeed(1, speed);			
		}  else if (m == m3) {
			setSpeed(2, speed);			
		}else if (m == m4) {
			setSpeed(3, speed);			
		}
	}
	
	public void setSpeed(Integer motorIndex, Integer speed)
	{
		myArduino.serialSend(AF_DCMOTOR_SET_SPEED, motorIndex, speed);
	}

	public void run(Integer motorIndex, Integer command) {
		myArduino.serialSend(AF_DCMOTOR_SET_SPEED, motorIndex, command);
	}

	// Stepper Motors
	public void step(int count, int direction, int type) {

	}

	// ----------- AFMotor API End --------------

	@Override
	public String getToolTip() {
		return "Adafruit Motor Shield Service";
	}
	
	//public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "  TODO

	public static final String ADAFRUIT_DEFINES = "\n\n"+
			"#include <AFMotor.h>\n\n" + 
			" #define AF_DCMOTOR_RUN_COMMAND 51\n" + 
			" #define AF_DCMOTOR_SET_SPEED 52\n" + 
			" AF_DCMotor m1(1);\n" + 
			" AF_DCMotor m2(2);\n" + 
			" AF_DCMotor m3(3);\n" + 
			" AF_DCMotor m4(4);\n" + 
			" AF_DCMotor* motors[4];\n" + 
			"\n\n";

	public static final String ADAFRUIT_SETUP = "\n\n" +
			"  motors[0] = &m1; \n" +
			"  motors[1] = &m2; \n" +
			"  motors[2] = &m3; \n" +
			"  motors[3] = &m4; \n" 
	;
	
	public static final String ADAFRUIT_CODE = "\n\n" +
			
			"            case AF_DCMOTOR_RUN_COMMAND: \n" +
			"             motors[ioCommand[2]]->run(ioCommand[3]); \n" +
			"            break; \n" +
			"            case AF_DCMOTOR_SET_SPEED: \n" +
			"             motors[ioCommand[2]]->setSpeed(ioCommand[3]); \n" +
			"            break; \n" 
	;

	public boolean attach() {
		return attach(myArduino);
	}

	// attachControllerBoard ???    FIXME FIXME FIXME - should  "attach" call another's attach?
	/**
	 *  an Arduino does not need to know about a shield but a shield must know about a Arduino
	 *  Arduino owns the script, but a Shield needs additional support
	 *  Shields are specific - but plug into a generalized Arduino
	 *  Arduino shields can not be plugged into other uCs
	 *  
	 *  TODO - Program Version & Type injection - with feedback + query to load
	 */
	public boolean attach(Arduino arduino) {

		if (arduino == null) {
			log.error("can't attach - arduino is invalid");
			return false;
		}
		
		myArduino = arduino;

		// arduinoName; FIXME - get clear on diction Program Script or Sketch
		StringBuffer newProgram = new StringBuffer();
		newProgram.append(myArduino.getProgram());

		// modify the program
		int insertPoint = newProgram.indexOf(Arduino.VENDOR_DEFINES_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_DEFINES_BEGIN.length() + insertPoint, ADAFRUIT_DEFINES);
		} else {
			log.error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}

		insertPoint = newProgram.indexOf(Arduino.VENDOR_SETUP_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_SETUP_BEGIN.length() + insertPoint, ADAFRUIT_SETUP);
		} else {
			log.error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}

		insertPoint = newProgram.indexOf(Arduino.VENDOR_CODE_BEGIN);

		if (insertPoint > 0) {
			newProgram.insert(Arduino.VENDOR_CODE_BEGIN.length() + insertPoint, ADAFRUIT_CODE);
		} else {
			log.error("could not find insert point in MRLComm.ino");
			// get info back to user
			return false;
		}

		// set the program
		myArduino.setProgram(newProgram.toString());
		// broadcast the arduino state - ArduinoGUI should subscribe to
		// setProgram
		broadcastState(); // state has changed let everyone know

		// servo9.attach(arduinoName, 9); // FIXME ??? - createServo(Integer i)
		// servo10.attach(arduinoName, 10);

		// log.error(String.format("couldn't find %s", arduinoName));
		return true;
	}

	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	@Override
	public void motorMove(String name) {
/*
		MotorData md = motors.get(name);
		MotorControl m = md.motor;
		float power = m.getPowerLevel();

		if (power < 0) {
			serialSend(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted() ? MOTOR_FORWARD : MOTOR_BACKWARD);
			serialSend(ANALOG_WRITE, md.PWMPin, Math.abs((int) (255 * m.getPowerLevel())));
		} else if (power > 0) {
			serialSend(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted() ? MOTOR_BACKWARD : MOTOR_FORWARD);
			serialSend(ANALOG_WRITE, md.PWMPin, (int) (255 * m.getPowerLevel()));
		} else {
			serialSend(ANALOG_WRITE, md.PWMPin, 0);
		}
*/		

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

	public boolean isAttached()
	{
		return myArduino != null;
	}
	// motor controller api

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		AdafruitMotorShield fruity = (AdafruitMotorShield) Runtime.createAndStart("fruity", "AdafruitMotorShield");
		fruity.attach();

		Runtime.createAndStart("gui01", "GUIService");

		// Runtime.createAndStart("jython", "Jython");

	}

}
