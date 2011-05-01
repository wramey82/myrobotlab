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

public class PID extends Service {

	public final static Logger LOG = Logger.getLogger(PID.class.getCanonicalName());

	double p = 0;
	double i = 0;
	double d = 0;
	
	double setPoint = 0;
	
	long deltaTime = 0; // delta since last input
	long lastTime = 0;
	
	double input = 0;
	double output = 0;
	
	double inputMin = 0;
	double inputMax = 1.0;
	
	double outputMin = 0;
	double outputMax = 1.0;
	
	double tolerance = 1.0;
	
	boolean isContinuous = false;
	
	double errorSum = 0;
	double lastError = 0;
	
	public PID(String n) {
		super(n, PID.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
	}	

	public void setPID (double p, double i, double d)
	{
		deltaTime = System.currentTimeMillis();
		this.p = p;
		this.i = i;
		this.d = d;		
	}
	
	public double Compute()
	{
		/*How long since we last calculated*/
		long now = System.currentTimeMillis(); 
		deltaTime = (now - lastTime);

		/*Compute all the working error variables*/
		double error = setPoint - input;
		errorSum += (error * deltaTime);
		double dErr = (error - lastTime) / deltaTime;
		 
		/*Compute PID Output*/
		output = p * error + i * errorSum + d * dErr;
		 
		/*Remember some variables for next time*/
		lastError = error;
		lastTime = now;	
		
		// TODO - limit with min/max
		return output;
	}
	
	public void setInput (int i)
	{
		// TODO - limit with min/max
		input = i;
		lastTime = System.currentTimeMillis();
	}
	
	public void setSetpoint (double setPoint)
	{
		this.setPoint = setPoint;
	}
	
	public void setInputRange (double min, double max)
	{
		this.inputMin = min;
		this.inputMax = max;
	}

	public void setOutputRange (double min, double max)
	{
		this.outputMin = min;
		this.outputMax = max;
	}
	
	public void setTolerance(double percent)
	{
		this.tolerance = percent;
	}
	
	public void setContinuous()
	{
		setContinuous(true);
	}

	public void setContinuous(boolean v)
	{
		isContinuous = v;
	}
	
	public boolean onTarget()
	{
		return false;
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		PID pid = new PID("pid");
		pid.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}
	
	@Override
	public String getToolTip() {
		return "<html>a PID control service,<br>"+
		"with very helpful tutorial from<br>"+
		"http://brettbeauregard.com/blog/2011/04/improving-the-beginners-pid-introduction/</html>";
	}


}
