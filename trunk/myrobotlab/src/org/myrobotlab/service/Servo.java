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

import java.util.Vector;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

@Root
public class Servo extends Service implements ServoControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Servo.class.getCanonicalName());

	ServoController controller = null;

	@Element
	private Integer position = null; // position -1 invalid not set yet
	@Element
	private int positionMin = 0;
	@Element
	private int positionMax = 180;
	
	private boolean inverted = false;
	
	Vector<String> controllers;

	// private float speed = 1.0f; // fractional speed component 0 to 1.0

	// FIXME - should be implemented inside the Arduino / ServoController - but
	// has to be tied to position
	int sweepStart = 0;
	int sweepEnd = 180;
	int sweepDelayMS = 1000;
	int sweepIncrement = 1;
	boolean sweeperRunning = false;
	transient Thread sweeper = null;

	public Servo(String n) {
		super(n, Servo.class.getCanonicalName());
		load();
	}

	@Override
	public boolean setController(ServoController controller) {
		if (controller == null) {
			log.error("setting null as controller");
			return false;
		}
		this.controller = controller;
		return true;
	}
	
	public void setInverted(boolean isInverted)
	{
		inverted = isInverted;
	}
	
	public boolean isInverted()
	{
		return inverted;
	}

	/**
	 * simple move servo to the location desired
	 */
	@Override
	public void moveTo(Integer pos) {
		/*
		if (pos == null) {
			return;
		}
		*/
		Integer newPos = pos;
		if (inverted)
		{
			newPos = 180 - pos;
		}
		if (controller == null) {
			log.error(String.format("%s's controller is not set", getName()));
			return;
		}
		if (newPos >= positionMin && newPos <= positionMax) {
			controller.servoWrite(getName(), newPos);
			position = newPos;
		} else {
			error(String.format("%s.moveTo(%d) out of range", getName(), newPos));
		}
	}

	/**
	 * moves the servo in the range -1.0 to 1.0 where in a typical servo -1.0 =
	 * 0 0.0 = 90 1.0 = 180 setting the min and max will affect the range where
	 * -1.0 will always be the minPos and 1.0 will be maxPos
	 */
	@Override
	public void move(Float pos) {
		Float amount = pos;
		if (inverted)
		{
			amount = pos * -1;
		}
		if (amount > 1 || amount < -1) {
			log.error("%s.move %d out of range", getName(), amount);
			return;
		}
	
		int range = positionMax - positionMin;
		int newPos = Math.abs((int) (range / 2 * amount - range / 2));

		controller.servoWrite(getName(), newPos);
		position = newPos;

	}

	public boolean isAttached() {
		return controller != null;
	}

	public void setPositionMin(Integer min) {
		this.positionMin = min;
	}

	public Integer getPositionMin() {
		return positionMin;
	}

	@Override
	public void setPositionMax(Integer max) {
		this.positionMax = max;
	}

	public Integer getPositionMax() {
		return positionMax;
	}

	public Integer getPosition() {
		if (inverted)
		{ return 180 - position; }
		else
		return position;
	}

	@Override
	public String getToolTip() {
		return "<html>service for a servo</html>";
	}

	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother
	 * function
	 * 
	 */
	private class Sweeper implements Runnable {
		@Override
		public void run() {

			while (sweeperRunning) {
				// controller.servoMoveTo(name, position);
				position += sweepIncrement;

				// switch directions
				if ((position <= sweepStart && sweepIncrement < 0) || (position >= sweepEnd && sweepIncrement > 0)) {
					sweepIncrement = sweepIncrement * -1;
				}

				moveTo(position);

				try {
					Thread.sleep(sweepDelayMS);
				} catch (InterruptedException e) {
					sweeperRunning = false;
					logException(e);
				}
			}
		}

	}

	public void sweep(int sweepStart, int sweepEnd, int sweepDelayMS, int sweepIncrement) {
		if (sweeperRunning) {
			stopSweep();
		}
		this.sweepStart = sweepStart;
		this.sweepEnd = sweepEnd;
		this.sweepDelayMS = sweepDelayMS;
		this.sweepIncrement = sweepIncrement;

		sweeperRunning = true;

		sweeper = new Thread(new Sweeper());
		sweeper.start();
	}

	public void stopSweep() {
		sweeperRunning = false;
		sweeper = null;
	}

	public void sweep() {
		sweep(sweepStart, sweepEnd, sweepDelayMS, sweepIncrement);
	}

	@Override
	public String getControllerName() {
		if (controller == null) {
			return null;
		}

		return controller.getName();
	}

	@Override
	public Integer getPin() {
		if (controller == null) {
			return null;
		}

		return controller.getServoPin(getName());
	}

	public void setSpeed(Float speed) {
		if (speed == null) {
			return;
		}

		controller.setServoSpeed(getName(), speed);
	}

	public void detach() {
		if (controller != null) {
			controller.servoDetach(getName());
		}
	}
	
	public void test()
	{
		for (int i = 0; i < 10000; ++i)
		{
			setSpeed(0.6f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.6f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.5f);
			moveTo(90);
			moveTo(180);
			setSpeed(0.5f);
			moveTo(90);
			moveTo(180);
		}
		
	}
	
	public Vector<String> refreshControllers()
	{
		controllers = Runtime.getServicesFromInterface(ServoController.class.getCanonicalName());
		return controllers;
	}

	public static void main(String[] args) throws InterruptedException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		// FIXME - routing of servo.attach("arduino", 3);

		Arduino arduino = (Arduino)Runtime.createAndStart("arduino", "Arduino");

		arduino.setSerialDevice("COM12");
		
		Servo right = new Servo("servo01");
		right.startService();
		
		arduino.servoAttach(right.getName(), 13);
		
		right.test();
		
		Runtime.createAndStart("gui", "GUIService");
		
		// right.attach(serviceName)
		/*
		 * Servo left = new Servo("left"); left.startService();
		 * 
		 * //Servo neck = new Servo("neck"); //neck.startService();
		 * 
		 * for (int i = 0; i < 30; ++i) {
		 * 
		 * right.attach("arduino", 2); left.attach("arduino", 3);
		 * 
		 * right.moveTo(120); // 70 back left.moveTo(70); // 118 back
		 * 
		 * Thread.sleep(10000);
		 * 
		 * right.moveTo(90); left.moveTo(90);
		 * 
		 * //right.detach(); //left.detach(); }
		 */
		

	}

	@Override
	public void stopServo() {
		controller.servoStop(getName());
	}

}
