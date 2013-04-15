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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.ClockEvent;
import org.slf4j.Logger;


public class Clock extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Clock.class.getCanonicalName());

	public boolean isClockRunning;
	public int interval = 100000;
	public String data = null;

//	public transient ClockThread myClock = null;

	ArrayList<ClockEvent> events = new ArrayList<ClockEvent>();
/*
	public class ClockThread implements Runnable {
		public Thread thread = null;

		ClockThread() {
			thread = new Thread(this, getName() + "_ticking_thread");
			thread.start();
		}

		public void run() {
			try {
				while (isClockRunning == true) {
					Date now = new Date();
					Iterator<ClockEvent> i = events.iterator();
					while (i.hasNext()) {
						ClockEvent event = i.next();
						if (now.after(event.time)) {
							// TODO repeat - don't delete set time forward
							// interval
							send(event.name, event.method, event.data);
							i.remove();
						}
					}
					invoke("pulse", data);
					Thread.sleep(interval);
				}
			} catch (InterruptedException e) {
				log.info("ClockThread interrupt");
				isClockRunning = false;
			}
		}
	}
*/
	public Clock(String n) {
		super(n, Clock.class.getCanonicalName());
	}

	public void startClock() {
		data = "HELLO WORLD";
		interval = 777;
		isClockRunning = true;
		/*
		if (myClock == null) {
			isClockRunning = true;
			myClock = new ClockThread();
			broadcastState();
		}
		*/
		broadcastState();
	}

	public void stopClock() {
		/*
		if (myClock != null) {
			log.info("stopping " + getName() + " myClock");
			isClockRunning = false;
			myClock.thread.interrupt();
			myClock.thread = null;
			myClock = null;
			broadcastState();
		}
		
		isClockRunning = false;
		broadcastState();
		*/
	}
	
	public String pulse()
	{
		return data;
	}

	public void setInterval(Integer milliseconds) {
		interval = milliseconds;
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

	public void addClockEvent(Date time, String name, String method, Object... data) {
		ClockEvent event = new ClockEvent(time, name, method, data);
		events.add(event);
	}

	public static void main(String[] args) throws ClassNotFoundException, CloneNotSupportedException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Runtime runtime = new Runtime("ras");
		runtime.startService();
		Clock clock = new Clock("clock");
		clock.startService();

//		Clock clock2 = (Clock)clock.clone();
		// clock.addClockEvent(Clock.getFutureDate(30), "clock", "booYa", new
		// Object[] { "BOOYA!!!" });
		// clock.startCountDownTimer();
		Runtime.createAndStart("remote", "RemoteAdapter");
		//Runtime.createAndStart("gui", "GUIService");

	}

}