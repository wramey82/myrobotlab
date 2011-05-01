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
import static com.googlecode.javacv.jna.cv.cvCvtColor;
import static com.googlecode.javacv.jna.cv.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_EPS;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.jna.cxcore.cvCreateImage;
import static com.googlecode.javacv.jna.cxcore.cvGetSize;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import com.googlecode.javacv.jna.cxcore.CvPoint2D32f;
import com.googlecode.javacv.jna.cxcore.CvSize;
import com.googlecode.javacv.jna.cxcore.CvTermCriteria;
import com.googlecode.javacv.jna.cxcore.IplImage;

import org.apache.log4j.Logger;

import org.myrobotlab.service.OpenCV;
import com.sun.jna.ptr.IntByReference;

public class OpenCVFilterGoodFeaturesToTrack extends OpenCVFilter {

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterGoodFeaturesToTrack.class.getCanonicalName());

	IplImage grey = null;
	IplImage eig = null;
	IplImage temp = null;
	IplImage mask = null; // ROI

	int win_size = 20;
	int maxPointCount = 30;

	CvPoint2D32f corners[] = null;
	HashMap<String, Integer> stableIterations = null;
	int totalIterations = 0;

	IntByReference cornerCount = new IntByReference(maxPointCount);

	// quality - Multiplier for the maxmin eigenvalue; specifies minimal
	// accepted quality of image corners
	double quality = 0.01;

	// minDistance - Limit, specifying minimum possible distance between
	// returned corners; Euclidian distance is used
	double minDistance = 20;

	// blockSize - Size of the averaging block, passed to underlying
	// cvCornerMinEigenVal or cvCornerHarris used by the function
	int blockSize = 3;

	// If nonzero, Harris operator (cvCornerHarris) is used instead of default
	// cvCornerMinEigenVal.
	int useHarris = 0;

	double k = 0.04;

	boolean needTrackingPoints = true;

	// init related
	CvSize cvWinSize;
	CvTermCriteria termCrit;

	// display related
	Graphics2D graphics = null;
	BufferedImage frameBuffer = null;

	public OpenCVFilterGoodFeaturesToTrack(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub
		cfg.set("filterType", OpenCVFilterGoodFeaturesToTrack.class
				.getCanonicalName());
		cfg.set("cornerCount", 30);
		cfg.save("cfg1.txt");
	}

	@Override
	public IplImage process(IplImage frame) {
		if (grey == null) {

			cvWinSize = new CvSize(win_size, win_size);
			termCrit = new CvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS,
					20, 0.03);

			/* allocate all the buffers */
			grey = cvCreateImage(cvGetSize(frame), 8, 1);
			mask = null;
			corners = CvPoint2D32f.createArray(maxPointCount);
			// stableIterations = new int[maxPointCount];
			stableIterations = new HashMap<String, Integer>();
			eig = cvCreateImage(cvGetSize(grey), 32, 1);
			temp = cvCreateImage(cvGetSize(grey), 32, 1);
		}

		cvCvtColor(frame, grey, CV_BGR2GRAY);

		if (needTrackingPoints) // warm up camera
		{
			/*
			 * cvGoodFeaturesToTrack(gray_frame, eig_image, // output
			 * temp_image, corners, &corner_count, quality_level, minDistance,
			 * NULL, eig_block_size, use_harris); cvScale(eig_image, eig_image,
			 * 100, 0.00);
			 * 
			 * cvGoodFeaturesToTrack( grey, eig, temp, corners, cornerCount,
			 * quality, minDistance, mask, 3, 0, 0.04 );
			 */
			++totalIterations;
			minDistance = 10;
			cornerCount.setValue(maxPointCount);
			cvGoodFeaturesToTrack(grey, eig, temp, corners, cornerCount,
					quality, minDistance, mask, blockSize, useHarris, k);

			// Log.error(corners[0]..size());

			// cxcore.cvScale(eig, eig, 100, 0.00);
			String d = "";
			for (int i = 0; i < corners.length; ++i) {
				// d += (int)corners[i].x + "," + (int)corners[i].y + " ";
				if (stableIterations.containsKey((int) corners[i].x + ","
						+ (int) corners[i].y)) {
					Integer it = stableIterations.get((int) corners[i].x + ","
							+ (int) corners[i].y);
					stableIterations.put((int) corners[i].x + ","
							+ (int) corners[i].y, ++it);
				} else {
					stableIterations.put((int) corners[i].x + ","
							+ (int) corners[i].y, 1);
				}

			}

			// LOG.error(d);

			// needTrackingPoints = false;
			// LOG.error("good features found " + cornerCount.getValue() +
			// " points");
		}

		return frame;
	}

	@Override
	public BufferedImage display(IplImage frame, Object[] data) {

		frameBuffer = frame.getBufferedImage(); // TODO - ran out of memory here
		graphics = frameBuffer.createGraphics();
		graphics.setColor(Color.green);

		int validPoints = 0;
		int x = 0;
		int y = 0;
		for (int i = 0; i < cornerCount.getValue(); ++i) {

			x = (int) corners[i].x;
			y = (int) corners[i].y;
			++validPoints;
			if (graphics != null) {
				Integer z = stableIterations.get(x + "," + y);
				int r = (z < 255) ? z : 255;
				// int g = (r < 254)?z:254;
				// int b = z%(255*255*255);
				int g = 0;
				int b = 0;
				graphics.setColor(new Color(r, g, b));
				graphics.drawLine(x - 1, y, x + 1, y);
				graphics.drawLine(x, y + 1, x, y - 1);
				// graphics.drawArc(x, y, z/5, z/5, 0, 360);
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

}
