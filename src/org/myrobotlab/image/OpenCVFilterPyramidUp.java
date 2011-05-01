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

import static com.googlecode.javacv.jna.cxcore.cvCreateImage;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cv;
import com.googlecode.javacv.jna.cxcore;
import com.googlecode.javacv.jna.cxcore.IplImage;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterPyramidUp extends OpenCVFilter {

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterPyramidUp.class.getCanonicalName());

	IplImage dst = null;
	BufferedImage frameBuffer = null;
	int filter = 7;

	public OpenCVFilterPyramidUp(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		frameBuffer = dst.getBufferedImage(); // TODO - ran out of memory here
		return frameBuffer;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub

	}

	@Override
	public IplImage process(IplImage image) {

		if (image == null) {
			LOG.error("image is null");
		}

		if (dst == null) {
			dst = cvCreateImage(cxcore
					.cvSize(2 * image.width, 2 * image.height), 8,
					image.nChannels);
		}

		cv.cvPyrUp(image, dst, filter);

		return dst;
	}

}
