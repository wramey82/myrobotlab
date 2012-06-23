/**
 * Helper class for instantiating objects using string values.
 */
package org.myrobotlab.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.myrobotlab.logging.Logger;

/**
 * Class to help make life easier when instantiating objects using
 * the String name of the class along with an optional array of
 * constructor parameters.
 * 
 * @author SwedaKonsult
 *
 */
public class Instantiator {
	/**
	 * Logger
	 */
	private static final Logger log = Logger.getLogger(Instantiator.class);
	
	/**
	 * Create an instance of the classname.
	 * 
	 * @param classname
	 * @param params
	 * @return null if anything fails
	 */
	public static <T> T getNewInstance(String classname, Object... params) {
		if (classname == null || classname.isEmpty()) {
			return null;
		}
		try {
			Class<?> c = Class.forName(classname);
			return Instantiator.<T>getNewInstance(c, params);
		} catch (ClassNotFoundException e) {
			log.error("Error", e);
		} catch (SecurityException e) {
			log.error("Error", e);
		} catch (RuntimeException e) {
			log.error("Error", e);
		}
		return null;
	}
	
	/**
	 * Create an instance of Class.
	 * 
	 * @param c
	 * @param params
	 * @return null if anything fails
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getNewInstance(Class<?> c, Object... params) {
		if (c == null) {
			return null;
		}
		try {
			Class<?>[] paramTypes = getParameterTypes(params);
			Constructor<?> mc = c.getConstructor(paramTypes);
			return (T) mc.newInstance(params);
		} catch (SecurityException e) {
			log.error("Error", e);
		} catch (NoSuchMethodException e) {
			log.error("Error", e);
		} catch (RuntimeException e) {
			log.error("Error", e);
		} catch (InstantiationException e) {
			log.error("Error", e);
		} catch (IllegalAccessException e) {
			log.error("Error", e);
		} catch (InvocationTargetException e) {
			log.error("Error", e);
		}
		return null;
	}

	/**
	 * Invoke in the context of this Service
	 * 	
	 * @param method
	 * @param params
	 * @return null if anything fails
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethod(Object object, String method, Object... params) 
	{
		if (object == null || method == null || method.isEmpty()) {
			return null;
		}
		Class<?> c = object.getClass();
		Class<?>[] paramTypes = getParameterTypes(params);
		try {
			Method meth = c.getMethod(method, paramTypes);
			return (T) meth.invoke(object, params);
		} catch (NoSuchMethodException e) {
			log.error("Error", e);
		} catch (SecurityException e) {
			log.error("Error", e);
		} catch (IllegalAccessException e) {
			log.error("Error", e);
		} catch (IllegalArgumentException e) {
			log.error("Error", e);
		} catch (InvocationTargetException e) {
			log.error("Error", e);
		}
		return null;
	}

	/**
	 * Parse the Class out of the passed-in objects. If an object is null, null will be used.
	 * 
	 * @param params
	 * @return
	 */
	private static Class<?>[] getParameterTypes(Object[] params) {
		Class<?>[] paramTypes = null;
		// Class<?>[] paramTypes = null;
		if (params == null) {
			return paramTypes;
		}
		paramTypes = new Class[params.length];
		for (int i = 0; i < params.length; ++i) {
			if (params[i] == null) {
				paramTypes[i] = null;
				continue;
			}
			paramTypes[i] = params[i].getClass();
		}
		return paramTypes;
	}
}
