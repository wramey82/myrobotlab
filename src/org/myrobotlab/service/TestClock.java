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

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;

public class TestClock extends Service {

	public final static Logger LOG = Logger.getLogger(TestClock.class
			.getCanonicalName());
	public int cnt = 0;
	public ArrayList<Integer> catcher = new ArrayList<Integer>();

	public TestClock(String n) {
		super(n, TestClock.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("interval", 1000);
	}

	public Integer pulse3() {
		Integer count = new Integer(999);
		LOG.info("pulse3 " + count);
		return count;
	}

	public Integer pulse(Integer count) {
		LOG.info("pulse " + count);
		return count;
	}

	public Integer recvAndAddPulse(Integer count) {
		LOG.info("recvAndAddPulse " + count);
		catcher.add(count);
		return count;
	}

	public Float pulseFloat(Integer count) {
		LOG.info("pulse " + count);
		return new Float(count);
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			++cnt;
			invoke("pulse", cnt);
			try {
				Thread.sleep(cfg.get("interval", 1000));
			} catch (InterruptedException e) {
				running = false;
			}
		}

	}

	@Override
	public String getToolTip() {
		return "<html>service for junit tests</html>";
	}
	
}
