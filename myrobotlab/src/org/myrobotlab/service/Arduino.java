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
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.arduino.PApplet;
//import org.myrobotlab.arduino.Sketch2;
import org.myrobotlab.arduino.compiler.AvrdudeUploader;
import org.myrobotlab.arduino.compiler.Compiler2;
import org.myrobotlab.arduino.compiler.Preferences2;
import org.myrobotlab.arduino.compiler.RunnerException;
import org.myrobotlab.arduino.compiler.Target;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.data.PinState;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorData;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Implementation of a Arduino Service connected to MRL through a serial port.
 * The protocol is basically a pass through of system calls to the Arduino
 * board. Data can be passed back from the digital or analog ports by request to
 * start polling. The serial port can be wireless (bluetooth), rf, or wired. The
 * communication protocol supported is in arduinoSerial.pde - located here :
 * 
 * Should support nearly all Arduino board types
 * 
 * References: <a
 * href="http://www.arduino.cc/playground/Main/RotaryEncoders">Rotary
 * Encoders</a>
 * 
 * FIXME - Preference2, Preferences, the xml parameters, "board", type ALL need to be reconciled to one
 * normalized value and container !
 * @author GroG
 */

@Root
public class Arduino extends Service implements SerialDeviceEventListener, SensorData, DigitalIO, AnalogIO,
		ServoController, MotorController, SerialDeviceService {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(Arduino.class.getCanonicalName());
	public static final int REVISION = 100;
	// FIXME - Add Android BlueTooth as possible Serial Device - remove
	// ArduinoBT
	SerialDevice serialDevice;

	static HashSet<File> libraries;

	static boolean commandLine;
	static public HashMap<String, Target> targetsTable;

	static File buildFolder;
	static public HashMap<String, File> importToLibraryTable;

	// FIXME - have SerialDevice read by length or by term string
	boolean rawReadMsg = false;
	int rawReadMsgLength = 4;

	@Element
	String board = "";
	@Element
	String portName = "";
	@Element
	int baudRate = 115200;
	@Element
	int dataBits = 8;
	@Element
	int parity = 0;
	@Element
	int stopBits = 1;

	// imported Arduino constants
	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;

	public static final int INPUT = 0x0;
	public static final int OUTPUT = 0x1;
	
	public static final int TCCR0B = 0x25; // register for pins 6,7
	public static final int TCCR1B = 0x2E; // register for pins 9,10
	public static final int TCCR2B = 0xA1; // register for pins 3,11

	// serial protocol functions
	public static final int DIGITAL_WRITE = 0;
	public static final int ANALOG_WRITE = 2;
	public static final int ANALOG_VALUE = 3;
	public static final int PINMODE = 4;
	public static final int PULSE_IN = 5;
	public static final int SERVO_ATTACH = 6;
	public static final int SERVO_WRITE = 7;
	public static final int SERVO_SET_MAX_PULSE = 8;
	public static final int SERVO_DETACH = 9;
	public static final int SET_PWM_FREQUENCY = 11;
	public static final int SERVO_READ = 12;
	public static final int ANALOG_READ_POLLING_START = 13;
	public static final int ANALOG_READ_POLLING_STOP = 14;
	public static final int DIGITAL_READ_POLLING_START = 15;
	public static final int DIGITAL_READ_POLLING_STOP = 16;
	public static final int SET_ANALOG_PIN_SENSITIVITY = 17;
	public static final int SET_ANALOG_PIN_GAIN = 18;

	// servo related
	public static final int SERVO_ANGLE_MIN = 0;
	public static final int SERVO_ANGLE_MAX = 180;
	public static final int SERVO_SWEEP = 10;
	public static final int MAX_SERVOS = 8;

	// servos
	boolean[] servosInUse = new boolean[MAX_SERVOS - 1];
	HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>();
	HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>();

	// from the Arduino IDE :)
	Compiler2 compiler;
	AvrdudeUploader uploader;
	
	// compile / upload
	private String buildPath ="";
	private String programName = "";
	private String program = "";

	/**
	 * list of serial port names from the system which the Arduino service is
	 * running
	 */
	public ArrayList<String> portNames = new ArrayList<String>();

	public Arduino(String n) {
		super(n, Arduino.class.getCanonicalName());
		load(); // attempt to load last configuration saved

		// attempt to get serial port based on there only being 1
		// or based on previous configuration

		// if there is only 1 port - attempt to initialize it
		portNames = getPorts();
		log.info("number of ports " + portNames.size());
		for (int j = 0; j < portNames.size(); ++j) {
			log.info(portNames.get(j));
		}

		if (portNames.size() == 1) { // heavy handed? YES - TODO - fix - base on previous preference
			log.info("only one serial port " + portNames.get(0));
			setPort(portNames.get(0));
		} else if (portNames.size() > 1) {
			if (portName != null && portName.length() > 0) {
				log.info("more than one port - last serial port is " + portName);
				setPort(portName);
			} else {
				// arduinoSerial.pde
				log.warn("more than one port or no ports, and last serial port not set");
				log.warn("need user input to select from " + portNames.size() + " possibilities ");
			}
		}

		for (int i = 0; i < servosInUse.length; ++i) {
			servosInUse[i] = false;
		}
		
	    Preferences2.init(null); 

		Preferences2.set("sketchbook.path", ".myrobotlab");
		Preferences2.set("board", "mega2560"); // FIXME - get "real" board type - all info in boards.txt
		Preferences2.set("board", "atmega328"); 
		
		Preferences2.set("target", "arduino"); // FIXME - board type
		
		// FIXME - set on load() & change
		if (getPortName() != null)
		{
				Preferences2.set("serial.port", getPortName());
		}
		Preferences2.setInteger("serial.debug_rate", 57600);
		Preferences2.set("serial.parity", "N"); // f'ing stupid, 
		Preferences2.setInteger("serial.databits", 8);
		Preferences2.setInteger("serial.stopbits", 1); // f'ing weird 1,1.5,2
		Preferences2.setBoolean("upload.verbose", true);

		// Get paths for the libraries and examples in the Processing folder
		// String workingDirectory = System.getProperty("user.dir");
		File examplesFolder = getContentFile("examples");
		File librariesFolder = getContentFile("libraries");
		File toolsFolder = getContentFile("tools");

		// Get the sketchbook path, and make sure it's set properly
		String sketchbookPath = Preferences2.get("sketchbook.path");

		// FIXME - all below should be done inside Compiler2
		try {

			targetsTable = new HashMap<String, Target>();
			loadHardware(getHardwareFolder());
			loadHardware(getSketchbookHardwareFolder());
			addLibraries(librariesFolder);
			File sketchbookLibraries = getSketchbookLibrariesFolder();
			addLibraries(sketchbookLibraries);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		compiler = new Compiler2(this);
		uploader = new AvrdudeUploader(this);

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

		for (String target : list) {
			File subfolder = new File(folder, target);
			targetsTable.put(target, new Target(target, subfolder));
		}
	}

	public String getPortName() {
		if (serialDevice != null) {
			return portName;
		}

		return null;
	}

	public ArrayList<String> getPorts() {

		ArrayList<String> ports = new ArrayList<String>();
		SerialDevice portId;
		// getPortIdentifiers - returns all ports "available" on the machine -
		// ie not ones already used
		ArrayList<SerialDevice> portList = SerialDeviceFactory.getSerialDevices();
		for (int i = 0; i < portList.size(); ++i) {
			portId = portList.get(i);
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getPortType() == SerialDevice.PORTTYPE_SERIAL) {
				ports.add(inPortName);
			}
		}

		// adding connected serial port if connected
		if (serialDevice != null) {
			if (serialDevice.getName() != null)
				ports.add(serialDevice.getName());
		}

		// adding custom ports if they were previously added with addPortName
		/*
		 * for (String key : customPorts.keySet()) { // customPorts.get(key)
		 * ports.add(key); }
		 */

		return ports;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public synchronized void serialSend(int function, int param1, int param2) {
		log.info("serialSend fn " + function + " p1 " + param1 + " p2 " + param2);
		try {
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

	// ---------------------------- Servo Methods Begin -----------------------

	/*
	 * servoAttach attach a servo to a pin
	 * 
	 * @see
	 * org.myrobotlab.service.interfaces.ServoController#servoAttach(java.lang
	 * .Integer)
	 */
	public boolean servoAttach(Integer pin) {
		if (serialDevice == null) {
			log.error("could not attach servo to pin " + pin + " serial port in null - not initialized?");
			return false;
		}
		// serialPort == null ??? make sure you chown it correctly !
		log.info("servoAttach (" + pin + ") to " + serialDevice.getName() + " function number " + SERVO_ATTACH);

		/*
		 * soft servo if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin !=
		 * 10 && pin != 11) { log.error(pin + " not valid for servo"); }
		 */

		for (int i = 0; i < servosInUse.length; ++i) {
			if (!servosInUse[i]) {
				servosInUse[i] = true;
				pinToServo.put(pin, i);
				servoToPin.put(i, pin);
				serialSend(SERVO_ATTACH, pinToServo.get(pin), pin);
				return true;
			}
		}

		log.error("servo " + pin + " attach failed - no idle servos");
		return false;
	}

	public boolean servoDetach(Integer pin) {
		log.info("servoDetach (" + pin + ") to " + serialDevice.getName() + " function number " + SERVO_DETACH);

		if (pinToServo.containsKey(pin)) {
			int removeIdx = pinToServo.get(pin);
			serialSend(SERVO_DETACH, pinToServo.get(pin), 0);
			servosInUse[removeIdx] = false;

			return true;
		}

		log.error("servo " + pin + " detach failed - not found");
		return false;

	}

	/*
	 * servoWrite(IOData io) interface that allows routing with a single
	 * parameter TODO - how to "route" to multiple parameters
	 */
	public void servoWrite(IOData io) {
		servoWrite(io.address, io.value);
	}

	// Set the angle of the servo in degrees, 0 to 180.
	// @Override - TODO - make interface - implements ServoController interface
	public void servoWrite(Integer pin, Integer angle) {
		if (serialDevice == null) {
			log.error("serialPort is NULL !");
			return;
		}

		log.info("servoWrite (" + pin + "," + angle + ") to " + serialDevice.getName() + " function number "
				+ SERVO_WRITE);

		if (angle < SERVO_ANGLE_MIN || angle > SERVO_ANGLE_MAX) {
			// log.error(pin + " angle " + angle + " request invalid");
			return;
		}

		serialSend(SERVO_WRITE, pinToServo.get(pin), angle);

	}

	// ---------------------------- Servo Methods End -----------------------

	// ---------------------- Serial Control Methods Begin ------------------

	public boolean setPort(String inPortName) {
		log.info("setPort requesting [" + inPortName + "]");

		if (serialDevice != null) {
			serialDevice.close();
			serialDevice = null;
		}

		if (inPortName == null || inPortName.length() == 0) {
			log.info("setting serial to nothing");
			return true;
		}

		try {
			SerialDevice device;

			ArrayList<SerialDevice> portList = SerialDeviceFactory.getSerialDevices();
			for (int i = 0; i < portList.size(); ++i) {
				device = portList.get(i);

				log.debug("checking port " + device.getName());
				if (device.getPortType() == SerialDevice.PORTTYPE_SERIAL) {
					log.debug("is serial");
					if (device.getName().equals(inPortName)) {
						log.debug("matches " + inPortName);
						serialDevice = device;
						serialDevice.open();

						serialDevice.addEventListener(this);
						serialDevice.notifyOnDataAvailable(true);

						// 115200 wired, 2400 IR ?? VW 2000??
						serialDevice.setParams(baudRate, dataBits, stopBits, parity);

						portName = inPortName;
						//log.info("opened " + getPortString());
						save(); // successfully bound to port - saving
						Preferences2.set("serial.port", portName);
						broadcastState(); // state has changed let everyone know
						break;

					}
				}
			}
		} catch (Exception e) {
			Service.logException(e);
		}

		if (serialDevice == null) {
			log.error(inPortName + " serialPort is null - bad init?");
			return false;
		}

		log.info(inPortName + " ready");
		return true;
	}


	// ---------------------- Serial Control Methods End ------------------
	// ---------------------- Protocol Methods Begin ------------------


	public void digitalReadPollStart(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}
	
	public void digitalReadPollStop(Integer address) {

		log.info("digitalRead (" + address + ") to " + serialDevice.getName());
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	public IOData digitalWrite(IOData io) {
		digitalWrite(io.address, io.value);
		return io;
	}

	public void digitalWrite(Integer address, Integer value) {
		log.info("digitalWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number "
				+ DIGITAL_WRITE);
		serialSend(DIGITAL_WRITE, address, value);
	}

	public void pinMode(IOData io) {
		pinMode(io.address, io.value);
	}

	public void pinMode(Integer address, Integer value) {
		log.info("pinMode (" + address + "," + value + ") to " + serialDevice.getName() + " function number " + PINMODE);
		serialSend(PINMODE, address, value);
	}

	public IOData analogWrite(IOData io) {
		analogWrite(io.address, io.value);
		return io;
	}

	public void analogWrite(Integer address, Integer value) {
		log.info("analogWrite (" + address + "," + value + ") to " + serialDevice.getName() + " function number "
				+ ANALOG_WRITE);
		serialSend(ANALOG_WRITE, address, value);
	}

	public PinData publishPin(PinData p) {
		log.info(p);
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

	public String getType() {
		return Arduino.class.getCanonicalName();
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

	public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
		// set the pinmodes on the 2 pins
		if (serialDevice != null) {
			pinMode(PWMPin, PinState.OUTPUT);
			pinMode(DIRPin, PinState.OUTPUT);
		} else {
			log.error("attempting to attach motor before serial connection to " + name + " Arduino is ready");
		}

	}

	public void motorDetach(String name) {
		// TODO Auto-generated method stub

	}

	public void motorMove(String name, Integer amount) {
		// TODO Auto-generated method stub

	}

	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getToolTip() {
		return "<html>Arduino is a service which interfaces with an Arduino micro-controller.<br>"
				+ "This interface can operate over radio, IR, or other communications,<br>"
				+ "but and appropriate .PDE file must be loaded into the micro-controller.<br>"
				+ "See http://myrobotlab.org/communication for details";
	}

	public void stopService() {
		super.stopService();
		if (serialDevice != null)
		{
			serialDevice.close();
		}
	}

	public Vector<Integer> getOutputPins() {
		Vector<Integer> ret = new Vector<Integer>();
		for (int i = 2; i < 13; ++i) {
			ret.add(i);
		}
		return ret;
	}

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

							// mrl protocol
							PinData p = new PinData();
							// p.time = System.currentTimeMillis();
							p.method = msg[0];
							p.pin = msg[1];
							// java assumes signed
							// http://www.rgagnon.com/javadetails/java-0026.html
							p.value = (msg[2] & 0xFF) << 8; // MSB - (Arduino
															// int is 2 bytes)
							p.value += (msg[3] & 0xFF); // LSB
							p.source = this.getName();
							invoke(SensorData.publishPin, p);
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

	// FIXME !!! - REMOVE ALL BELOW - except compile(File) compile(String) upload(File) upload(String)
	// supporting methods for Compiler & UPloader may be necessary
	
	

	static public String getAvrBasePath() {
		if (Platform.isLinux()) {
			return ""; // avr tools are installed system-wide and in the path
		} else {
			return getHardwarePath() + File.separator + "tools" + File.separator + "avr" + File.separator + "bin"
					+ File.separator;
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


	static public Map<String, String> getBoardPreferences() {
		Target target = getTarget();
		if (target == null)
			return new LinkedHashMap();
		Map map = target.getBoards();
		if (map == null)
			return new LinkedHashMap();
		map = (Map) map.get(Preferences2.get("board"));
		if (map == null)
			return new LinkedHashMap();
		return map;
	}

	static public Target getTarget() {
		return targetsTable.get(Preferences2.get("target"));
	}

	static public String getSketchbookLibrariesPath() {
		return getSketchbookLibrariesFolder().getAbsolutePath();
	}

	static public File getSketchbookHardwareFolder() {
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
			showError("You forgot your sketchbook", "Arduino cannot run because it could not\n"
					+ "create a folder to store your sketchbook.", null);
		}

		return sketchbookFolder;
	}

	static public String showError(String error, String desc, Exception e) {
		return error;
	}

	static public File getSketchbookLibrariesFolder() {
		return new File(getSketchbookFolder(), "libraries");
	}

	static public File getSketchbookFolder() {
		return new File(Preferences2.get("sketchbook.path"));
	}

	static public File getBuildFolder() {
		if (buildFolder == null) {
			String buildPath = Preferences2.get("build.path");
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

	static public void removeDescendants(File dir) {
		if (!dir.exists())
			return;

		String files[] = dir.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equals(".") || files[i].equals(".."))
				continue;
			File dead = new File(dir, files[i]);
			if (!dead.isDirectory()) {
				if (!Preferences2.getBoolean("compiler.save_build_files")) {
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
	static public void removeDir(File dir) {
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

	static public File getSettingsFile(String filename) {
		return new File(getSettingsFolder(), filename);
	}

	static public File getSettingsFolder() {
		File settingsFolder = null;

		String preferencesPath = Preferences2.get("settings.path");
		if (preferencesPath != null) {
			settingsFolder = new File(preferencesPath);

		} else {
			try {
				settingsFolder = new File(".myrobotLab");// platform.getSettingsFolder();
			} catch (Exception e) {
				showError("Problem getting data folder", "Error getting the Arduino data folder.", e);
			}
		}

		// create the folder if it doesn't exist already
		if (!settingsFolder.exists()) {
			if (!settingsFolder.mkdirs()) {
				showError("Settings issues", "Arduino cannot run because it could not\n"
						+ "create a folder to store your settings.", null);
			}
		}
		return settingsFolder;
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
			String packages[] = Compiler2.headerListFromIncludePath(subfolder.getAbsolutePath());
			for (String pkg : packages) {
				importToLibraryTable.put(pkg, subfolder);
			}

			ifound = true;
		}
		return ifound;
	}
	
	public String showMessage (String msg, String desc)
	{
		log.info("showMessage " + msg);
		return msg;
	}
	
	public SerialDevice getSerialDevice()
	{
		return serialDevice;
	}
	
	@Override
	public ArrayList<String> getSerialDeviceNames() {
		return portNames;
	}

	@Override
	public boolean setSerialDevice(String name, int rate, int databits, int stopbits, int parity) {
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (sd != null)
			{
				serialDevice = sd;
				return true;
			}
		} catch (SerialDeviceException e) {
			logException(e);
		}
		return false;
	}
	
	public int setCompilingProgress(int progress)
	{
		log.info(String.format("progress %d ", progress));
		return progress;
	}
	
	public String createBuildPath(String programName)
	{
		// make a work/tmp directory if one doesn't exist - TODO - new time stamp?
		Date d = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
		formatter.setCalendar(cal);

		String tmpdir = String.format("obj%s%s.%s",File.separator,programName,formatter.format(d));
		new File(tmpdir).mkdirs();
		
		return tmpdir;
		
	}
	
	
	public void compile(String programName, String program)
	{
		// FYI - not thread safe
		this.programName = programName;
		this.buildPath = createBuildPath(programName);
		this.program = program;
		
		try {
			compiler.compile(programName, program, buildPath, true);
		} catch (RunnerException e) {
			logException(e);
			invoke("compilerError", e.getMessage());
		}
		log.debug(program);
	}
	
	public String compilerError (String error)
	{
		return error;
	}
	
	//public void upload(String file) throws RunnerException, SerialDeviceException
	// FIXME - stupid - should take a binary string or the path to the .hex file
	public void upload() throws RunnerException, SerialDeviceException
	{
		//uploader.uploadUsingPreferences("C:\\mrl\\myrobotlab\\obj", "MRLComm", false);
		uploader.uploadUsingPreferences(buildPath, programName, false);
	}
	
	public static void main(String[] args) throws RunnerException, SerialDeviceException {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Arduino arduino = new Arduino("arduino");
		arduino.startService();		
/*		
		String code = FileIO.fileToString(".\\arduino\\libraries\\MyRobotLab\\examples\\MRLComm\\MRLComm.ino");
		
		arduino.compile("MRLComm", code);
		arduino.setPort("COM6"); //- test re-entrant
		arduino.upload();
*/		
		// FIXME - I BELIEVE THIS LEAVES THE SERIAL PORT IN A CLOSED STATE !!!! 
		
		
//		arduino.compileAndUploadSketch(".\\arduino\\libraries\\MyRobotLab\\examples\\MRLComm\\MRLComm.ino");
//		arduino.pinMode(44, Arduino.OUTPUT);
//		arduino.digitalWrite(44, Arduino.HIGH);
		
		Runtime.createAndStart("gui01", "GUIService");


	}


}
