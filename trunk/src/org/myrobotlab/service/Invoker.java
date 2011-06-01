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
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ToolTip;
import org.myrobotlab.service.interfaces.GUI;

/*
 *   I would have liked to dynamically extract the possible Services from the code itself - unfortunately this is near impossible
 *   The methods to get this information are different depending on if app is running from the filesystem, from a jar, or from an applet.
 *   Additionally in some cases "all" classes need to be pulled back then filtered on services - which is not very practical.
 *   Sadly, these means a list needs to be maintained within this class.
 * 
 * 
 */

public class Invoker extends Service {

	private static final long serialVersionUID = 1L;

	public Invoker(String instanceName) {
		super(instanceName, Invoker.class.getCanonicalName());
	}

	public final static Logger LOG = Logger.getRootLogger();
	static GUI gui = null;

	static void help() {
		System.out.println("Invoker " + version());
		System.out.println("-h       # help ");
		System.out.println("-list        # list services");
		System.out.println("-service [Service] [Service Name] ...");
		System.out.println("example:");
		System.out.println(helpString);
	}

	static String version() {
		String v = FileIO.getResourceFile("version.txt");
		System.out.println(v);
		return v;
	}

	static String helpString = "java -Djava.library.path=./bin org.myrobotlab.service.Invoker -service Invoker services GUIService gui";

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
				// TODO -remove GUIService2
				LOG.info("attempting to invoke : org.myrobotlab.service."
						+ cmdline.getSafeArgument("-service", i, "") + " "
						+ cmdline.getSafeArgument("-service", i + 1, ""));
				Service s = (Service) Service.getNewInstance(
						"org.myrobotlab.service."
								+ cmdline.getSafeArgument("-service", i, ""),
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

	static public String[] getServiceShortClassNames() {
		// return getShortClassNames("org.myrobotlab.service",false);
		return new String[] { "Arduino", "Arm", "AudioCapture", "AudioFile",
				"ChessGame", "Clock", "DifferentialDrive",
				"FaceTracking", "FSM", "GeneticProgramming", "Graphics", "GUIService",
				"HTTPClient", "Invoker", "JFugue", "JoystickService",
				"Logging", "MediaSource", "Motor", "OpenCV",
				"ParallelPort", "PICAXE", "PID", "Player", "PlayerStage",
				"RecorderPlayer", "RemoteAdapter", "Rose", "SensorMonitor",
				"Serial", "Servo", "SLAM", "SoccerGame", 
				"Speech", "SpeechRecognition", "StepperMotor",
				"SystemInformation", "TrackingService", "WiiDAR", "Wii" };
	}

	static public void removeService(String name) {
		LOG.info("removing service " + name);
		//super.removeServices(sdu) TODO
	}

	// TODO - 3 values - description/example input & output
	@ToolTip("Add a new Services to the system")
	static public Service addService(String className, String newName) {
		LOG.info("adding service " + newName);
		Service s = (Service) Service.getNewInstance("org.myrobotlab.service."
				+ className, newName);
		s.startService();
		return s;
	}

	public void setLogLevel(String level) {
		if (level == null) {
			LOG.setLevel(Level.DEBUG);
			return;
		}

		if (level.compareTo("INFO") == 0) {
			//LOG.setLevel(Level.INFO);
			Logger.getRootLogger().setLevel(Level.INFO);
		}
		if (level.compareTo("WARN") == 0) {
			//LOG.setLevel(Level.WARN);
			Logger.getRootLogger().setLevel(Level.WARN);
		}
		if (level.compareTo("ERROR") == 0) {
			//LOG.setLevel(Level.ERROR);
			Logger.getRootLogger().setLevel(Level.ERROR);
		}
		if (level.compareTo("FATAL") == 0) {
			//LOG.setLevel(Level.FATAL);
			Logger.getRootLogger().setLevel(Level.FATAL);
		} else {
			//LOG.setLevel(Level.DEBUG);
			Logger.getRootLogger().setLevel(Level.DEBUG);
		}
	}

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
		
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		try {

			// org.apache.log4j.BasicConfigurator.configure();
			// Logger.getRootLogger().setLevel(Level.DEBUG);

			// Logger logger = Logger.getLogger("log.out");
			// #define the appender named
			// FILE log4j.appender.FILE=org.apache.log4j.FileAppender
			// log4j.appender.FILE.File=${user.home}/log.out

			/*
			 * # Set root logger level to DEBUG and its only appender to A1.
			 * log4j.rootLogger=DEBUG, A1
			 * 
			 * # A1 is set to be a ConsoleAppender.
			 * log4j.appender.A1=org.apache.log4j.ConsoleAppender
			 * 
			 * # A1 uses PatternLayout.
			 * log4j.appender.A1.layout=org.apache.log4j.PatternLayout
			 * log4j.appender.A1.layout.ConversionPattern=%-4r [%t] %-5p %c %x -
			 * %m%n
			 */

			// excellent documentation
			// http://logging.apache.org/log4j/1.2/manual.html
			// SimpleLayout layout = new SimpleLayout();
			
			if (cmdline.containsKey("-logToConsole"))
			{
				org.apache.log4j.BasicConfigurator.configure();
				Logger.getRootLogger().setLevel(Level.DEBUG);
			} else {			
				PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x - %m%n");
	
				RollingFileAppender appender = null;
				try {
					String userDir = System.getProperty("user.dir");
					appender = new RollingFileAppender(layout, userDir +  File.separator + "log.txt", false);
				} catch (Exception e) {
					System.out.println(Service.stackToString(e));
				}
	
				LOG.addAppender(appender);
				LOG.setLevel(Level.DEBUG);
			}
			/*
			 * Annotation check Class[] p = new Class[1]; p[0] = String.class;
			 * 
			 * String tt = getMethodToolTip(Arduino.class.getCanonicalName(),
			 * "serialSend", p);
			 */

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND

			// libararyPath += ":" + userDir + "/bin";
			// System.setProperty("java.library.path", libararyPath);
			// LOG.debug("new java.library.path [" + libararyPath + "]");

			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work

			/*
			 * System.setProperty( "java.library.path", libararyPath );
			 * 
			 * Field fieldSysPath = ClassLoader.class.getDeclaredField(
			 * "sys_paths" ); fieldSysPath.setAccessible( true );
			 * fieldSysPath.set( null, null );
			 */

			invokeCMDLine(cmdline);
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

	@Override
	public String getToolTip() {
		return "<html>Service used to create other services. This service, like other services,<br>"
				+ "does not need a GUI.<br>"
				+ "Services can be created on the command line by using the Invoker in the following way.<br> "
				+ "-service [Service] [Service Name] ...<br>"
				+ "The following example will create an Invoker service named<br> \"services\" and a GUIService named \"gui\"<br>"
				+ "<b><i>" + helpString + "</i></b></html>";
	}

}
