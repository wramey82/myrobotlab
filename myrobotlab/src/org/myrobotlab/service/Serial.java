package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.slf4j.Logger;

public class Serial extends Service implements SerialDeviceService, SerialDeviceEventListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Serial.class.getCanonicalName());

	private transient SerialDevice serialDevice;
	public ArrayList<String> portNames = new ArrayList<String>();

	// buffer stuff
	private int limit = 0;
	private int position = 0;
	private int capacity = 2048;
	byte[] buffer = new byte[capacity];
	BlockingQueue<Object> blockingData;
	
	private int recievedByteCount = 0;

	boolean publish = true;

	private boolean connected = false;
	private String portName = "";

	public static final int PUBLISH_BYTE = 0;
	public static final int PUBLISH_LONG = 1;
	public static final int PUBLISH_INT = 2;
	public static final int PUBLISH_CHAR = 3;
	public static final int PUBLISH_BYTE_ARRAY = 3;
	public static final int PUBLISH_STRING = 3;

	public boolean useFixedWidth = false;
	public int msgWidth = 10;
	public String delimeter = "\n";

	public int publishType = PUBLISH_BYTE;
	
	public final int BYTE_SIZE_LONG = 4;

	public Serial(String n) {
		super(n, Serial.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public String getPortName() {
		return portName;
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

				byte newByte;

				// FIXME - serialDevice is bullshit - it returns a byte not an int !
				while (serialDevice.isOpen() && (newByte = (byte)serialDevice.read()) >= 0) { 
					++recievedByteCount;
					// TODO allow msg length based on delimeter or fixed size
					if (publish) {
						switch (publishType) {
						// long of Arduino is 4 bytes
						case PUBLISH_LONG: {
							buffer[recievedByteCount-1] = newByte;
							if (recievedByteCount%BYTE_SIZE_LONG == 0)
							{								
								long value = 0;
								for (int i = 0; i < BYTE_SIZE_LONG; i++)
								{
								   value = (value << 8) + (buffer[i] & 0xff);
								}
								
								invoke("publishLong", value);
								blockingData.add(value);
								recievedByteCount = 0;
							}
							break;
						}
						case PUBLISH_BYTE: {
							invoke("publishByte", newByte);
							break;
						}
						}
					}
				}

			} catch (IOException e) {
				Logging.logException(e);
			}

			break;
		}

	}

	@Override
	public ArrayList<String> getPortNames() {
		return SerialDeviceFactory.getSerialDeviceNames();
	}

	@Override
	public SerialDevice getSerialDevice() {
		return serialDevice;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		if (name == null || name.length() == 0)
		{
			log.info("got emtpy connect name - disconnecting");
			return disconnect();
		}
		try {
			serialDevice = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (serialDevice != null) {
				if (!serialDevice.isOpen()) {
					serialDevice.open();
					serialDevice.addEventListener(this);
					serialDevice.notifyOnDataAvailable(true);
					sleep(1000);
				}

				serialDevice.setParams(rate, databits, stopbits, parity);
				portName = serialDevice.getName();
				connected = true;
				save(); // successfully bound to port - saving
				broadcastState(); // state has changed let everyone know
				return true;

			} else {
				log.error("could not get serial device");
			}
		} catch (Exception e) {
			logException(e);
		}
		return false;
	}

	@Override
	public boolean connect(String name) {
		return connect(name, 57600, 8, 1, 0);
	}

	/**
	 * ------ publishing points begin -------
	 */

	// FIXME - fixed width and message delimeter
	// FIXME - block read(until block size)

	public long publishLong(Long data) {
		return data;
	}

	public int publishInt(Integer data) {
		return data;
	}

	public byte publishByte(Byte data) {
		return data;
	}

	public char publishChar(Character data) {
		return data;
	}

	public byte[] publishByteArray(byte[] data) {
		return data;
	}

	public String publishString(String data) {
		return data;
	}

	/**
	 * ------ publishing points end -------
	 */

	/**
	 * -------- blocking reads begin --------
	 * @throws InterruptedException 
	 */

	public long readLong() throws InterruptedException {
		long value = ((Long)blockingData.take()).longValue();
		return value;
	}

	public int readInt() {
		return 32;
	}

	public byte readByte() {
		return 32;
	}
 
	public char readChar() {
		return 3;
	}

	public byte[] readByteArray() {
		return new byte[]{10};
	}

	public String readString() {
		return "";
	}

	/**
	 * -------- blocking reads begin --------
	 */

	public boolean isConnected() {
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected;
	}

	@Override
	public void write(String data) throws IOException {
		write(data.getBytes());
	}

	@Override
	public void write(byte[] data) throws IOException {
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

	@Override
	public boolean disconnect() {
		if (serialDevice == null) {
			connected = false;
			portName = "";
			return false;
		}

		serialDevice.close();
		connected = false;
		portName = "";

		broadcastState();
		return true;

	}

	public static void main(String[] args) throws IOException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		
		
		byte[] buffer = new byte[]{1,1,1,1};
		
		
		long value = 0;
		for (int i = 0; i < buffer.length; i++)
		{
		   value = (value << 8) + (buffer[i] & 0xff);
		}
		log.info("{}", value);
		
		Serial serial = new Serial("serial");
		serial.startService();

		serial.connect("COM4", 57600, 8, 1, 0);

		byte a = 1;
		int b = 2;
		serial.write(a);

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
