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

public class TestCatcher extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(TestCatcher.class
			.getCanonicalName());
	public ArrayList<Integer> catchList = new ArrayList<Integer>();
	public ArrayList<Integer> lowCatchList = new ArrayList<Integer>();

	public TestCatcher(String n) {
		super(n, TestCatcher.class.getCanonicalName());
	}

	public TestCatcher(String n, String hostname) {
		super(n, TestCatcher.class.getCanonicalName(), hostname);
	}

	public void loadDefaultConfiguration() {
	}

	public void catchNothing() {
		LOG.info("***CATCH*** catchNothing ");
		Integer c = 1;
		synchronized (catchList) {
			catchList.add(c);
			catchList.notify();
		}

	}

	public Object returnNull() {
		return null;
	}

	public Integer catchInteger(Integer count) {
		LOG.info("***CATCH*** catchInteger " + count);
		synchronized (catchList) {
			catchList.add(count);
			catchList.notify();
		}
		return count;
	}

	public Integer lowCatchInteger(Integer count) {
		LOG.info("***CATCH*** lowCatchInteger " + count);
		synchronized (lowCatchList) {
			lowCatchList.add(count);
			lowCatchList.notify();
		}
		return count;

	}

	public Integer bothHandsCatchInteger(Integer firstBall, Integer secondBall) {
		LOG.info("***CATCH*** bothHandsCatchInteger " + firstBall + ","
				+ secondBall);
		LOG.info(catchList.size());

		synchronized (catchList) {
			catchList.add(firstBall);
			catchList.add(secondBall);
			catchList.notify();
		}

		LOG.info("bothHandsCatchInteger " + firstBall + "," + secondBall);
		LOG.info("bothHandsCatchInteger size " + catchList.size());

		return catchList.size();
	}

	public Integer twoHandedPrimitiveCatchInt(int firstBall, int secondBall) {
		LOG.info("***CATCH*** twoHandedPrimitiveCatchInt " + firstBall + ","
				+ secondBall);
		synchronized (catchList) {
			catchList.add(firstBall);
			catchList.add(secondBall);
			catchList.notify();
		}
		return lowCatchList.size();

	}

	public Integer throwBack(Integer count) {
		LOG.info("throwBack " + count);
		return count;
	}

	public void waitForCatches(int numberOfCatches, int maxWaitTimeMilli) {
		LOG.info(getName() + ".waitForCatches waiting for " + numberOfCatches
				+ " currently " + catchList.size());
		synchronized (catchList) {
			while (catchList.size() < numberOfCatches) {
				try {
					catchList.wait(maxWaitTimeMilli);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					LOG.error("waitForCatches " + numberOfCatches
							+ " interrupted");
					// logException(e);
				}
			}
		}

	}

	public void waitForLowCatches(int numberOfCatches, int maxWaitTimeMilli) {
		LOG.info(getName() + ".waitForLowCatches waiting for " + numberOfCatches
				+ " currently " + lowCatchList.size());
		synchronized (lowCatchList) {
			while (lowCatchList.size() < numberOfCatches) {
				try {
					lowCatchList.wait(maxWaitTimeMilli);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					LOG.error("testObject1List " + numberOfCatches
							+ " interrupted");
					// logException(e);
				}
			}
		}

	}

	@Override
	public String getToolTip() {
		return "<html>service for junit tests</html>";
	}
		
}
