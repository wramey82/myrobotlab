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
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class Clock extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Clock.class.getCanonicalName());

	transient ArrayList<ClockEvent> events = new ArrayList<ClockEvent>(); // FIXME
																			// -
																			// should
																			// be
																			// serializable
																			// -
																			// but
																			// isnt

	public static class ClockEvent {

		Date time = null;
		String name;
		String method;
		Object[] data = null;

		public ClockEvent(Date time, String name, String method, Object[] data) {
			this.time = time;
			this.name = name;
			this.method = method;
			this.data = data;
		}
	}

	// fields
	@Element
	public int interval = 1000;
	@Element
	public PulseDataType pulseDataType = PulseDataType.none;
	@Element(required = false)
	public String pulseDataString = null;
	@Element
	public int pulseDataInteger;

	// TODO - design concept - this should (probably) be private - yet the
	// current framework does
	// not allow copying of private data - setting this does not "make sense"
	// without the
	// appropriate action - which is creating a clock thread
	public boolean isClockRunning = false;

	public transient ClockThread myClock = null;

	// types
	public enum PulseDataType {
		none, integer, increment, string, date
	};

	public class ClockThread implements Runnable {
		public Thread thread = null;
		public Date stopDate = null;

		ClockThread() {
			thread = new Thread(this, getName() + "_ticking_thread");
			thread.start();
		}

		public void run() {
			try {
				while (isClockRunning == true) {
					Date now = new Date();

					if (pulseDataType == PulseDataType.increment) // TODO
																	// refactor
																	// to
																	// pulse(Object
																	// ?) ???
					{
						invoke("pulse", pulseDataInteger);
						++pulseDataInteger;
					} else if (pulseDataType == PulseDataType.integer) {
						invoke("pulse", pulseDataInteger);
					} else if (pulseDataType == PulseDataType.none) {
						invoke("pulse");
					} else if (pulseDataType == PulseDataType.string) {
						invoke("pulse", pulseDataString);
					} else if (pulseDataType == PulseDataType.date) {
						// invoke("pulse", new Date());

						long diff = stopDate.getTime() - now.getTime();
						invoke("countdown", diff);

					}

					Iterator<ClockEvent> i = events.iterator();
					while (i.hasNext()) {
						ClockEvent event = i.next();
						if (now.after(event.time)) {
							send(event.name, event.method, event.data); // TODO
																		// -
																		// repeat
																		// -
																		// dont
																		// delete
																		// - set
																		// time
																		// forward
																		// interval
							i.remove();
						}
					}

					if (stopDate != null) {
						if (now.after(stopDate)) {
							log.info("stopping countdown");
							invoke("finished", stopDate);
							isClockRunning = false;
						}
					}

					Thread.sleep(interval);
				}
			} catch (InterruptedException e) {
				log.info("ClockThread interrupt");
				isClockRunning = false;
			}
		}
	}

	public Clock(String n) {
		super(n, Clock.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	// TODO - how
	public void setPulseDataType(PulseDataType t) {
		pulseDataType = t;
	}

	public void startCountDown(Date futureDate) {
		if (myClock != null) {
			stopClock();
		}

		myClock = new ClockThread();
		pulseDataType = PulseDataType.date;
		myClock.stopDate = futureDate;
		interval = 1000;
		startClock();
	}

	/**
	 * publish point for countdown pulses -
	 * 
	 * @param amtRemaining
	 *            - amount of time remaining in seconds
	 * @return
	 */
	public Long countdown(Long amtRemaining) {
		log.info(amtRemaining);
		return amtRemaining;
	}

	public void startClock() {
		if (myClock == null) {
			myClock = new ClockThread();
		}

		isClockRunning = true;
		invoke("publishState"); // TODO - a "bit" heavy handed? appropriate I
								// think
	}

	public void stopClock() {
		if (myClock != null) {
			log.info("stopping " + getName() + " myClock");
			myClock.thread.interrupt();
			myClock.thread = null;
			myClock = null;
		}

		isClockRunning = false;
		invoke("publishState"); // TODO - a "bit" heavy handed? appropriate I
								// think
	}

	// TODO - enum pretty unsuccessful as
	// type does not make it through Action
	public void setType(String t) {
		if (t.compareTo("none") == 0) {
			pulseDataType = PulseDataType.none;
		} else if (t.compareTo("increment") == 0) {
			pulseDataType = PulseDataType.increment;

		} else if (t.compareTo("string") == 0) {
			pulseDataType = PulseDataType.string;

		} else if (t.compareTo("date") == 0) {
			pulseDataType = PulseDataType.date;

		} else if (t.compareTo("integer") == 0) {
			pulseDataType = PulseDataType.integer;

		} else {
			log.error("unknown type " + t);
		}
	}

	public void setType(PulseDataType t) {
		pulseDataType = t;
	}

	public void pulse() {
	}

	public Integer pulse(Integer count) {
		log.info("pulse " + count);
		return count;
	}

	public String pulse(String d) {
		return d;
	}

	// TODO - reflectively do it in Service? !?
	// No - the overhead of a Service warrants a data only proxy - so to
	// a single container class "ClockData data = new ClockData()" could allow
	// easy maintenance and extensibility - possibly even reflective sync if
	// names are maintained
	/*
	 * public Clock setState(Clock o) { this.interval = o.interval;
	 * this.pulseDataInteger = o.pulseDataInteger; this.pulseDataString =
	 * o.pulseDataString; //this.myClock = o.myClock; this.pulseDataType =
	 * o.pulseDataType; return o; }
	 */

	// TODO - you "could" get rid of these functions
	public String setPulseDataString(String s) {
		pulseDataString = s;
		return s;
	}

	public Integer setPulseDataInteger(Integer s) {
		pulseDataInteger = s;
		return s;
	}

	// new state functions end ----------------------------

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

	public Date finished(Date finishedDate) {
		log.info(finishedDate);
		return finishedDate;
	}

	public void addClockEvent(Date time, String name, String method, Object... data) {
		ClockEvent event = new ClockEvent(time, name, method, data);
		events.add(event);
	}

	public ClockEvent publishClockEvent(ClockEvent clockEvent) {
		return clockEvent;
	}

	public static Date getFutureDate(Integer seconds) {
		return getFutureDate(null, null, seconds);
	}

	public static Date getFutureDate(Integer minutes, Integer seconds) {
		return getFutureDate(null, minutes, seconds);
	}

	public static Date getFutureDate(Integer hours, Integer minutes, Integer seconds) {
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		if (hours != null)
			cal.add(Calendar.HOUR, hours);
		if (minutes != null)
			cal.add(Calendar.MINUTE, minutes);
		if (seconds != null)
			cal.add(Calendar.SECOND, seconds);
		return cal.getTime();
	}

	public static Date add(Date startDate, Integer seconds) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.add(Calendar.SECOND, seconds);
		return cal.getTime();
	}

	public void booYa(String s) {
		log.info(s);
	}

	public void booYa() {
		log.info("boink");
	}

	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Clock clock = new Clock("clock");
		clock.startService();

		clock.addClockEvent(Clock.getFutureDate(30), "clock", "booYa", new Object[] { "BOOYA!!!" });
		// clock.startCountDownTimer();

		Runtime.createAndStart("gui", "GUIService");

	}

}