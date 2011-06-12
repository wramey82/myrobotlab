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
import java.util.TooManyListenersException;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;

public class Serial extends Service implements SerialPortEventListener {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(Serial.class
			.getCanonicalName());

	// TODO - bundle data into a class - put it on a static list
	CommPortIdentifier portId;
	CommPortIdentifier saveportId;
	Enumeration<?> portList;
	InputStream inputStream;
	SerialPort serialPort;
	Thread readThread;
	String messageString = "0";
	OutputStream outputStream;
	boolean outputBufferEmptyFlag = false;

	// TODO - map of named ports
	// TODO - Message structure - SOAP follows Envelope/Head/Body - need to be
	// abled to do simple REST GET too - GET Adapter
	// TODO - begin looking into C# web services

	public Serial(String n) {
		super(n, Serial.class.getCanonicalName());
	}

	public void loadDefaultConfiguration() {
		// TODO - populate this
		// cfg.set("messageString", "0");
	}

	public boolean init() {
		return init(null);
	}

	public boolean init(String portName) {
		boolean portFound = false;
		String defaultPort;

		// TODO - this should be done in the constructor? - why should I have to
		// do a SerialString.run() for it to work?
		// determine the name of the serial port on several operating systems
		String osname = System.getProperty("os.name", "").toLowerCase();
		if (osname.startsWith("windows")) {
			// windows
			defaultPort = "COM1";
		} else if (osname.startsWith("linux")) {
			// linux
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
		// TODO - operator needs to accept a GET or a SOAP POST or a REST GET
		// TODO Arduino Communication Service - uses the serial service - Doc
		// Message structure PIN FUNCTION
		// TODO public attachToPort (Port Service better? versus Serial
		// service?)
		// TODO - parameratize all variables - default them - load all variables
		// on a innerclass - name the subclass and put on a map
		try {

			serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);

			inputStream = serialPort.getInputStream();

			serialPort.addEventListener(this);

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			// set port parameters
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			outputStream = serialPort.getOutputStream();

			serialPort.notifyOnOutputEmpty(true);

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
		}

		return portFound;
	}

	// TODO - support digitalwrite?
	// TODO - make static?
	public void write(Integer data) {
		if (serialPort == null) {
			LOG
					.error("serial write \""
							+ data
							+ "\" attempt not possible - serialPort is null - bad init?");
			return;
		}
		LOG.info("serial write \"" + data + "\" to " + serialPort.getName());
		try {
			// write string to serial port
			// outputStream.write(messageString.getBytes());
			outputStream.write(data);
			// outputStream.write(0);
		} catch (IOException e) {
		}
		{
			return;
		}
	}

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
			byte[] readBuffer = new byte[20];
			try {
				// read data
				while (inputStream.available() > 0) {
					// int numBytes = inputStream.read(readBuffer);
					inputStream.read(readBuffer);
				}
				// print data
				String result = new String(readBuffer);
				LOG.info("Read: " + result);
			} catch (IOException e) {
			}

			break;
		}
	}

	@Override
	public String getToolTip() {
		return "<html>serial service</html>";
	}
	
}
