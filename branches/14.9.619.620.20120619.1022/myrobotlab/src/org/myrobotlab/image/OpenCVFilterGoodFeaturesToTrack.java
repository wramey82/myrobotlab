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

	public final static Logger log = Logger.getLogger(OpenCVFilterGoodFeaturesToTrack.class.getCanonicalName());

	IplImage grey = null;
	IplImage eig = null;
	IplImage temp = null;
	IplImage mask = null; // ROI

	public int maxPointCount = 46;
	public int totalIterations = 0;
	
	// quality - Multiplier for the maxmin eigenvalue; specifies minimal
	// accepted quality of image corners
	public double qualityLevel = 0.05;
	// minDistance - Limit, specifying minimum possible distance between
	// returned corners; Euclidian distance is used
	public double minDistance = 10.0;
	// blockSize - Size of the averaging block, passed to underlying
	// cvCornerMinEigenVal or cvCornerHarris used by the function
	public int blockSize = 3;
	// If nonzero, Harris operator (cvCornerHarris) is used instead of default cvCornerMinEigenVal.
	public int useHarris = 0;
	//Free parameter of Harris detector; used only if useHarris != 0
	public double k = 0.0;

	public HashMap<String, Integer> stableIterations = null;	
	
	int lastMaxPointCount = 0;
	transient IntByReference cornerCount = new IntByReference(maxPointCount);
	transient CvPoint2D32f corners = null; // new way?
    int[] corner_count = { maxPointCount };
	
    public boolean needTrackingPoints = true;


	public OpenCVFilterGoodFeaturesToTrack(OpenCV service, String name) {
		super(service, name);
	}

	@Override // TODO - use annotations
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

			stableIterations = new HashMap<String, Integer>();
		}
		
		if (lastMaxPointCount != maxPointCount)
		{
			cornerCount.setValue(maxPointCount);		
		    corner_count = new int[]{ maxPointCount };

			lastMaxPointCount = maxPointCount;
		}
		
		corners = new CvPoint2D32f(maxPointCount); // this must be new'd every iteration

		cvCvtColor(frame, grey, CV_BGR2GRAY);

		if (needTrackingPoints) // warm up camera
		{
			++totalIterations;
			
	        cvGoodFeaturesToTrack(grey, eig, temp, corners,
	                corner_count, qualityLevel, minDistance, mask, blockSize, useHarris, k);

			//needTrackingPoints = false;
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

	}

}
