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

public class Servo extends Service implements
		org.myrobotlab.service.interfaces.Servo {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(Servo.class
			.getCanonicalName());
	// int pos = 0;
	// int pin = 0;

	boolean isAttached = false;

	// sweep related
	int sweepStart = 89;
	int sweepEnd = 91;
	int sweepDelayMS = 1000;
	int sweepIncrement = 1;
	boolean sweeperRunning = false;
	Thread sweeper = null;

	String controllerName = ""; // without an attached controller name - a Servo
								// is not very interesting
	
	private int pos = -1; // position -1 invalid not set yet
	private int posMin = 40;
	private int posMax = 110;
	private int pin = -1; // pin on controller servo is attached to -1 invalid not set yet

	public Servo(String n) {
		super(n, Servo.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	/*
	 * TODO - implement an Arduino sweep method Sweeper - is a class which can
	 * sweep the servo, however, it is implemented on the Java side. If smoother
	 * sweeping is desired an Arduino side sweep should be considered using
	 * Arduino threads
	 */
	private class Sweeper implements Runnable {

		@Override
		public void run() {

			while (sweeperRunning) {
				// controller.servoMoveTo(name, pos);
				int pos = cfg.get("pos", 90);
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
					e.printStackTrace();
				}
			}
		}

	}

	public void attach(String controller, Integer pin) {
		setControllerName(controller);
		attach(pin);
	}

	public boolean isAttached() {
		return isAttached;
	}

	/*
	 * -----Publishing Point-------------
	 * 
	 * setPin - sets the Servo data pin which will correspond to the
	 * ServoControllers hardware pin this is stubbed out in an accessor so that
	 * events which change the pin can be broadcast to listeners
	 */

	public Integer setPin(Integer pin) {
		this.pin = pin;
		return pin;
	}

	/*
	 * -----pos Publishing Point-------------
	 * 
	 * setPos - sets the Servo data pos which will correspond to the
	 * ServoControllers current angle this is stubbed out in an accessor so that
	 * events which change the pos can be broadcast to listeners
	 */

	public Integer setPos(Integer pos) {
		this.pos = pos;
		return pos;
	}

	public Integer getPin() { // TODO - won't this get boxed up?
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
	
	public String getControllerName() // TODO - strangely this is not CFG'd -
										// but perhaps that is OK??
	{
		return controllerName;
	}

	public String setControllerName(String cname) // TODO - strangely this is
													// not CFG'd - but perhaps
													// that is OK??
	{
		controllerName = cname;
		return controllerName;
	}

	/*
	 * attach - attaches the software Servo service to the servo of a
	 * ServoController this function should not need to be broadcast nor routed
	 * but is a command function to be used to initialize the communication
	 * between two Services
	 */

	public void attach(Integer pin) {
		invoke("setPin", pin); // broadcasting change of pin

		if (controllerName.length() == 0) {
			LOG.error("can not attach, controller name is blank");
			return;
		}
		
		if (isAttached)
		{
			LOG.warn("servo " + name +  " is already attached - detach before re-attaching");
			return;
		}

		// TODO
		// send / sendDirect here - I don't want a route established but I do
		// want to send the message - AND i want to block
		// for the response !!!!!!
		// TODO if (Boolean)sendBlocking(controllerName,
		// ServoController.servoAttach, name, pin)
		send(controllerName, ServoController.servoAttach, pin);

		
		// TODO - addMsgListener
/*		DONT SET UP READING POSITION UNLESS REQUESTED TO	
		Object[] params = new Object[4];
		params[0] = "readServo";
		params[1] = name;
		params[2] = "readServo";
		params[3] = PinData.class.getCanonicalName();
		send(controllerName, "notify", params);
*/
		
		// set up msg routing  TODO - don't set up message routing - use "send" from servoMove fn
		notify("servoWrite", controllerName, ServoController.servoWrite, IOData.class);

		// TODO - this may need to block? - Simple if it did?
		// get the position of the attached servo
		// send(controllerName, ServoController.servoRead, pin);  THIS IS COMPLETELY WRONG !
		isAttached = true;
	}

	// TODO - still used? deprecate
	public PinData readServo(PinData p) {
		LOG.info(p);
		setPos(p.value);
		return p;
	}

	public PinData publishPin(PinData p) {
		LOG.info(p);
		setPos(p.value);
		return p;
	}

	/*
	 * detach - detaches the software Servo service from the servo of the
	 * ServoController this function should not need to be broadcast nor routed
	 * but is a command function to be used to tear down the communication
	 * between two Services
	 */
	public void detach() {
		send(controllerName, ServoController.servoDetach, pin); // TODO
																				// if
																				// (Boolean)sendBlocking(....
		removeNotify("servoWrite", controllerName, ServoController.servoWrite, IOData.class);
		isAttached = false;
	}

	// TODO - client methods - moveTo & move & sweep & whatever
	// server/controller methods - write (on notify?) i guess - the attach is a
	// boolean - should block again

	// move to absolute position 0 - 180
	public Integer moveTo(Integer pos) {
		LOG.info("moveTo" + pos);
		this.pos = pos; 
		invoke("servoWrite", pos);
		return pos;
	}

	public IOData servoWrite(Integer pos) {
		IOData d = new IOData();
		d.address = pin;
		d.value = pos;
		return d;
	}

	public int move(Integer amount) // TODO - possibly depricate and handle
										// details internally?
	{
		int p = pos + amount;
		if (p < posMax && p > posMin) {
			pos = p;
			LOG.info("move" + pos);
			invoke("servoWrite", pos);

		} else {
			LOG.error("servo out of bounds pin " + pin + " pos " + pos + " amount " + amount);
		}

		return pos;
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

	/*
	 * Arduino needs to implement the interface ServoController Communication
	 * details stored in Configuration Servo - has reference to ServoController
	 * 
	 * refresh rate min interval max interval NUMBER !
	 */

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Arduino arduino = new Arduino("arduino");
		arduino.startService();

		Servo servo = new Servo("servo");
		servo.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

	@Override
	public String getToolTip() {
		return "<html>service for a servo</html>";
	}

}
