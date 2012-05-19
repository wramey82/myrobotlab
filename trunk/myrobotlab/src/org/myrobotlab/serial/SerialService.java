package org.myrobotlab.serial;

public interface SerialService {

	boolean send(int[] data);
	public abstract boolean setSerialPortParams(int baudRate, int dataBits, int stopBits, int parity);

}
