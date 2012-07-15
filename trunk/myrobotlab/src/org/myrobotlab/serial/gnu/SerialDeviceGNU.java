package org.myrobotlab.serial.gnu;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EventListener;
import java.util.TooManyListenersException;

import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceEvent;
import org.myrobotlab.serial.SerialDeviceEventListener;
import org.myrobotlab.serial.SerialDeviceException;

/**
 * @author GroG
 * 
 *         A silly but necessary wrapper class for gnu.io.SerialPort, since
 *         RXTXComm driver initializes loadlibrary in a static block and the
 *         driver dynamically loaded is loaded with a hardcoded string :P
 * 
 */
public class SerialDeviceGNU implements SerialDevice, SerialPortEventListener {

	public final static Logger log = Logger.getLogger(SerialDeviceGNU.class.getCanonicalName());

	private gnu.io.SerialPort port;

	// defaults
	private int rate = 57600;
	private int databits = 8;
	private int stopbits = 1;
	private int parity = 0;

	transient InputStream input;
	transient OutputStream output;

	/* no internal buffer
	 * the result can be a broken message if
	 * an Arduino is loaded while consuming messages - since we cannot "clear"
	 * the broken message from here 
	 * A possible solution would be to handle messages based on length or stop character
	 * which migh be a nice addition - but currently not implemented
	byte buffer[] = new byte[32768];
	int bufferIndex;
	int bufferLast;
	*/

	protected EventListenerList listenerList = new EventListenerList();
	private CommPortIdentifier commPortId;

	
	public SerialDeviceGNU(CommPortIdentifier portId) throws SerialDeviceException {
		this.commPortId = portId;
	}

	public SerialDeviceGNU(CommPortIdentifier portId, int rate, int databits, int stopbits, int parity) {
		this.commPortId = portId;
		this.rate = rate;
		this.databits = databits;
		this.stopbits = stopbits;
		this.parity = parity;
	}

	public String getPortString()
	{
		return String.format(("%s/%d/%d/%d/%d"),port.getName(),port.getBaudRate(),
				port.getDataBits(),port.getParity(),port.getStopBits());
	}
	
	@Override
	public boolean isOpen()
	{
		return port != null;
	}

	@Override
	public void close() {
		log.debug(String.format("closing %s", commPortId.getName()));
		
		if (port == null)
		{
			log.warn(String.format("serial device %s already closed", commPortId.getName()));
			return;
		}
		port.removeEventListener();
	
		Object[] listeners = listenerList.getListenerList();
		for (int i = 1; i < listeners.length; i+=2) {
			listenerList.remove(EventListener.class, (EventListener)listeners[i]);
		  }
		
		
		log.info(String.format("closing SerialDevice %s",getPortString()));
		try {
			// do io streams need to be closed first?
			if (input != null)
				input.close();
			if (output != null)
				output.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * new Thread(){
		 * 
		 * @Override public void run(){ serialPort.removeEventListener();
		 * serialPort.close(); serialPort = null; } }.start();
		 */

		input = null;
		output = null;
		if (port != null)
			port.close();
		port = null;
		log.debug(String.format("closed %s", commPortId.getName()));
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
		//return port.getName();
		return commPortId.getName();
	}

	@Override
	public void setParams(int rate, int databits, int stopbits, int parity) throws SerialDeviceException
	 {
		try {
			log.debug(String.format("setSerialPortParams %d %d %d %d", rate, databits, stopbits, parity));
			port.setSerialPortParams(rate, databits, stopbits, parity);
		} catch (UnsupportedCommOperationException e) {
			throw new SerialDeviceException("unsupported comm operation " + e.getMessage());
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
	public void notifyOnDataAvailable(boolean enable) {
		port.notifyOnDataAvailable(enable);
	}

	@Override
	public void addEventListener(SerialDeviceEventListener lsnr)
			throws TooManyListenersException {
		// proxy events
		listenerList.add(SerialDeviceEventListener.class, lsnr);
		port.addEventListener(this);
	}

	@Override
	public void serialEvent(SerialPortEvent spe) {

		fireSerialDeviceEvent(new SerialDeviceEvent(port, spe.getEventType(),
				spe.getOldValue(), spe.getNewValue()));
	}

	// This methods allows classes to unregister for MyEvents
	public void removeSerialDeviceEventListener(SerialDeviceEventListener listener) {
		log.debug("removeSerialDeviceEventListener");
		listenerList.remove(SerialDeviceEventListener.class, listener);
	}

	// This private class is used to fire MyEvents
	void fireSerialDeviceEvent(SerialDeviceEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == SerialDeviceEventListener.class) {
				((SerialDeviceEventListener) listeners[i + 1]).serialEvent(evt);
			}
		}
	}


	@Override
	public void write(int[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			output.write(data[i]);
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		for (int i = 0; i < data.length; ++i) {
			output.write(data[i]);
		}
	}

	@Override
	public void write(String data) throws IOException {
		for (int i = 0; i < data.length(); ++i) {
			output.write(data.charAt(i));
		}
	}

	@Override
	public void write(int data) throws IOException {
		output.write(data);
	}

	@Override
	public void write(byte data) throws IOException {
		output.write(data);
	}

	@Override
	public void write(char data) throws IOException {
		output.write(data);
	}

	@Override
	public String getCurrentOwner() {
		if (commPortId != null)
			return commPortId.getCurrentOwner();
		return null;
	}

	@Override
	public int getPortType() {
		return commPortId.getPortType();
	}

	@Override
	public boolean isCurrentlyOwned() {
		return commPortId.isCurrentlyOwned();
	}

	@Override
	public void open() throws SerialDeviceException {
		try {
			log.info(String.format("opening %s", commPortId.getName()));
			port = (SerialPort)commPortId.open(commPortId.getName(), 1000);
			port.setSerialPortParams(rate, databits, stopbits, parity);
			output = port.getOutputStream();
			input = port.getInputStream();
			log.info(String.format("opened %s", commPortId.getName()));
		} catch(PortInUseException e) 
		{
			Service.logException(e);
			throw new SerialDeviceException("port in use " + e.getMessage());
		} catch (UnsupportedCommOperationException e) {
			Service.logException(e);
			throw new SerialDeviceException("UnsupportedCommOperationException " + e.getMessage());
		} catch (IOException e) {
			Service.logException(e);
			throw new SerialDeviceException("IOException " + e.getMessage());
		}
	}
	
	@Override
	public int read() throws IOException 
	{
		return input.read();
	}

}
