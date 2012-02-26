package org.myrobotlab.serial;

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

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.simpleframework.xml.Element;

public class Port implements SerialPortEventListener {

	public final static Logger LOG = Logger.getLogger(Port.class.getCanonicalName());
	
	protected Service myService = null;

	// non serializable serial objects
	transient SerialPort serialPort;
	transient InputStream inputStream;
	transient OutputStream outputStream;
	transient static HashMap<String, CommDriver> customPorts = new HashMap<String, CommDriver>();
	private boolean isReady = false;

	@Element
	String portName; 
	@Element
	int baud;
	@Element
	int dataBits;
	@Element
	int parity;
	@Element
	int stopBits;

	String cfgFileName;
	
	// serial protocol functions
	public static final int DIGITAL_WRITE = 0;


	/**
	 * returns true if port is open and we have a valid input & output stream
	 * not "Ready" if we don't
	 * @return 
	 */
	public boolean isReady()
	{
		return isReady;
	}
	
	/**
	 *  list of serial port names from the system which the Port service is 
	 *  running
	 */
	public ArrayList<String> portNames = new ArrayList<String>(); 
	
	public Port(Service service)
	{
		this(service, null, 115200, 8, 0, 1);
	}
	
	/**
	 * simple constructor takes the Service it belongs too
	 * @param name
	 */
	public Port(Service service, String defaultPortName, int defaultBaud, int defaultDataBits, int defaultParity, int defaultStopBits) {
		
		myService = service;
		portName = defaultPortName;
		baud = defaultBaud;
		dataBits = defaultDataBits;
		parity = defaultParity;
		stopBits = defaultStopBits;
		// get ports - return array of strings
		// set port? / init port
		// detach port
		
		//cfgFileName = service.getName() + "." + portName.replaceAll("[$/\\\\;:]", "") + ".xml";
		cfgFileName = myService.getName() + ".serial.xml";
		if (!myService.load(this, cfgFileName))
		{
			LOG.info("no " + cfgFileName + " configuration for port");
		}
		
		
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
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			LOG.info(inPortName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				ports.add(inPortName);
			}
		}

		// adding connected serial port if connected
		/* FIXME - windows returns all ports - there is also isCurrentlyOwned()
		if (serialPort != null) {
			if (serialPort.getName() != null)
				ports.add(cleanName(serialPort.getName()));
		}
		*/

		// adding custom ports if they were previously added with addPortName+
		for (String key : customPorts.keySet()) {
			// customPorts.get(key)
			ports.add(key);
		}

		return ports;
	}

	/**
	 * Windows (for whatever reason) returns something different from
	 * what portname was requested
	 * 
	 * @param portName
	 * @return
	 */
	public String cleanName(String portName)
	{
		if (org.myrobotlab.service.Runtime.isWindows())
		{
			return portName.replaceAll(".\\\\", "");
		}
		
		return portName;
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

	@ToolTip("sends an array of data to the serial port which an Port is attached to")
	public void serialSend(String data) {
		LOG.error("serialSend [" + data + "]");
		serialSend(data.getBytes());
	}

	public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
	
	public static final int byteArrayToInt(byte [] b) {
        return (b[0] << 24)
                + ((b[1] & 0xFF) << 16)
                + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
	}	
	
	public synchronized void serialSend(int data) {
		try {			
			outputStream.write((byte)data);
		} catch (IOException e) {
			LOG.error("serialSend " + e.getMessage());
		}
	}
	
	
	public synchronized void serialSend(int[] data) {
		try {
			if (LOG.isDebugEnabled())
			{
				for (int i = 0; i < data.length; ++i) {
					LOG.debug(data[i]);
				}
			}
			for (int i = 0; i < data.length; ++i) {
				outputStream.write((byte)data[i]);
			}
		} catch (IOException e) {
			LOG.error("serialSend " + e.getMessage());
		}
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
	      isReady = false;

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
	
	public boolean setPort (String inPortName)
	{
		return setPort(inPortName, baud, dataBits, stopBits, parity);
	}
	
	public boolean setPort(String inPortName, int inBaud, int inDataBits, int inStopBits, int inParity) {
		baud = inBaud;
		dataBits = inDataBits;
		stopBits = inStopBits;
		parity = inParity;
		
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
						serialPort.setSerialPortParams(baud,
														dataBits, 
														stopBits,
														parity);

						Thread.sleep(200); // the initialization of the hardware
											// takes a little time

						if (inputStream != null && outputStream != null)
						{
							isReady = true;
						}


						// portName = serialPort.getName(); BUG - serialPort.getName != the name which is requested
						// Windows you ask for "COM1" but when you ask for it back you get "/.//COM1"
						portName = inPortName;
						LOG.debug("opened " + getPortString());
						myService.save(this, cfgFileName); // successfully bound to port - saving
						//broadcastState(); // state has changed let everyone know
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
	
	public boolean setBaud(int baud)
	{
		if (serialPort == null)
		{
			LOG.error("setBaudBase - serialPort is null");
			return false;
		}
		try {
			// boolean ret = serialPort.set.setBaudBase(baud); // doesnt work - operation not allowed
			boolean ret = setSerialPortParams(baud, serialPort.getDataBits(), serialPort.getStopBits(), serialPort.getParity());
			this.baud = baud;
			myService.save();
			//broadcastState(); // state has changed let everyone know
			return ret;
		} catch (Exception e) {
			Service.logException(e);
		}
		return false;
	}
	
	public int getBaudRate()
	{
		return baud;
	}
	
	public boolean setSerialPortParams (int baud, int dataBits, int stopBits, int parity)
	{
		if (serialPort == null)
		{
			LOG.error("setSerialPortParams - serialPort is null");
			return false;
		}
		
		try {
			serialPort.setSerialPortParams(baud, dataBits, stopBits, parity);
		} catch (UnsupportedCommOperationException e) {
			Service.logException(e);
		}
		
		return true;
	}
	
	// ---------------------- Serial Control Methods End ------------------

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

						// raw protocol
						String s = new String(msg);
						LOG.info(s);
						myService.invoke("readSerial", msg);
						//myService.invoke("readSerialMessage", s);

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
		return Port.class.getCanonicalName();
	}

		
}
