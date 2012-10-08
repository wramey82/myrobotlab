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

import java.util.Properties;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;

// TODO - implements Motor interface
// This mimics a DPDT Motor
public class Motor extends Service {
	/*
	 * Move (-1 ... 1)  input represents power
	 * FIXME - this implementation needs to be hidden by the motor controller
	 * TODO - there are only 2 ways to control a simple DC motor - SMOKE and
	 * H-Bridge/DPDT Where DPDT - one digital line is off/on - the other is CW
	 * CCW
	 * 
	 * Pwr Dir D0 D1 0 0 STOP (CCW) 0 1 STOP (CW) 1 0 GO (CCW) 1 1 GO (CW)
	 * 
	 * POWER - PWM is controlled only on the Pwr Line only - 1 PWM line
	 * 
	 * The other is 1 digital line each for each direction and power (SMOKE) if
	 * both are high
	 * 
	 * Pwr Pwr D0 D1 0 0 STOP 0 1 CW 1 0 CCW 1 1 SHORT & BURN - !!!! NA !!!
	 * 
	 * POWER - PWM must be put on both lines - 2 PWM lines
	 */

	// TODO - Motor type or controller type
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Motor.class.toString());

	boolean isAttached = false;
	
	private int powerPin;
	private int directionPin;
	private int powerMultiplier = 255; // FIXME - remove | default to Arduino analogWrite max
								
	int FORWARD = 1;
	int BACKWARD = 0;

	float power = 0;
	float maxPower = 1;
	
	public boolean inMotion = false;

	boolean locked = false; // for locking the motor in a stopped position
	String controllerName = null; // board name
	
	transient EncoderTimer durationThread = null;

	/**
	 * Motor constructor takes a single unique name for identification.
	 * e.g. Motor left = new Motor("left");
	 * 
	 * @param name
	 */
	public Motor(String name) {
		super(name, Motor.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	/**
	 * attach (controllerName, powerPin, directionPin is primarily for simple motor
	 * controllers such as the Arduino, PicAxe, or other controllers which can
	 * typically control motors with 2 bits.  One for power with pulse width modulation
	 * and another for direction.
	 * 
	 * If more configuration data is needed then use 
	 * {@link #attach(String, Properties) attach with Properties}
	 * 
	 * @param controllerName
	 * @param powerPin
	 * @param directionPin
	 */
	public void attach(String controllerName, int powerPin, int directionPin) {
		this.controllerName = controllerName;
		this.powerPin = powerPin;
		this.directionPin = directionPin;
		send(controllerName, "motorAttach", this.getName(), powerPin, directionPin);
	}

	/**
	 * attach associates the motor controller which could be an Arduino, PICAXE
	 * or other circuitry.  The Properties are used to send the controller the
	 * necessary details of the connection and configuration.
	 * 
	 * @param controllerName - name of the controller controlling this Motor
	 * @param config - all the configuration needed for the motor controller
	 * most times this will just require a powerPin and directionPin to describe which
	 * pins will be used to control power and direction of the motor
	 */
	public void attach(String controllerName, Properties config) {
		this.controllerName = controllerName;
		send(controllerName, "motorAttach", this.getName(), config);
	}
	
	
	// motor primitives begin ------------------------------------
	public void invertDirection() {
		FORWARD = 0;
		BACKWARD = 1;
	}

	// TODO - make setPower too
	// TODO - Events (inherit)  hasStopped hasBegunToMove 
	// TODO - isMoving isStopped
	public void incrementPower(float increment) {
		if (power + increment > maxPower || power + increment < -maxPower) 
		{
			log.error("power " + power + " out of bounds with increment "+ increment);
			return;
		}
		power += increment;
		move(power);
	}

	public void move(float newPowerLevel) {
		if (locked) return;

		// check if the direction has changed - send command if necessary
		if (newPowerLevel > 0 && power <= 0) {
			send(controllerName, DigitalIO.digitalWrite, directionPin, FORWARD); 
		} else if (newPowerLevel < 0 && power >= 0) {
			send(controllerName, DigitalIO.digitalWrite, directionPin, BACKWARD); 
		}

		//log.error("direction " + ((newPowerLevel > 0) ? "FORWARD" : "BACKWARD"));
		log.error(getName() + " power " + (int) (newPowerLevel * 100) + "% actual " + (int) (newPowerLevel * powerMultiplier));
		// FIXME - MotorController needs a "scalePWM" which takes a float - the controller
		// then maps it to what would be appropriate - in Arduino 0-255 - remove "powerMultiplier"
		send(controllerName, AnalogIO.analogWrite, powerPin, Math.abs((int) (newPowerLevel * powerMultiplier)));

		power = newPowerLevel;

	}
	
	public void setMaxPower(float max) {
		if (maxPower > 1 || maxPower < 0)
		{
			log.error("max power must be between 0.0 and 0.1");
			return;
		}
		maxPower = max;
	}


	
	public void stop() {
		move(0);
	}

	public void unLock() {
		log.info("unLock");
		locked = false;
	}

	public void lock() {
		log.info("lock");
		locked = true;
	}

	public void stopAndLock() {
		log.info("stopAndLock");
		move(0);
		lock();
	}


	
	// FIXME - implements an Encoder interface
	// get a named instance - stopping and starting should not be creating & destroying
	transient Object lock = new Object();
	class EncoderTimer extends Thread
	{
		public float power = 0.0f;
		public int duration = 0;
		
		Motor instance = null;
		
		EncoderTimer(float power, int duration, Motor instance)
		{
			super (instance.getName() + "_duration");
			this.power = power;
			this.duration = duration;
			this.instance = instance;
		}
		
		public void run ()
		{
			while (isRunning()) // this is from Service - is OK?
			{
				synchronized (lock) {
					try {
						lock.wait();
						
						instance.move(this.power);
						inMotion = true;
						
						Thread.sleep(this.duration);
						
						instance.stop();
						inMotion = false;
						
					} catch (InterruptedException e) {
						log.warn("duration thread interrupted");
					}
				}
			}
		}
	}

	public void moveFor (float power, int duration)
	{
		// default is not to block
		moveFor(power, duration, false);
	}
	
	// TODO - operate from thread pool
	public void moveFor (float power, int duration, boolean block)
	{
		// TODO - remove - Timer which implements SensorFeedback should be used
		if (!block)
		{
			// non-blocking call to move for a duration
			if (durationThread == null)
			{
				durationThread = new EncoderTimer(power, duration, this);
				durationThread.start();
			} else {
				if (inMotion)
				{
					log.error("duration is busy with another move" + durationThread.duration);
				} else {
				 	synchronized (lock) {
						durationThread.power = power;
						durationThread.duration = duration;
						lock.notifyAll();
					}
				}
			}
		} else {
			// block the calling thread
			move(this.power);
			inMotion = true; 
			
			try {
				Thread.sleep(duration);
			} catch (InterruptedException e) {
				logException(e);
			}
			
			stop();
			inMotion = false;

		}
		
		
	}

	
	@Override
	public String getToolTip() {
		return "general motor service";
	}
	
}

/* TODO - implement in Arduino

	int targetPosition = 0;
	boolean movingToPosition = false;


public void attachEncoder(String encoderName, int pin) // TODO Encoder Interface
{
	this.encoderName = encoderName;
	PinData pd = new PinData();
	pd.pin = pin;
	encoderPin = pin; // TODO - have encoder own pin - send name for event

	// TODO - Make Encoder Interface

	MRLListener MRLListener = new MRLListener();

	MRLListener.getName() = name;
	MRLListener.outMethod = "publishPin";
	MRLListener.inMethod = "incrementPosition";
	MRLListener.paramType = PinData.class.getCanonicalName();
	send(encoderName, "addListener", MRLListener);

}


	public int getPosition() {
		return position;
	}

	// feedback
	// public static final String FEEDBACK_TIMER = "FEEDBACK_TIMER";
	enum FeedbackType {
		Timer, Digital
	}

	Timer timer = new Timer();
	FeedbackType poistionFeedbackType = FeedbackType.Timer;

	enum BlockingMode {
		Blocking, Staggered, Overlap
	}

	BlockingMode blockingMode = BlockingMode.Blocking;

	
	 TODO - motors should not have any idea as to what their "pins" are - this
	 should be maintained by the controller
	

String encoderName = null; // feedback device
int encoderPin = 0; // TODO - put in Encoder class


	public int incrementPosition(PinData p) {
		if (p.pin != encoderPin) // TODO TODO TODO - this is wrong - should be
									// filtering on the Arduino publish !!!!
			return 0;

		if (direction == FORWARD) {
			position += 1;
			if (movingToPosition && position >= targetPosition) {
				stopMotor();
				movingToPosition = false;
			}

		} else {
			position -= 1;
			if (movingToPosition && position <= targetPosition) {
				stopMotor();
				movingToPosition = false;
			}
		}

		return position;

	}


	// move to relative amount - needs position feedback
	public void move(int amount) // TODO - "amount" should be moveTo
	{
		// setPower(lastPower);
		if (amount == 0) {
			return;
		} else if (direction == FORWARD) {
			direction = FORWARD;
			position += amount;
		} else if (direction == BACKWARD) {
			direction = BACKWARD;
			position -= amount;
		}

		move();
		amount = Math.abs(amount);
		if (poistionFeedbackType == FeedbackType.Timer
				&& blockingMode == BlockingMode.Blocking) {
			try {
				Thread.sleep(amount * positionMultiplier);
			} catch (InterruptedException e) {
				logException(e);
			}
			// TODO - this is overlapp mode (least useful)
			// timer.schedule(new FeedbackTask(FeedbackTask.stopMotor, amount *
			// positionMultiplier), amount * positionMultiplier);
		}

		stopMotor();
	}

		// TODO - enums pinMode & OUTPUT
		// TODO - abstract "controller" Controller.OUTPUT

		send(controllerName, "pinMode", powerPin, Arduino.OUTPUT); // TODO THIS IS NOT!!! A FUNCTION OF THE MOTOR - THIS NEEDS TO BE TAKEN CARE OF BY THE BOARD
		send(controllerName, "pinMode", directionPin, Arduino.OUTPUT);

	public void move(int direction, float power, int amount) {
		setDir(direction);
		setPower(power);
		move(amount);
	}


	public void setDir(int direction) {
		if (locked) return;
		
		this.direction = direction;		
	}

	public void move(int direction, float power) {
		if (locked) return;
		
		setDir(direction);
		setPower(power);
		move();
	}

	public void moveTo(Integer newPos)
	{
		targetPosition = newPos;
		movingToPosition = true;
		if (position - newPos < 0) {
			setDir(FORWARD);
			// move(Math.abs(position - newPos));
			setPower(0.5f);
			move();
		} else if (position - newPos > 0) {
			setDir(BACKWARD);
			// move(Math.abs(position - newPos));
			setPower(0.5f);
			move();
		} else
			return;
	}

	public void moveCW() {
		setDir(FORWARD);
		move();
	}

	public void moveCCW() {
		setDir(BACKWARD);
		move();
	}

	public void setPower(float power) {
		if (locked) return;
		
		if (power > maxPower || power < -maxPower) 
		{
			log.error(power + " power out of bounds - max power is "+ maxPower);
			return;
		}
		
		this.power = power;
		move(power);
	}

	int positionMultiplier = 1000;
	boolean useRamping = false;
	public void setUseRamping(boolean ramping) {
		useRamping = ramping;
	}

	// motor primitives end ------------------------------------
	/*
	 * Power and Direction parameters work on the principle that they are values
	 * of a motor, but are not operated upon until a "move" command is issued.
	 * "Move" will direct the motor to move to use the targeted power and
	 * direction.
	 * 
	 * All of the following functions use primitives and are basically composite
	 * functions
	 */

