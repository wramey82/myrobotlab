package org.myrobotlab.webgui;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class TypeConverter {

	public final static Logger log = LoggerFactory.getLogger(TypeConverter.class.getCanonicalName());

	private static TypeConverter converterInstance;

	private TypeConverter() {
	}

	public static synchronized TypeConverter getInstance() {
		if (converterInstance == null) {
			converterInstance = new TypeConverter();
		}
		return converterInstance;
	}

	// FIXME - not thread safe !!!
	// Pointers to arrays of Typed paramters - necessary to get the correctly matched method
	// Possible Optimization -> pointers to known method signatures - optimization so that once a
	// method's signature is processed and
	// known conversion exists - it is saved
	static public HashMap<String, Method[]> knownMethodSignatureConverters = new HashMap<String, Method[]>();
	// pointers to conversion methods
	//static public HashMap<String, Method> conversions = new HashMap<String, Method>();

	// FIXME - these possibly "should not" be static for thread safety
	// -------- primitive boxed types conversion begin ------------
	static public byte StringToByte(String in) {
		return Byte.parseByte(in);
	}

	static public short StringToShort(String in) {
		return Short.parseShort(in);
	}

	static public int StringToInteger(String in) {
		return Integer.parseInt(in);
	}

	static public long StringToLong(String in) {
		return Long.parseLong(in);
	}

	static public float StringToFloat(String in) {
		return Float.parseFloat(in);
	}

	static public double StringToDouble(String in) {
		return Double.parseDouble(in);
	}

	static public boolean StringToBoolean(String in) {
		return Boolean.parseBoolean(in);
	}

	static public char StringToChar(String in) {
		return in.charAt(0);
	}

	static public String StringToString(String in) {
		return in;
	}
	
	// -------- primitive boxed types conversion end ------------
	
	/**
	 * this method tries to get the appropriate 'Typed parameter arry for a specific method
	 * It "converts" parameters of strings into typed parameters which can then be used to 
	 * reflectively invoke the appropriate method
	 * 
	 * @param clazz
	 * @param method
	 * @param stringParams
	 * @return
	 */
	static public Object[] getTypedParams(Class<?> clazz, String method, String[] stringParams)
	{
		// make method/ordinal signature key
		// NOTE - ordinal loses info (ambiguous) - an extra hack would be
		// to resolve ambiguity with method which type conversions are possible
		String key = String.format("%s.%d", method, stringParams.length);
		
		Method[] converter = null;
		try {
		if (!knownMethodSignatureConverters.containsKey(key)) {
			// getDeclaredMethods - this class only
			// getMethods - all methods
			Method[] methods = clazz.getMethods();
			for (int i = 0; i < methods.length; ++i) {
				Method m = methods[i];
				Class<?>[] types = m.getParameterTypes();
				if (method.equals(m.getName()) && stringParams.length == types.length) {
					log.info("method with same ordinal of params found {}.{} - building new converter", method, stringParams.length);

					// create a converter - hopefully we can build it !
					// fixme - this will creat a "primitive only signature" -
					// you need both for methods out there which
					// use the boxed types
					
						Method[] conversion = new Method[types.length];
						for (int j = 0; j < types.length; ++j) {
							Class<?> pType = types[j];
							// TODO - optimize String out
							if (pType == String.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToString", new Class<?>[] { String.class });
							} else if (pType == byte.class || pType == Byte.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToByte", new Class<?>[] { String.class });
							} else if (pType == short.class || pType == Short.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToShort", new Class<?>[] { String.class });
							} else if (pType == int.class || pType == Integer.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToInteger", new Class<?>[] { String.class });
							} else if (pType == long.class || pType == Long.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToLong", new Class<?>[] { String.class });
							} else if (pType == float.class || pType == Float.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToFloat", new Class<?>[] { String.class });
							} else if (pType == double.class || pType == Double.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToDouble", new Class<?>[] { String.class });
							} else if (pType == boolean.class || pType == Boolean.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToBoolean", new Class<?>[] { String.class });
							} else if (pType == char.class || pType == Character.class) {
								conversion[j] = converterInstance.getClass().getMethod("StringToChar", new Class<?>[] { String.class });
							} else {
								log.error("can not convert String to needed parameter type %s", pType.getSimpleName());
								return null;
							}
						}
						
						knownMethodSignatureConverters.put(key, conversion);
						converter = conversion;
						break;
				}
			}
		} else {
			// typed parameter signature already known
			converter = knownMethodSignatureConverters.get(key);
		}
		
		// we've made it this far, which means we've either built a 
		// converter or retrieved a cached version
		// now we are going to make a new type'd parameter array
		
		Object[] newTypedParams = new Object[stringParams.length];
		for (int i = 0; i < stringParams.length; ++i)
		{
			// static calls on conversion - probably not thread safe
			newTypedParams[i] = converter[i].invoke(null, stringParams[i]);
		}
		
		return newTypedParams;
		
		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
		
	}
	
	static public Object[] convert(String[] stringParams, Method[] converter)
	{
		try {
		Object[] newTypedParams = new Object[stringParams.length];
		for (int i = 0; i < stringParams.length; ++i)
		{
			// static calls on conversion - probably not thread safe
			newTypedParams[0] = converter[i].invoke(null, stringParams[i]);
		}
		
		return newTypedParams;
		} catch(Exception e)
		{
			Logging.logException(e);
		}
		
		return null;
	}

	public static void main(String[] args) {
		
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		TypeConverter.getInstance();
		
		org.myrobotlab.service.Runtime.createAndStart("clock", "Clock");
		
		ServiceInterface si = org.myrobotlab.service.Runtime.getService("clock");
		
		String stringParams[] = new String[] { "13", "1" };
		String method = "digitalWrite";
		Class<?> clazz = si.getClass();
		
		Object[] params = getTypedParams(clazz, method, stringParams);
		
		si.invoke(method, params);
		
		log.info("here");

		Object[] params2 = getTypedParams(clazz, method, stringParams);
		log.info("here");
	}

}
