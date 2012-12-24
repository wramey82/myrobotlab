package org.myrobotlab.tracking;

import org.myrobotlab.service.Servo;

/**
 * ControlSystem is responsible for the underlying control of
 * the tracking system.  Motors, platforms, or servos might
 * need control from the control system.
 * 
 * Direct control or messaging?  
 * 
 * Decided to go with direct control to avoid the possibility 
 * of stacked message buffer. This requires local actuator services.
 * 
 */
public class ControlSystem {
	
	//private boolean isRunning = false;
	private Servo pan;
	private Servo tilt;
	
	/*
	public void run()
	{
		isRunning = true;
		while(isRunning)
		{
			
		}
	}
	
	
	public void release()
	{
		isRunning = false;
	}
	*/
	
	public void setServoX(Servo x)
	{
		this.pan = x;
	}

	public void setServoY(Servo y)
	{
		this.tilt = y;
	}
	
	public void moveXTo(int pos)
	{
		pan.moveTo(pos);
	}

	public void moveYTo(int pos)
	{
		tilt.moveTo(pos);
	}
}
