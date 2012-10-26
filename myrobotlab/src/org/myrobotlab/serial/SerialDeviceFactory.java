package org.myrobotlab.serial;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Platform;

/**
 * @author GroG
 * 
 *         This factory was created to reconcile the different SerialDevices and
 *         serial device frameworks which are incompatible depending on platform
 *         specifics (eg. Android's bluetooth & gnu serial)
 */
public class SerialDeviceFactory  {

	public final static Logger log = Logger.getLogger(SerialDeviceFactory.class.getCanonicalName());

	final public static String TYPE_GNU = "org.myrobotlab.serial.gnu.SerialDeviceFactoryGNU";
	final public static String TYPE_ANDROID_BLUETOOTH = "android.somethin";

	static public ArrayList<String> getSerialDeviceNames() {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<SerialDevice> devices = getSerialDevices();
		for (int i = 0; i < devices.size(); ++i)
		{
			ret.add(devices.get(i).getName());
		}
		return ret;
	}
	
	static public ArrayList<SerialDevice> getSerialDevices() {
		if (Platform.isDavlik())
		{
			return getSerialDevices(TYPE_ANDROID_BLUETOOTH);
		} else {
			return getSerialDevices(TYPE_GNU);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	static public ArrayList<SerialDevice> getSerialDevices(String type) {
		ArrayList<SerialDevice> ret = new ArrayList<SerialDevice>();
		try {
			Class<?> c = Class.forName(type);
			log.info("Loaded class: " + c);
			Object serialDeviceFramework = c.newInstance();
			Method m = c.getDeclaredMethod("getSerialDevices", (Class<?>[])null);
			log.info("Got method: " + m);
			return (ArrayList<SerialDevice>) m.invoke(serialDeviceFramework, (Object[])null);
		} catch (Exception e) {
			log.error(e.getMessage());// FIXME - logexception
		}
		return ret;
	}
	
	static public SerialDevice getSerialDevice(String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException
	{
		if (Platform.isDavlik())
		{
			// FIXME Bluetooth rate databits stopbits & parity are all meaningless
			return getSerialDevice(TYPE_ANDROID_BLUETOOTH, name, rate, databits, stopbits, parity);
		} else {
			return getSerialDevice(TYPE_GNU, name, rate, databits, stopbits, parity);
		}		
	}
	
	static public SerialDevice getSerialDevice(String factoryType, String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException
	{
		log.info(String.format("getSerialDevice %s|%d|%d|%d|%d", name,rate,databits,stopbits,parity));
		
		SerialDevice port = null;
		
		try {
			Class<?> c = Class.forName(factoryType);
			log.info("Loaded class: " + c);
			Object serialDeviceFramework = c.newInstance();
			Method m = c.getDeclaredMethod("getSerialDevice", new Class<?>[]{String.class, int.class, int.class, int.class, int.class});
			log.info("Got method: " + m);
			return (SerialDevice) m.invoke(serialDeviceFramework, new Object[]{name, rate, databits, stopbits, parity});
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());// FIXME - logexception
		}
	
		return port;
	}
	
	public static void main(String[] args) throws IOException {
		org.apache.log4j.BasicConfigurator.configure();
		//Logger.getRootLogger().setLevel(Level.DEBUG);
		
		ArrayList<SerialDevice> serialDevices =  SerialDeviceFactory.getSerialDevices();
		for (int i = 0; i < serialDevices.size(); ++i)
		{
			SerialDevice serialDevice = serialDevices.get(i);
			log.info(serialDevice.getName());
		}
		
		Class<?>[] d = new Class<?>[]{int.class};
		
		String portName = "COM7";
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(portName, 57600, 8, 1, 0); // TODO/FIXME - serialdevice identifier - opened by someone else
			sd.open();
			log.info(sd.isOpen());
			log.info(sd.isOpen());
			sd.write(new byte[]{0,1,2,3,4,5,6,7});
			sd.close();
			log.info(sd.isOpen());
			sd.open();
			sd.write(new byte[]{0,1,2,3,4,5,6,7});
			log.info(sd.isOpen());
		} catch (SerialDeviceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
