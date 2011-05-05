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
import org.myrobotlab.service.data.NameValuePair;

public class Clock extends Service {

	public final static Logger LOG = Logger.getLogger(Clock.class.getCanonicalName());
	private int interval = 1000;
	
	ClockThread myClock = null;
	
	public class ClockThread implements Runnable
	{
		public Thread thread = null;
		public int interval;
		public int cnt;
		public boolean isRunning = true;
		
		ClockThread(int interval)
		{
			this.interval = interval;
			thread = new Thread(this,name + "_ticking_thread");
			thread.start();
		}
				
		public void run()
		{			
			try {
				while (isRunning == true)
				{
					Thread.sleep(interval);
					invoke("pulse", cnt);
					++cnt;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				LOG.info("ClockThread interrupt");
				isRunning = false;
			}
		}
	}

	public Clock(String n) {
		super(n, Clock.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
	}
	
	public void startClock()
	{
		if (myClock == null)
		{
			myClock = new ClockThread(interval);
		}
	}
	
	public void stopClock()
	{
		if (myClock != null) 
		{
			LOG.info("stopping " + name + " myClock");
			myClock.isRunning = false;
			myClock.thread.interrupt();
			myClock.thread = null;
			myClock = null;
		}
	}


	public Integer pulse(Integer count) {
		LOG.info("pulse " + count);
		return count;
	}

	
	public void setInterval(Integer milliseconds) {
		interval = milliseconds;
	}

	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		Clock clock = new Clock("clock");
		clock.startService();

		//OpenCV camera = new OpenCV("camera");
		//camera.startService();
		
		Logging log = new Logging("log");
		log.startService();
		
		clock.notify("pulse", "log", "log", Integer.class);
		//log.notify("ditty", "gui", "dood", Integer.class);
		//log.notify("shmooker", "gui", "pooker", Integer.class);
		
		//Invoker invoker = new Invoker("invoker");
		//invoker.startService();
		//invoker.notify("pulse", "log", "invoker", Integer.class);

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}

	@Override
	public void stopService() {
		stopClock();
		super.stopService();
	}
	
	@Override
	public String getToolTip() {
		return "used to generate pulses";
	}


}
