package org.myrobotlab.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

public interface SerialDevice {
	//---------- gnu.io.CommPort interface begin -------
	public abstract void enableReceiveFraming(int f)
			throws UnsupportedCommOperationException;

	public abstract void disableReceiveFraming();

	public abstract boolean isReceiveFramingEnabled();

	public abstract int getReceiveFramingByte();

	public abstract void disableReceiveTimeout();

	public abstract void enableReceiveTimeout(int time)
			throws UnsupportedCommOperationException;

	public abstract boolean isReceiveTimeoutEnabled();

	public abstract int getReceiveTimeout();

	public abstract void enableReceiveThreshold(int thresh)
			throws UnsupportedCommOperationException;

	public abstract void disableReceiveThreshold();

	public abstract int getReceiveThreshold();

	public abstract boolean isReceiveThresholdEnabled();

	public abstract void setInputBufferSize(int size);

	public abstract int getInputBufferSize();

	public abstract void setOutputBufferSize(int size);

	public abstract int getOutputBufferSize();

	public abstract void close();

	public abstract InputStream getInputStream() throws IOException;

	public abstract OutputStream getOutputStream() throws IOException;

	public abstract String getName();

	public abstract String toString();
	//---------- gnu.io.CommPort interface end -------

	//----------   gnu.io.SerialPort interface begin ----------
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

	public abstract void setSerialPortParams(int b, int d, int s, int p)
			throws UnsupportedCommOperationException;

	public abstract int getBaudRate();

	public abstract int getDataBits();

	public abstract int getStopBits();

	public abstract int getParity();

	public abstract void setFlowControlMode(int flowcontrol)
			throws UnsupportedCommOperationException;

	public abstract int getFlowControlMode();

	public abstract boolean isDTR();

	public abstract void setDTR(boolean state);

	public abstract void setRTS(boolean state);

	public abstract boolean isCTS();

	public abstract boolean isDSR();

	public abstract boolean isCD();

	public abstract boolean isRI();

	public abstract boolean isRTS();

	public abstract void sendBreak(int duration);

	public abstract void addEventListener(SerialDeviceEventListener lsnr)
			throws TooManyListenersException;

	public abstract void removeEventListener();

	public abstract void notifyOnDataAvailable(boolean enable);

	public abstract void notifyOnOutputEmpty(boolean enable);

	public abstract void notifyOnCTS(boolean enable);

	public abstract void notifyOnDSR(boolean enable);

	public abstract void notifyOnRingIndicator(boolean enable);

	public abstract void notifyOnCarrierDetect(boolean enable);

	public abstract void notifyOnOverrunError(boolean enable);

	public abstract void notifyOnParityError(boolean enable);

	public abstract void notifyOnFramingError(boolean enable);

	public abstract void notifyOnBreakInterrupt(boolean enable);

	/*
		public abstract void setRcvFifoTrigger(int trigger);
	         deprecated
	 */
	/* ----------------------   end of commapi ------------------------ */

	/*
		can't have static abstract?
	
		public abstract static boolean staticSetDTR( String port, boolean flag )
			throws UnsupportedCommOperationException;
		public abstract static boolean staticSetRTS( String port, boolean flag )
			throws UnsupportedCommOperationException;
	 */
	public abstract byte getParityErrorChar()
			throws UnsupportedCommOperationException;

	public abstract boolean setParityErrorChar(byte b)
			throws UnsupportedCommOperationException;

	public abstract byte getEndOfInputChar()
			throws UnsupportedCommOperationException;

	public abstract boolean setEndOfInputChar(byte b)
			throws UnsupportedCommOperationException;

	public abstract boolean setUARTType(String type, boolean test)
			throws UnsupportedCommOperationException;

	public abstract String getUARTType()
			throws UnsupportedCommOperationException;

	public abstract boolean setBaudBase(int BaudBase)
			throws UnsupportedCommOperationException, IOException;

	public abstract int getBaudBase() throws UnsupportedCommOperationException,
			IOException;

	public abstract boolean setDivisor(int Divisor)
			throws UnsupportedCommOperationException, IOException;

	public abstract int getDivisor() throws UnsupportedCommOperationException,
			IOException;

	public abstract boolean setLowLatency()
			throws UnsupportedCommOperationException;

	public abstract boolean getLowLatency()
			throws UnsupportedCommOperationException;

	public abstract boolean setCallOutHangup(boolean NoHup)
			throws UnsupportedCommOperationException;

	public abstract boolean getCallOutHangup()
			throws UnsupportedCommOperationException;
	
	//----------   gnu.io.SerialPort interface end ----------


}