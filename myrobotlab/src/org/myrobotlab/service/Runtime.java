package org.myrobotlab.service;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;

/**
 * Runtime is just a service wrapper for the methods which already exist
 * in RuntimeEnvironment object - it also can handle notifications and event
 * of the global RuntimeEnvironment changing...
 * 
 * It also wraps the real JVM Runtime object
 *
 */
public class Runtime extends Service {

	private static final long serialVersionUID = 1L;
	
	public final static Logger LOG = Logger.getLogger(Runtime.class.getCanonicalName());

	public Runtime(String n) {
		super(n, Runtime.class.getCanonicalName());
	}
	
	public Service register(Service s)
	{		
		return RuntimeEnvironment.register(s);
	}

	public boolean register(URL url, ServiceEnvironment se)
	{
		return RuntimeEnvironment.register(url, se);
	}
	
	/**
	 * registration event
	 * @param name - the name of the Service which was successfully registered
	 * @return
	 */
	public String registered (String name)
	{
		return name;
	}
	
	/**
	 * release event 
	 * @param name - the name of the Service which was successfully released
	 * @return
	 */
	public String released (String name)
	{
		return name;
	}
	
	/**
	 * collision event - when a registration is attempted but there is a 
	 * name collision
	 * @param name - the name of the two Services with the same name
	 * @return
	 */
	public String collision (String name)
	{
		return name;
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "Runtime singleton service";
	}
	
	public int exec (String[] params)
	{
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		try {
			Process p = r.exec(params);
			return p.exitValue();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}
		
		return 0;
	}
		
	// dorky pass-throughs to the real JVM Runtime
	public static final long getTotalMemory()
	{
	    return java.lang.Runtime.getRuntime().totalMemory();
	}

	public static final long getFreeMemory()
	{
	    return java.lang.Runtime.getRuntime().freeMemory();
	}

	public static final int availableProcessors()
	{
	    return java.lang.Runtime.getRuntime().availableProcessors();
	}

	public static final void exit(int status)
	{
	    java.lang.Runtime.getRuntime().exit(status);
	}

	public static final void gc()
	{
	    java.lang.Runtime.getRuntime().gc();
	}

	public static final void load(String filename)
	{
	    java.lang.Runtime.getRuntime().load(filename);
	}

	public static final void loadLibrary(String filename)
	{
	    java.lang.Runtime.getRuntime().loadLibrary(filename);
	}
	
	public static final Platform getPlatform()
	{
		return RuntimeEnvironment.getPlatform();
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Runtime template = new Runtime("runtime");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
