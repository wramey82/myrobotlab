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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 *   References:
 *   http://www.arduino.cc/playground/Main/RotaryEncoders
 *   
 */
public class PICAXE extends Service implements SerialPortEventListener, DigitalIO, AnalogIO, ServoController {

	public final static Logger LOG = Logger.getLogger(PICAXE.class.getCanonicalName());
	CommPortIdentifier portId;
	CommPortIdentifier saveportId;
	Enumeration portList;
	InputStream inputStream;
	SerialPort serialPort;
	Thread readThread;
	String messageString = "0";
	OutputStream outputStream;
	boolean outputBufferEmptyFlag = false;

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

	public PICAXE(String n) {
		super(n, PICAXE.class.getCanonicalName());
		init();
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	/*
	 * serialSend communicate to the arduino using our simple language 3 bytes 3
	 * byte functions - |function name| d0 | d1
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
		// serialPort == null ??? make sure you chown it correctly !
		LOG.info("servoAttach (" + pin + ") to " + serialPort.getName()
				+ " function number " + SERVO_ATTACH);

		if (pin != 3 && pin != 5 && pin != 6 && pin != 9 && pin != 10
				&& pin != 11) {
			LOG.error(pin + " not valid for servo");
		}

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
			LOG.error(pin + " angle " + angle + " request invalid");
			return;
		}

		serialSend(SERVO_WRITE, pinToServo.get(pin), angle);
	}

	// TODO - does not return Integer - does not block
	// use PinData to retrieve info
	public void servoRead(Integer pin) {
		serialSend(SERVO_READ, pinToServo.get(pin), 0);
	}

	public boolean init() {
		return init(null);
	}

	/*
	 * Setup local serial interface requires rxtx - should be part of
	 * constructor TODO - check for unsatisfied link error
	 */

	public boolean init(String portName) {
		boolean portFound = false;
		String defaultPort;

		// TODO - this should be done in the constructor? - why should I have to
		// do a SerialString.run() for it to work?
		// determine the name of the serial port on several operating systems
		String osname = System.getProperty("os.name", "").toLowerCase();
		if (osname.startsWith("windows")) {
			// windows
			// defaultPort = "COM1";
			defaultPort = "COM5";
		} else if (osname.startsWith("linux")) {
			// linux
			// defaultPort = "/dev/ttyUSB1";
			defaultPort = "/dev/ttyUSB0";
		} else if (osname.startsWith("mac")) {
			// mac
			defaultPort = "????";
			LOG.error("Sorry, your operating system is not supported");
		} else {
			LOG.error("Sorry, your operating system is not supported");
			return false;
		}

		// TODO - what is the preferred method of testing empty or null ?
		if ((portName == null) || (portName == "")) {
			portName = defaultPort;
		}

		LOG.info("Set default port to " + portName);

		// parse ports and if the default port is found, initialized the reader
		portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements() && !portFound) {
			portId = (CommPortIdentifier) portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (portId.getName().equals(portName)) {
					System.out.println("Found port: " + portName);
					portFound = true;
					// init reader thread
					// SerialCommTest2 reader = new SerialCommTest2();
				}
			}

		}
		if (!portFound) {
			LOG.error("port " + portName + " not found.");
			return false;
		}

		// initialize serial port
		try {

			serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);

			inputStream = serialPort.getInputStream();

			serialPort.addEventListener(this);

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			// set port parameters 9600 57600
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			outputStream = serialPort.getOutputStream();

			// serialPort.notifyOnOutputEmpty(true); TODO - VERY CURIOUS -
			// SYMMETRIC COMMUNICATION?

			if (serialPort == null) {
				LOG.error("serialPort is null - bad init?");
				return false;
			}

			for (int i = 0; i < servosInUse.length; ++i) {
				servosInUse[i] = false;
			}

			Thread.sleep(200); // TODO - don't know why a delay is necessary -
								// should enter a bug serialPort
								// initialization????

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
		}

		return portFound;
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
			byte[] msg = new byte[3];
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
					if (totalBytes == 3) {
						LOG.info("Read: " + msg[0] + " " + msg[1] + " "
								+ msg[2]);
						PinData p = new PinData();
						p.function = msg[0];
						p.pin = msg[1];
						p.value = msg[2];

						if (p.function == SERVO_READ) {
							invoke("readServo", p);
						} else if (p.function == ANALOG_VALUE) {
							// invoke("analogValue", p);
							analogValue(p);
						} else // TODO !
						{
							invoke("publishPin", p); // TODO - DEPRICATE - NOT
														// VALID !!!
							// digitalValue(p)
						}

						totalBytes = 0;
						msg[0] = -1;
						msg[1] = -1;
						msg[2] = -1;

					}
				}
				LOG.info("msg: " + readBuffer[0] + readBuffer[1]
						+ readBuffer[2]);

			} catch (IOException e) {
			}

			break;
		}
	}

	@Override
	public String getType() {
		return PICAXE.class.getCanonicalName();
	}

	// force an digital read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void digitalReadPollingStart(int pin) {
		serialSend(DIGITAL_READ_POLLING_START, pin, 0); // last param is not
														// used in read
	}

	public void digitalReadPollingStop(int pin) {
		serialSend(DIGITAL_READ_POLLING_STOP, pin, 0); // last param is not used
														// in read
	}

	// force an analog read - data will be published in a call-back
	// TODO - make a serialSendBlocking
	public void analogReadPollingStart(int pin) {
		serialSend(ANALOG_READ_POLLING_START, pin, 0); // last param is not used
														// in read
	}

	public void analogReadPollingStop(int pin) {
		serialSend(ANALOG_READ_POLLING_STOP, pin, 0); // last param is not used
														// in read
	}

	// call back stub

	/*
	 * Call back stub
	 */

	public PinData analogValue(PinData pinData) {
		// TODO - breakout based on mode

		invoke("analogpublishPin" + pinData.pin, pinData.value);

		return pinData;
	}

	/*
	 * Publishing Points
	 */

	public int analogValuePin0(int data) {
		return data;
	}

	public int analogValuePin1(int data) {
		return data;
	}

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		GUIService gui = new GUIService("gui");
		PICAXE platform = new PICAXE("picaxe");
		platform.startService();
		gui.startService();
		gui.display();
		//platform.startRobot();
	}

	@Override
	public String getToolTip() {
		return "(not implemented yet) used to interface PICAXE";
	}
	
}
