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

public class Adafruit16CServoDriver extends Service implements ArduinoShield {
	/** version of the library */
	static public final String VERSION = "0.9";

	private static final long serialVersionUID = 1L;

	// Depending on your servo make, the pulse width min and max may vary, you 
	// want these to be as small/large as possible without hitting the hard stop
	// for max range. You'll have to tweak them as necessary to match the servos you
	// have!
	public final static int SERVOMIN = 150; // this is the 'minimum' pulse length count (out of 4096)
	public final static int SERVOMAX = 600; // this is the 'maximum' pulse length count (out of 4096)


	transient public Arduino myArduino = null;

	HashMap<String, Integer> servoMap = new HashMap<String, Integer>();
	Motor[] motors = new Motor[4];

	public final int AF_BEGIN = 50;
	public final int AF_SET_PWM_FREQ = 51;
	public final int AF_DRIVE_PWM = 52;
	public final int AF_SET_SERVO_NUM = 53;
	

	public transient final static Logger log = LoggerFactory.getLogger(Adafruit16CServoDriver.class.getCanonicalName());

	public Adafruit16CServoDriver(String n) {
		super(n, Adafruit16CServoDriver.class.getCanonicalName());
	}

	public void startService() {
		super.startService();
		myArduino = new Arduino(String.format("%s_arduino", getName()));
		attach();
		myArduino.startService();
		// TODO - request myArduino - re connect
	}

	
	// VENDOR SPECIFIC LIBRARY METHODS BEGIN /////
	// ----------- AF16C API Begin --------------
	public void begin() { 
		myArduino.serialSend(AF_BEGIN, 0, 0);
	}

	public void setPWMFreq(Integer hz) { // Analog servos run at ~60 Hz updates
		myArduino.serialSend(AF_SET_PWM_FREQ, hz, 0);
	}

	
	/**
	 * set servo sets the active servo number
	 */
	public void setServoNum(Integer servoNum)
	{
		myArduino.serialSend(AF_SET_SERVO_NUM, servoNum, 0);
	}
	
	// drive the servo
	public void setPWM(Integer servoNum, Integer pulseWidthOn, Integer pulseWidthOff) {
		setServoNum(servoNum);
		myArduino.serialSend(AF_DRIVE_PWM, pulseWidthOn, pulseWidthOff);
	}

	// ----------- AFMotor API End --------------

	@Override
	public String getToolTip() {
		return "Adafruit Motor Shield Service";
	}

	// public static final String ADAFRUIT_SCRIPT_TYPE = "#define SCRIPT_TYPE "
	// TODO

	public static final String ADAFRUIT_DEFINES =  
			"\n#define AF_DRIVE_PWM 52\n" +
					"#define AF_SET_SERVO_NUM 53\n" +
					"#define SERVOMIN  150 // this is the 'minimum' pulse length count (out of 4096)\n" +
					"#define SERVOMAX  600 // this is the 'maximum' pulse length count (out of 4096)\n\n" +
			"\n\n#include <Wire.h>\n" +
	"#include <Adafruit_PWMServoDriver.h>\n" +
	"// called this way, it uses the default address 0x40\n" +
	"Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver();\n" +

	"int servoNum = 0; // servoNum is currently active servo";
	

	public static final String ADAFRUIT_SETUP = "\n\n" + "   pwm.begin();\n   pwm.setPWMFreq(60);  // Analog servos run at ~60 Hz updates \n";

	public static final String ADAFRUIT_CODE = "\n\n" +

	"            case AF_DRIVE_PWM: \n" + "             pwm.setPWM(servoNum, ioCommand[2], ioCommand[3]); \n" + "            break; \n"
			+ "            case AF_SET_SERVO_NUM: \n" + "             servoNum = ioCommand[2]; \n" + "            break; \n";

	public boolean attach() {
		boolean ret = true;
		ret &= attach(myArduino); // TODO - check to see if Arduino is connected
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

	public boolean isAttached() {
		return myArduino != null;
	}

	public void setSerialDevice(String comPort) {
		myArduino.setSerialDevice(comPort);
	}
	// motor controller api

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Runtime.createAndStart("python", "Python");
		Adafruit16CServoDriver pwm = (Adafruit16CServoDriver) Runtime.createAndStart("pwm", "Adafruit16CServoDriver");
		Runtime.createAndStart("gui01", "GUIService");
		
		pwm.setSerialDevice("COM9");
		
		for (int i = SERVOMIN; i < SERVOMAX; ++i)
		{
			pwm.setPWM(0, 0, i);
		}
		
		pwm.setPWM(0, 0, SERVOMIN);
		pwm.setPWM(0, 0, SERVOMAX);
		pwm.setPWM(0, 0, SERVOMIN);
		pwm.setPWM(0, 0, SERVOMAX);
		pwm.setPWM(0, 0, SERVOMIN);
		pwm.setPWM(0, 0, SERVOMAX);
		
		//pwm.attach();
		//pwm.sets



	}


}
