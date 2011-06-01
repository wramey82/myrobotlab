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

import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvLine;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGoodFeaturesToTrack;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.sun.jna.ptr.IntByReference;


public class OpenCVFilterGoodFeaturesToTrack extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterGoodFeaturesToTrack.class.getCanonicalName());

	IplImage grey = null;
	IplImage eig = null;
	IplImage temp = null;
	IplImage mask = null; // ROI

	int maxPointCount = 30;
	int totalIterations = 0;
	
	// quality - Multiplier for the maxmin eigenvalue; specifies minimal
	// accepted quality of image corners
	double qualityLevel = 0.05;
	// minDistance - Limit, specifying minimum possible distance between
	// returned corners; Euclidian distance is used
	double minDistance = 10.0;
	// blockSize - Size of the averaging block, passed to underlying
	// cvCornerMinEigenVal or cvCornerHarris used by the function
	int blockSize = 3;
	// If nonzero, Harris operator (cvCornerHarris) is used instead of default cvCornerMinEigenVal.
	int useHarris = 0;
	//Free parameter of Harris detector; used only if useHarris != 0
	double k = 0.0;

	CvPoint2D32f corners = null; // new way?
	HashMap<String, Integer> stableIterations = null;	
	IntByReference cornerCount = new IntByReference(maxPointCount);
    int[] corner_count = { maxPointCount };
	boolean needTrackingPoints = true;


	public OpenCVFilterGoodFeaturesToTrack(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	@Override
	public IplImage process(IplImage frame) {
		if (grey == null) {

			grey = cvCreateImage(cvGetSize(frame), 8, 1);
			eig = cvCreateImage(cvGetSize(grey), 32, 1);
			temp = cvCreateImage(cvGetSize(grey), 32, 1);

			corners = new CvPoint2D32f(maxPointCount);
			stableIterations = new HashMap<String, Integer>();
			cornerCount.setValue(maxPointCount);
		}

		cvCvtColor(frame, grey, CV_BGR2GRAY);

		if (needTrackingPoints) // warm up camera
		{
			++totalIterations;
			
	        cvGoodFeaturesToTrack(grey, eig, temp, corners,
	                corner_count, qualityLevel, minDistance, mask, blockSize, useHarris, k);
	        
			
	        /*
	        cvFindCornerSubPix(imgA, cornersA, corner_count[0],
	                cvSize(win_size, win_size), cvSize(-1, -1),
	                cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
	         */	                
			

			// cxcore.cvScale(eig, eig, 100, 0.00);
/*			
			for (int i = 0; i < corners.asBuffer(maxPointCount).hasArray()corners..length(); ++i) {
				// d += (int)corners[i].x() + "," + (int)corners[i].y + " ";
				if (stableIterations.containsKey((int) corners[i].x() + ","
						+ (int) corners[i].y())) {
					Integer it = stableIterations.get((int) corners[i].x() + ","
							+ (int) corners[i].y());
					stableIterations.put((int) corners[i].x() + ","
							+ (int) corners[i].y(), ++it);
				} else {
					stableIterations.put((int) corners[i].x() + ","
							+ (int) corners[i].y(), 1);
				}

			}
*/

			needTrackingPoints = false;
			// LOG.error("good features found " + cornerCount.getValue() +
			// " points");
		}

		return frame;
	}

	@Override
	public BufferedImage display(IplImage frame, Object[] data) {

        for (int i = 0; i < corner_count[0]; i++) {
        	corners.position(i);
        	CvPoint p0 = cvPoint(Math.round(corners.x()),
                    Math.round(corners.y()));
            cvLine(frame, p0, p0, CV_RGB(255, 0, 0), 2, 8, 0);
        }		
		
		return frame.getBufferedImage(); // TODO - ran out of memory here
	/*
		graphics = frameBuffer.createGraphics();
		graphics.setColor(Color.green);

		int validPoints = 0;
		int x = 0;
		int y = 0;
		for (int i = 0; i < cornerCount.getValue(); ++i) {

//			x = (int) corners[i].x();
//			y = (int) corners[i].y();
			++validPoints;
			if (graphics != null) {
				//Integer z = stableIterations.get(x + "," + y);
				//int r = (z < 255) ? z : 255;
				// int g = (r < 254)?z:254;
				// int b = z%(255*255*255);
				int g = 0;
				int b = 0;
				//graphics.setColor(new Color(r, g, b));
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
*/

	}

}
