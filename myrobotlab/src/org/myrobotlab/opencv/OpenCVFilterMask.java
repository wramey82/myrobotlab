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

import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterMask extends OpenCVFilter {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(OpenCVFilterMask.class.getCanonicalName());
	transient IplImage dst = null;
	public String maskName = "";

	// TODO - get list of masks for gui

	public OpenCVFilterMask(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return dst.getBufferedImage();
	}

	@Override
	public IplImage process(IplImage image) {

		maskName = "kd";
		if (myService.masks.containsKey(maskName)) {
			if (dst == null || dst.width() != image.width() || image.nChannels() != image.nChannels()) {
				dst = cvCreateImage(cvSize(image.width(), image.height()), image.depth(), image.nChannels());
			}
			cvCopy(image, dst, myService.masks.get(maskName));
			return dst;
		}

		return image;
	}

	@Override
	public void imageChanged(IplImage frame) {
		// TODO Auto-generated method stub
		
	}

}
