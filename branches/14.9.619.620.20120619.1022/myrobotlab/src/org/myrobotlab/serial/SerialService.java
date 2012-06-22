package org.myrobotlab.serial;

import java.io.IOException;

public interface SerialService {

	void send(int[] data) throws IOException;
	public abstract boolean setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity);

}
