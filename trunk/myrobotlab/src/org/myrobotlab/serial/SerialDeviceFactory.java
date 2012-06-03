package org.myrobotlab.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.arduino.compiler.SerialNotFoundException;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;

import org.myrobotlab.framework.Platform;

/**
 * @author GroG
 * 
 *         This factory was created to reconcile the different SerialDevices and
 *         serial device frameworks which are incompatible depending on platform
 *         specifics (eg. Android's bluetooth & gnu serial)
 */
public class SerialDeviceFactory implements SerialDeviceEventListener {

	public final static Logger log = Logger.getLogger(SerialDeviceFactory.class.getCanonicalName());

	final public static String TYPE_GNU = "org.myrobotlab.serial.gnu";
	final public static String TYPE_ANDROID_BLUETOOTH = "android.somethin";

	public static ArrayList<SerialDeviceIdentifier> getDeviceIdentifiers() {
		
		
		if (Platform.isDavlik())
		{
			return getDeviceIdentifiers(TYPE_ANDROID_BLUETOOTH);
		} else {
			return getDeviceIdentifiers(TYPE_GNU);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<SerialDeviceIdentifier> getDeviceIdentifiers(String type) {
		ArrayList<SerialDeviceIdentifier> ret = new ArrayList<SerialDeviceIdentifier>();
		try {
			Class<?> c = Class.forName("org.myrobotlab.serial.gnu.SerialDeviceIdentifierGNU");
			System.out.println("Loaded class: " + c);
			Method m = c.getDeclaredMethod("getSerialDeviceIdentifiers", (Class<?>[])null);
			System.out.println("Got method: " + m);
			return (ArrayList<SerialDeviceIdentifier>) m.invoke(null, (Object[])null);
		} catch (Exception e) {
			Service.logException(e);
		}
		return ret;
	}

	@Override
	public void serialEvent(SerialDeviceEvent event) {
		log.info(event.getEventType());
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		SerialDeviceFactory sdf = new SerialDeviceFactory();
		sdf.test();

	}

	public void test()
	{
		ArrayList<SerialDeviceIdentifier> devices = SerialDeviceFactory.getDeviceIdentifiers(SerialDeviceFactory.TYPE_GNU);
		for (int i = 0; i < devices.size(); ++i)
		{
			SerialDeviceIdentifier sdi = devices.get(i);
			log.info(sdi);
			if (sdi.getName().equals("COM9"))
			{
				try {
					SerialDevice device = sdi.open("robot overloads", i);
					device.addEventListener(this);
					OutputStream out = device.getOutputStream();
					out.write(Arduino.ANALOG_READ_POLLING_START);
					out.write(15);// ??? Is this right
					out.write(0); // 0 - 180

					//out.write(new byte[]{0,1,0,1});
				} catch (Exception e) {
					log.error(e.getMessage());
				}
			}
		}
	}
	
	public SerialDevice getSerialDevice(String name, int rate, int parity, int databits, int stopbits) throws SerialException
	{
		SerialDevice port = null;
		try {
			ArrayList<SerialDeviceIdentifier> portList = SerialDeviceFactory
					.getDeviceIdentifiers(SerialDeviceFactory.TYPE_GNU);
			for (int i = 0; i < portList.size(); ++i) {
				SerialDeviceIdentifier portId = portList.get(i);
				if (portId.getPortType() == SerialDeviceIdentifier.PORT_SERIAL) {
					// System.out.println("found " + portId.getName());
					if (portId.getName().equals(name)) {
						// System.out.println("looking for "+iname);
						port = (SerialDevice) portId.open("serial madness", 2000);
						port.setSerialPortParams(rate, databits, stopbits, parity);
						port.addEventListener(this);
						port.notifyOnDataAvailable(true);
						// System.out.println("opening, ready to roll");
					}
				}
			}
		} catch (Exception e) {
			throw new SerialException("Error opening serial port '" + name + "'." + e.getMessage(), e);
			// FIXME - fix back with 2 exceptions
			/*
			 * throw new SerialException("Serial port '" + iname +
			 * "' already in use.  Try quiting any programs that may be using it."
			 * ); } catch (Exception e) { throw new
			 * SerialException("Error opening serial port '" + iname + "'.", e);
			 */
			// //errorMessage("<init>", e);
			// //exception = e;
			// //e.printStackTrace();
		}

		if (port == null) {
			throw new SerialNotFoundException("Serial port '" + name
					+ "' not found.  Did you select the right one from the Tools > Serial Port menu?");
		}
		
		return port;
	}
	
}
