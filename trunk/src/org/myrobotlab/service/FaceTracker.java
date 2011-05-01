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
 * Enjoy.
 * 
 * */

package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cxcore.CvPoint;
import org.myrobotlab.framework.Service;

public class FaceTracker extends Service {

	public final static Logger LOG = Logger.getLogger(FaceTracker.class.getCanonicalName());

	/*
	 *  TODO - dead zone - scan / search
	 */
	Servo tilt = new Servo("tilt");
	Servo pan = new Servo("pan");
	OpenCV camera = new OpenCV("camera");
	 
	Logging logger = new Logging("logger");
	
	public FaceTracker(String n) {
		super(n, FaceTracker.class.getCanonicalName());
		
		tilt.startService();
		pan.startService();
		camera.startService();
		logger.startService();

		camera.notify("publish", name, "input");
		notify("pan", "logger", "log");
		notify("tilt", "logger", "log");
	}
	
	@Override
	public void loadDefaultConfiguration() {
	}
	
	public void startTracking()
	{
	}
	
	public void stopTracking()
	{
	}
	
	public void input (CvPoint point)
	{
		LOG.info(point);
		invoke("pan", point.x);
		invoke("tilt", point.y);
	}
	
	public Integer tilt (Integer position)
	{
		return position;
	}

	public Integer pan (Integer position)
	{
		return position;
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		FaceTracker ft = new FaceTracker("clock");
		ft.startService();

		/*
		OpenCV camera = new OpenCV("camera");
		camera.startService();
		
		Logging log = new Logging("log");
		log.startService();
		*/
		//clock.notify("pulse", "log", "log", Integer.class);

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}
	
	@Override
	public String getToolTip() {
		return "used to generate pulses";
	}


}
