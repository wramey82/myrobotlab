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

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;

public class OpenCVFilterKinectDepthMask extends OpenCVFilter {
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterKinectDepthMask.class.getCanonicalName());

	IplImage kinectDepth = null;
	IplImage ktemp = null;
	IplImage black = null;
	IplImage itemp = null;
	IplImage itemp2 = null;
	IplImage mask = null;
	
	
	BufferedImage frameBuffer = null;

	
	// data for gui <--> filter exchange
	public boolean useHue = false; 
	
	
	public OpenCVFilterKinectDepthMask(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		return image.getBufferedImage(); // TODO - ran out of memory here
	}

	@Override
	public String getDescription() {
		return null;
	}

	int useMask = 0;
	
	@Override
	public void loadDefaultConfiguration() {
	}

	String imageKey = "kinectDepth";
	
	int mWidth = 0;
	int mHeight = 0;
	int mX = 0;
	int mY = 0;
	
	int scale = 2;
	
	
	@Override
	public IplImage process(IplImage image) {

		
		/*
		
		0 - is about 23 "
		30000 - is about 6'
		There is a blackzone in between - (sign issue?)
		
		CvScalar min = cvScalar( 30000, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(100000, 0.0, 0.0, 0.0);
		
		*/
		
		
		// TODO - clean up - remove input parameters? only use storage? 
		if (imageKey != null)
		{
			kinectDepth = getIplImage(imageKey);
		} else {
			kinectDepth = image;
		}

		// cv Pyramid Down
		
		if (mask == null || image.width() != mask.width())
		{
			mask = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
			ktemp = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 16, 1);
			black = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 1);
			cvZero(black); 								
			itemp = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 3);
			itemp2 = cvCreateImage(cvSize(kinectDepth.width()/scale, kinectDepth.height()/scale), 8, 3);
		}

		cvPyrDown(image, itemp, 7);
		cvPyrDown(kinectDepth, ktemp, 7);
		

		CvScalar min = cvScalar(0, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(30000, 0.0, 0.0, 0.0);
									
		cvInRangeS(ktemp, min, max, mask);
		
		int offsetX = 0;
		int offsetY = 0;
		mWidth = 607/scale - offsetX;
		mHeight = 460/scale - offsetY;
		mX = 25/scale + offsetX;
		mY = 20/scale + offsetY;
		
		// shifting mask 32 down and to the left 25 x 25 y 
		cvSetImageROI(mask, cvRect(mX, 0, mWidth, mHeight)); // 615-8 = to remove right hand band
		cvSetImageROI(black, cvRect(0, mY, mWidth, mHeight)); 
		cvCopy(mask, black);
		cvResetImageROI(mask);
		cvResetImageROI(black);
		cvCopy(itemp, itemp2, black);

		
		myService.invoke("publishFrame", "kinectDepth", ktemp.getBufferedImage());
		myService.invoke("publishFrame", "kinectMask", mask.getBufferedImage());

		return itemp2;

	}

}
