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

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;

public class Logging extends Service {

	public final static Logger LOG = Logger.getLogger(Logging.class.getCanonicalName());
	
	/*
	 * TODO - allow options to record and playback message log - serialize to disk etc
	 */
	
	public Logging(String n) {
		this(n, null);
	}

	public Logging(String n, String serviceDomain) {
		super(n, Logging.class.getCanonicalName(), serviceDomain);
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub
		
	}
	
	public Message log (Message m)
	{
		return m;
	}
	
	public void run ()
	{
		try {			
			
			while (isRunning) {
				Message m = getMsg();
				if (m.method.compareTo("log") == 0)
				{
					invoke("log", m);
				} else {
					process(m);;
				}
				
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// TODO - do in Service
	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		Logging toy = new Logging("toy");
		toy.startService();
	}

	@Override
	public String getToolTip() {
		return "logging service";
	}
	
}
