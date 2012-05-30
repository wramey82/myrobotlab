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
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class Servo extends Service implements
		org.myrobotlab.service.interfaces.Servo {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Servo.class.getCanonicalName());

	boolean isAttached = false;

	@Element
	String controllerName = ""; 
	
	@Element
	public int pos = -1; // position -1 invalid not set yet
	@Element
	public int posMin = 0;
	@Element
	public int posMax = 180;
	@Element
	public int pin = -1; // pin on controller servo is attached to -1 invalid not set yet
	
	// sweep related TODO - should be implemented in Arduino protocol
	int sweepStart = 89;
	int sweepEnd = 91;
	int sweepDelayMS = 1000;
	int sweepIncrement = 1;
	boolean sweeperRunning = false;
	transient Thread sweeper = null;
	
	public Servo(String n) {
		super(n, Servo.class.getCanonicalName());
		load();
	}

	@Override
	public void loadDefaultConfiguration() {
	}


	public void attach(String controllerName, Integer pin) {
		this.controllerName = controllerName;
		this.pin = pin;
		if (attach())
		{
			save(); // bound to controller and pin
			broadcastState(); // state has changed let everyone know
		}
	}

	public boolean attach()
	{
		if (controllerName.length() == 0) {
			log.error("can not attach, controller name is blank");
			return false;
		}
		
		if (isAttached)
		{
			log.warn("servo " + getName() +  " is already attached - detach before re-attaching");
			return false;
		}

		send(controllerName, ServoController.servoAttach, pin);
		//notify("servoWrite", controllerName, ServoController.servoWrite, IOData.class);
		// TODO - notice between publishing point and direct message
		// currently removing publishing point

		isAttached = true;
		return isAttached;
	}
	
	public boolean isAttached() {
		return isAttached;
	}

	public Integer setPos(Integer pos) {
		this.pos = pos;
		return pos;
	}

	public Integer getPin() { 
		return new Integer(pin);
	}

	public Integer getPos() {
		return new Integer(pos);
	}
	
	public void setPosMin(Integer posMin)
	{
		this.posMin = posMin; 
	}

	public void setPosMax(Integer posMax)
	{
		this.posMax = posMax; 
	}
	
	public String getControllerName() 
	{
		return controllerName;
	}

	public int readServo() {
		return pos;
	}

	// callback from controller
	public PinData publishPin(PinData p) {
		log.info(p);
		setPos(p.value);
		return p;
	}

	public void detach() {
		send(controllerName, ServoController.servoDetach, pin); // TODO - possible
		// configurable publishing point versus direct send
		//removeNotify("servoWrite", controllerName, ServoController.servoWrite, IOData.class);
		isAttached = false;
		broadcastState();
	}

	/**
	 * moveTo - used to move the servo to a new position
	 * @param pos - absolute position to move to normally servos can move between
	 * 0 - 180 unless other limits are set
	 * @return
	 */
	public Integer moveTo(Integer pos) {
		log.info(getName() + " moveTo " + pos);
		send(controllerName, ServoController.servoWrite, pin, pos); 
		this.pos = pos; 
		// invoke("servoWrite", pos); TODO - consider
		// the differences between direct send and publishing to a point
		// make it configurable? - at the moment the expectation of a working Servo
		// means it "must" be associated with something which implements ServoController
		// so we are going to direct send only at the momo
		return pos;
	}

	/**
	 * moveTo() should be used by the Services or user wanting to control the servo
	 * This function in turn is invoked and is used as an interface to the 
	 * ServoController
	 * @param pos - position to move
	 * @return
	 */
	public IOData servoWrite(Integer pos) {
		IOData d = new IOData();
		d.address = pin;
		d.value = pos;
		return d;
	}


	/**
	 * move (pos) is used to move the servo a relative amount to its
	 * current position
	 * @param amount relative position
	 * @return the current position
	 */
	public int move(Integer amount) 
	{
		int p = pos + amount;
		if (p < posMax && p > posMin) {
			pos = p;
			log.info("move" + pos);
			invoke("servoWrite", pos);

		} else {
			log.error("servo out of bounds pin " + pin + " pos " + pos + " amount " + amount);
		}

		return pos;
	}

	@Override
	public String getToolTip() {
		return "<html>service for a servo</html>";
	}
	
	
	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother function
	 *
	 */
	private class Sweeper implements Runnable { 
		@Override
		public void run() {

			while (sweeperRunning) {
				// controller.servoMoveTo(name, pos);
				pos += sweepIncrement;

				// switch directions
				if ((pos <= sweepStart && sweepIncrement < 0)
						|| (pos >= sweepEnd && sweepIncrement > 0)) {
					sweepIncrement = sweepIncrement * -1;
				}

				// moveTo(pos); hmm vs send ??? TODO - what is better??? - refer
				// to documentation
				invoke("servoWrite", pos);

				try {
					Thread.sleep(sweepDelayMS);
				} catch (InterruptedException e) {
					sweeperRunning = false;
					logException(e);
				}
			}
		}

	}

	public void sweep(int sweepStart, int sweepEnd, int sweepDelayMS,
			int sweepIncrement) {
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

	public static void main(String[] args) throws InterruptedException {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		/*
		Arduino arduino = new Arduino("arduino");
		arduino.startService();
		*/

		Servo right = new Servo("right");
		right.startService();

		Servo left = new Servo("left");
		left.startService();

		//Servo neck = new Servo("neck");
		//neck.startService();
		
		for (int i = 0; i < 30; ++i)
		{
		
			right.attach("arduino", 2);
			left.attach("arduino", 3);
			
			right.moveTo(120); // 70 back
			left.moveTo(70); // 118 back
	
			Thread.sleep(10000);
			
			right.moveTo(90);		
			left.moveTo(90);
	
			right.detach();
			left.detach();
		}

		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
		
	}
	
}
