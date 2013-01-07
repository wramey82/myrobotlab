package org.myrobotlab.serial;

import java.util.ArrayList;

public interface SerialDeviceService {

	/**
	 * methods to return read-only information regarding a serialDevice
	 */
	public ArrayList<String> getSerialDeviceNames();

	/**
	 * @return a read-only copy of the SerialDevice if it has bee serialized
	 *         over the network - the InputStream & OutputStream are transient
	 */
	public SerialDevice getSerialDevice();

	public boolean setSerialDevice(String name, int rate, int databits, int stopbits, int parity);

}
