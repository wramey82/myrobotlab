package org.myrobotlab.service;

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
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.SensorDataPublisher;
import org.slf4j.Logger;

/**
 * @author Gareth & GroG
 * 
 *         References: http://myrobotlab.org/service/propellor
 * 
 *         TODO REFACTOR OUT A SERIAL SERVICE - bleh !!!
 * 
 * 
 */

public class Propeller extends Service implements SerialDeviceEventListener, SerialDeviceService {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Propeller.class.getCanonicalName());

	private transient SerialDevice serialDevice;
	int rawReadMsgLength = 5;

	private boolean connected = false;
	public ArrayList<String> serialDeviceNames = new ArrayList<String>();
	int errorCount = 0;
	public final static int MAGIC_NUMBER = 170; // 10101010
	boolean rawReadMsg = false;

	public Propeller(String n) {
		super(n, Propeller.class.getCanonicalName());
	}

	// looks like we have to keep bits 31+30 clear for RXTX pins..

	@Override
	public String getToolTip() {
		return "Propellor";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	// ----------------------------- begin interface methods
	// ---------------------------------------
	public boolean connect() {

		if (serialDevice == null) {
			message("\ncan't connect, serialDevice is null\n"); // TODO -
																// "errorMessage vs message"
			log.error("can't connect, serialDevice is null");
			return false;
		}

		message(String.format("\nconnecting to serial device %s\n", serialDevice.getName()));

		try {
			if (!serialDevice.isOpen()) {
				serialDevice.open();
				serialDevice.addEventListener(this);
				serialDevice.notifyOnDataAvailable(true);
			} else {
				log.warn(String.format("\n%s is already open, close first before opening again\n", serialDevice.getName()));
				message(String.format("%s is already open, close first before opening again", serialDevice.getName()));
			}
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}

		message(String.format("\nconnected to serial device %s\n", serialDevice.getName()));
		message("good times...\n");
		connected = true;
		return true;
	}

	public void message(String msg) {
		log.info(msg);
		invoke("publishMessage", msg);
	}

	// FIXME - extract serial service with interfaces
	@Override
	public ArrayList<String> getSerialDeviceNames() {
		// TODO Auto-generated method stub
		return serialDeviceNames;
	}

	@Override
	public SerialDevice getSerialDevice() {
		// TODO Auto-generated method stub
		return serialDevice;
	}

	public boolean setSerialDevice(String name) {
		return setSerialDevice(name, 9600, 8, 1, 0);
	}

	@Override
	public boolean setSerialDevice(String name, int rate, int databits, int stopbits, int parity) {
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(name, rate, databits, stopbits, parity);
			if (sd != null) {
				serialDevice = sd;

				connect();
				save(); // successfully bound to port - saving
				broadcastState(); // state has changed let everyone know
				return true;
			}
		} catch (Exception e) {
			logException(e);
		}
		return false;
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

					if (numBytes == 0 && newByte != MAGIC_NUMBER) {
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
							Pin p = new Pin(msg[2], msg[1], (((msg[3] & 0xFF) << 8) + (msg[4] & 0xFF)), getName());
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
	
	public boolean isConnected() {
		// I know not normalized
		// but we have to do this - since
		// the SerialDevice is transient
		return connected;
	}


	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		Propeller prop = new Propeller("prop");
		prop.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

	}

}
