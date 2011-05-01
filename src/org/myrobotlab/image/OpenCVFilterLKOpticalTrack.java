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


package org.myrobotlab.image;

import static com.googlecode.javacv.jna.cv.CV_BGR2GRAY;
import static com.googlecode.javacv.jna.cv.CV_LKFLOW_PYR_A_READY;
import static com.googlecode.javacv.jna.cv.cvCalcOpticalFlowPyrLK;
import static com.googlecode.javacv.jna.cv.cvCvtColor;
import static com.googlecode.javacv.jna.cv.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.jna.cxcore.CV_RGB;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_EPS;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.jna.cxcore.cvCreateImage;
import static com.googlecode.javacv.jna.cxcore.cvDrawLine;
import static com.googlecode.javacv.jna.cxcore.cvGetSize;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cv;
import com.googlecode.javacv.jna.cxcore;
import com.googlecode.javacv.jna.cxcore.CvPoint;
import com.googlecode.javacv.jna.cxcore.CvPoint2D32f;
import com.googlecode.javacv.jna.cxcore.CvSize;
import com.googlecode.javacv.jna.cxcore.CvTermCriteria;
import com.googlecode.javacv.jna.cxcore.IplImage;
import org.myrobotlab.service.OpenCV;
import com.sun.jna.ptr.IntByReference;

public class OpenCVFilterLKOpticalTrack extends OpenCVFilter {

	public final static Logger LOG = Logger.getLogger(OpenCVFilterLKOpticalTrack.class.getCanonicalName());

	IplImage image = null;
	IplImage grey = null;
	IplImage prev_grey = null;
	IplImage pyramid = null;
	IplImage prev_pyramid = null;
	IplImage swap_temp = null;
	IplImage eig = null;
	IplImage temp = null;
	IplImage mask = null;

	int win_size = 20;
	int maxCount = 3;
	
	// TODO - all initializations of arrays will need to be done in 1st iteration
	float[] horizontalDisparity = new float[maxCount];
	IntByReference featurePointCount = new IntByReference(maxCount);

	CvPoint2D32f current_features[] = null;
	CvPoint2D32f previous_features[] = null;
	CvPoint2D32f saved_features[] = null;
	CvPoint2D32f swap_points[] = null;

	float distance[] = new float[maxCount];

	byte[] status = new byte[maxCount];
	float[] error = null;
	byte status_value = 0;
	int count = 0;
	int flags = 0;

	double quality = 0.01;
	double min_distance = 10;
	boolean needTrackingPoints = true; // TODO - remove - should not use
										// collection

	int featureSetDump = 0;

	// init related
	CvSize cvWinSize;
	CvTermCriteria termCrit;

	// display related
	Graphics2D graphics = null;
	BufferedImage frameBuffer = null;

	public OpenCVFilterLKOpticalTrack(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub
		cfg.set("filterType", OpenCVFilterLKOpticalTrack.class.getCanonicalName());
		cfg.set("featurePointCount", 30);
		cfg.set("pixelsPerDegree", 7);
	}

	boolean needToInitialize = true;

	@Override
	public BufferedImage display(IplImage frame, Object[] data) {
		// TODO - realize that you don't have to convert to BufferedImage -
		// in fact if you are going to pipeline the image it is definitely
		// faster if you use IplImage

		frameBuffer = frame.getBufferedImage();
		graphics = frameBuffer.createGraphics();
		graphics.setColor(Color.green);

		int validPoints = 0;
		int x = 0;
		int y = 0;

		for (int i = 0; i < count; ++i) {

			x = (int) current_features[i].x;
			y = (int) current_features[i].y;

			if (status[i] == 1) {
				++validPoints;
				if (graphics != null) {
					graphics.setColor(Color.red);
					graphics.drawLine(x - 2, y, x + 2, y);
					graphics.drawLine(x, y + 2, x, y - 2);
					graphics.drawString(x + "," + y, x, y);
				}
			}
		}

		if (graphics != null) {
			graphics.drawString("valid " + validPoints, 10, 10);
		}

		// TODO - check if this is correct dispose after ever new frame?
		if (graphics != null) {
			graphics.dispose();
		}
		graphics = null;

		return frameBuffer;

	}

	int add_remove_pt = 0;

	public void samplePoint(MouseEvent event) {
		// MouseEvent me = (MouseEvent)params[0];
		if (count < maxCount && event.getButton() == 1) {
			// current_features[count++] = new
			// cxcore.CvPoint2D32f(event.getPoint().x, event.getPoint().y);
			pt.x = event.getPoint().x;
			pt.y = event.getPoint().y;
			add_remove_pt = 1;
			/*
			 * cv.cvFindCornerSubPix( grey, current_features[count - 1], 1,
			 * cxcore.cvSize(win_size,win_size).byValue(),
			 * cxcore.cvSize(-1,-1).byValue(),
			 * cxcore.cvTermCriteria(CV_TERMCRIT_ITER
			 * |CV_TERMCRIT_EPS,20,0.03).byValue());
			 */
		} else {
			clearPoints();
		}

		// add_remove_pt = 0;
	}

