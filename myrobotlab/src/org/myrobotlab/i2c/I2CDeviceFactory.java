package org.myrobotlab.i2c;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class I2CDeviceFactory {

	public static I2CDevice createI2CDevice(String clazz, int bus, int address) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException,
			InstantiationException, IllegalAccessException, InvocationTargetException {

		Object[] param = new Object[0];

		Class<?> c;
		c = Class.forName(clazz);
		Class<?>[] paramTypes = new Class[param.length];
		for (int i = 0; i < param.length; ++i) {
			paramTypes[i] = param[i].getClass();
		}
		Constructor<?> mc = c.getConstructor(paramTypes);
		return (I2CDevice) mc.newInstance(param);

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
