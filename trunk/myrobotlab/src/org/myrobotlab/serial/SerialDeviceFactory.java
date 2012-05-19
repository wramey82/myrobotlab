package org.myrobotlab.serial;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;

/**
 * @author GroG
 * 
 *         This factory was created to reconcile the different SerialDevices and
 *         serial device frameworks which are incompatible depending on platform
 *         specifics (eg. Android's bluetooth & gnu serial)
 */
public class SerialDeviceFactory implements SerialDeviceEventListener {

	public final static Logger LOG = Logger.getLogger(SerialDeviceFactory.class.getCanonicalName());

	final public static String TYPE_GNU = "org.myrobotlab.serial.gnu";
	final public static String TYPE_ANDROID_BLUETOOTH = "org.myrobotlab.serial.gnu";

	public static ArrayList<SerialDeviceIdentifier> getDeviceIdentifiers(String type) {
		ArrayList<SerialDeviceIdentifier> ret = new ArrayList<SerialDeviceIdentifier>();
		try {
			if (TYPE_GNU.equals(type)) {
				Class<?> c = Class.forName("org.myrobotlab.serial.gnu.SerialDeviceIdentifierGNU");
				System.out.println("Loaded class: " + c);
				Method m = c.getDeclaredMethod("getSerialDeviceIdentifiers", (Class<?>[])null);
				System.out.println("Got method: " + m);
				return (ArrayList<SerialDeviceIdentifier>) m.invoke(null, (Object[])null);
			} else if (TYPE_ANDROID_BLUETOOTH.equals(type)) {
				Class.forName(type);
			} else {
				LOG.error(type + " not found");
			}

		} catch (Exception e) {
			Service.logException(e);
		}
		return ret;
	}

	@Override
	public void serialEvent(SerialDeviceEvent event) {
		LOG.info(event.getEventType());
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
			LOG.info(sdi);
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
					LOG.error(e.getMessage());
				}
			}
		}
	}
	
}
