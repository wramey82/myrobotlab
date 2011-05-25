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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TooManyListenersException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Clock.ClockThread;
import org.myrobotlab.service.Clock.PulseDataType;
import org.myrobotlab.service.data.IOData;
import org.myrobotlab.service.data.PinData;
import org.myrobotlab.service.interfaces.AnalogIO;
import org.myrobotlab.service.interfaces.DigitalIO;
import org.myrobotlab.service.interfaces.ServoController;

/*
 *  Currently supports:
 *   	Arduino Duemilanove - http://arduino.cc/en/Main/ArduinoBoardDuemilanove
 *   
 *   - Find Arduino Message set - DigitalWrite (pin, data?)
 *   - ArduinoProgram HashMap<Key, Program>
 *   - loadProgram (Key)
 *   - key - default key & program   
 *   
 *   References:
 *   http://www.arduino.cc/playground/Main/RotaryEncoders
 *   
 */
public class PICAXE extends Service //implements SerialPortEventListener, DigitalIO, AnalogIO, ServoController 
{

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(PICAXE.class.getCanonicalName());

	// fields
	public int interval = 1000;
	public PulseDataType pulseDataType = PulseDataType.none;
	public String pulseDataString = null;
	public int pulseDataInteger;	
	public transient PICAXEThread myPICAXE = null;

	// types
	public enum PulseDataType {none, integer, increment, string};

	
	public class PICAXEThread implements Runnable
	{
		public Thread thread = null;
		public boolean isRunning = true;
		
		PICAXEThread()
		{
			thread = new Thread(this,name + "_ticking_thread");
			thread.start();
		}
				
		public void run()
		{			
			try {
				while (isRunning == true)
				{
					if (pulseDataType == PulseDataType.increment)
					{
						invoke("pulse", pulseDataInteger);
						++pulseDataInteger;
					} else if (pulseDataType == PulseDataType.integer) {
						invoke("pulse", pulseDataInteger);
					} else if (pulseDataType == PulseDataType.none) {
						invoke("pulse");						
					} else if (pulseDataType == PulseDataType.string) {
						invoke("pulse", pulseDataString);												
					}

					Thread.sleep(interval);
				}
			} catch (InterruptedException e) {
				LOG.info("PICAXEThread interrupt");
				isRunning = false;
			}
		}
	}

	public PICAXE(String n) {
		super(n, PICAXE.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	// TODO - how 
	public void setPulseDataType (PulseDataType t)
	{
		pulseDataType = t;		
	}
	
	public void startPICAXE()
	{
		if (myPICAXE == null)
		{
			myPICAXE = new PICAXEThread();
		}
	}
	
	public void stopPICAXE()
	{
		if (myPICAXE != null) 
		{
			LOG.info("stopping " + name + " myPICAXE");
			myPICAXE.isRunning = false;
			myPICAXE.thread.interrupt();
			myPICAXE.thread = null;
			myPICAXE = null;
		}
	}

	// TODO - enum pretty unsuccessful as
	// type does not make it through Action
	public void setType (String t)
	{
		if (t.compareTo("none") == 0)
		{
			pulseDataType = PulseDataType.none;
		} else if (t.compareTo("increment") == 0)
		{
			pulseDataType = PulseDataType.increment;
			
		} else if (t.compareTo("string") == 0)
		{
			pulseDataType = PulseDataType.string;
			
		} else if (t.compareTo("integer") == 0)
		{
			pulseDataType = PulseDataType.integer;
			
		} else {
			LOG.error("unknown type " + t);
		}
	}
	
	public void setType (PulseDataType t)
	{
		pulseDataType = t;
	}

	public void pulse() {
	}
	
	public Integer pulse(Integer count) {
		LOG.info("pulse " + count);
		return count;
	}

	public String pulse(String d) {
		return d;
	}
	
	// new state functions begin --------------------------
	public PICAXE publishState()
	{
		return this;
	}

	// TODO - reflectively do it in Service? !?
	// No - the overhead of a Service warrants a data only proxy - so to
	// a single container class "PICAXEData data = new PICAXEData()" could allow
	// easy maintenance and extensibility - possibly even reflective sync if names are maintained   
	public PICAXE setState(PICAXE o)
	{
		this.interval = o.interval;
		this.pulseDataInteger = o.pulseDataInteger;
		this.pulseDataString = o.pulseDataString;
		//this.myPICAXE = o.myPICAXE;  
		this.pulseDataType = o.pulseDataType;
		return o;
	}
	
	public PICAXE getState()
	{
		return this;
	}
	
	
	public String setPulseDataString(String s)
	{
		pulseDataString = s;
		return s;
	}

	public Integer setPulseDataInteger (Integer s)
	{
		pulseDataInteger = s;
		return s;
	}
	
	// new state functions end ----------------------------
	
	public void setInterval(Integer milliseconds) {
		interval = milliseconds;
	}

	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
						
		//RemoteAdapter remote = new RemoteAdapter("remote");
		//remote.startService(); 
		// test
		
		PICAXE PICAXE = new PICAXE("PICAXE");
		PICAXE.startService();
		
		RemoteAdapter remote = new RemoteAdapter("remote");
		remote.startService();

						
//		Logging log = new Logging("log");
//		log.startService();
		
//		PICAXE.notify("pulse", "log", "log", Integer.class);

//		GUIService gui = new GUIService("gui");
//		gui.startService();	
//		gui.display();


/*		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			
		       fos = new FileOutputStream("test.backup");
		       out = new ObjectOutputStream(fos);
		       out.writeObject(remote);
		       out.writeObject(log);
		       out.writeObject(PICAXE);
		       out.writeObject(gui);
		       out.close();
		    
			
		       FileInputStream fis = new FileInputStream("test.backup");
		       ObjectInputStream in = new ObjectInputStream(fis);
		       Logging log = (Logging)in.readObject();
		       PICAXE PICAXE = (PICAXE)in.readObject();
		       GUIService gui = (GUIService)in.readObject();
		       in.close();
		       
		       log.startService();

		       PICAXE.startService();
		       PICAXE.startPICAXE();
		       
		       gui.startService();
		       gui.display();
		    
		       
		} catch (Exception e)
		{
			LOG.error(e.getMessage());
			LOG.error(stackToString(e));
		}

		*/

		
	}

	@Override
	public void stopService() {
		stopPICAXE();
		super.stopService();
	}
	
	@Override
	public String getToolTip() {
		return "(not implemented yet) used to interface PICAXE";
	}

	
}
