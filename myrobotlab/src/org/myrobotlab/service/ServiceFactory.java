/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.ivy.Main;
import org.apache.ivy.util.cli.CommandLineParser;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.net.*;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Dependency;
import org.myrobotlab.framework.Ivy2;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.interfaces.GUI;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/*
 *   I would have liked to dynamically extract the possible Services from the code itself - unfortunately this is near impossible
 *   The methods to get this information are different depending on if app is running from the filesystem, from a jar, or from an applet.
 *   Additionally in some cases "all" classes need to be pulled back then filtered on services - which is not very practical.
 *   Sadly, these means a list needs to be maintained within this class.
 * 
 * 	References:
 * http://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java
 * 
 */

@Root
public class ServiceFactory extends Service {

	//public final static Logger LOG = Logger.getRootLogger();
	public final static Logger LOG = Logger.getLogger(ServiceFactory.class.getCanonicalName());
	public final static ServiceInfo info = ServiceInfo.getInstance();

	@Element
	public String proxyHost;
	@Element
	public String proxyPort;
	@Element
	public String proxyUserName;
	@Element
	public String proxyPassword;
	@Element
	public static String ivyFileName = "ivychain.xml";
	
	static GUI gui = null;
	private static final long serialVersionUID = 1L;	
		
	public ServiceFactory(String instanceName) {
		super(instanceName, ServiceFactory.class.getCanonicalName());
	}

	static void help() {
		System.out.println("ServiceFactory " + version());
		System.out.println("-h       			# help ");
		System.out.println("-list        		# list services");
		System.out.println("-logToConsole       # redirects logging to console");
		System.out.println("-logLevel        	# log level [DEBUG | INFO | WARNING | ERROR | FATAL]");
		System.out.println("-service [Service Name] [Service] ...");
		System.out.println("example:");
		System.out.println(helpString);
	}

	static String version() {
		String v = FileIO.getResourceFile("version.txt");
		System.out.println(v);
		return v;
	}

	static String helpString = "java -Djava.library.path=./bin org.myrobotlab.service.ServiceFactory -service services ServiceFactory gui GUIService -logLevel DEBUG -logToConsole";

	@Override
	public void loadDefaultConfiguration() {

	}

	public final static void invokeCMDLine(CMDLine cmdline) {

		if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
			help();
			return;
		}

		if (cmdline.containsKey("-v") || cmdline.containsKey("--version")) {
			version();
			return;
		}

		System.out.println("service count "
				+ cmdline.getArgumentCount("-service") / 2);