	public void samplePoint(Point p) {
		if (count < maxCount) {
			pt.x = p.x;
			pt.y = p.y;
			add_remove_pt = 1;
		}
	}

	public void clearPoints() {
		count = 0;
	}

	int i, k;
	CvPoint pt = new CvPoint(0, 0);
	CvPoint circle_pt = new CvPoint(0, 0);

	// boolean tracking = false;

	@Override
	public IplImage process(IplImage frame) {
		if (frame == null) {
			return null;
		}

		if (needToInitialize) {

			// was in init
			featurePointCount.setValue(maxCount);
			cvWinSize = new CvSize(win_size, win_size);
			termCrit = new CvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS,
					20, 0.03);

			/* allocate all the buffers */
			image = cvCreateImage(cvGetSize(frame), 8, 3);
			image.origin = frame.origin;
			grey = cvCreateImage(cvGetSize(frame), 8, 1);
			prev_grey = cvCreateImage(cvGetSize(frame), 8, 1);
			pyramid = cvCreateImage(cvGetSize(frame), 8, 1);
			prev_pyramid = cvCreateImage(cvGetSize(frame), 8, 1);
			// mask = cvCreateImage( cvGetSize(frame), 8, 1 );
			mask = null; // TODO - create maskROI FROM motion template !!!
			current_features = CvPoint2D32f.createArray(maxCount);
			previous_features = CvPoint2D32f.createArray(maxCount);
			saved_features = CvPoint2D32f.createArray(maxCount);
			flags = 0;
			eig = cvCreateImage(cvGetSize(grey), 32, 1);
			temp = cvCreateImage(cvGetSize(grey), 32, 1);
			needToInitialize = false;
			add_remove_pt = 0;

		}

		cvCvtColor(frame, grey, CV_BGR2GRAY);

		if (needTrackingPoints) // warm up camera TODO CFG
		{

			count = maxCount;
			cvGoodFeaturesToTrack(grey, eig, temp, current_features, 
					featurePointCount, quality, min_distance, mask, 3, 0, 0.04);
			count = featurePointCount.getValue(); 
			needTrackingPoints = false;
			LOG.info("good features found " + featurePointCount.getValue() + " points");

		} else if (count > 0) // weird logic - but guarantees a swap after
								// features are found
		{

			cvCalcOpticalFlowPyrLK(prev_grey, grey, prev_pyramid, pyramid,
					previous_features, current_features, count, cvWinSize.byValue(), 
					3, status, error, termCrit.byValue(),
					flags);

			flags |= CV_LKFLOW_PYR_A_READY;

			for (i = k = 0; i < count; i++) {
				if (add_remove_pt == 1) {
					double dx = pt.x - current_features[i].x;
					double dy = pt.y - current_features[i].y;

					if (dx * dx + dy * dy <= 25) // what the hell?
					{
						add_remove_pt = 0;
						continue;
					}
				}

				if (status[i] == 0)
					continue;

				current_features[k++] = current_features[i];
				circle_pt.x = (int) current_features[i].x;
				circle_pt.y = (int) current_features[i].y;
				// cxcore.cvCircle( frame, circle_pt.byValue(), 1,
				// cxcore.CV_RGB(255, 0,0).byValue(), -1, 8,0);
			}
			count = k;
			if (count > 0) {
				myService.invoke("publish", (Object) current_features);
			}
		}

		if (add_remove_pt == 1 && count < maxCount) {
			current_features[count].x = pt.x;
			current_features[count].y = pt.y;
			count++;
			cv.cvFindCornerSubPix(grey, current_features[count - 1], 1, cxcore
					.cvSize(win_size, win_size).byValue(), cxcore
					.cvSize(-1, -1).byValue(), cxcore.cvTermCriteria(
					CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03).byValue());
			add_remove_pt = 0;
		}

		swap_temp = prev_grey;
		prev_grey = grey;
		grey = swap_temp;

		swap_temp = prev_pyramid;
		prev_pyramid = pyramid;
		pyramid = swap_temp;

		swap_points = previous_features;
		previous_features = current_features;
		current_features = swap_points;

		// TODO - possible instead of having a "display" but to
		// add the changes depending on config
		// display(frame);

		return frame;
	}

	CvPoint dp0 = new CvPoint();
	CvPoint dp1 = new CvPoint();

	public void display(IplImage frame) {
		int validPoints = 0;
		int x = 0;
		int y = 0;
		// calculate Z or calculate Distance
		float Z = 0;
		for (int i = 0; i < count; ++i) {

			dp0.x = (int) current_features[i].x - 1;
			dp1.x = dp0.x + 2;
			y = (int) current_features[i].y;
			if (status[i] == 1) {
				++validPoints;
				if (graphics != null) {
					if (x < 15 || x > 315 || y < 5 || y > 235) {
						graphics.setColor(Color.gray);
						--validPoints;
						status[i] = 0;
						cvDrawLine(frame, dp0.byValue(), dp1.byValue(), CV_RGB(
								150, 150, 150), 1, 8, 0);
					} else {
						graphics.setColor(Color.red);
						cvDrawLine(frame, dp0.byValue(), dp1.byValue(), CV_RGB(
								255, 0, 0), 1, 8, 0);
					}
				}
			}
		}

	}

}
