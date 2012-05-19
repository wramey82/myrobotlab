package org.myrobotlab.serial.gnu;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import javax.swing.event.EventListenerList;

import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.UnsupportedCommOperationException;

/**
 * @author GroG
 * 
 * A silly but necessary wrapper class for gnu.io.SerialPort, since 
 * RXTXComm driver initializes loadlibrary in a static block and
 * the driver dynamically loaded is loaded with a hardcoded string :P
 *
 */
public class SerialDeviceGNU implements SerialDevice, SerialPortEventListener {

	private gnu.io.SerialPort port;
    protected EventListenerList listenerList = new EventListenerList();
    
	public SerialDeviceGNU(SerialPort port) {
		this.port = port;
	}

	@Override
	public void enableReceiveFraming(int f) throws UnsupportedCommOperationException {
		try {
			port.enableReceiveFraming(f);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public void disableReceiveFraming() {
		port.disableReceiveFraming();
	}

	@Override
	public boolean isReceiveFramingEnabled() {
		return port.isReceiveFramingEnabled();
	}

	@Override
	public int getReceiveFramingByte() {
		return port.getReceiveFramingByte();
	}

	@Override
	public void disableReceiveTimeout() {
		port.disableReceiveTimeout();
	}

	@Override
	public void enableReceiveTimeout(int time) throws UnsupportedCommOperationException {
		try {
			port.enableReceiveTimeout(time);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean isReceiveTimeoutEnabled() {
		return port.isReceiveTimeoutEnabled();
	}

	@Override
	public int getReceiveTimeout() {
		return port.getReceiveTimeout();
	}

	@Override
	public void enableReceiveThreshold(int thresh) throws UnsupportedCommOperationException {
		try {
			port.enableReceiveThreshold(thresh);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public void disableReceiveThreshold() {
		port.disableReceiveThreshold();
	}

	@Override
	public int getReceiveThreshold() {
		return port.getReceiveThreshold();
	}

	@Override
	public boolean isReceiveThresholdEnabled() {
		return port.isReceiveThresholdEnabled();
	}

	@Override
	public void setInputBufferSize(int size) {
		port.setInputBufferSize(size);
	}

	@Override
	public int getInputBufferSize() {
		return port.getInputBufferSize();
	}

	@Override
	public void setOutputBufferSize(int size) {
		port.setOutputBufferSize(size);
	}

	@Override
	public int getOutputBufferSize() {
		return port.getOutputBufferSize();
	}

	@Override
	public void close() {
		port.close();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return port.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return port.getOutputStream();
	}

	@Override
	public String getName() {
		return port.getName();
	}

	@Override
	public void setSerialPortParams(int b, int d, int s, int p) throws UnsupportedCommOperationException {
		try {
			port.setSerialPortParams(b, d, s, p);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public int getBaudRate() {
		return port.getBaudRate();
	}

	@Override
	public int getDataBits() {
		return port.getDataBits();
	}

	@Override
	public int getStopBits() {
		return port.getStopBits();
	}

	@Override
	public int getParity() {
		return port.getParity();
	}

	@Override
	public void setFlowControlMode(int flowcontrol) throws UnsupportedCommOperationException {
		try {
			port.setFlowControlMode(flowcontrol);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}		
	}

	@Override
	public int getFlowControlMode() {
		return port.getFlowControlMode();
	}

	@Override
	public boolean isDTR() {
		return port.isDTR();
	}

	@Override
	public void setDTR(boolean state) {
		port.setDTR(state);
	}

	@Override
	public void setRTS(boolean state) {
		port.setRTS(state);
	}

	@Override
	public boolean isCTS() {
		return port.isCTS();
	}

	@Override
	public boolean isDSR() {
		return port.isDSR();
	}

	@Override
	public boolean isCD() {
		return port.isCD();
	}

	@Override
	public boolean isRI() {
		return port.isRI();
	}

	@Override
	public boolean isRTS() {
		return port.isRTS();
	}

	@Override
	public void sendBreak(int duration) {
		 port.sendBreak(duration);
	}

	@Override
	public void addEventListener(SerialDeviceEventListener lsnr) throws TooManyListenersException {
		 //proxy events
		listenerList.add(SerialDeviceEventListener.class, lsnr);
		port.addEventListener(this);	
	}

	@Override
	public void removeEventListener() {
		 port.removeEventListener();
		 // FIXME there are more removes which should be implemented
		 //removeSerialDeviceEventListener  
	}

	@Override
	public void notifyOnDataAvailable(boolean enable) {
		 port.notifyOnDataAvailable(enable);
	}

	@Override
	public void notifyOnOutputEmpty(boolean enable) {
		 port.notifyOnOutputEmpty(enable);
	}

	@Override
	public void notifyOnCTS(boolean enable) {
		 port.notifyOnCTS(enable);
	}

	@Override
	public void notifyOnDSR(boolean enable) {
		 port.notifyOnDSR(enable);
	}

	@Override
	public void notifyOnRingIndicator(boolean enable) {
		 port.notifyOnRingIndicator(enable);
	}

	@Override
	public void notifyOnCarrierDetect(boolean enable) {
		 port.notifyOnCarrierDetect(enable);
	}

	@Override
	public void notifyOnOverrunError(boolean enable) {
		 port.notifyOnOverrunError(enable);
	}

	@Override
	public void notifyOnParityError(boolean enable) {
		 port.notifyOnParityError(enable);
	}

	@Override
	public void notifyOnFramingError(boolean enable) {
		 port.notifyOnFramingError(enable);
	}

	@Override
	public void notifyOnBreakInterrupt(boolean enable) {
		 port.notifyOnBreakInterrupt(enable);
	}

	@Override
	public byte getParityErrorChar() throws UnsupportedCommOperationException {
		 try {
			return port.getParityErrorChar();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setParityErrorChar(byte b) throws UnsupportedCommOperationException {
		 try {
			return port.setParityErrorChar(b);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public byte getEndOfInputChar() throws UnsupportedCommOperationException {
		 try {
			return port.getEndOfInputChar();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setEndOfInputChar(byte b) throws UnsupportedCommOperationException {
		 try {
			return port.setEndOfInputChar(b);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setUARTType(String type, boolean test) throws UnsupportedCommOperationException {
		 try {
			return port.setUARTType(type, test);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public String getUARTType() throws UnsupportedCommOperationException {
		 try {
			return port.getUARTType();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setBaudBase(int BaudBase) throws UnsupportedCommOperationException, IOException {
		 try {
			return port.setBaudBase(BaudBase);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public int getBaudBase() throws UnsupportedCommOperationException, IOException {
		 try {
			return port.getBaudBase();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setDivisor(int Divisor) throws UnsupportedCommOperationException, IOException {
		 try {
			return port.setDivisor(Divisor);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public int getDivisor() throws UnsupportedCommOperationException, IOException {
		 try {
			return port.getDivisor();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setLowLatency() throws UnsupportedCommOperationException {
		 try {
			return port.setLowLatency();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean getLowLatency() throws UnsupportedCommOperationException {
		 try {
			return port.getLowLatency();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean setCallOutHangup(boolean NoHup) throws UnsupportedCommOperationException {
		 try {
			return port.setCallOutHangup(NoHup);
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public boolean getCallOutHangup() throws UnsupportedCommOperationException {
		 try {
			return port.getCallOutHangup();
		} catch (gnu.io.UnsupportedCommOperationException e) {
			throw new UnsupportedCommOperationException(e.getMessage());
		}
	}

	@Override
	public void serialEvent(SerialPortEvent spe) {
		
		fireSerialDeviceEvent(new SerialDeviceEvent(port, spe.getEventType(), spe.getOldValue(), spe.getNewValue()));
	}
	
	   // This methods allows classes to unregister for MyEvents
    public void removeSerialDeviceEventListener(SerialDeviceEventListener listener) {
        listenerList.remove(SerialDeviceEventListener.class, listener);
    }
	
	   // This private class is used to fire MyEvents
    void fireSerialDeviceEvent(SerialDeviceEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==SerialDeviceEventListener.class) {
                ((SerialDeviceEventListener)listeners[i+1]).serialEvent(evt);
            }
        }
    }

}
