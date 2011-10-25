package org.myrobotlab.framework;

import java.lang.reflect.Constructor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.service.Speech;

public class ServiceFactory {

	public final static Logger LOG = Logger.getLogger(ServiceFactory.class.toString());

	static public synchronized Service createService(String name, String type)
	{
		ServiceWrapper sw = RuntimeEnvironment.getService(name);
		if ( sw != null)
		{
			LOG.debug("service " + name + " already exists");
			return sw.service; 
		}
		
	       try {
	           String methodName = "toLowerCase";

	           // get String Class
	           String typeName = "org.myrobotlab.service." + type;
	           Class<?> cl = Class.forName(typeName);

	           // get the constructor with one parameter
	           Constructor<?> constructor = cl.getConstructor (new Class[] {String.class});

	           // create an instance
	           Object invoker = constructor.newInstance (new Object[]{name});

	           Service newService =  (Service) invoker;
	           
	           newService.startService(); // TODO make safe
	           
	           return newService;
	           /*
	           // the method has no argument
	           Class<?>  arguments[] = new Class[] { };

	           // get the method
	           java.lang.reflect.Method objMethod =
	              cl.getMethod(methodName, arguments);

	           // convert "REAL'S HOWTO" to "real's howto"
	           Object result =
	              objMethod.invoke
	                (invoker, (Object[])arguments);

	           System.out.println(result);
	           */
	         }
	         catch (Exception e) {
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

		Speech speech = (Speech)ServiceFactory.createService("mySpeechService", "Speech");
		speech.speak("hello world");
	}

}
