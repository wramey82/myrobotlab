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

import gnu.io.CommDriver;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorData;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 *  Implementation of a Arduino Service connected to MRL through a serial port.  
 *  The protocol is basically a pass through of system calls to the Arduino board.  Data 
 *  can be passed back from the digital or analog ports by request to start polling. The
 *  serial port can be wireless (bluetooth), rf, or wired. The communication protocol
 *  supported is in arduinoSerial.pde - located here :
 *  
 *	Should support nearly all Arduino board types  
 *   
 *   TODO:
 *   Data should be serializable in xml or properties, binary should already work
 *   
 *   References:
 *    <a href="http://www.arduino.cc/playground/Main/RotaryEncoders">Rotary Encoders</a> 
 *   @author GroG
 */

@Root
public class Arduino extends Service implements SerialPortEventListener,
		SensorData, DigitalIO, AnalogIO, ServoController, MotorController {
	
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Arduino.class.getCanonicalName());

	// non serializable serial object
	transient SerialPort serialPort;
	transient InputStream inputStream;
	transient OutputStream outputStream;
	transient static HashMap<String, CommDriver> customPorts = new HashMap<String, CommDriver>();

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
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

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

	// servo related
	public static final int SERVO_ANGLE_MIN = 0;
	public static final int SERVO_ANGLE_MAX = 180;
	public static final int SERVO_SWEEP = 10;
	public static final int MAX_SERVOS = 8; // TODO dependent on board?

	// servos
	boolean[] servosInUse = new boolean[MAX_SERVOS - 1];
	HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>(); 
	HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>(); 

	/**
	 *  list of serial port names from the system which the Arduino service is 
	 *  running
	 */
	public ArrayList<String> portNames = new ArrayList<String>(); 
	
	public Arduino(String n) {
		super(n, Arduino.class.getCanonicalName());
		// get ports - return array of strings
		// set port? / init port
		// detach port
		
		load(); // attempt to load config

		// attempt to get serial port based on there only being 1
		// or based on previous config
		
		// if there is only 1 port - attempt to initialize it
		portNames = getPorts();
		LOG.info("number of ports " + portNames.size());
		for (int j = 0; j < portNames.size(); ++j) {
			LOG.info(portNames.get(j));
		}

		if (portNames.size() == 1) { // heavy handed?
			LOG.info("only one serial port " + portNames.get(0));
			setPort(portNames.get(0));
		} else if (portNames.size() > 1) {
			if (portName != null && portName.length() > 0) {
				LOG.info("more than one port - last serial port is "
						+ portName);
				setPort(portName);
			} else {
				// idea - auto discovery attempting to auto-load arduinoSerial.pde
				LOG.warn("more than one port or no ports, and last serial port not set");
				LOG.warn("need user input to select from " + portNames.size()
						+ " possibilities ");
			}
		}

		for (int i = 0; i < servosInUse.length; ++i) {
			servosInUse[i] = false;
		}

	}
	
	/**
	 * @return the current serials port name or null if not opened
	 */
	public String getPortName()
	{
		if (serialPort != null)
		{
			return portName;
		}
		
		return null;
	}
	
	/**
	 * getPorts returns all serial ports, including the current port being used, 
	 * and any custom added ports (e.g. wii ports) 
	 * 
	 * @return array of port names which are currently being used, or serial, or custom
	 */
	public ArrayList<String> getPorts() {
		
		ArrayList<String> ports = new ArrayList<String>();
		CommPortIdentifier portId;
		// getPortIdentifiers - returns all ports "available" on the machine -
		// ie not ones already used
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		int cnt = 0;
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			LOG.info(inPortName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				ports.add(inPortName);
			}
		}

		// adding connected serial port if connected
		if (serialPort != null) {
			if (serialPort.getName() != null)
				ports.add(serialPort.getName());
		}

		// adding custom ports if they were previously added with addPortName
		for (String key : customPorts.keySet()) {
			// customPorts.get(key)
			ports.add(key);
		}

		return ports;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	/**
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
	 * 
	 * if outputStream is null: Important note to Fedora 13 make sure
	 * /var/lock/uucp /var/spool/uucp /var/spool/uucppublic and all are chown'd
	 * by uucp:uucp
	 */
	public synchronized void serialSend(int function, int param1, int param2) {
		LOG.info("serialSend fn " + function + " p1 " + param1 + " p2 "
				+ param2);
		try {
			outputStream.write(function);
			outputStream.write(param1);
			outputStream.write(param2); // 0 - 180
		} catch (IOException e) {
			LOG.error("serialSend " + e.getMessage());
		}

	}

	@ToolTip("sends an array of data to the serial port which an Arduino is attached to")
	public void serialSend(String data) {
		LOG.error("serialSend [" + data + "]");
		serialSend(data.getBytes());
	}

	public synchronized void serialSend(byte[] data) {
		try {
			for (int i = 0; i < data.length; ++i) {
				outputStream.write(data[i]);
			}
		} catch (IOException e) {
			LOG.error("serialSend " + e.getMessage());
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


	/*
	 * Servo Commands Arduino has a concept of a software Servo - and supports
	 * arrays Although Services could talk directly to the Arduino software
	 * servo in order to control the hardware the Servo service was created to
	 * store/handle the details, provide a common interface for other services
	 * regardless of the controller (Arduino in this case but could be any
	 * uController)
	 */
	
	// ---------------------------- Servo Methods Begin -----------------------
	
	/* servoAttach
	 * attach a servo to a pin
	 * @see org.myrobotlab.service.interfaces.ServoController#servoAttach(java.lang.Integer)
	 */
	public boolean servoAttach(Integer pin) { if (serialPort == null) {
			LOG.error("could not attach servo to pin " + pin
					+ " serial port in null - not initialized?");
			return false;
		}
		// serialPort == null ??? make sure you chown it correctly !
		LOG.info("servoAttach (" + pin + ") to " + serialPort.getName()
				+ " function number " + SERVO_ATTACH);

		/*
		 * soft servo if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin !=
		 * 10 && pin != 11) { LOG.error(pin + " not valid for servo"); }
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

		LOG.error("servo " + pin + " attach failed - no idle servos");
		return false;
	}

	public boolean servoDetach(Integer pin) {
		LOG.info("servoDetach (" + pin + ") to " + serialPort.getName()
				+ " function number " + SERVO_DETACH);

		if (pinToServo.containsKey(pin)) {
			int removeIdx = pinToServo.get(pin);
			serialSend(SERVO_DETACH, pinToServo.get(pin), 0);
			servosInUse[removeIdx] = false;

			return true;
		}

		LOG.error("servo " + pin + " detach failed - not found");
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
		if (serialPort == null) // TODO - remove this only for debugging without
		// Arduino
		{
			return;
		}

		LOG.info("servoWrite (" + pin + "," + angle + ") to "
				+ serialPort.getName() + " function number " + SERVO_WRITE);

		if (angle < SERVO_ANGLE_MIN || angle > SERVO_ANGLE_MAX) {
			// LOG.error(pin + " angle " + angle + " request invalid");
			return;
		}

		serialSend(SERVO_WRITE, pinToServo.get(pin), angle);

	}

	// ---------------------------- Servo Methods End -----------------------
	
	// ---------------------- Serial Control Methods Begin ------------------
	  public void setDTR(boolean state) {
		    serialPort.setDTR(state);
	  }

	  public void setRTS(boolean state) {
		  serialPort.setRTS(state);
	  }
	  	
	public void releaseSerialPort() {
		LOG.debug("releaseSerialPort");
	    try {
	        // do io streams need to be closed first?
	        if (inputStream != null) inputStream.close();
	        if (outputStream != null) outputStream.close();

	      } catch (Exception e) {
	        e.printStackTrace();
	      }
	      inputStream = null;
	      outputStream = null;

	      /* what a f*ing mess rxtxbug*/
	      /*
	      new Thread(){
	    	    @Override
	    	    public void run(){
	    	        serialPort.removeEventListener();
	    	        serialPort.close();
	    		      serialPort = null;
	    	    }
	    	}.start();
	     */
	      
	      if (serialPort != null)
	      {
	    	  LOG.error("WARNING - native code has bug which blocks forever - if you dont see next statement");
	    	  serialPort.removeEventListener();
	    	  serialPort.close();
	    	  LOG.error("WARNING - Hurray! successfully closed Yay!");
	      }
	      
	      try {
	        //if (serialPort != null) serialPort.close();  // close the port
	    	Thread.sleep(300); // wait for thread to terminate

	      } catch (Exception e) {
	        e.printStackTrace();
	      }

	    LOG.info("released port");
	}

	/**
	 * setPort - sets the serial port to the requested port name
	 * and initially attempts to open with 115200 8N1
	 * @param inPortName name of serial port 
	 * 			Linux [ttyUSB0, ttyUSB1, ... S0, S1, ...]
	 * 			Windows [COM1, COM2, ...]
	 * 			OSX [???]
	 * @return if successful
	 * 
	 */
	public boolean setPort(String inPortName) {
		LOG.debug("setPort requesting [" + inPortName + "]");

		if (serialPort != null) 
		{
			releaseSerialPort();
		}
		
		if (inPortName == null || inPortName.length() == 0)
		{
			LOG.info("setting serial to nothing");
			return true;
		}

		try {
			CommPortIdentifier portId;

			if (customPorts.containsKey(inPortName)) { // adding custom port
														// (wiicomm) to query
														// right back
				CommPortIdentifier.addPortName(inPortName,
						CommPortIdentifier.PORT_SERIAL, customPorts
								.get(inPortName));
			}

			Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
			serialPort = null;

			while (portList.hasMoreElements()) {
				portId = (CommPortIdentifier) portList.nextElement();

				LOG.debug("checking port " + portId.getName());
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					LOG.debug("is serial");
					if (portId.getName().equals(inPortName)) {
						LOG.debug("matches " + inPortName);
						// System.out.println("looking for "+iname);
						serialPort = (SerialPort) portId.open("robot overlords", 2000);
						inputStream = serialPort.getInputStream();
						outputStream = serialPort.getOutputStream();

						serialPort.addEventListener(this);
						serialPort.notifyOnDataAvailable(true);

						// 115200 wired, 2400 IR ?? VW 2000??
						serialPort.setSerialPortParams(baudRate,
														dataBits, 
														stopBits,
														parity);

						Thread.sleep(200); // the initialization of the hardware
											// takes a little time

						// portName = serialPort.getName(); BUG - serialPort.getName != the name which is requested
						// Windows you ask for "COM1" but when you ask for it back you get "/.//COM1"
						portName = inPortName;
						LOG.debug("opened " + getPortString());
						save(); // successfully bound to port - saving
						broadcastState(); // state has changed let everyone know
						break;

					}
				}
			}
		} catch (Exception e) {
			Service.logException(e);
		}

		if (serialPort == null) {
			LOG.error(inPortName + " serialPort is null - bad init?");
			return false;
		}

		LOG.info(inPortName + " ready");
		return true;
	}

	public String getPortString()
	{
		if (serialPort != null)
		{
			try {
				return portName  + "/" // can't use serialPort.getName() 
						+ serialPort.getBaudRate() + "/" 
						+ serialPort.getDataBits() + "/"
						+ serialPort.getParity() + "/"
						+ serialPort.getStopBits();
			} catch (Exception e) {
				Service.logException(e);
				return null;
			}
		} else {
			return null;
		}
	}
	
	public boolean setBaud(int baudRate)
	{
		if (serialPort == null)
		{
			LOG.error("setBaudBase - serialPort is null");
			return false;
		}
		try {
			// boolean ret = serialPort.set.setBaudBase(baudRate); // doesnt work - operation not allowed
			boolean ret = setSerialPortParams(baudRate, serialPort.getDataBits(), serialPort.getStopBits(), serialPort.getParity());
			this.baudRate = baudRate;
			save();
			broadcastState(); // state has changed let everyone know
			return ret;
		} catch (Exception e) {
			Service.logException(e);
		}
		return false;
	}
	
	public int getBaudRate()
	{
		return baudRate;
	}
	
	public boolean setSerialPortParams (int baudRate, int dataBits, int stopBits, int parity)
	{
		if (serialPort == null)
		{
			LOG.error("setSerialPortParams - serialPort is null");
			return false;
		}
		
		try {
			serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parity);
		} catch (UnsupportedCommOperationException e) {
			Service.logException(e);
		}
		
		return true;
	}
	
	public void digitalReadPollStart(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + serialPort.getName());
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}
	// ---------------------- Serial Control Methods End ------------------
	// ---------------------- Protocol Methods Begin ------------------

	public void digitalReadPollStop(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + serialPort.getName());
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	public void digitalWrite(IOData io) {
		digitalWrite(io.address, io.value);
	}

	public void digitalWrite(Integer address, Integer value) {
		LOG.info("digitalWrite (" + address + "," + value + ") to "
				+ serialPort.getName() + " function number " + DIGITAL_WRITE);
		serialSend(DIGITAL_WRITE, address, value);
	}

	public void pinMode(IOData io) {
		pinMode(io.address, io.value);
	}

	public void pinMode(Integer address, Integer value) {
		LOG.info("pinMode (" + address + "," + value + ") to "
				+ serialPort.getName() + " function number " + PINMODE);
		serialSend(PINMODE, address, value);
	}

	public void analogWrite(IOData io) {
		analogWrite(io.address, io.value);
	}

	public void analogWrite(Integer address, Integer value) {
		LOG.info("analogWrite (" + address + "," + value + ") to "
				+ serialPort.getName() + " function number " + ANALOG_WRITE);
		serialSend(ANALOG_WRITE, address, value);
	}

	public PinData publishPin(PinData p) {
		LOG.info(p);
		return p;
	}

	// DEPRICATE !
	public PinData readServo(PinData p) {
		// TODO - translation back to pin identifier
		// e.g. pin 6 could be servo[0] - sending back we need to put pin back
		// pin is actually servo index until this translation bleh
		p.pin = servoToPin.get(p.pin);
		LOG.info(p);
		return p;
	}

	public static void addPortName(String n, int portType, CommDriver cpd) {
		// it IS misleading to have addPortName put the port in, but not
		// available through getPortIdentifiers !
		// http://en.wikibooks.org/wiki/Serial_Programming/Serial_Java -
		// The method CommPortIdentifier.addPortName() is misleading,
		// since driver classes are platform specific and their
		// implementations are not part of the public API

		customPorts.put(n, cpd);
		// CommPortIdentifier.addPortName(n, portType, cpd); // this does
		// nothing of relevance - because it does not
		// persist across getPortIdentifier calls
	}

	// TODO - blocking call which waits for serial return
	// not thread safe - use mutex? - block on expected byte count?
	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it

	public String readSerialMessage(String s) {
		return s;
	}

	boolean rawReadMsg = false;
	int rawReadMsgLength = 4;

	// char rawMsgBuffer

	public void setRawReadMsg(Boolean b) {
		rawReadMsg = b;
	}

	public void setReadMsgLength(Integer length) {
		rawReadMsgLength = length;
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:

			try {
				// read data

				byte[] msg = new byte[rawReadMsgLength];
				int newByte;
				int numBytes = 0;
				int totalBytes = 0;

				// TODO - refactor big time ! - still can't dynamically change
				// msg length
				// also need a byLength or byStopString - with options to remove
				// delimiter
				while ((newByte = inputStream.read()) >= 0) {
					msg[numBytes] = (byte) newByte;
					++numBytes;
					// totalBytes += numBytes;

					// LOG.info("read " + numBytes + " target msg length " +
					// rawReadMsgLength);

					if (numBytes == rawReadMsgLength) {
						/*
						 * Diagnostics StringBuffer b = new StringBuffer(); for
						 * (int i = 0; i < rawReadMsgLength; ++i) {
						 * b.append(msg[i] + " "); }
						 * 
						 * LOG.error("msg" + b.toString());
						 */
						totalBytes += numBytes;

						if (rawReadMsg) {
							// raw protocol

							String s = new String(msg);
							LOG.info(s);
							invoke("readSerialMessage", s);
						} else {

							// mrl protocol

							PinData p = new PinData();
							p.time = System.currentTimeMillis();
							p.function = msg[0];
							p.pin = msg[1];
							// java assumes signed
							// http://www.rgagnon.com/javadetails/java-0026.html
							p.value = (msg[2] & 0xFF) << 8; // MSB - (Arduino
															// int is 2 bytes)
							p.value += (msg[3] & 0xFF); // LSB

							// if (p.function == SERVO_READ) { COMPLETELY
							// DEPRICATED !!!
							// invoke("readServo", p);
							// } else {
							if (p.function == ANALOG_VALUE) {
								p.type = 1;
							}
							p.source = this.name;
							invoke(SensorData.publishPin, p);
							// }
						}

						// totalBytes = 0;
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

	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it
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
		// in read
	}

	public void analogReadPollingStop(Integer pin) {
		serialSend(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
		// in read
	}

	/*
	 * Another means of distributing the data would be to publish to individual
	 * functions which might be useful for some reason in the future - initially
	 * this was started because of the overlap on the Arduino board where the
	 * analog pin addresses overlapped the digital vs 14 - 19 analog pins they
	 * are addressed 0 - 1 with analog reads
	 */

	class MotorData {
		boolean isAttached = false;
	}
	
	HashMap<String, MotorData> motorMap = new HashMap<String, MotorData>();

	// @Override - only in Java 1.6 - its only a single reference not all
	// supertypes define it
	public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
		// set the pinmodes on the 2 pins
		if (serialPort != null) {
			pinMode(PWMPin, Arduino.OUTPUT);
			pinMode(DIRPin, Arduino.OUTPUT);
		} else {
			LOG.error("attempting to attach motor before serial connection to "
					+ name + " Arduino is ready");
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
		releaseSerialPort();
	}

	public Vector<Integer> getOutputPins()
	{
		// TODO - base on "type"
		Vector<Integer> ret = new Vector<Integer>();
		for (int i = 2; i < 13; ++i )
		{
			ret.add(i);
		}
		return ret;
	}
	
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
	
		//Arduino arduino = (Arduino) ServiceFactory.create("arduino", "Arduino");
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		//arduino.setPort("/dev/ttyS50");
		
		//Motor left = new Motor("left");
		//left.startService();

		arduino.save();
		
		Servo hand = new Servo("right");
		hand.startService();

		Servo left = new Servo("left");
		left.startService();

		Servo neck = new Servo("neck");
		neck.startService();
		
		GUIService gui = new GUIService("lapgui");
		gui.startService();
		gui.display();
			
	}
	
}
