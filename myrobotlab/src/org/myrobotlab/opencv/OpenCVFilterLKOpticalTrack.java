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

package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_core.CV_TERMCRIT_EPS;
import static com.googlecode.javacv.cpp.opencv_core.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_core.cvTermCriteria;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindCornerSubPix;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.cpp.opencv_video.CV_LKFLOW_PYR_A_READY;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.data.Point2Df;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.CvTermCriteria;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.sun.jna.ptr.IntByReference;

public class OpenCVFilterLKOpticalTrack extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenCVFilterLKOpticalTrack.class.getCanonicalName());

	int i;
	boolean needToInitialize = true;

	// quality - Multiplier for the maxmin eigenvalue; specifies minimal
	// accepted quality of image corners
	double qualityLevel = 0.05;
	// minDistance - Limit, specifying minimum possible distance between
	// returned corners; Euclidian distance is used
	double minDistance = 5.0;
	// blockSize - Size of the averaging block, passed to underlying
	// cvCornerMinEigenVal or cvCornerHarris used by the function
	int blockSize = 3;
	// If nonzero, Harris operator (cvCornerHarris) is used instead of default
	// cvCornerMinEigenVal.
	int useHarris = 0;
	// Free parameter of Harris detector; used only if useHarris != 0
	double k = 0.0;

	public int win_size = 20;
	public int maxPointCount = 30;

	byte[] status = new byte[maxPointCount];
	float[] error = null;
	int count = 0;
	int flags = 0;

	boolean needTrackingPoints = true;

	boolean publishOpenCVObjects = false; // TODO put in Parent

	HashMap<String, Integer> stableIterations = null;
	IntByReference cornerCount = new IntByReference(maxPointCount);
	int[] corner_count = { maxPointCount };

	// non serializable JavaCV / JNI related
	transient CvPoint2D32f corners = null; // new way?

	transient CvPoint pt = new CvPoint(0, 0);
	transient CvPoint circle_pt = new CvPoint(0, 0);

	transient IntByReference featurePointCount = new IntByReference(maxPointCount);

	transient IplImage image = null;
	transient IplImage grey = null;
	transient IplImage prev_grey = null;
	transient IplImage pyramid = null;
	transient IplImage prev_pyramid = null;
	transient IplImage swap_temp = null;
	transient IplImage eig = null;
	transient IplImage temp = null;
	transient IplImage mask = null;

	transient CvPoint2D32f current_features = null;
	transient CvPoint2D32f previous_features = null;
	transient CvPoint2D32f saved_features = null;
	transient CvPoint2D32f swap_points = null;

	// init related
	transient CvSize cvWinSize;
	transient CvTermCriteria termCrit;

	// display related
	transient Graphics2D graphics = null;
	transient BufferedImage frameBuffer = null;

	public OpenCVFilterLKOpticalTrack(OpenCV service, String name) {
		super(service, name);
	}


	@Override
	public BufferedImage display(IplImage frame, Object[] data) {

		frameBuffer = frame.getBufferedImage();
		graphics = frameBuffer.createGraphics();
		graphics.setColor(Color.green);

		int validPoints = 0;
		int pixelX;
		int pixelY;
		float x;
		float y;

		for (int i = 0; i < count; ++i) {

			x = current_features.position(i).x();
			y = current_features.position(i).y();
			pixelX = (int) x;
			pixelY = (int) y;

			if (status[i] == 1) {
				++validPoints;
				if (graphics != null) {
					graphics.setColor(Color.red);
					graphics.drawLine(pixelX - 2, pixelY, pixelX + 2, pixelY);
					graphics.drawLine(pixelX, pixelY + 2, pixelX, pixelY - 2);
					graphics.drawString(String.format("%f,%f", x / frame.width(), y / frame.height()), pixelX, pixelY);
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

	public void samplePoint(Integer x, Integer y) {
		// MouseEvent me = (MouseEvent)params[0];
		if (count < maxPointCount) {
			// current_features[count++] = new
			// cxcore.CvPoint2D32f(event.getPoint().x(), event.getPoint().y());
			pt.x(x);
			pt.y(y);
			add_remove_pt = 1;
		} else {
			clearPoints();
		}

		// add_remove_pt = 0;
	}

	public void samplePoint(Float x, Float y) {
		if (width == 0 || height == 0) {
			log.error("width and height need to be set");
			return;
		}

		samplePoint((int) (x * width), (int) (y * height));

	}

	/*
	 * public void samplePoint(Point p) { if (count < maxPointCount) {
	 * pt.x(p.x); pt.y(p.y); add_remove_pt = 1; } }
	 */
	public void clearPoints() {
		count = 0;
	}

	int width = 0;
	int height = 0;

	@Override
	public IplImage process(IplImage frame) {
		if (frame == null) {
			return null;
		}

		if (needToInitialize) {

			// was in init
			featurePointCount.setValue(maxPointCount);
			cvWinSize = cvSize(win_size, win_size);
			termCrit = cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03);

			/* allocate all the buffers */
			image = cvCreateImage(cvGetSize(frame), 8, 3);
			image.origin(frame.origin());
			grey = cvCreateImage(cvGetSize(frame), 8, 1);
			prev_grey = cvCreateImage(cvGetSize(frame), 8, 1);
			pyramid = cvCreateImage(cvGetSize(frame), 8, 1);
			prev_pyramid = cvCreateImage(cvGetSize(frame), 8, 1);

			mask = null; // TODO - create maskROI FROM motion template !!!
			current_features = new CvPoint2D32f(maxPointCount);
			previous_features = new CvPoint2D32f(maxPointCount);
			saved_features = new CvPoint2D32f(maxPointCount);

			// init get good features
			corners = new CvPoint2D32f(maxPointCount);

			flags = 0;
			eig = cvCreateImage(cvGetSize(grey), 32, 1);
			temp = cvCreateImage(cvGetSize(grey), 32, 1);
			add_remove_pt = 0;
			cornerCount.setValue(maxPointCount);
			needToInitialize = false;

			width = frame.width();
			height = frame.height();

		}

		if (frame.nChannels() == 3) {
			cvCvtColor(frame, grey, CV_BGR2GRAY);
		} else {
			cvCopy(frame, grey);
		}

		if (needTrackingPoints) // warm up camera TODO CFG
		{

			count = maxPointCount;
			// cvGoodFeaturesToTrack(grey, eig, temp, current_features,
			// featurePointCount, quality, min_distance, mask, 3, 0, 0.04);
			cvGoodFeaturesToTrack(grey, eig, temp, corners, corner_count, qualityLevel, minDistance, mask, blockSize, useHarris, k);

			count = featurePointCount.getValue();
			needTrackingPoints = false;
			log.info("good features found " + featurePointCount.getValue() + " points");

		} else if (count > 0) // weird logic - but guarantees a swap after
								// features are found
		{

			cvCalcOpticalFlowPyrLK(prev_grey, grey, prev_pyramid, pyramid, previous_features, current_features, count, cvWinSize, 3, status, error, termCrit, flags);

			flags |= CV_LKFLOW_PYR_A_READY;
			int k = 0;

			for (i = k = 0; i < count; i++) {
				if (add_remove_pt == 1) {
					double dx = pt.x() - current_features.position(i).x();
					double dy = pt.y() - current_features.position(i).y();

					if (dx * dx + dy * dy <= 25) // what the hell?
					{
						add_remove_pt = 0;
						continue;
					}
				}

				if (status[i] == 0)
					continue;
				++k;
				current_features.position(k).x(current_features.position(i).x());
				current_features.position(k).y(current_features.position(i).y());
				// circle_pt.x((int) current_features[i].x());
				// circle_pt.y((int) current_features[i].y());
				// cxcore.cvCircle( frame, circle_pt, 1,
				// cxcore.CV_RGB(255, 0,0), -1, 8,0);
			}
			count = k;
			if (count > 0) {
				if (publishOpenCVObjects) {
					myService.invoke("publish", (Object) current_features);
				} else {
					CvPoint2D32f p = current_features;
					myService.invoke("publish", new Point2Df(p.x() / frame.width(), p.y() / frame.height()));
				}
			}
		}

		if (add_remove_pt == 1 && count < maxPointCount) {
			current_features.position(count).x(pt.x());
			current_features.position(count).y(pt.y());
			count++;
			cvFindCornerSubPix(grey, current_features.position(count - 1), 1, cvSize(win_size, win_size), cvSize(-1, -1),
					cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
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
	int validPoints = 0;

	@Override
	public void imageChanged(IplImage frame) {
		// TODO Auto-generated method stub
		
	}

}
