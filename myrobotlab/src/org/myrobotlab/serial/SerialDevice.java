package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

public interface SerialDevice {

	public static final int DATABITS_5 = 5;
	public static final int DATABITS_6 = 6;
	public static final int DATABITS_7 = 7;
	public static final int DATABITS_8 = 8;
	public static final int PARITY_NONE = 0;
	public static final int PARITY_ODD = 1;
	public static final int PARITY_EVEN = 2;
	public static final int PARITY_MARK = 3;
	public static final int PARITY_SPACE = 4;
	public static final int STOPBITS_1 = 1;
	public static final int STOPBITS_2 = 2;
	public static final int STOPBITS_1_5 = 3;
	public static final int FLOWCONTROL_NONE = 0;
	public static final int FLOWCONTROL_RTSCTS_IN = 1;
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;
	public static final int FLOWCONTROL_XONXOFF_IN = 4;
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;
	public static final int PORTTYPE_SERIAL = 1;

	// identification
	public abstract String getName();
	//public abstract String getCurrentOwner();
	//public abstract int getPortType();
	//public abstract boolean isCurrentlyOwned();
	public abstract int	available();
	
	// open / close
	public abstract void open() throws SerialDeviceException;
	//public abstract SerialDevice open(FileDescriptor f) throws SerialDeviceException; 
	//public abstract SerialDevice open(String TheOwner, int i) throws SerialDeviceException;
	public abstract boolean isOpen();

	public abstract void close();
	
	// input/output 
	//public abstract InputStream getInputStream() throws IOException;
	//public abstract OutputStream getOutputStream() throws IOException;

	// serial parameters
	public abstract void setParams(int b, int d, int s, int p) throws SerialDeviceException;
	
	// special serial methods/states
//	public abstract boolean isDTR();
	public abstract void setDTR(boolean state);
	public abstract void setRTS(boolean state);
//	public abstract boolean isCTS();
//	public abstract boolean isDSR();
//	public abstract boolean isCD();
//	public abstract boolean isRI();
//	public abstract boolean isRTS();


	// reading/listening events
	public abstract void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException;
	
	public abstract void notifyOnDataAvailable(boolean enable);


	// write methods 
	public abstract void write(int data) throws IOException;
	public abstract void write(byte data) throws IOException;
	public abstract void write(char data) throws IOException;
	public abstract void write(int[] data) throws IOException;
	public abstract void write(byte[] data) throws IOException;
	public abstract void write(String data) throws IOException;
	
	public abstract int read() throws IOException;

	
}