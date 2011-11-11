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

import java.awt.Rectangle;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

public class TrackingService extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(TrackingService.class
			.getCanonicalName());

	// TODO center vs - lock
	/*
	 * 
	 * There is several kinds of tracking - centering on a point creating a
	 * bounding box - keeping as many points as possible within the bounding box
	 * getting a set of points - filtering/eliminating stragglers & setting new
	 * points
	 * 
	 * motion establishes initial ROI - publishes ROI tracker? gets ROI sends
	 * ROI to opencv and requests for set of points within ROI set of tracking
	 * points are published iterative tracking - points or camera is in motion
	 * points which do not move together are eliminated
	 * 
	 * if a set of points reach a threshold - tracker attempts to center the
	 * centeroid of points
	 * 
	 * 
	 * parameters: DeadZone
	 * 
	 * 
	 * also - you may or may not know where the servos are to keep it
	 * generalized - assume you don't although there would be advantages if
	 * "something" knew where they were... relative v.s. absolute coordinates
	 * 
	 * BIG JUMP assess error recalculate multiplication factor small correction
	 * 
	 * CONCEPT - calibrate with background - background should have its own set
	 * of key/tracking points
	 * 
	 * Better head gimble - less jumping around
	 * 
	 * Single formula to trac because the X Y access of the servos do not
	 * necessarily align with the X Y axis of opencv - so the ration to move to
	 * the right may correspond to -3X +1Y to move along the opencv X axis
	 * 
	 * 
	 * Background set array of points & saved picture
	 * 
	 * NEEDS needs to be smoother - less jerky needs to create new points needs
	 * to make a roi from motion needs to focus at the top of the motion
	 */

	boolean tracking = true;

	float XMultiplier = (float) 0.15; // 0.14 ~ 0.16
	float YMultiplier = (float) 0.15;

	transient CvPoint2D32f lastPoint = null;
	transient CvPoint2D32f targetPoint = null;
	Rectangle deadzone = new Rectangle();

	public TrackingService(String n) {
		super(n, TrackingService.class.getCanonicalName());

		deadzone.x = 150;
		deadzone.width = 20;
		deadzone.y = 110;
		deadzone.height = 20;
		
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub
		/*
		 * input range min & max output range min & max coordinate system polar
		 * / cartesian calibrate x & y direction scalar
		 */

	}

	final static public Integer correctX(Integer f) {
		LOG.info("correctX " + f);
		return f;
	}

	final static public Integer correctY(Integer f) {
		LOG.info("correctY " + f);
		return f;
	}


	// note - using pt.x() - gets the first point if an array is sent
	final public void center(CvPoint2D32f pt) {
		//pt.
		trackX((int) pt.x());
		trackY((int) pt.y());
	}

	final public void center(CvPoint pt) {
		deadzone.x = 155;
		deadzone.width = 10;
		deadzone.y = 115;
		deadzone.height = 10;

		trackX((int) pt.x());
		trackY((int) pt.y());
	}

	float XCorrection = 0;
	float YCorrection = 0;

	final public void trackX(Integer x) {
		XMultiplier = (float) -0.12;

		// FIXME - bad - but can't put it into constructor or ServiceTest will fail with UnsatisfiedLinkError
		if (targetPoint == null)
		{
			targetPoint = new CvPoint2D32f(160, 120);
		}
		LOG.info("trackPointX " + x);
		XCorrection = (x - targetPoint.x()) * XMultiplier;
		if (tracking && (x < deadzone.x || x > deadzone.x + deadzone.width)) {
			invoke("correctX", (int) XCorrection);
		}

	}

	final public void trackY(Integer y) {
		YMultiplier = (float) -0.12;
		LOG.info("trackPointY " + y);
		YCorrection = (y - targetPoint.y()) * YMultiplier;
		if (tracking && (y < deadzone.y || y > deadzone.y + deadzone.width)) {
			invoke("correctY", (int) YCorrection);
		}
	}

	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation - full PID not implemented yet :P";
	}
	
	
}
