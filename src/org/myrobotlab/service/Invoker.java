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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;

/*
 *   I would have liked to dynamically extract the possible Services from the code itself - unfortunately this is near impossible
 *   The methods to get this information are different depending on if app is running from the filesystem, from a jar, or from an applet.
 *   Additionally in some cases "all" classes need to be pulled back then filtered on services - which is not very practical.
 *   Sadly, these means a list needs to be maintained within this class.
 * 
 * 
 */

public class Invoker extends Service {

	public Invoker(String instanceName) {
		super(instanceName, Invoker.class.getCanonicalName());
	}

	public final static Logger LOG = Logger.getRootLogger();
	static GUIService gui = null;


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


	public final static void invokeCMDLine(String[] args) {
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

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
				// TODO -remove GUIService
				LOG.info("attempting to invoke : org.myrobotlab.service."
								+ cmdline.getSafeArgument("-service", i, "")
								+ " "
								+ cmdline
										.getSafeArgument("-service", i + 1, ""));
				Service s = (Service) Service.getNewInstance(
						"org.myrobotlab.service."
								+ cmdline.getSafeArgument("-service", i, ""),
						cmdline.getSafeArgument("-service", i + 1, ""));
				s.startService();
				if (s.serviceClass.compareTo(GUIService.class
						.getCanonicalName()) == 0) {
					gui = (GUIService) s;
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
				"ChessGame", "Clock", "ComediDriver", "DifferentialDrive",
				"FaceTracker", "GeneticProgramming",
				"Graphics", "GUIService",
				"HTTPClient", "Invoker", "JFugue", "JoystickService",
				"Logging",
				"MediaSource", "MoMo", "Motor", "OpenCV",
				"ParallelPort", "PICAXE", "PID", "Player", "PlayerStage", "RecorderPlayer",
				"RemoteAdapter", "Rose",  "SensorMonitor", "Serial",
				"Servo", "SLAM", "SoccerGame", "SOHDARService", "Speech",
				"SpeechRecognition", "StepperMotor", "SystemInformation",
				"TrackingService",
				"WiiDAR", "Wii" };
	}

	public void removeService(String name) {
		LOG.info("removing service " + name);
	}

	public void addService(String className, String newName) {
		LOG.info("adding service " + newName);
		Service s = (Service) Service.getNewInstance("org.myrobotlab.service." + className, newName);
		s.startService();
	}

	
	public static void main(String[] args) {

		try{
			
		
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
				
		invokeCMDLine(args);
		} catch (Exception e)
		{
			System.out.print("ouch !");
			e.getStackTrace();
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
		return "<html>Service used to create other services. This service, like other services,<br>" +
		"does not need a GUI.<br>" +
		"Services can be created on the command line by using the Invoker in the following way.<br> " +
		"-service [Service] [Service Name] ...<br>" +
		"The following example will create an Invoker service named<br> \"services\" and a GUIService named \"gui\"<br>" +
		"<b><i>" + helpString + "</i></b></html>";
	}
	
}
