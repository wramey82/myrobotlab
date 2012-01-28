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

import java.util.Timer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;

/**
 * @author GroG
 *
 *	http://en.wikipedia.org/wiki/Dead_reckoning#Differential_steer_drive_dead_reckoning
 *
 *  Timer & TimerTask
 *  http://www.java2s.com/Code/Java/Development-Class/TimerScheduleataskthatexecutesonceeverysecond.htm
 *  
 *  finish Tweedle Dee & Dummer
 *  
 *  calibrate - 
 *  make it go straight
 *  find the delta for going straight (are there two? ie servo differences)
 *  find the delta going CW vs CCW
 *  find the delta of error of having timing done on the uC versus the puter - MOVE_FOR int
 *  find the delta in time/distance at speed versus rest
 *  find the delta in time/distance at lower battery
 *  find the speed for some constant level (at rest)
 *  find the speed of a turn (constant level) (at rest)
 *  find the drift for shutting off speed
 *  what is WHEEL_BASE ?
 *  
 *  Do some maneuvering tests
 *  
 *  Find out what the "real" min max and error is of the IR sensor
 *  
 *  Go forward (straight line!! error ouch!!) until something is reached (inside max range of sensor stop) - record/graph the time - draw a line (calibrate this)
 *  Turn heading until parallel with the wall (you must do this slowly)
 *  
 *  SLAM --------
 *  calibrate as best as possible
 *  
 *  guess where you are with little data (time)
 *  
 *  when you get data corroberate it with what you have (saved info)
 *  
 *  
 */

public class MyRobot extends Service {

	private static final long serialVersionUID = 1L;

	public Timer timer;
	
	// cartesian
	public float positionX = 0;
	public float positionY = 0;
	
	public float theta = 0;
	public float distance = 0;
	
	public int targetX = 0;
	public int targetY = 0;
	
	public int headingCurrent = 0;
	
	public Servo left;
	public Servo right;
	public Servo neck;
	
	public Arduino arduino;

	public final static Logger LOG = Logger.getLogger(MyRobot.class.getCanonicalName());

	public MyRobot(String n) {
		this(n, null);
	}

	public MyRobot(String n, String serviceDomain) {
		super(n, MyRobot.class.getCanonicalName(), serviceDomain);
	}
	
	@Override
	public void loadDefaultConfiguration() {
	}
	
	public MyRobot publishState(MyRobot t)
	{
		return t;
	}
	
	public void createServices () {
		
		neck = new Servo(name + "_neck");
		right = new Servo(name + "_right");
		left = new Servo(name + "_left");
		arduino = new Arduino(name + "_bbb");

		neck.startService();
		right.startService();
		left.startService();
		arduino.startService();
		
		neck.attach(arduino.name, 9);
		right.attach(arduino.name, 4);
		left.attach(arduino.name, 5);				
	}
	
	// control functions begin -------------------
	
	// TODO spinLeft(int power, int time)
	
	public void spinLeft(int power)
	{
		right.moveTo(-power);
		left.moveTo(power);
	}
	
	public void spinRight(int power)
	{
		right.moveTo(power);
		left.moveTo(-power);
	}
	
	public void move(int power)
	{
		right.moveTo(power);
		left.moveTo(-power);
	}
	
	public void moveTo (float distance)
	{
		
	}
	
	// command to change heading and/or position
	public void setHeading (int value) // maintainHeading ?? if PID is operating
	{
		//headingTarget = value;
		setHeading(headingCurrent);// HACK? - a way to get all of the recalculations publish
	}
	
	public void setTargetPosition (int x, int y)
	{
		targetX = x;
		targetY = y;
	}
	
	@Override
	public String getToolTip() {
		return "<html>used to encapsulate many of the functions and formulas regarding 2 motor platforms.<br>" +
		"encoders and other feedback mechanisms can be added to provide heading, location and other information</html>";
	}
	
	// turning related end --------------------------
	
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		MyRobot dee = new MyRobot("dee");
		dee.createServices();
		dee.startService();
		
		SensorMonitor sensors = new SensorMonitor("sensors");
		sensors.startService();
		
		Graphics graphics = new Graphics("graphics");
		graphics.startService();

		Jython jython = new Jython("jython");
		jython.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		//platform.startRobot();
	}

}
