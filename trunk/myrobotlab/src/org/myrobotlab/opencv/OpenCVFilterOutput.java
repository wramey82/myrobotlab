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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterOutput extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenCVFilterOutput.class.getCanonicalName());

	// display related
	Graphics2D graphics = null;
	BufferedImage frameBuffer = null;

	public OpenCVFilterOutput(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// invert ???
	}

	@Override
	public IplImage process(IplImage image) {

		return image;
	}

	@Override
	public BufferedImage display(IplImage frame, Object[] data) {
		frameBuffer = frame.getBufferedImage(); // TODO - ran out of memory here
		return frameBuffer;

	}

}
