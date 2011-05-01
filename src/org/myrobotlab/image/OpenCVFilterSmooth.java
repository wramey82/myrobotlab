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

import static com.googlecode.javacv.jna.cv.CV_BGR2HSV;
import static com.googlecode.javacv.jna.cxcore.CV_RGB;
import static com.googlecode.javacv.jna.cxcore.cvDrawRect;
import static com.googlecode.javacv.jna.cxcore.cvScalar;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JTextField;

import com.googlecode.javacv.jna.cv;
import com.googlecode.javacv.jna.cxcore;
import com.googlecode.javacv.jna.cxcore.CvPoint;
import com.googlecode.javacv.jna.cxcore.CvScalar;
import com.googlecode.javacv.jna.cxcore.IplImage;

import org.apache.log4j.Logger;

import org.myrobotlab.service.OpenCV;

public class OpenCVFilterSmooth extends OpenCVFilter {

	public final static Logger LOG = Logger.getLogger(OpenCVFilterSmooth.class
			.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	int convert = CV_BGR2HSV; // TODO - convert to all schemes
	JFrame myFrame = null;

	public OpenCVFilterSmooth(OpenCV service, String name) {
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

		// cvDrawRect(image, startPoint.byValue(), startPoint.byValue(),
		// fillColor.byValue(), 2, 1, 0);
		cv.cvSmooth(image, image, cv.CV_GAUSSIAN, 9, 7, 7, 1);

		return image;

	}

}
