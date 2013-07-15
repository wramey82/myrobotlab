package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
	public ArrayList<String> serialDeviceNames = new ArrayList<String>();
	private boolean connected = false;
	int rawReadMsgLength = 5;
	
	ByteArrayOutputStream bos = new ByteArrayOutputStream();

	public Serial(String n) {
		super(n, Serial.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
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

				int newByte;

				while (serialDevice.isOpen() && (newByte = serialDevice.read()) >= 0) {
					// TODO allow msg length based on delimeter or fixed size
					invoke("read", newByte);
					bos.write(newByte);
				}

			} catch (IOException e) {
				Logging.logException(e);
			}

			break;
		}

	}

	@Override
	public ArrayList<String> getSerialDeviceNames() {
		return serialDeviceNames;
	}

	@Override
	public SerialDevice getSerialDevice() {
		return serialDevice;
	}

	@Override
	public boolean connect(String name, int rate, int databits, int stopbits, int parity) {
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (sd != null) {
				serialDevice = sd;

				connect();

				serialDevice.setParams(rate, databits, stopbits, parity);

				save(); // successfully bound to port - saving
				broadcastState(); // state has changed let everyone know
				return true;
			}
		} catch (Exception e) {
			logException(e);
		}
		return false;
	}

	public boolean connect() {

		if (serialDevice == null) {
			error("can't connect, serialDevice is null");
			return false;
		}

		info(String.format("connecting to serial device %s", serialDevice.getName()));

		try {
			if (!serialDevice.isOpen()) {
				serialDevice.open();
				serialDevice.addEventListener(this);
				serialDevice.notifyOnDataAvailable(true);
				sleep(1000);
			} else {
				warn(String.format("%s is already open, close first before opening again", serialDevice.getName()));
			}
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}

		info(String.format("connected to serial device %s", serialDevice.getName()));
		connected = true;

		return true;
	}
	
	/**
	 * publishing point for read events
	 * 
	 * @param b
	 * @return
	 */
	public Integer read(Integer data)
	{
		return data;
	}

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

	public static void main(String[] args) throws IOException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Serial serial = new Serial("serial");
		serial.startService();

		serial.connect("COM4", 57600, 8, 1, 0);
		serial.connect();
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
