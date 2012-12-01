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

import org.apache.log4j.Logger;
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
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.ArduinoShield;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Root;

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
public class Arduino extends Service implements SerialDeviceEventListener, SensorDataPublisher, DigitalIO, 
AnalogIO, ServoController, MotorController, SerialDeviceService, MessageConsumer {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(Arduino.class.getCanonicalName());
	public static final int REVISION = 100;
	
	public static final String BOARD_TYPE_UNO = "uno";
	public static final String BOARD_TYPE_ATMEGA168 = "atmega168";
	public static final String BOARD_TYPE_ATMEGA328P = "atmega328p";
	public static final String BOARD_TYPE_ATMEGA2560 = "atmega2560";
	public static final String BOARD_TYPE_ATMEGA1280 = "atmega1280";
	public static final String BOARD_TYPE_ATMEGA32U4 = "atmega32u4";
	
	// serial device info
	private transient SerialDevice serialDevice;

	// from Arduino IDE (yuk)
	static HashSet<File> libraries;

	static boolean commandLine;
	public HashMap<String, Target> targetsTable = new HashMap<String, Target>();

	static File buildFolder;
	static public HashMap<String, File> importToLibraryTable;

	// FIXME - have SerialDevice read by length or by term string
	boolean rawReadMsg = false;
	int rawReadMsgLength = 5;
	
	// imported Arduino constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;

	public static final int MOTOR_FORWARD = 1;
	public static final int MOTOR_BACKWARD = 0;
	
	private boolean connected = false;
	
	/**
	 * MotorData is the combination of a Motor and any controller data needed to 
	 * implement all of MotorController API
	 *
	 */
	class MotorData implements Serializable
	{
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
	public final static int MAGIC_NUMBER = 170; //10101010

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

	// servo related
	public static final int SERVO_SWEEP = 10;
	public static final int MAX_SERVOS = 12; // FIXME - more depending on board (mega)
	
	// vendor specific
	public static final String VENDOR_DEFINES_BEGIN = "// --VENDOR DEFINE SECTION BEGIN--";
	public static final String VENDOR_SETUP_BEGIN = "// --VENDOR SETUP BEGIN--";
	public static final String VENDOR_CODE_BEGIN = "// --VENDOR CODE BEGIN--";
	
	public static final int ACEDUINO_MOTOR_SHIELD_START = 50;
	public static final int ACEDUINO_MOTOR_SHIELD_STOP = 51;
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_POSITION = 52;	
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_MIN_BOUNDS = 53;
	public static final int ACEDUINO_MOTOR_SHIELD_SERVO_SET_MAX_BOUNDS = 54;
	
	// non final for vendor mods
	public static int ARDUINO_SKETCH_TYPE = 1; 
	public static int ARDUINO_SKETCH_VERSION = 1;
	
	public static final int SOFT_RESET = 253;
	// error
	public static final int SERIAL_ERROR = 254;
	
	/**
	 *  pin description of board
	 */
	ArrayList<Pin> pinList = null;
	
	// servos
	/**
	 * ServoController data needed to run a servo
	 *
	 */
	class ServoData implements Serializable
	{
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
	private String sketchName = "";
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
		preferences = new Preferences(String.format("%s.preferences.txt",getName()),null);
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
			Service.logException(e);
		}

		compiler = new Compiler(this);
		uploader = new AvrdudeUploader(this);
		
		querySerialDeviceNames();

		// FIXME - hilacious long wait - need to incorporate .waitTillServiceReady
		// especially if there are multiple initialization threads
		// SWEEEET ! - Service already provides an isReady - just need to overload it with a Thread.sleep check -> broadcast setState
		
		createPinList();
		
		String filename = "MRLComm.ino";
		String resourcePath = String.format("Arduino/%s/%s",filename.substring(0,filename.indexOf(".")), filename);
		log.info(String.format("loadResourceFile %s", resourcePath));
		String defaultSketch = FileIO.getResourceFile(resourcePath);
		this.sketch = defaultSketch;
	}
	
	// FIXME - add const BOARD TYPE strings
	public void setBoard(String board)
	{
		preferences.set("board",board);
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

	@Override
	public void loadDefaultConfiguration() {
	}
	

	public synchronized void serialSend(int function, int param1, int param2) {
		log.info("serialSend magic | fn " + function + " p1 " + param1 + " p2 " + param2);
		try {
			// not CRC16 - but cheesy error correction of bytestream
			// http://www.java2s.com/Open-Source/Java/6.0-JDK-Modules-sun/misc/sun/misc/CRC16.java.htm
			// #include <util/crc16.h>
			// _crc16_update (test, testdata);
			
			serialDevice.write(MAGIC_NUMBER);
			serialDevice.write(function);
			serialDevice.write(param1);
			serialDevice.write(param2); // 0 - 180
			
		} catch (IOException e) {
			log.error("serialSend " + e.getMessage());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.Serial#serialSend(java.lang.String)
	 */
	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	public void serialSend(String data) {
		log.error("serialSend [" + data + "]");
		serialSend(data.getBytes());
	}

	public synchronized void serialSend(byte[] data) {
		try {
			for (int i = 0; i < data.length; ++i) {
				serialDevice.write(data[i]);
			}
		} catch (IOException e) {
			log.error("serialSend " + e.getMessage());
		}
	}

	public void setPWMFrequency(IOData io) {
		int freq = io.value;
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

		serialSend(SET_PWM_FREQUENCY, io.address, prescalarValue);
	}

	// ---------------------------- ServoController begin  -----------------------

	@Override
	public boolean servoAttach(String servoName, Integer pin) {
		log.info(String.format("servoAttach %s pin %d", servoName, pin));
		
		if (serialDevice == null) {
			log.error("could not attach servo to pin " + pin + " serial port in null - not initialized?");
			return false;
		}
		
		// serialPort == null ??? make sure you chown it correctly !
		log.info("servoAttach (" + pin + ") to " + serialDevice.getName() + " function number " + SERVO_ATTACH);
		if (servos.containsKey(servoName))
		{
			log.warn("servo already attach - detach first");
			return false;
		}
		
		ServoData sd = new ServoData();
		sd.pin = pin;

		for (int i = 0; i < servosInUse.length; ++i) {
			if (!servosInUse[i]) {
				servosInUse[i] = true;
				sd.servoIndex = i;
				serialSend(SERVO_ATTACH, sd.servoIndex, pin);
				servos.put(servoName, sd);
				ServiceWrapper sw = Runtime.getServiceWrapper(servoName);
				if (sw == null || sw.service == null)
				{
					log.error(String.format("%s does not exist in registry", servoName));
					return false;
				}
				
				try {
					ServoControl sc = (ServoControl)sw.service;
					sd.servo = sc;
					sc.setController(this);
					return true;
				} catch(Exception e)
				{
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
			serialSend(SERVO_DETACH, sd.servoIndex, 0);
			servosInUse[sd.servoIndex] = false;
			sd.servo.setController(null);
			servos.remove(servoName);
			return true;
		}
		

		log.error(String.format("servo %s detach failed - not found",servoName));
		return false;

	}


	@Override
	public void servoWrite(String servoName, Integer newPos) {
		if (serialDevice == null) {
			log.error("serialPort is NULL !");
			return;
		}

		log.info(String.format("servoWrite %s %d", servoName, newPos));

		serialSend(SERVO_WRITE, servos.get(servoName).servoIndex, newPos);

	}
	
	@Override
	public Integer getServoPin(String servoName) {
		if (servos.containsKey(servoName))
		{
			return servos.get(servoName).pin;
		}
		return null;
	}



	// ---------------------------- ServoController End -----------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStart(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}

	public void digitalReadPollStop(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	// FIXME - deprecate
	public IOData digitalWrite(IOData io) {
		digitalWrite(io.address, io.value);
		return io;
	}

	public void digitalWrite(Integer address, Integer value) {
		log.info("digitalWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + DIGITAL_WRITE);
		serialSend(DIGITAL_WRITE, address, value);
	}

	// FIXME - deprecate
	public void pinMode(IOData io) {
		pinMode(io.address, io.value);
	}

	public void pinMode(Integer address, Integer value) {
		log.info("pinMode (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + PINMODE);
		serialSend(PINMODE, address, value);
	}

	// FIXME - deprecate
	public IOData analogWrite(IOData io) {
		analogWrite(io.address, io.value);
		return io;
	}

	public void analogWrite(Integer address, Integer value) {
		log.info("analogWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + ANALOG_WRITE);
		serialSend(ANALOG_WRITE, address, value);
	}

	public Pin publishPin(Pin p) {
		//log.debug(p);
		return p;
	}

	public String readSerialMessage(String s) {
		return s;
	}

	// char rawMsgBuffer
	public void setRawReadMsg(Boolean b) {
		rawReadMsg = b;
	}

	public void setReadMsgLength(Integer length) {
		rawReadMsgLength = length;
	}


	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
		// used in read
	}

	public void digitalReadPollingStop(Integer pin) {
		serialSend(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(Integer pin) {
		serialSend(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
	}

	public void analogReadPollingStop(Integer pin) {
		serialSend(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
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

	int errorCount = 0;
	
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

			try {

				byte[] msg = new byte[rawReadMsgLength];
				int newByte;
				int numBytes = 0;

				while (serialDevice.isOpen() && (newByte = serialDevice.read()) >= 0) {
					
					if (numBytes == 0 && newByte != MAGIC_NUMBER)
					{
						// ERROR ERROR ERROR !!!!
						++errorCount;
						// TODO call error method - notify rest of system
						continue;
					}
					
					msg[numBytes] = (byte) newByte;
					++numBytes;

					// FIXME - read by length or termination character
					// FIXME - publish (length) or termination character
					if (numBytes == rawReadMsgLength) {

						if (rawReadMsg) {
							// raw protocol
							String s = new String(msg);
							log.info(s);
							invoke("readSerialMessage", s);
						} else {

							// MRL Arduino protocol 
							// msg[0] MAGIC_NUMBER
							// msg[1] METHOD
							// msg[2] PIN
							// msg[3] HIGHBYTE
							// msg[4] LOWBYTE
							Pin p = new Pin(msg[2],msg[1], (((msg[3] & 0xFF) << 8) + (msg[4] & 0xFF)), getName());
							invoke(SensorDataPublisher.publishPin, p);
						}

						numBytes = 0;

						// reset buffer
						for (int i = 0; i < rawReadMsgLength; ++i) {
							msg[i] = -1;
						}

					}
				}

			} catch (IOException e) {
			}

			break;
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
	public boolean setSerialDevice(String name, int rate, int databits, int stopbits, int parity) {
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (sd != null) {
				serialDevice = sd;
				
				connect();

				// 115200 wired, 2400 IR ?? VW 2000??
				serialDevice.setParams(57600, 8, 1, 0); // FIXME hardcoded until Preferences are removed

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
	
	public Integer publishCompilingProgress(Integer progress)
	{
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


	// public void upload(String file) throws RunnerException,
	// SerialDeviceException
	// FIXME - stupid - should take a binary string or the path to the .hex file
	public void upload() throws Throwable {
		// uploader.uploadUsingPreferences("C:\\mrl\\myrobotlab\\obj",
		// "MRLComm", false);
		if (sketchName == null)
		{
			log.error("invalid sketchname");
			return;
		}
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
	public ArrayList<Pin> getPinList()
	{
		return pinList;
	}
	
	public ArrayList<Pin> createPinList() {
		pinList = new ArrayList<Pin>();
		String type = preferences.get("board");
		int pinType = Pin.DIGITAL_VALUE;

		if ("mega2560".equals(type)) {
			for (int i = 0; i < 70; ++i) {
	
				if (i < 1 || (i > 13 && i < 54))
				{
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
				if  (i < 14)
				{
					pinType = Pin.DIGITAL_VALUE;
				} else if (i > 53) {
					pinType = Pin.ANALOG_VALUE;
				}
				
				if (i == 3 || i == 5 || i == 6 || i == 9 || i == 10 || i == 11) {
					pinType = Pin.PWM_VALUE;
				}
				pinList.add(new Pin(i, pinType, 0, getName()));			}
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

	public String publishMessage(String msg)
	{
		return msg;
	}
	
	public boolean connect()
	{
		
		if (serialDevice == null)
		{
			message("\ncan't connect, serialDevice is null\n"); // TODO - "errorMessage vs message" 
			log.error("can't connect, serialDevice is null");
			return false;
		}

		message(String.format("\nconnecting to serial device %s\n", serialDevice.getName()));
		
		try {
			if (!serialDevice.isOpen())
			{
				serialDevice.open();
				serialDevice.addEventListener(this);
				serialDevice.notifyOnDataAvailable(true);
			} else {
				log.warn(String.format("\n%s is already open, close first before opening again\n",serialDevice.getName()));
				message(String.format("%s is already open, close first before opening again",serialDevice.getName()));
			}
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		
		message(String.format("\nconnected to serial device %s\n", serialDevice.getName()));
		message("good times...\n");
		connected = true;
		return true;
	}
	
	public boolean isConnected()
	{
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected; 
	}
	
	public boolean disconnect()
	{
		connected = false;
		if (serialDevice == null)
		{
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
		if (!sw.isLocal())
		{
			log.error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		ServiceInterface service = sw.service;
		MotorControl motor = (MotorControl)service; // BE-AWARE - local optimization ! Will not work on remote !!!
		return motorAttach(motor, motorData);
	}
	
	public boolean motorAttach(String motorName, Integer PWMPin, Integer directionPin)
	{
		return motorAttach(motorName, new Object[]{PWMPin, directionPin});
	}

	/**
	 * implementation of motorAttach(String motorName, Object... motorData)
	 * is private so that interfacing consistently uses service names to attach,
	 * even though service is local
	 * 
	 * @param motor
	 * @param motorData
	 * @return
	 */
	private boolean motorAttach(MotorControl motor, Object... motorData)
	{
		if (motor == null || motorData == null)
		{
			log.error("null data or motor - can't attach motor");
			return false;
		}
		
		if (motorData.length != 2 || motorData[0] == null || motorData[1] == null)
		{
			log.error("motor data must be of the folowing format - motorAttach(Integer PWMPin, Integer directionPin)");
			return false;
		}
		
		MotorData md = new MotorData();
		md.motor = motor;
		md.PWMPin = (Integer)motorData[0];
		md.directionPin = (Integer)motorData[1];
		motors.put(motor.getName(), md);
		motor.setController(this);
		serialSend(PINMODE, md.PWMPin, OUTPUT);
		serialSend(PINMODE, md.directionPin, OUTPUT);
		return true;

	}
	

	@Override
	public boolean motorDetach(String motorName) {
		boolean  ret = motors.containsKey(motorName);
		if (ret)
		{
			motors.remove(motorName);
		}
		return ret;
	}
	
	public void motorMove(String name) {
		
		MotorData md = motors.get(name);
		MotorControl m = md.motor;
		float power = m.getPowerLevel();
		
		if (power < 0)
		{
			serialSend(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted()?MOTOR_FORWARD:MOTOR_BACKWARD);
			serialSend(ANALOG_WRITE, md.PWMPin, Math.abs((int) (255*m.getPowerLevel())));
		} else if (power > 0)
		{
			serialSend(DIGITAL_WRITE, md.directionPin, m.isDirectionInverted()?MOTOR_BACKWARD:MOTOR_FORWARD);
			serialSend(ANALOG_WRITE, md.PWMPin, (int) (255*m.getPowerLevel()));
		} else {
			serialSend(ANALOG_WRITE, md.PWMPin, 0);
		}
	}

	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}


	// ----------- MotorController API End ----------------


	public boolean attach (String serviceName, Object...data)
	{
		log.info(String.format("attaching %s", serviceName));
		ServiceWrapper sw = Runtime.getServiceWrapper(serviceName);
		if (sw == null)
		{
			log.error(String.format("could not attach % - not found in registry", serviceName));
			return false;
		}
		if (sw.get() instanceof Servo) // Servo or ServoControl ???
		{
			if (data.length != 1)
			{
				log.error("can not attach a Servo without a pin number");
				return false;
			}
			if (!sw.isLocal())
			{
				log.error("servo controller and servo must be local");
				return false;
			}
			return servoAttach(serviceName, (Integer)(data[0]));
		}
		
		if (sw.get() instanceof Motor) // Servo or ServoControl ???
		{
			if (data.length != 2)
			{
				log.error("can not attach a Motor without a PWMPin & directionPin ");
				return false;
			}
			if (!sw.isLocal())
			{
				log.error("motor controller and motor must be local");
				return false;
			}
			return motorAttach(serviceName, data);
		}
		
		if (sw.get() instanceof ArduinoShield) // Servo or ServoControl ???
		{
			
			if (!sw.isLocal())
			{
				log.error("motor controller and motor must be local");
				return false;
			}
			
			return ((ArduinoShield)sw.get()).attach(this);
		}
		
		log.error("don't know how to attach");
		return false;
	}
	
	
	public String getSketch()
	{
		return this.sketch;		
	}
	
	public String setSketch(String newSketch)
	{
		sketch = newSketch;
		return sketch;
	}
	
	public String loadSketchFromFile(String filename)
	{
		String newSketch = FileIO.fileToString(filename);
		if (newSketch != null)
		{
			sketch = newSketch;
			return sketch;
		}
		return null;
	}
	
	
	@Override
	public Object[] getMotorData(String motorName) {
		MotorData md = motors.get(motorName);
		Object [] data = new Object[]{md.PWMPin, md.directionPin};
		return data;
	}
	
	public void softReset()
	{
		serialSend(SOFT_RESET, 0, 0);
	}
	
	@Override
	public void setServoSpeed(String servoName, Float speed) {
		if (speed == null || speed < 0.0f || speed > 1.0f)
		{
			log.error(String.format("speed %f out of bounds", speed));
			return;
		}
		serialSend(SET_SERVO_SPEED, servos.get(servoName).servoIndex, (int)(speed * 100));
	}
	
	@Override
	public void releaseService() {
		super.releaseService();
		disconnect();
	}
	
	public static void main(String[] args) throws RunnerException, SerialDeviceException, IOException {

		org.apache.log4j.BasicConfigurator.configure();
		//Logger.getRootLogger().setLevel(Level.INFO);

		/*
		for (int i = 0; i < 10000; ++i)
		{
			if (i%1 == 0)
			{
				log.info("mod 1");
			}
			if (i%10 == 0)
			{
				log.info("mod 10");
			}
			if (i%100 == 0)
			{
				log.info("mod 100");
			}
		}

		*/
		
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		
		Servo servo01 = new Servo("servo01");
		servo01.startService();
		
		Runtime.createAndStart("python", "Python");
		
		
		/*
		SensorMonitor sensors = new SensorMonitor("sensors");
		sensors.startService();
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
		//Runtime.createAndStart("python", "Python");

	}





}
