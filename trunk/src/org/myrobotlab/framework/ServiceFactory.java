package org.myrobotlab.framework;

import java.lang.reflect.Constructor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.service.Speech;

public class ServiceFactory {

	public final static Logger LOG = Logger.getLogger(ServiceFactory.class.toString());

	static public synchronized Service createService(String name, String type) {
		try {

			// get String Class
			String typeName = "org.myrobotlab.service." + type;
			Class<?> cl = Class.forName(typeName);
			return createService(name, cl);
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	static public synchronized Service createService(String name, Class<?> cls) {
		ServiceWrapper sw = RuntimeEnvironment.getService(name);
		if (sw != null) {
			LOG.debug("service " + name + " already exists");
			return sw.service;
		}

		if (name == null || name.equals("") || cls == null)
				//|| !cls.isInstance(Service.class)) \
		{
			LOG.error(cls + " not a type or " + name + " not defined ");
			return null;
		}

		try {
			// get the constructor with one parameter
			Constructor<?> constructor = cls
					.getConstructor(new Class[] { String.class });

			// create an instance
			Object invoker = constructor.newInstance(new Object[] { name });

			Service newService = (Service) invoker;

			return newService;
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Speech speech = (Speech) ServiceFactory.createService(
				"mySpeechService", "Speech");
		speech.speak("hello world");
	}

}