		if (cmdline.getArgumentCount("-service") > 0
				&& cmdline.getArgumentCount("-service") % 2 == 0) {

			for (int i = 0; i < cmdline.getArgumentCount("-service"); i += 2) {
								
				LOG.info("attempting to invoke : org.myrobotlab.service."
						+ cmdline.getSafeArgument("-service", i + 1, "") + " named " +
				 cmdline.getSafeArgument("-service", i, ""));

				Service s = ServiceFactory.create(
						cmdline.getSafeArgument("-service", i, ""),
						cmdline.getSafeArgument("-service", i + 1, ""));
				
				s.startService();
				
				Class<?> c = s.getClass().getSuperclass();				
				if (c.equals(GUI.class)) {
					gui = (GUI) s;
				}
			}
			if (gui != null) {
				gui.display();
			}

		} else if (cmdline.hasSwitch("-list")) {
			System.out.println(getServiceShortClassNames());

		} else {
			help();
			return;
		}
	}

	/**
	 * TODO - remove this and maintain it only in the map - getServiceShortNames will get a sorted list
	 * if keys
	 * @return - list of Service Names available
	 */
	static public String[] getServiceShortClassNames() {
		
		return info.getShortClassNames();
		// return getShortClassNames("org.myrobotlab.service",false);
		/*
		return new String[] { "Arduino", "Arm", "AudioCapture", "AudioFile",
				"ChessGame", "Clock", "DifferentialDrive",
				"FaceTracking", "FSM", "GeneticProgramming", "Graphics", "GUIService",
				"HTTPClient",  "JFugue", "JoystickService", "Jython","Keyboard",
				"Logging", "Motor", "OpenCV",
				"ParallelPort", "PICAXE", "PID", "PlayerStage",
				"RecorderPlayer", "RemoteAdapter", "Roomba","SensorMonitor",
				"Servo", "SLAM",  
				"Speech", "SpeechRecognition", "ServiceFactory",
				"SystemInformation", "TrackingService", "WiiDAR", "Wii" };
				*/
	}

	
	/**
	 * initially I thought that is would be a good idea to dynamically laod Services
	 * and append their definitions to the class path.
	 * This would "theoretically" be done with ivy to get/download the appropriate 
	 * dependent jars from the repo.  Then use a custom ClassLoader to load the new
	 * service.
	 * 
	 * Ivy works for downloading the appropriate jars & artifacts
	 * However, the ClassLoader became very problematic
	 * 
	 * There is much mis-information around ClassLoaders.  The most knowledgeable article
	 * I have found has been this one :
	 * http://blogs.oracle.com/sundararajan/entry/understanding_java_class_loading
	 * 
	 * Overall it became a huge PITA with really very little reward.
	 * The consequence is all Services' dependencies and categories are defined here
	 * rather than the appropriate Service class.
	 * 
	 * @return
	 */
	
	/**
	 * @param name - name of Service to be removed and whos resources will be released
	 */
	static public void removeService(String name) {
		RuntimeEnvironment.release(name);
	}

	// TODO - 3 values - description/example input & output
	/*
	@ToolTip("Add a new Services to the system")
	static public Service addService(String className, String newName) {
		LOG.info("adding service " + newName);
		Service s = (Service) Service.getNewInstance("org.myrobotlab.service."
				+ className, newName);
		s.startService();
		return s;
	}
	*/
	
	/**
	 * this "should" be the gateway function to a MyRobotLab instance
	 * going through this main will allow the see{@link}MyRobotLabClassLoader 
	 * to load the appropriate classes and give access to the addURL to allow dynamic
	 * additions of new modules without having to restart.
	 * 
	 * TODO :   -cmd <method> invokes the appropriate static method e.g. -cmd setLogLevel DEBUG
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		URL url = null;
		try {
			 url = new URL ("http://0.0.0.0:0");
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		System.out.println(url.getHost());
		System.out.println(url.getPort());

		// set the new dynamic class loader - done in the boot.jar / MyRobotLabClassLoader
		// Thread.currentThread().setContextClassLoader(MyRobotLabClassLoader.getInstance());
		// MyRobotLabClassLoader.classLoaderTreeString(o)
		
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		try {
			if (cmdline.containsKey("-logToConsole"))
			{
				addAppender(LOGGING_APPENDER_CONSOLE);
				setLogLevel(LOG_LEVEL_DEBUG);
			} else if (cmdline.containsKey("-logToRemote")) {
				String host = cmdline.getSafeArgument("-logToRemote", 0, "localhost");
				String port = cmdline.getSafeArgument("-logToRemote", 1, "4445"); 
				addAppender(LOGGING_APPENDER_SOCKET, host, port);
				setLogLevel(LOG_LEVEL_DEBUG);
			} else {			
				addAppender(LOGGING_APPENDER_ROLLING_FILE);
				setLogLevel(LOG_LEVEL_WARN);
			}
						
			if (cmdline.containsKey("-logLevel"))
			{
				setLogLevel(cmdline.getSafeArgument("-logLevel", 0, "DEBUG"));
			}

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND
			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work
			
			if (cmdline.containsKey("-update"))
			{
				// force all updates
				update();
				return;
			} else {
				invokeCMDLine(cmdline);
			}
		} catch (Exception e) {
			LOG.error(Service.stackToString(e));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	
	/*
	 * Service Loader
	 * Ivy
	 * ia64 is Intel Itanium so it is def not for you, also amd64 is not for you either. you want to look for x86, i386, i586 or i686 packages, I am not too sure about ia32, but if nothing else is there try ia32.
	 * repo interface - expectation is <cpu-platform>.<bitsize>.<os> e.g. x86.32.linux for confs
	 * http://www.cooljeff.co.uk/2009/08/01/handling-native-dependencies-with-apache-ivy/
	 * @see org.myrobotlab.framework.Service#getToolTip()
	 */
	
	/* Writing customer appenders
	 * 
	 *  http://www.javaworld.com/javaworld/jw-12-2004/jw-1220-toolbox.html?page=3
	 *  log4j.rootLogger=DEBUG, FILE, CONSOLE, REMOTE
		log4j.appender.FILE=org.apache.log4j.FileAppender
		log4j.appender.FILE.file=/tmp/logs/log.txt
		log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
		log4j.appender.FILE.layout.ConversionPattern=[%d{MMM dd HH:mm:ss}] %-5p (%F:%L) - %m%n
		log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
		log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
		log4j.appender.CONSOLE.layout.ConversionPattern=[%d{MMM dd HH:mm:ss}] %-5p (%F:%L) - %m%n
		log4j.appender.REMOTE=com.holub.log4j.RemoteAppender
		log4j.appender.REMOTE.Port=1234
		log4j.appender.REMOTE.layout=org.apache.log4j.PatternLayout
		log4j.appender.REMOTE.layout.ConversionPattern=[%d{MMM dd HH:mm:ss}] %-5p (%F:%L) - %m%n
	 */
	
	
	static public Service createAndStart (String name, String type)
	{
		Service s = create(name, type);
		if (s == null)
		{
			LOG.error("cannot start service " + name);
			return null;
		}
		s.startService();
		return s;
	}
	

	static public synchronized Service create(String name, String type) {
		return create(name, "org.myrobotlab.service.", type);
	}

	/**
	 * @param name - name of Service
	 * @param pkgName - package of Service in case Services are created in different packages
	 * @param type - type of Service
	 * @return
	 */
	static public synchronized Service create(String name, String pkgName,
			String type) {
		try {
			LOG.debug("ServiceFactory.create - Class.forName");
			// get String Class
			String typeName = pkgName + type;
			//Class<?> cl = Class.forName(typeName);
			//Class<?> cl = Class.forName(typeName, false, ClassLoader.getSystemClassLoader());
			return createService(name, typeName);
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	
	static public void update()
	{
		  Iterator<String> it = ServiceInfo.getKeySet().iterator();
		  while (it.hasNext()) {
		        String s = it.next();
		        getDependencies(s);
		    }
		  // TODO if (Ivy2.hasNewDependencies()) - schedule restart
	}
	
	static public void getDependencies(String fullTypeName)
	{
		LOG.debug("getDependencies " + fullTypeName);
		
		try {
			// use Ivy standalone			
			// Main.main(cmd.toArray(new String[cmd.size()]));
			// Method getDependencies = cls.getMethod("getDependencies");
			// Programmatic use of Ivy
			// https://cwiki.apache.org/IVY/programmatic-use-of-ivy.html
			ArrayList<Dependency> d = ServiceInfo.getDependencies(fullTypeName);

			if (d != null)
			{
				LOG.info(fullTypeName + " found " + d.size() + " dependencies");
				for (int i=0; i < d.size(); ++i)
				{					
					Dependency dep = d.get(i);					
					
					ArrayList<String> cmd = new ArrayList<String>();
					
					cmd.add("-cache");
					cmd.add(".ivy");
	
					cmd.add("-retrieve");
					cmd.add("libraries/[type]/[artifact].[ext]");
	
					cmd.add("-settings");
					//cmd.add("ivysettings.xml");
					cmd.add(ivyFileName);
	
					//cmd.add("-cachepath");
					//cmd.add("cachefile.txt");					
					
					cmd.add("-dependency");
					cmd.add(dep.organisation); // org
					cmd.add(dep.module); 		// module		
					cmd.add(dep.version); 	// version
					
					cmd.add("-confs");
					String confs = "runtime,"+RuntimeEnvironment.getArch()+"."+
					RuntimeEnvironment.getBitness()+"." + 
					RuntimeEnvironment.getOS();
					cmd.add(confs);
					
					CommandLineParser parser = Main.getParser();
					
					try {
						Ivy2.run(parser, cmd.toArray(new String[cmd.size()]));
					} catch (Exception e)
					{
						logException(e);
					}
					
					// local config - 
					
					// if the Service is downloaded we have to dynamically 
					// load the classes - if we are not going to restart
					// http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
				}
			} else {
				if (d == null)
				{
					LOG.info(fullTypeName + " returned no dependencies");
				}
			}
		} catch (Exception e) {
			Service.logException(e);
		}		
	}
	
	/**
	 * @param name
	 * @param cls
	 * @return
	 */
	static public synchronized Service createService(String name, String fullTypeName) {
		LOG.debug("ServiceFactory.createService");
		if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) //|| !cls.isInstance(Service.class)) \
		{
			LOG.error(fullTypeName + " not a type or " + name + " not defined ");
			return null;
		}
		
		ServiceWrapper sw = RuntimeEnvironment.getService(name);
		if (sw != null) {
			LOG.debug("service " + name + " already exists");
			return sw.service;
		}
				
		File ivysettings = new File(ivyFileName);
		if (ivysettings.exists())
		{
			getDependencies(fullTypeName);
			// TODO - if (Ivy2.newDependencies()) - schedule restart
		} else {
			LOG.debug(ivyFileName + " not available - will not manage dependencies");
		}

		try {
			
			// TODO - determine if there have been new classes added from ivy			
			LOG.debug("ABOUT TO LOAD CLASS");

			// MyRobotLabClassLoader loader = (MyRobotLabClassLoader)ClassLoader.getSystemClassLoader(); 
			// loader.addURL((new File("libraries/jar/RXTXcomm.jar").toURI().toURL()));
			// ClassLoader Info
			LOG.info("loader for this class " + ServiceFactory.class.getClassLoader().getClass().getCanonicalName());
			LOG.info("parent " + ServiceFactory.class.getClassLoader().getParent().getClass().getCanonicalName());
			LOG.info("system class loader " + ClassLoader.getSystemClassLoader());
			LOG.info("parent should be null" + ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
			LOG.info("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
			LOG.info("thread context parent " + Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
			//MyRobotLabClassLoader loader = (MyRobotLabClassLoader) = Thread.currentThread().getContextClassLoader();
			LOG.info("refreshing classloader");
			//MyRobotLabClassLoader.refresh();
//			Class<?> cls = Class.forName(fullTypeName);
//			Class<?> cls = MyRobotLabClassLoader.getClassLoader().loadClass(fullTypeName);
//			Class<?> cls = ServiceFactory.class.getClassLoader().loadClass(fullTypeName);
			Class<?> cls = Class.forName(fullTypeName);
 			Constructor<?> constructor = cls.getConstructor(new Class[] { String.class });

			// create an instance
			Object newService = constructor.newInstance(new Object[] { name });
			LOG.info("returning " + fullTypeName);
			return (Service)newService;
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}
	
	
	@Override
	public String getToolTip() {
		return "<html>Service used to create other services. This service, like other services,<br>"
				+ "does not need a GUI.<br>"
				+ "Services can be created on the command line by using the ServiceFactory in the following way.<br> "
				+ "-service [Service] [Service Name] ...<br>"
				+ "The following example will create an ServiceFactory service named<br> \"services\" and a GUIService named \"gui\"<br>"
				+ "<b><i>" + helpString + "</i></b></html>";
	}

}
