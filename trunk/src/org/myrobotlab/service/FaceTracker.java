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
	
	Arduino arduino = new Arduino("arduino");
	
	Logging logger = new Logging("logger");
	
	public FaceTracker(String n) {
		super(n, FaceTracker.class.getCanonicalName());
		
		tilt.startService();
		pan.startService();
		camera.startService();
		logger.startService();
		arduino.startService();
		//pan.attach(arduino.name, 13);

		camera.notify("publish", name, "input");
		//notify("pan", "logger", "log");
		//notify("tilt", "logger", "log");
		notify("pan", "pan", "move");
		notify("tilt", "tilt", "move");
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

	// TODO - put cfg
	int width = 160;
	int height = 120;
	int centerX = width/2;
	int centerY = height/2;
	int errorX = 0;
	int errorY = 0;
	
	public void input (CvPoint point)
	{
		// TODO - handle multiple resolutions
		// currently I am only going to do 160 x 120
		errorX = (point.x - centerX)/2 * -1;
		errorY = (point.y - centerY)/2 * -1;
		
		if (point.x > centerX)
		{
			errorX = -1;
		} else {
			errorX = 1;			
		}

		if (point.y > centerY)
		{
			errorY = -1;
		} else {
			errorY = 1;			
		}
		
		
		LOG.info(point);
		
		invoke("pan", errorX);
		invoke("tilt", errorY);
		/*
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
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
		
		FaceTracker ft = new FaceTracker("face tracker");
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
