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
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;

import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.cvAvg;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawRect;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFloodFill;

import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterFloorFinder extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterFloorFinder.class.getCanonicalName());

	int width = 180;
	int height = 120;
	
	
	public OpenCVFilterFloorFinder(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return image.getBufferedImage();
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	@Override
	public IplImage process(IplImage image) {
		
		//for (int i = 0; i < image.width())
		cvAvg(image, null);
		
		width = 50;
		height = 40;
		// make mask - shape - triangle square - dimensions
		CvPoint pt1 = new CvPoint(image.width()/2 - width/2, image.height()-height);
		CvPoint pt2 = new CvPoint(image.width()/2 + width/2, image.height()-1);
		cvDrawRect(image, pt1, pt2, CvScalar.RED, 1, 8, 0);
		// average the mask
		//CvScalar template = cvAvg(image, null);

		// or bitwise inverse & and - find max deviation - average
		
		return image;
	}

}
