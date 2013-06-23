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


import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorControl;
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
	// makes "attaching" impossible
	// Servo servo10;

	// Motor identifiers
	// final public int M1 = 0;
	// final public int FORWARD = 0;

	/*
	 * TODO - if arduino has a last port defined - then auto-connect if the
	 * connect was unsuccessful - warn the user if it was successful (report
	 * yay) - query the version & type of INO if version & type match - report
	 * Yay proceed to attach all motors - (save config regarding last state of
	 * shieldd - dcmotors vs steppers)
	 */

	final public int FORWARD = 1;
	final public int BACKWARD = 2;
	final public int BRAKE = 3;
	final public int RELEASE = 4;

	final public int SINGLE = 1;
	final public int DOUBLE = 2;
	final public int INTERLEAVE = 3;
	final public int MICROSTEP = 4;

	// dc motorMap
	private Motor m1 = null;
	private Motor m2 = null;
	private Motor m3 = null;
	private Motor m4 = null;

	private Arduino myArduino = null;

	HashMap<String, Integer> motorMap = new HashMap<String, Integer>();
	Motor[] motors = new Motor[4];

	final int AF_DCMOTOR_SET_SPEED = 51;
	final int AF_DCMOTOR_RUN_COMMAND = 52;

	public transient final static Logger log = LoggerFactory.getLogger(AdafruitMotorShield.class.getCanonicalName());

	public AdafruitMotorShield(String n) {
		super(n, AdafruitMotorShield.class.getCanonicalName());
		myArduino = new Arduino(String.format("%s_arduino", n));
		createM1M2DCMotors();
		createM3M4DCMotors();
		attach();
	}

	public void startService() {
		super.startService();
		myArduino.startService();
		// TODO - request myArduino - re connect
	}

	// TODO - 2 calls as the business logic is 1/2 - 2 motorMap or 1 stepper
	public void createM1M2DCMotors() {
		m1 = new Motor(String.format("%s_%s", getName(), "m1"));
		m2 = new Motor(String.format("%s_%s", getName(), "m2"));
		motorMap.put(m1.getName(), 1);
		motorMap.put(m2.getName(), 2);
		motors[0] = m1;
		motors[1] = m2;
		m1.startService();
		m2.startService();
	}

	public void createM3M4DCMotors() {
		m3 = new Motor(String.format("%s_%s", getName(), "m3"));
		m4 = new Motor(String.format("%s_%s", getName(), "m4"));
		motorMap.put(m3.getName(), 3);
		motorMap.put(m4.getName(), 4);
		motors[2] = m3;
		motors[3] = m4;
		m3.startService();
		m4.startService();
	}

	public void releaseM1M2Motor() {
		motorMap.remove(m1);
		motorMap.remove(m2);
		m1.releaseService();
		m2.releaseService();
		motors[0] = null;
		motors[1] = null;
	}

	public void releaseM3M4Motor() {
		motorMap.remove(m3);
		motorMap.remove(m4);
		m3.releaseService();
		m4.releaseService();
		motors[2] = null;
		motors[3] = null;
	}

	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// DC Motors
	// ----------- AFMotor API Begin --------------
	public void setSpeed(String name, Integer speed) { // FIXME - sloppy
		setSpeed(motorMap.get(name) - 1, speed);
	}

	public void setSpeed(Integer motorPortNumber, Integer speed) {
		myArduino.sendMsg(AF_DCMOTOR_SET_SPEED, motorPortNumber - 1, speed);
	}

	public void run(Integer motorPortNumber, Integer command) {
		myArduino.sendMsg(AF_DCMOTOR_RUN_COMMAND, motorPortNumber - 1, command);
	}

	public void runForward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, FORWARD);
	}

	public void runBackward(Integer motorPortNumber, Integer speed) {
		setSpeed(motorPortNumber, speed);
		run(motorPortNumber, BACKWARD);
	}

	public void stop(Integer motorPortNumber) {
		// setSpeed(motorNumber, speed);
		run(motorPortNumber, RELEASE);
	}

	// Stepper Motors
	public void step(int count, int direction, int type) {

	}

	// ----------- AFMotor API End --------------

	@Override
	public String getToolTip() {
		return "Adafruit Motor Shield Service";
	}

	// public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "
	// TODO

	public static final String ADAFRUIT_DEFINES = "\n\n" + "#include <AFMotor.h>\n\n" + " #define AF_DCMOTOR_SET_SPEED 51\n" + " #define AF_DCMOTOR_RUN_COMMAND 52\n"
			+ " AF_DCMotor m1(1);\n" + " AF_DCMotor m2(2);\n" + " AF_DCMotor m3(3);\n" + " AF_DCMotor m4(4);\n" + " AF_DCMotor* motorMap[4];\n" + "\n\n";

	public static final String ADAFRUIT_SETUP = "\n\n" + "  motorMap[0] = &m1; \n" + "  motorMap[1] = &m2; \n" + "  motorMap[2] = &m3; \n" + "  motorMap[3] = &m4; \n";

	public static final String ADAFRUIT_CODE = "\n\n" +

	"            case AF_DCMOTOR_RUN_COMMAND: \n" + "             motorMap[ioCommand[2]]->run(ioCommand[3]); \n" + "            break; \n"
			+ "            case AF_DCMOTOR_SET_SPEED: \n" + "             motorMap[ioCommand[2]]->setSpeed(ioCommand[3]); \n" + "            break; \n";

	public boolean attach() {
		boolean ret = true;
		ret &= attach(myArduino); // TODO - check to see if Arduino is connected

		m1.setController(this);
		m2.setController(this);
		m3.setController(this);
		m4.setController(this);

		m1.broadcastState();

		return ret;
	}

	// attachControllerBoard ??? FIXME FIXME FIXME - should "attach" call
	// another's attach?
	/**
	 * an Arduino does not need to know about a shield but a shield must know
	 * about a Arduino Arduino owns the script, but a Shield needs additional
	 * support Shields are specific - but plug into a generalized Arduino
	 * Arduino shields can not be plugged into other uCs
	 * 
	 * TODO - Program Version & Type injection - with feedback + query to load
	 */
	public boolean attach(Arduino arduino) {

		if (arduino == null) {
			log.error("can't attach - arduino is invalid");
			return false;
		}

		myArduino = arduino;

		// arduinoName; FIXME - get clear on diction Program Script or Sketch
		StringBuffer newProgram = new StringBuffer();
		newProgram.append(myArduino.getSketch());

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
		myArduino.setSketch(newProgram.toString());
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

		// a bit weird indirection - but this would support
		// adafruit to be attached to motors defined outside of
		// initialization
		MotorControl mc = (MotorControl) Runtime.getServiceWrapper(name).get();
		Float pwr = mc.getPowerLevel();
		int pwm = (int) (pwr * 255);
		int motorPortNumber = motorMap.get(name);

		if (pwr > 0) {
			runForward(motorPortNumber, pwm);
		} else if (pwr < 0) {
			runBackward(motorPortNumber, -1 * pwm);
		} else {
			stop(motorPortNumber);
		}

	}

	@Override
	public boolean motorDetach(String data) {
		return false;

	}

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		ServiceWrapper sw = Runtime.getServiceWrapper(motorName);
		if (!sw.isLocal()) {
			log.error("motor needs to be in same instance of mrl as controller");
			return false;
		}

		Motor m = (Motor) sw.get();
		m.setController(this);
		m.broadcastState();
		return true;
	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAttached() {
		return myArduino != null;
	}

	// motor controller api

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		AdafruitMotorShield fruity = (AdafruitMotorShield) Runtime.createAndStart("fruity", "AdafruitMotorShield");
		fruity.attach();

		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("gui01", "GUIService");

	}

	@Override
	public Object[] getMotorData(String motorName) {
		String ret = String.format("m%d", motorMap.get(motorName));
		Object[] data = new Object[] { ret };
		return data;
	}

}
