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
		System.out.println("-h       # help ");
		System.out.println("-list        # list services");
		System.out.println("-service [Service] [Service Name]");
		System.out.println("-service GUIService gui");
	}
	
	static String helpString = "java -Djava.library.path=./bin  -cp \"myrobotlab.jar;lib/*\" org.myrobotlab.service.Invoker -service Invoker";

	@Override
	public void loadDefaultConfiguration() {

	}


	public final static void invokeCMDLine(String[] args) {
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		if (cmdline.containsKey("-h")) {
			help();
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
				"Calibrator", "Clock", "ComediDriver", "DifferentialDrive",
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
		if (gui != null)
		{
			// TODO - this "could" be messaged vs direct reference to support remote
			gui.loadTabPanels();
		}
		s.startService();
	}

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		invokeCMDLine(args);
	}

	public int test()
	{
		int x = 0;
		return x;
	}

	public int test(int a, float b, boolean c, boolean[]d)
	{
		int x = 0;
		return x;
	}

	public Integer test(Integer i)
	{
		int x = 0;
		return x;
	}

	@Override
	public String getToolTip() {
		return "<html>Service used to create other services, does not need a GUI.<br>" +
		"Services can be created with this service on the command line<br> <b><i>" +
		helpString + "</i></b></html>";
	}
	
}
