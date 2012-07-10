package org.myrobotlab.serial.gnu;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceException;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceFrameworkFactory;

public class SerialDeviceFactoryGNU implements SerialDeviceFrameworkFactory {

	public final static Logger log = Logger.getLogger(SerialDeviceFactory.class
			.getCanonicalName());

	public SerialDevice getSerialDevice(String name, int rate, int databits,
			int stopbits, int parity) throws SerialDeviceException {

		CommPortIdentifier portId;
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getName().equals(name)) {
				return new SerialDeviceGNU(portId, rate, databits, stopbits, parity);
			}
		}
		return null;
	}

	public ArrayList<SerialDevice> getSerialDevices() {
		ArrayList<SerialDevice> ret = new ArrayList<SerialDevice>();
		CommPortIdentifier portId;
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				SerialDeviceGNU sd;
				try {
					sd = new SerialDeviceGNU(portId);
					ret.add(sd);
				} catch (SerialDeviceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

}
