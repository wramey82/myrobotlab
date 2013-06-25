/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.arduino.PApplet;
import org.myrobotlab.arduino.compiler.AvrdudeUploader;
import org.myrobotlab.arduino.compiler.Compiler;
import org.myrobotlab.arduino.compiler.MessageConsumer;
import org.myrobotlab.arduino.compiler.Preferences;
import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.arduino.compiler.Target;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.myrobotlab.service.interfaces.StepperController;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

/**
 * Implementation of a Arduino Service connected to MRL through a serial port.
 * The protocol is basically a pass through of system calls to the Arduino
 * board. Data can be passed back from the digital or analog ports by request to
 * start polling. The serial port can be wireless (bluetooth), rf, or wired. The
 * communication protocol supported is in MRLComm.ino
 * 
 * Should support nearly all Arduino board types
 * 
 */

@Root
public class Arduino extends Service implements SerialDeviceEventListener, SensorDataPublisher, ServoController, MotorController, StepperController, SerialDeviceService,
		MessageConsumer {

	private static final long serialVersionUID = 1L;
	public transient final static Logger log = LoggerFactory.getLogger(Arduino.class.getCanonicalName());
	public transient static final int REVISION = 100;

	public transient static final String BOARD_TYPE_UNO = "uno";
	public transient static final String BOARD_TYPE_ATMEGA168 = "atmega168";
	public transient static final String BOARD_TYPE_ATMEGA328P = "atmega328p";
	public transient static final String BOARD_TYPE_ATMEGA2560 = "atmega2560";
	public transient static final String BOARD_TYPE_ATMEGA1280 = "atmega1280";
	public transient static final String BOARD_TYPE_ATMEGA32U4 = "atmega32u4";

	// serial device info
	private transient SerialDevice serialDevice;

	// from Arduino IDE (yuk)
	static HashSet<File> libraries;

	static boolean commandLine;
	public HashMap<String, Target> targetsTable = new HashMap<String, Target>();

	static File buildFolder;
	static public HashMap<String, File> importToLibraryTable;

	// imported Arduino constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_FORWARD = 1;
	public static final int MOTOR_BACKWARD = 0;

	private boolean connected = false;

	BlockingQueue<String> blockingData = new LinkedBlockingQueue<String>();

	/**
	 * MotorData is the combination of a Motor and any controller data needed to
	 * implement all of MotorController API
	 * 
	 */
	class MotorData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient MotorControl motor = null;
		int PWMPin = -1;
		int directionPin = -1;
	}

	HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

	// needed to dynamically adjust PWM rate (D. only?)
	public static final int TCCR0B = 0x25; // register for pins 6,7
	public static final int TCCR1B = 0x2E; // register for pins 9,10
	public static final int TCCR2B = 0xA1; // register for pins 3,11

	// serial protocol functions
	public transient final static int MAGIC_NUMBER = 170; // 10101010

	// MRL ---> Arduino methods
	public static final int DIGITAL_WRITE = 0;
	// public static final int DIGITAL_VALUE = 1; // normalized with PinData
	public static final int ANALOG_WRITE = 2;
	// public static final int ANALOG_VALUE = 3; // normalized with PinData
	public static final int PINMODE = 4;
	public static final int PULSE_IN = 5;
	public static final int SERVO_ATTACH = 6;
	public static final int SERVO_WRITE = 7;
	public static final int SERVO_SET_MAX_PULSE = 8;
	public static final int SERVO_DETACH = 9;
	public static final int SET_PWM_FREQUENCY = 11;
	public static final int SET_SERVO_SPEED = 12;
	public static final int ANALOG_READ_POLLING_START = 13;
	public static final int ANALOG_READ_POLLING_STOP = 14;
	public static final int DIGITAL_READ_POLLING_START = 15;
	public static final int DIGITAL_READ_POLLING_STOP = 16;
	public static final int SET_ANALOG_PIN_SENSITIVITY = 17;
	public static final int SET_ANALOG_PIN_GAIN = 18;
	public static final int DIGITAL_DEBOUNCE_ON = 21;
	public static final int DIGITAL_DEBOUNCE_OFF = 22;
	public static final int DIGITAL_TRIGGER_ONLY_ON = 23;
	public static final int DIGITAL_TRIGGER_ONLY_OFF = 24;
	public static final int SET_SERIAL_RATE = 25;
	public static final int GET_MRLCOMM_VERSION = 26;

	// Arduino ---> MRL methods
	public static final int DIGITAL_VALUE = 1;
	public static final int ANALOG_VALUE = 3;

	// servo related
	public static final int SERVO_STOP_AND_REPORT = 10;
	// FIXME - more depending on board (mega)
	public static final int MAX_SERVOS = 12;

	// vendor specific pins start at 50
	public static final String VENDOR_DEFINES_BEGIN = "// --VENDOR DEFINE SECTION BEGIN--";
	public static final String VENDOR_SETUP_BEGIN = "// --VENDOR SETUP BEGIN--";
	public static final String VENDOR_CODE_BEGIN = "// --VENDOR CODE BEGIN--";


	// non final for vendor mods
	public static int ARDUINO_SKETCH_TYPE = 1;
	public static int ARDUINO_SKETCH_VERSION = 1;

	public static final int SOFT_RESET = 253;
	// error
	public static final int SERIAL_ERROR = 254;

	/**
	 * pin description of board
	 */
	ArrayList<Pin> pinList = null;

	// servos
	/**
	 * ServoController data needed to run a servo
	 * 
	 */
	class ServoData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient ServoControl servo = null;
		Integer pin = null;
		int servoIndex = -1;
	}

	/**
	 * the local name to servo info
	 */
	HashMap<String, ServoData> servos = new HashMap<String, ServoData>();

	/**
	 * represents the Arduino pde array of servos and their state
	 */
	boolean[] servosInUse = new boolean[MAX_SERVOS];

	// from the Arduino IDE :P
	public Preferences preferences;
	transient Compiler compiler;
	transient AvrdudeUploader uploader;

	// compile / upload
	private String buildPath = "";
	private String sketchName = "MRLComm";
	private String sketch = "";

	/**
	 * list of serial port names from the system which the Arduino service is
	 * running - this list is refreshed on querySerialDevices
	 */
	public ArrayList<String> serialDeviceNames = new ArrayList<String>();

	public Arduino(String n) {
		super(n, Arduino.class.getCanonicalName());
		load();

		// target arduino
		// board atmenga328
		preferences = new Preferences(String.format("%s.preferences.txt", getName()), null);
		preferences.set("sketchbook.path", ".myrobotlab");

		preferences.setInteger("serial.debug_rate", 57600);
		preferences.set("serial.parity", "N"); // f'ing stupid,
		preferences.setInteger("serial.databits", 8);
		preferences.setInteger("serial.stopbits", 1); // f'ing weird 1,1.5,2
		preferences.setBoolean("upload.verbose", true);

		File librariesFolder = getContentFile("libraries");

		// FIXME - all below should be done inside Compiler2
		try {

			targetsTable = new HashMap<String, Target>();
			loadHardware(getHardwareFolder());
			loadHardware(getSketchbookHardwareFolder());
			addLibraries(librariesFolder);
			File sketchbookLibraries = getSketchbookLibrariesFolder();
			addLibraries(sketchbookLibraries);
		} catch (IOException e) {
			Logging.logException(e);
		}

		compiler = new Compiler(this);
		uploader = new AvrdudeUploader(this);

		querySerialDeviceNames();

		// FIXME - hilacious long wait - need to incorporate
		// .waitTillServiceReady
		// especially if there are multiple initialization threads
		// SWEEEET ! - Service already provides an isReady - just need to
		// overload it with a Thread.sleep check -> broadcast setState

		createPinList();

		String filename = "MRLComm.ino";
		String resourcePath = String.format("Arduino/%s/%s", filename.substring(0, filename.indexOf(".")), filename);
		log.info(String.format("loadResourceFile %s", resourcePath));
		String defaultSketch = FileIO.getResourceFile(resourcePath);
		this.sketch = defaultSketch;
	}

	// FIXME - add const BOARD TYPE strings
	public void setBoard(String board) {
		preferences.set("board", board);
		createPinList();
		preferences.save();
		broadcastState();
	}

	protected void loadHardware(File folder) {
		if (!folder.isDirectory())
			return;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.')
					return false;
				if (name.equals("CVS"))
					return false;
				return (new File(dir, name).isDirectory());
			}
		});
		// if a bad folder or something like that, this might come back null
		if (list == null)
			return;

		// alphabetize list, since it's not always alpha order
		// replaced hella slow bubble sort with this feller for 0093
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		// after that lovely searching of dirs - will come back with
		// [arduino, tools]

		for (String target : list) {
			File subfolder = new File(folder, target);
			targetsTable.put(target, new Target(target, subfolder, this));
		}
	}

	public void setPreference(String name, String value) {
		preferences.set(name, value);
		if ("board".equals(name)) {
			broadcastState();
		}
	}

	public String getSerialDeviceName() {
		if (serialDevice != null) {
			return serialDevice.getName();
		}

		return null;
	}

	public ArrayList<String> querySerialDeviceNames() {

		log.info("queryPortNames");

		serialDeviceNames = SerialDeviceFactory.getSerialDeviceNames();

		// adding connected serial port if connected
		if (serialDevice != null) {
			if (serialDevice.getName() != null)
				serialDeviceNames.add(serialDevice.getName());
		}

		return serialDeviceNames;
	}

	/**
	 * MRL protocol method
	 * 
	 * @param function
	 * @param param1
	 * @param param2
	 * 
	 *            TODO - take the cheese out of this method .. it shold be
	 *            sendMsg(byte[]...data)
	 */
	StringBuffer debugBuffer = new StringBuffer();

	public synchronized void sendMsg(int function, int... params) {
		// log.debug("sendMsg magic | fn " + function + " p1 " + param1 + " p2 "
		// + param2);
		try {
			// not CRC16 - but cheesy error correction of bytestream
			// http://www.java2s.com/Open-Source/Java/6.0-JDK-Modules-sun/misc/sun/misc/CRC16.java.htm
			// #include <util/crc16.h>
			// _crc16_update (test, testdata);

			serialDevice.write(MAGIC_NUMBER);

			// msg size = function byte + x param bytes
			// msg size does not include MAGIC_NUMBER & size
			// MAGIC_NUMBER|3|FUNCTION|PARAM0|PARAM2 would be valid
			serialDevice.write(1 + params.length);

			serialDevice.write(function);

			for (int i = 0; i < params.length; ++i) {
				serialDevice.write(params[i]);
			}

			if (log.isDebugEnabled()) {
				debugBuffer.append("sendMsg -> MAGIC_NUMBER|");
				debugBuffer.append("SZ ").append(1 + params.length);
				debugBuffer.append(String.format("|FN %d", function));
				for (int i = 0; i < params.length; ++i) {
					if (log.isDebugEnabled()) {
						debugBuffer.append(String.format("|P%d %d", i, params[i]));
					}
				}
				log.debug(debugBuffer.toString());
				debugBuffer.setLength(0);
			}

		} catch (IOException e) {
			log.error("sendMsg " + e.getMessage());
		}

	}

	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	public synchronized void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			serialDevice.write(data[i]);
		}
	}

	@Override
	public void write(char data) throws IOException {
		serialDevice.write(data);
	}

	@Override
	public void write(int data) throws IOException {
		serialDevice.write(data);
	}

	public void setPWMFrequency(Integer address, Integer freq) {
		
		int prescalarValue = 0;

		switch (freq) {
		case 31:
		case 62:
			prescalarValue = 0x05;
			break;
		case 125:
		case 250:
			prescalarValue = 0x04;
			break;
		case 500:
		case 1000:
			prescalarValue = 0x03;
			break;
		case 4000:
		case 8000:
			prescalarValue = 0x02;
			break;
		case 32000:
		case 64000:
			prescalarValue = 0x01;
			break;
		default:
			prescalarValue = 0x03;
		}

		sendMsg(SET_PWM_FREQUENCY, address, prescalarValue);
	}

	// ----------ServoController begin------------- 
	// FIXME - is this re-entrant ???
	// FIXME - refactor - don't use name use Servo type
	// FIXME - use attach(Servo servo) 
	@Override
	public boolean servoAttach(String servoName, Integer pin) {
		log.info(String.format("servoAttach %s pin %d", servoName, pin));

		if (serialDevice == null) {
			error("could not attach servo to pin " + pin + " serial port in null - not initialized?");
			return false;
		}

		// serialPort == null ??? make sure you chown it correctly !
		log.info("servoAttach (" + pin + ") to " + serialDevice.getName() + " function number " + SERVO_ATTACH);
		if (servos.containsKey(servoName)) {
			log.warn("servo already attach - detach first");
			return false;
		}

		ServoData sd = new ServoData();
		sd.pin = pin;

		for (int i = 0; i < servosInUse.length; ++i) {
			if (!servosInUse[i]) {
				servosInUse[i] = true;
				sd.servoIndex = i;
				sendMsg(SERVO_ATTACH, sd.servoIndex, pin);
				servos.put(servoName, sd);
				ServiceWrapper sw = Runtime.getServiceWrapper(servoName);
				if (sw == null || sw.service == null) {
					log.error(String.format("%s does not exist in registry", servoName));
					return false;
				}

				try {
					ServoControl sc = (ServoControl) sw.service;
					sd.servo = sc;
					sc.setController(this);
					return true;
				} catch (Exception e) {
					log.error(String.format("%s not a valid ServoController", servoName));
					return false;
				}
			}
		}

		log.error("servo " + pin + " attach failed - no idle servos");
		return false;
	}

	@Override
	public boolean servoDetach(String servoName) {
		log.info(String.format("servoDetach(%s)", servoName));

		if (servos.containsKey(servoName)) {
			ServoData sd = servos.get(servoName);
			sendMsg(SERVO_DETACH, sd.servoIndex, 0);
			servosInUse[sd.servoIndex] = false;
			sd.servo.setController(null);
			servos.remove(servoName);
			return true;
		}

		log.error(String.format("servo %s detach failed - not found", servoName));
		return false;

	}

	@Override
	public void servoWrite(String servoName, Integer newPos) {
		if (serialDevice == null) {
			log.error("serialPort is NULL !");
			return;
		}

		log.info(String.format("servoWrite %s %d", servoName, newPos));

		sendMsg(SERVO_WRITE, servos.get(servoName).servoIndex, newPos);

	}

	@Override
	public Integer getServoPin(String servoName) {
		if (servos.containsKey(servoName)) {
			return servos.get(servoName).pin;
		}
		return null;
	}

	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStart(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		sendMsg(DIGITAL_READ_POLLING_START, address, 0);

	}

	public void digitalReadPollStop(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		sendMsg(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	public void digitalWrite(Integer address, Integer value) {
		log.info("digitalWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + DIGITAL_WRITE);
		sendMsg(DIGITAL_WRITE, address, value);
	}

	public String getVersion() {
		try {
			if (serialDevice != null) {
				sendMsg(GET_MRLCOMM_VERSION, 0, 0);
				blockingData.clear();
				return blockingData.poll(1000, TimeUnit.MILLISECONDS);
			} else {
				return null;
			}

		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	public String publishVersion(String version, String vendorVersion) {
		blockingData.add(version);
		return version;
	}

	public void pinMode(Integer address, Integer value) {
		log.info("pinMode (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + PINMODE);
		sendMsg(PINMODE, address, value);
	}

	public void analogWrite(Integer address, Integer value) {
		log.info("analogWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + ANALOG_WRITE);
		sendMsg(ANALOG_WRITE, address, value);
	}

	/**
	 * This method is called with Pin data whene a pin value is changed on the
	 * Arduino board the Arduino must be told to poll the desired pin(s). This
	 * is done with a analogReadPollingStart(pin) or digitalReadPollingStart()
	 */
	public Pin publishPin(Pin p) {
		// log.debug(p);
		pinList.get(p.pin).value = p.value;
		return p;
	}

	public String readSerialMessage(String s) {
		return s;
	}

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin) {
		sendMsg(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
		// used in read
	}

	public void digitalReadPollingStop(Integer pin) {
		sendMsg(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(Integer pin) {
		sendMsg(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
	}

	public void analogReadPollingStop(Integer pin) {
		sendMsg(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	@Override
	public String getToolTip() {
		return "<html>Arduino is a service which interfaces with an Arduino micro-controller.<br>" + "This interface can operate over radio, IR, or other communications,<br>"
				+ "but and appropriate .PDE file must be loaded into the micro-controller.<br>" + "See http://myrobotlab.org/communication for details";
	}

	public void stopService() {
		super.stopService();
		if (serialDevice != null) {
			serialDevice.close();
		}
	}

	int txErrors = 0;
	int rxErrors = 0;
	static final public int MAX_MSG_LEN = 64;
	StringBuffer rxDebug = new StringBuffer();

	byte[] msg = new byte[64]; // TODO define outside
	int newByte;
	int byteCount = 0;
	int msgSize = 0;

	StringBuffer dump = new StringBuffer();

	@Override
	public void serialEvent(SerialDeviceEvent event) {
		switch (event.getEventType()) {
		case SerialDeviceEvent.BI:
		case SerialDeviceEvent.OE:
		case SerialDeviceEvent.FE:
		case SerialDeviceEvent.PE:
		case SerialDeviceEvent.CD:
		case SerialDeviceEvent.CTS:
		case SerialDeviceEvent.DSR:
		case SerialDeviceEvent.RI:
		case SerialDeviceEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialDeviceEvent.DATA_AVAILABLE:

			// at this point we should have a complete message
			// the msg contains only daat // METHOD | P0 | P1 ... | PN

			// msg buffer
			// msg[0] METHOD
			// msg[1] P0
			// msg[2] P1
			// ...
			// msg[N] PN+1
			try {

				/*
				 * DON'T EVER MAKE THIS NON-MONOLITHIC ! - the overhead of going into another method
				 * is high and will end up loss of data in serial communications !
				 * 
				 * Optimize this as much as possible !
				 */
				while (serialDevice.isOpen() && (newByte = serialDevice.read()) > -1) {

					++byteCount;

					if (byteCount == 1) {
						if (newByte != MAGIC_NUMBER) {
							++rxErrors;
							byteCount = 0;
							msgSize = 0;
							error(String.format("%d rx errors", rxErrors));
							log.error("MAGIC_NUMBER {}", newByte);
							dump.setLength(0);
						}
						continue;
					}else if (byteCount == 2) {
						// get the size of message
						if (newByte > 64) {
							byteCount = 0;
							msgSize = 0;
							error(String.format("%d rx sz errors", ++rxErrors));
							continue;
						}
						msgSize = (byte) newByte;
						//dump.append(String.format("MSG|SZ %d", msgSize));
					} else if (byteCount > 2) {
						// remove header - fill msg data - (2) headbytes -1
						// (offset)
						//dump.append(String.format("|P%d %d", byteCount, newByte));
						msg[byteCount - 3] = (byte) newByte;
					}

					// process valid message
					if (byteCount == 2 + msgSize) {
						//log.error("A {}", dump.toString());
						//dump.setLength(0);
						
						switch (msg[0]) {
						case GET_MRLCOMM_VERSION: {
							// TODO - get vendor version
							invoke("publishVersion", String.format("%d", msg[1]), String.format("%d", msg[2]));
							break;
						}
						case ANALOG_VALUE: {
							Pin p = new Pin(msg[1], msg[0], (((msg[2] & 0xFF) << 8) + (msg[3] & 0xFF)), getName());
							invoke("publishPin", p);
							break;
						}
						case DIGITAL_VALUE: {
							Pin p = new Pin(msg[1], msg[0], msg[2], getName());
							invoke("publishPin", p);
							break;
						}
						} // end switch

						if (log.isDebugEnabled()) {
							rxDebug.append(String.format(" serialEvent ->  MAGIC_NUMBER|SZ %d|FN %d", msgSize, msg[0]));
							for (int i = 1; i < msgSize; ++i) {
								rxDebug.append(String.format("|P%d %d", i, msg[i]));
							}
							log.debug(rxDebug.toString());
							rxDebug.setLength(0);
						}

						// processed msg
						// reset msg buffer
						msgSize = 0;
						byteCount = 0;
					}
				} // while (serialDevice.isOpen() && (newByte = serialDevice.read()) > -1

			} catch (Exception e) {
				Logging.logException(e);
			}

		}

	}

	// FIXME !!! - REMOVE ALL BELOW - except compile(File) compile(String)
	// upload(File) upload(String)
	// supporting methods for Compiler & UPloader may be necessary

	static public String getAvrBasePath() {
		if (Platform.isLinux()) {
			return ""; // avr tools are installed system-wide and in the path
		} else {
			return getHardwarePath() + File.separator + "tools" + File.separator + "avr" + File.separator + "bin" + File.separator;
		}
	}

	static public String getHardwarePath() {
		return getHardwareFolder().getAbsolutePath();
	}

	static public File getHardwareFolder() {
		// calculate on the fly because it's needed by Preferences.init() to
		// find
		// the boards.txt and programmers.txt preferences files (which happens
		// before the other folders / paths get cached).
		return getContentFile("hardware");
	}

	static public File getContentFile(String name) {
		String path = System.getProperty("user.dir");

		// Get a path to somewhere inside the .app folder
		if (Platform.isMac()) {
			String javaroot = System.getProperty("javaroot");
			if (javaroot != null) {
				path = javaroot;
			}
		}

		path += File.separator + "arduino";

		File working = new File(path);
		return new File(working, name);
	}

	public Map<String, String> getBoardPreferences() {
		Target target = getTarget();
		if (target == null)
			return new LinkedHashMap();
		Map map = target.getBoards();
		if (map == null)
			return new LinkedHashMap();
		map = (Map) map.get(preferences.get("board"));
		if (map == null)
			return new LinkedHashMap();
		return map;
	}

	public Target getTarget() {
		return targetsTable.get(preferences.get("target"));
	}

	public String getSketchbookLibrariesPath() {
		return getSketchbookLibrariesFolder().getAbsolutePath();
	}

	public File getSketchbookHardwareFolder() {
		return new File(getSketchbookFolder(), "hardware");
	}

	protected File getDefaultSketchbookFolder() {
		File sketchbookFolder = null;
		try {
			sketchbookFolder = new File("./.myrobotlab");// platform.getDefaultSketchbookFolder();
		} catch (Exception e) {
		}

		// create the folder if it doesn't exist already
		boolean result = true;
		if (!sketchbookFolder.exists()) {
			result = sketchbookFolder.mkdirs();
		}

		if (!result) {
			showError("You forgot your sketchbook", "Arduino cannot run because it could not\n" + "create a folder to store your sketchbook.", null);
		}

		return sketchbookFolder;
	}

	public File getSketchbookLibrariesFolder() {
		return new File(getSketchbookFolder(), "libraries");
	}

	public File getSketchbookFolder() {
		return new File(preferences.get("sketchbook.path"));
	}

	public File getBuildFolder() {
		if (buildFolder == null) {
			String buildPath = preferences.get("build.path");
			if (buildPath != null) {
				buildFolder = new File(buildPath);

			} else {
				// File folder = new File(getTempFolder(), "build");
				// if (!folder.exists()) folder.mkdirs();
				buildFolder = createTempFolder("build");
				buildFolder.deleteOnExit();
			}
		}
		return buildFolder;
	}

	static public File createTempFolder(String name) {
		try {
			File folder = File.createTempFile(name, null);
			// String tempPath = ignored.getParent();
			// return new File(tempPath);
			folder.delete();
			folder.mkdirs();
			return folder;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void removeDescendants(File dir) {
		if (!dir.exists())
			return;

		String files[] = dir.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals(".."))
				continue;
			File dead = new File(dir, files[i]);
			if (!dead.isDirectory()) {
				if (!preferences.getBoolean("compiler.save_build_files")) {
					if (!dead.delete()) {
						// temporarily disabled
						System.err.println("Could not delete " + dead);
					}
				}
			} else {
				removeDir(dead);
				// dead.delete();
			}
		}
	}

	/**
	 * Remove all files in a directory and the directory itself.
	 */
	public void removeDir(File dir) {
		if (dir.exists()) {
			removeDescendants(dir);
			if (!dir.delete()) {
				System.err.println("Could not delete " + dir);
			}
		}
	}

	/**
	 * Return an InputStream for a file inside the Processing lib folder.
	 */
	static public InputStream getLibStream(String filename) throws IOException {
		return new FileInputStream(new File(getContentFile("lib"), filename));
	}

	static public void saveFile(String str, File file) throws IOException {
		File temp = File.createTempFile(file.getName(), null, file.getParentFile());
		PApplet.saveStrings(temp, new String[] { str });
		if (file.exists()) {
			boolean result = file.delete();
			if (!result) {
				throw new IOException("Could not remove old version of " + file.getAbsolutePath());
			}
		}
		boolean result = temp.renameTo(file);
		if (!result) {
			throw new IOException("Could not replace " + file.getAbsolutePath());
		}
	}

	public static boolean isCommandLine() {
		return commandLine;
	}

	protected boolean addLibraries(File folder) throws IOException {
		if (!folder.isDirectory())
			return false;

		String list[] = folder.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// skip .DS_Store files, .svn folders, etc
				if (name.charAt(0) == '.')
					return false;
				if (name.equals("CVS"))
					return false;
				return (new File(dir, name).isDirectory());
			}
		});
		// if a bad folder or something like that, this might come back null
		if (list == null)
			return false;

		// alphabetize list, since it's not always alpha order
		// replaced hella slow bubble sort with this feller for 0093
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		boolean ifound = false;

		// reset the set of libraries
		libraries = new HashSet<File>();
		// reset the table mapping imports to libraries
		importToLibraryTable = new HashMap<String, File>();

		for (String libraryName : list) {
			File subfolder = new File(folder, libraryName);

			libraries.add(subfolder);
			String packages[] = Compiler.headerListFromIncludePath(subfolder.getAbsolutePath());
			for (String pkg : packages) {
				importToLibraryTable.put(pkg, subfolder);
			}

			ifound = true;
		}
		return ifound;
	}

	public String showMessage(String msg, String desc) {
		log.info("showMessage " + msg);
		return msg;
	}

	public SerialDevice getSerialDevice() {
		return serialDevice;
	}

	@Override
	public ArrayList<String> getSerialDeviceNames() {
		return serialDeviceNames;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (sd != null) {
				serialDevice = sd;

				connect();

				// 115200 wired, 2400 IR ?? VW 2000??
				serialDevice.setParams(57600, 8, 1, 0); // FIXME hardcoded until
														// Preferences are
														// removed

				save(); // successfully bound to port - saving
				preferences.set("serial.port", serialDevice.getName());
				preferences.save();
				broadcastState(); // state has changed let everyone know
				return true;
			}
		} catch (Exception e) {
			logException(e);
		}
		return false;
	}

	public void setCompilingProgress(Integer progress) {
		log.info(String.format("progress %d ", progress));
		invoke("publishCompilingProgress", progress);
	}

	public Integer publishCompilingProgress(Integer progress) {
		return progress;
	}

	public String createBuildPath(String sketchName) {
		// make a work/tmp directory if one doesn't exist - TODO - new time
		// stamp?
		Date d = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
		formatter.setCalendar(cal);

		String tmpdir = String.format("obj%s%s.%s", File.separator, sketchName, formatter.format(d));
		new File(tmpdir).mkdirs();

		return tmpdir;

	}

	public void compile(String sketchName, String sketch) {
		// FYI - not thread safe
		this.sketchName = sketchName;
		this.sketch = sketch;
		this.buildPath = createBuildPath(sketchName);

		try {
			compiler.compile(sketchName, sketch, buildPath, true);
		} catch (RunnerException e) {
			logException(e);
			invoke("compilerError", e.getMessage());
		}
		log.debug(sketch);
	}

	
	public void upload(String sketch) throws Throwable {
		compile("MRLComm", sketch); // FIXME throw push error();
		uploader.uploadUsingPreferences(buildPath, sketchName, false);
	}

	/**
	 * Get the number of lines in a file by counting the number of newline
	 * characters inside a String (and adding 1).
	 */
	static public int countLines(String what) {
		int count = 1;
		for (char c : what.toCharArray()) {
			if (c == '\n')
				count++;
		}
		return count;
	}

	/**
	 * Grab the contents of a file as a string.
	 */
	static public String loadFile(File file) throws IOException {
		String[] contents = PApplet.loadStrings(file);
		if (contents == null)
			return null;
		return PApplet.join(contents, "\n");
	}

	@Override
	public ArrayList<Pin> getPinList() {
		return pinList;
	}

	public ArrayList<Pin> createPinList() {
		pinList = new ArrayList<Pin>();
		String type = preferences.get("board");
		int pinType = Pin.DIGITAL_VALUE;

		if (type != null && type.startsWith("mega")) {
			for (int i = 0; i < 70; ++i) {

				if (i < 1 || (i > 13 && i < 54)) {
					pinType = Pin.DIGITAL_VALUE;
				} else if (i > 53) {
					pinType = Pin.ANALOG_VALUE;
				} else {
					pinType = Pin.PWM_VALUE;
				}
				pinList.add(new Pin(i, pinType, 0, getName()));
			}
		} else {
			for (int i = 0; i < 20; ++i) {
				if (i < 14) {
					pinType = Pin.DIGITAL_VALUE;
				} else if (i > 53) {
					pinType = Pin.ANALOG_VALUE;
				}

				if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
					pinType = Pin.PWM_VALUE;
				}
				pinList.add(new Pin(i, pinType, 0, getName()));
			}
		}

		return pinList;
	}

	@Override
	public void message(String msg) {
		log.info(msg);
		invoke("publishMessage", msg);
	}

	static public String showError(String error, String desc, Exception e) {
		return error;
	}

	public String compilerError(String error) {
		return error;
	}

	public String publishMessage(String msg) {
		return msg;
	}

	public final String expectedVersion = "1";

	public boolean connect() {

		if (serialDevice == null) {
			error("can't connect, serialDevice is null");
			return false;
		}

		message(String.format("\nconnecting to serial device %s\n", serialDevice.getName()));

		try {
			if (!serialDevice.isOpen()) {
				serialDevice.open();
				serialDevice.addEventListener(this);
				serialDevice.notifyOnDataAvailable(true);
				sleep(1000);

				// TODO boolean config - supress getting version
				// String version = getVersion();
				String version = null;
				if (version == null) {
					warn("did not get response from arduino....");
				} else if (!version.equals(expectedVersion)) {
					warn(String.format("MRLComm.ino responded with version %s expected version is %s", version, expectedVersion));
				} else {
					info(String.format("responded with expected version %s ... goodtimes...", version));
				}
			} else {
				warn(String.format("\n%s is already open, close first before opening again\n", serialDevice.getName()));
			}
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}

		info(String.format("connected to serial device %s", serialDevice.getName()));
		info("good times...");
		connected = true;

		return true;
	}

	/**
	 * valid means - connected and MRLComm was found at the correct version
	 * 
	 * @return
	 */
	public boolean isValid() {
		return expectedVersion.equals(getVersion());
	}

	public boolean isConnected() {
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected;
	}

	public boolean disconnect() {
		connected = false;
		if (serialDevice == null) {
			return false;
		}

		serialDevice.close();

		broadcastState();
		return true;
	}

	// ----------- Motor Controller API Begin ----------------

	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		ServiceWrapper sw = Runtime.getServiceWrapper(motorName);
		if (!sw.isLocal()) {
			log.error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		ServiceInterface service = sw.service;
		MotorControl motor = (MotorControl) service; // BE-AWARE - local
														// optimization ! Will
														// not work on remote
														// !!!
		return motorAttach(motor, motorData);
	}

	public boolean motorAttach(String motorName, Integer PWMPin, Integer directionPin) {
		return motorAttach(motorName, new Object[] { PWMPin, directionPin });
	}

	/**
	 * implementation of motorAttach(String motorName, Object... motorData) is
	 * private so that interfacing consistently uses service names to attach,
	 * even though service is local
	 * 
	 * @param motor
	 * @param motorData
	 * @return
	 */
	private boolean motorAttach(MotorControl motor, Object... motorData) {
		if (motor == null || motorData == null) {
			log.error("null data or motor - can't attach motor");
			return false;
		}

		if (motorData.length != 2 || motorData[0] == null || motorData[1] == null) {
			log.error("motor data must be of the folowing format - motorAttach(Integer PWMPin, Integer directionPin)");
			return false;
		}

		MotorData md = new MotorData();
		md.motor = motor;
		md.PWMPin = (Integer) motorData[0];
		md.directionPin = (Integer) motorData[1];
		motors.put(motor.getName(), md);
		motor.setController(this);
		sendMsg(PINMODE, md.PWMPin, OUTPUT);
		sendMsg(PINMODE, md.directionPin, OUTPUT);
		return true;

	}

	@Override
	public boolean motorDetach(String motorName) {
		boolean ret = motors.containsKey(motorName);
		if (ret) {
			motors.remove(motorName);
		}
		return ret;
	}

	public void motorMove(String name) {

		MotorData md = motors.get(name);
		MotorControl m = md.motor;
		float power = m.getPowerLevel();

		if (power < 0) {
			sendMsg(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted() ? MOTOR_FORWARD : MOTOR_BACKWARD);
			sendMsg(ANALOG_WRITE, md.PWMPin, Math.abs((int) (255 * m.getPowerLevel())));
		} else if (power > 0) {
			sendMsg(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted() ? MOTOR_BACKWARD : MOTOR_FORWARD);
			sendMsg(ANALOG_WRITE, md.PWMPin, (int) (255 * m.getPowerLevel()));
		} else {
			sendMsg(ANALOG_WRITE, md.PWMPin, 0);
		}
	}

	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	public void digitalDebounceOn() {
		sendMsg(DIGITAL_DEBOUNCE_ON, 0, 0);
	}

	public void digitalDebounceOff() {
		sendMsg(DIGITAL_DEBOUNCE_OFF, 0, 0);
	}

	// ----------- MotorController API End ----------------

	// FIXME - too complicated.. too much code bloat .. its nice you use names
	// BUT
	// IT MAKES NO SENSE TO HAVE SERVOS "connecte" ON A DIFFERENT INSTANCE
	// SO USING ACTUAL TYPES SIMPLIFIES LIFE !

	public boolean attach(String serviceName, Object... data) {
		log.info(String.format("attaching %s", serviceName));
		ServiceWrapper sw = Runtime.getServiceWrapper(serviceName);
		if (sw == null) {
			log.error(String.format("could not attach % - not found in registry", serviceName));
			return false;
		}
		if (sw.get() instanceof Servo) // Servo or ServoControl ???
		{
			if (data.length != 1) {
				log.error("can not attach a Servo without a pin number");
				return false;
			}
			if (!sw.isLocal()) {
				log.error("servo controller and servo must be local");
				return false;
			}
			return servoAttach(serviceName, (Integer) (data[0]));
		}

		if (sw.get() instanceof Motor) // Servo or ServoControl ???
		{
			if (data.length != 2) {
				log.error("can not attach a Motor without a PWMPin & directionPin ");
				return false;
			}
			if (!sw.isLocal()) {
				log.error("motor controller and motor must be local");
				return false;
			}
			return motorAttach(serviceName, data);
		}

		if (sw.get() instanceof ArduinoShield) // Servo or ServoControl ???
		{

			if (!sw.isLocal()) {
				log.error("motor controller and motor must be local");
				return false;
			}

			return ((ArduinoShield) sw.get()).attach(this);
		}

		log.error("don't know how to attach");
		return false;
	}

	public String getSketch() {
		return this.sketch;
	}

	public String setSketch(String newSketch) {
		sketch = newSketch;
		return sketch;
	}

	public String loadSketchFromFile(String filename) {
		String newSketch = FileIO.fileToString(filename);
		if (newSketch != null) {
			sketch = newSketch;
			return sketch;
		}
		return null;
	}

	@Override
	public Object[] getMotorData(String motorName) {
		MotorData md = motors.get(motorName);
		Object[] data = new Object[] { md.PWMPin, md.directionPin };
		return data;
	}

	public void softReset() {
		sendMsg(SOFT_RESET, 0, 0);
	}

	@Override
	public void setServoSpeed(String servoName, Float speed) {
		if (speed == null || speed < 0.0f || speed > 1.0f) {
			log.error(String.format("speed %f out of bounds", speed));
			return;
		}
		sendMsg(SET_SERVO_SPEED, servos.get(servoName).servoIndex, (int) (speed * 100));
	}

	@Override
	public void releaseService() {
		super.releaseService();
		disconnect();
	}

	public void setDigitalTriggerOnly(Boolean b) {
		if (b)
			sendMsg(DIGITAL_TRIGGER_ONLY_ON, 0, 0);
		else
			sendMsg(DIGITAL_TRIGGER_ONLY_OFF, 0, 0);

	}

	public void setSerialRate(int rate) {
		try {
			log.info(String.format("setSerialRate %d", rate));
			sendMsg(SET_SERIAL_RATE, rate, 0);
			serialDevice.setParams(rate, 8, 1, 0);
		} catch (SerialDeviceException e) {
			logException(e);
		}
	}

	// FIXME - soon to be interface for Serial service
	public boolean setSerialDevice(String port, Integer bitrate, Integer databits, Integer stopbits, Integer parity) {
		return connect(port, databits, databits, stopbits, parity);
	}

	public boolean setSerialDevice(String comPort) {
		return connect(comPort, 57600, 8, 1, 0);
	}

	// -- StepperController begin ----
	
	@Override
	public boolean stepperAttach(String stepperName, Integer steps, Integer pin1, Integer pin2, Integer pin3, Integer pin4)
	{
		if (!isConnected())
		{
			error(String.format("can not attach servo %s before Arduino %s is connected", stepperName, getName()));
			return false;
		}
		
		ServiceWrapper sw = Runtime.getServiceWrapper(stepperName);
		if (!sw.isLocal()) {
			log.error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		ServiceInterface service = sw.service;
		ServoControl stepper = (ServoControl) service; 
		return stepperAttach(stepper, steps, pin1, pin2, pin3, pin4);
	}
	
	// FIXME - COMPLETE IMPLEMENTATION BEGIN --
	public boolean stepperAttach(ServoControl stepper, Integer steps, Integer pin1, Integer pin2, Integer pin3, Integer pin4)
	{
		// set all pins to output mode
		sendMsg(PINMODE, pin1, OUTPUT);
		sendMsg(PINMODE, pin2, OUTPUT);
		sendMsg(PINMODE, pin3, OUTPUT);
		sendMsg(PINMODE, pin4, OUTPUT);
		
		stepper.setController(this);
		return false;
	}
	// FIXME - COMPLETE IMPLEMENTATION END --

	@Override
	public void stepperMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepperMove(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean stepperDetach(String name) {
		// TODO Auto-generated method stub
		return false;
	}
	// -- StepperController begin ----

	public static void main(String[] args) throws RunnerException, SerialDeviceException, IOException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		Arduino arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
		
		Runtime.createAndStart("python", "Python");
		Runtime.createAndStart("gui01", "GUIService");

	}
	
}
