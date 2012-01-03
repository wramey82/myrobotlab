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
import static com.googlecode.javacv.cpp.opencv_core.cvAvg;
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

public class OpenCVFilterFloodFill extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterFloodFill.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	int convert = CV_BGR2HSV; // TODO - convert to all schemes
	JFrame myFrame = null;
	JTextField pixelsPerDegree = new JTextField("8.5"); // TODO - needs to pull
														// from SOHDARService
														// configuration

	CvPoint startPoint = new CvPoint(180, 120);
	CvScalar fillColor = cvScalar(255.0, 0.0, 0.0, 1.0);
	CvScalar lo_diff = CV_RGB(20.0, 20.0, 20.0);// cvScalar(20, 0.0, 0.5, 1.0);
	CvScalar up_diff = CV_RGB(20.0, 20.0, 20.0);

	public OpenCVFilterFloodFill(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		// CvScalar avg = cxcore.cvAvg(image, null);
		// cv.cvFloodFill(image, startPoint, fillColor,
		// lo_diff, up_diff, null, 4, null);
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
		// if (startPoint == null)
		{
			startPoint = new CvPoint(image.width() / 2, image.height() - 4);
		}

		//fillColor = cvScalar(255.0, 255.0, 255.0, 1.0);
		fillColor = cvScalar(0.0, 0.0, 0.0, 1.0);

		//lo_diff = CV_RGB(25, 1, 1);// cvScalar(20, 0.0, 0.5, 1.0);
		//up_diff = CV_RGB(125, 1, 1);
		
		lo_diff = CV_RGB(25, 1, 1);// cvScalar(20, 0.0, 0.5, 1.0);
		up_diff = CV_RGB(125, 1, 1);

		cvFloodFill(image, startPoint, fillColor,
				lo_diff, up_diff, null, 8, null);

		//fillColor = cvScalar(0.0, 255.0, 0.0, 1.0);
		//cvDrawRect(image, startPoint, startPoint, fillColor, 2, 1, 0);
		return image;

	}

}
