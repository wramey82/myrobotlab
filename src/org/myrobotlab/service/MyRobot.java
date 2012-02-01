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
import java.util.TimerTask;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.PinAlert;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.SensorData;

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
 *  1. start - move forward - keep track of time
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

	public Timer timer = new Timer();
	private Object event = new Object();
	
	// cartesian
	public float positionX = 0;
	public float positionY = 0;
	
	// polar
	public float theta = 0;
	public float distance = 0;
	
	public int targetX = 0;
	public int targetY = 0;
	
	public int headingCurrent = 0;
	
	int leftPin = 4;
	int rightPin = 5;
	int neckPin = 9;
	
	int rightStopPos 	= 90;
	int leftStopPos 	= 90;
	
	transient public Servo left;
	transient public Servo right;
	transient public Servo neck;
	transient public SensorMonitor sensors;
	
	/**
	 * servos do not go both directions at the same speed - this will be 
	 * a constant to attempt to adjust for the mechanical/electrical differences
	 */
	int leftError;
	int rightError;
	
	
	/**
	 * start & stops are not instantaneous - this adjustment is included as a constant
	 * in maneuvers which include stops & starts
	 */
	int startError;
	int stopError;
	
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
		
	// control functions begin -------------------
	
	// TODO spinLeft(int power, int time)
	// TODO - possibly have uC be the timer 
	// TODO - bury any move or stop with attach & detach in the uC
	// TODO - make continuous rotational Servo handle all this
	public void moveUntil (int power, int time)
	{
		// start timer;
		timer.schedule(new TimedTask(), time);
		right.attach(rightPin); // FIXME - attach right & left in single uC call
		left.attach(leftPin);
		right.moveTo(power);
		left.moveTo(-power);
		waitForEvent(); // blocks
	}
	
	// queued ?
	/*
	public void moveUntil2 (int power, int time)
	{
		in(createMessage(name, "speakGoogle", toSpeak));				
	}
	*/
	
	public void waitForEvent() 
	{
		synchronized (event)
		{
			try {
				event.wait();
			} catch (InterruptedException e) {				
			}
		}
		
	}
	
	/**
	 * a timed event task - used to block in dead reckoning
	 *
	 */
	class TimedTask extends TimerTask {
	    public void run() {
	    	stop();
			synchronized (event)
			{
				event.notifyAll();
			}
	    }
	  }
	
	public void stop()
	{
		right.moveTo(rightStopPos);
		left.moveTo(leftStopPos);
		right.detach();
		left.detach();
	}
	
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
		right.moveTo(rightStopPos + power);
		left.moveTo(leftStopPos - power);
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

	public final static String STATE_IDLE = "IDLE";

	public final static String ALERT_WALL = "ALERT_WALL";

	String state = STATE_IDLE;
	
	// fsm ------------------------------------
	public void start() {
		
		neck = new Servo(name + "Neck");
		right = new Servo(name + "Right");
		left = new Servo(name + "Left");
		arduino = new Arduino(name + "BBB");
		sensors = new SensorMonitor(name + "Sensors");
		
		this.startService();
		sensors.startService();
		neck.startService();
		right.startService();
		left.startService();
		arduino.startService();
		
		
		
		neck.attach(arduino.name, 9);
		right.attach(arduino.name, 3);
		left.attach(arduino.name, 4);

/*
//		Graphics graphics = new Graphics("graphics");
//		graphics.startService();
*/
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		

		
		// don't know where I am...
		// set neck forward 
		// neck.moveTo(90);
		// neck.moveTo(90);
		move(10);
		stop();

		// set a route of data from arduino to the sensor monitor
		arduino.notify(SensorData.publishPin, sensors.name, "sensorInput", PinData.class);

		// set an alert from sensor monitor to MyRobot
		sensors.notify("publishPinAlert", this.name, "sensorAlert", PinAlert.class);
		sensors.addAlert(arduino.name, ALERT_WALL, 600, 700, 3, 5, 0);
		
		// move & set timer
		move(20);

	}
	
	public void sensorAlert(PinAlert alert)
	{
		stop();
		state = STATE_IDLE;
	}
	
	public void explore()
	{
		
	}
	
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		MyRobot dee = new MyRobot("dee");
		dee.start();
	}

}
