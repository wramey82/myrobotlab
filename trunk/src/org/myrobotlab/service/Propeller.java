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
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
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
import java.util.TooManyListenersException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.SensorData;
import org.myrobotlab.service.interfaces.ServoController;

/*
 *  Currently supports:
 *   	Arduino Duemilanove - http://arduino.cc/en/Main/ArduinoBoardDuemilanove
 *   
 *   - Find Arduino Message set - DigitalWrite (pin, data?)
 *   - ArduinoProgram HashMap<Key, Program>
 *   - loadProgram (Key)
 *   - key - default key & program   
 *   
 *   
 *   TODO - attach to ttyUSB ----> starts "attach" to gui
 *   
 *   
 *   References:
 *   http://www.arduino.cc/playground/Main/RotaryEncoders
 *   
 */
public class Propeller extends Service implements SerialPortEventListener,
		SensorData, DigitalIO, AnalogIO, ServoController, MotorController {

	public final static Logger LOG = Logger.getLogger(Propeller.class
			.getCanonicalName());

	SerialPort serialPort;
	InputStream inputStream;
	OutputStream outputStream;
	
	String lastSerialPortName; // 

	public static final int HIGH = 0x1;
	public static final int LOW = 0x0;
	public static final int OUTPUT = 0x1;
	public static final int INPUT = 0x0;

	public static final int TCCR0B = 0x25; // register for pins 6,7
	public static final int TCCR1B = 0x2E; // register for pins 9,10
	public static final int TCCR2B = 0xA1; // register for pins 3,11

	// functions
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

	public boolean repeatSerialCommand = false;
	public int repeatCommandNumber = 3; // TODO - move to config - number of
										// additional serial sends

	boolean[] servosInUse = new boolean[MAX_SERVOS - 1];
	HashMap<Integer, Integer> pinToServo = new HashMap<Integer, Integer>(); // mapping
																			// of
																			// pin
																			// to
																			// servo
																			// index
	HashMap<Integer, Integer> servoToPin = new HashMap<Integer, Integer>(); // mapping
																			// of
																			// pin
																			// to
																			// servo
																			// index

	static HashMap<String, CommDriver> customPorts = new HashMap<String, CommDriver>(); // a
																						// list
																						// of
																						// custom
																						// ports

	public Propeller(String n) {
		super(n, Propeller.class.getCanonicalName());
		// get ports - return array of strings
		// set port? / init port
		// detach port

		// if there is only 1 port - attempt to initialize it
		ArrayList<String> p = getPorts();
		if (p.size() == 1) {
			setSerialPort(p.get(0));
		} else if (p.size() > 1) {
			if (lastSerialPortName != null)
			{
				setSerialPort(lastSerialPortName);
			}
		}
		
		for (int i = 0; i < servosInUse.length; ++i) {
			servosInUse[i] = false;
		}

	}

	/*
	 * getPorts - get ALL yes ALL the ports available
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPorts() {

		ArrayList<String> ports = new ArrayList<String>();
		CommPortIdentifier portId;
		// getPortIdentifiers - returns all ports "available" on the machine -
		// ie not ones already used
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String portName = portId.getName();
			LOG.info(portName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				// filter - we don't want Fedora's ttyS* devices
				// if ((portName.length() > 9) && (portName.substring(0,
				// 9).compareTo("/dev/ttyS") == 0))
				// {
				// LOG.warn("found " + portName + " but disregarding");
				// } else {
				ports.add(portName);
				// }
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

	public String getCurrentPort() {
		if (serialPort == null || serialPort.getName() == null) {
			return "";
		} else {
			return serialPort.getName();
		}
	}

	@Override
	public void loadDefaultConfiguration() {

		lastSerialPortName = cfg.get("lastSerialPortName","");
	}

	/*
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
	 * 
	 * if outputStream is null: Important note to Fedora 13 make sure
	 * /var/lock/uucp /var/spool/uucp /var/spool/uucppublic and all are chown'd
	 * by uucp:uucp
	 */

	// TODO depricate the ones thats not used

	public synchronized void serialSend(int function, int param1, int param2) {
		LOG.debug("serialSend fn " + function + " p1 " + param1 + " p2 "
				+ param2);
		try {
			outputStream.write(function);
			outputStream.write(param1);
			outputStream.write(param2); // 0 - 180
			if (repeatSerialCommand) {
				for (int i = 0; i < repeatCommandNumber; ++i) {
					outputStream.write(function);
					outputStream.write(param1);
					outputStream.write(param2); // 0 - 180
				}

			}
		} catch (IOException e) {
			LOG.error("serialSend " + e.getMessage());
		}

	}

	/*
	 * DEPRECATED ---------------------------------- public synchronized void
	 * serialSend (int function, int pin, byte data[]) {
	 * //LOG.info("serialSend fn " + function + " p1 " + param1 + " p2 " +
	 * param2); try { outputStream.write(function); outputStream.write(pin);
	 * outputStream.write(data); // 0 - 180 } catch (IOException e) {
	 * LOG.error("serialSend " + e.getMessage()); }
	 * 
	 * }
	 */
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
	 * can the int pin be suppressed an managed in CFG? & communicated back and
	 * forth using Servo.getCFG("pin")
	 */

	/*
	 * Servo Commands Arduino has a concept of a software Servo - and supports
	 * arrays Although Services could talk directly to the Arduino software
	 * servo in order to control the hardware the Servo service was created to
	 * store/handle the details, provide a common interface for other services
	 * regardless of the controller (Arduino in this case but could be any
	 * uController)
	 */
	public boolean servoAttach(Integer pin) {
		if (serialPort == null) {
			LOG.error("could not attach servo to pin " + pin
					+ " serial port in null - not initialized?");
			return false;
		}
		// serialPort == null ??? make sure you chown it correctly !
		LOG.info("servoAttach (" + pin + ") to " + serialPort.getName()
				+ " function number " + SERVO_ATTACH);

/* soft servo		
		if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin != 10
				&& pin != 11) {
			LOG.error(pin + " not valid for servo");
		}
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

	// TODO - does not return Integer - does not block
	// use PinData to retrieve info
	public void servoRead(Integer pin) {
		serialSend(SERVO_READ, pinToServo.get(pin), 0);
	}

	public void releaseSerialPort() {
		serialPort.removeEventListener();
		serialPort.close();
	}

	public boolean setSerialPort(String portName) {
		LOG.debug("setPortIdFromName requesting " + portName);

		// initialize serial port
		try {
			CommPortIdentifier portId;

			if (customPorts.containsKey(portName)) { // adding to query right
														// back
				CommPortIdentifier.addPortName(portName,
						CommPortIdentifier.PORT_SERIAL, customPorts
								.get(portName));
			}
			portId = CommPortIdentifier.getPortIdentifier(portName);

			serialPort = (SerialPort) portId.open("Arduino", 2000);

			inputStream = serialPort.getInputStream();

			serialPort.addEventListener(this);

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			// 115200 wired, 2400 IR ?? VW 2000??
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			outputStream = serialPort.getOutputStream();

			if (serialPort == null) {
				LOG.error("serialPort is null - bad init?");
				return false;
			}

			Thread.sleep(200); // TODO - don't know why a delay is necessary -
								// should enter a bug serialPort
								// initialization????
			
			cfg.set("lastSerialPortName", portName); 

		} catch (PortInUseException e) {
			LOG.error("PortInUseException " + e.getMessage());
			return false;
		} catch (IOException e) {
			LOG.error("IOException " + e.getMessage());
			return false;
		} catch (TooManyListenersException e) {
			LOG.error("TooManyListenersException " + e.getMessage());
			return false;
		} catch (UnsupportedCommOperationException e) {
			LOG.error("UnsupportedCommOperationException " + e.getMessage());
			return false;
		} catch (InterruptedException e) {
			LOG.error("InterruptedException " + e.getMessage());
			return false;
		} catch (NoSuchPortException e) {
			// TODO Auto-generated catch block
			LOG.error("NoSuchPortException " + portName);
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc) The Arduino Duemilanove supports
	 * 
	 * @see
	 * org.myrobotlab.generatedCode.hardware.DigitalIO#digitalRead(java.lang
	 * .Integer)
	 * 
	 * digitalReadPollStart - a signal to Arduino to begin polling a particular
	 * ping with digital reads the output will be put on the serial line.
	 */
	@Override
	public void digitalReadPollStart(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + serialPort.getName());
		serialSend(DIGITAL_READ_POLLING_START, address, 0);

	}

	@Override
	public void digitalReadPollStop(Integer address) {

		LOG.info("digitalRead (" + address + ") to " + serialPort.getName());
		serialSend(DIGITAL_READ_POLLING_STOP, address, 0);

	}

	/*
	 * The Arduino Duemilanove supports 14 digital output pins access is
	 * straight forward - possible address are 0 - 13 (like good c programmers
	 * :)) value is a single bit errors are reported if these values are out of
	 * bounds
	 * 
	 * the serial communication mechanism is compressed into a single byte -
	 * where 0 = pin 0 low 1 = pin 0 high 2 = pin 1 low 3 = pin 1 high ... the
	 * needed Arduino code is here << TODO - create it >>
	 * 
	 * @see
	 * org.myrobotlab.generatedCode.hardware.DigitalIO#digitalWrite(java.lang
	 * .Integer, java.lang.Integer)
	 */
	@Override
	public void digitalWrite(IOData io) // TODO - deprecate since multiple
										// parameters are now supported
	{
		digitalWrite(io.address, io.value);
	}

	public void digitalWrite(Integer address, Integer value) {
		LOG.info("digitalWrite (" + address + "," + value + ") to "
				+ serialPort.getName() + " function number " + DIGITAL_WRITE);
		serialSend(DIGITAL_WRITE, address, value);
	}

	// TODO - depricate
	public void pinMode(IOData io) {
		pinMode(io.address, io.value);
	}

	public void pinMode(Integer address, Integer value) {
		LOG.info("pinMode (" + address + "," + value + ") to "
				+ serialPort.getName() + " function number " + PINMODE);
		serialSend(PINMODE, address, value);
	}

	@Override
	public void analogWrite(IOData io) // TODO - change interface so IOData is
										// no longer needed
	{
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
	@Override
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
			// we get here if data has been received
			byte[] readBuffer = new byte[1];
			byte[] msg = new byte[4];
			readBuffer[0] = -1;
			// readBuffer[1] = -1;
			// readBuffer[2] = -1;
			try {
				// read data

				int numBytes = 0;
				int totalBytes = 0;
				while ((numBytes = inputStream.read(readBuffer, 0, 1)) >= 0) {
					msg[totalBytes] = readBuffer[0];
					totalBytes += numBytes;
					if (totalBytes == 4) {
						LOG.error("Read: " + msg[0] + " " + msg[1] + " "
								+ msg[2] + " " + msg[3]);
						PinData p = new PinData();
						p.time = System.currentTimeMillis();
						p.function = msg[0];
						p.pin = msg[1];
						// java assumes signed
						// http://www.rgagnon.com/javadetails/java-0026.html
						p.value = (msg[2] & 0xFF) << 8; // MSB - (Arduino int is
														// 2 bytes)
						p.value += (msg[3] & 0xFF); // LSB

						if (p.function == SERVO_READ) {
							invoke("readServo", p);
						} else {
							if (p.function == ANALOG_VALUE) {
								p.type = 1;
							}
							invoke(SensorData.publishPin, p);
						}

						totalBytes = 0;
						msg[0] = -1;
						msg[1] = -1;
						msg[2] = -1;
						msg[3] = -1;

					}
				}
				// LOG.info("msg: " + readBuffer[0] + readBuffer[1] +
				// readBuffer[2] + readBuffer[3]);

			} catch (IOException e) {
			}

			break;
		}
	}

	@Override
	public String getType() {
		return Propeller.class.getCanonicalName();
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

	/*
	 * Deprecated until resurrected // break out begin --------------------- //
	 * call back stub public PinData analogValue(PinData pinData) { // TODO -
	 * breakout based on mode
	 * 
	 * invoke ("analogpublishPin" + pinData.pin, pinData.value);
	 * 
	 * return pinData; }
	 * 
	 * // publishing points public int analogValuePin0 (int data) { return data;
	 * }
	 * 
	 * public int analogValuePin1 (int data) { return data; }
	 * 
	 * // break out end ---------------------
	 */

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Propeller arduino = new Propeller("arduino");
		arduino.startService();

		// Servo hand = new Servo("hand");
		// hand.start();

		Servo wrist = new Servo("wrist");
		wrist.startService();

		Servo hand = new Servo("hand");
		hand.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

	// internal data object - used to keep track of vars associated with Motors
	// TODO - as motor defintions begin to exist on boards and then in micro-controller code - this will
	// allow Motor services to offload data / commands features to the boards and integrate other possiblites? dunno
	class Motor
	{
		boolean isAttached = false;
		
	}
	
	HashMap<String, Motor> motorMap = new HashMap<String, Motor>(); 
	
	@Override
	public void motorAttach(String name, Integer PWMPin, Integer DIRPin) {
		// set the pinmodes on the 2 pins 
		if (serialPort != null)
		{
			pinMode(PWMPin, Propeller.OUTPUT);
			pinMode(DIRPin, Propeller.OUTPUT);
		} else {
			LOG.error("attempting to attach motor before serial connection to " + name + " Arduino is ready");
		}
		
	}

	@Override
	public void motorDetach(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMove(String name, Integer amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getToolTip() {
		
		return "stubbed out for Propeller (not implemented)";
	}

}
