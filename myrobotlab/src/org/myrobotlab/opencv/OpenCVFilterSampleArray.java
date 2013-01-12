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

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.image.ColoredPoint;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterSampleArray extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenCVFilterSampleArray.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;

	ColoredPoint points[] = new ColoredPoint[1];

	public OpenCVFilterSampleArray(OpenCV service, String name) {
		super(service, name);

		points[0] = new ColoredPoint();
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return frameBuffer;
	}


	@Override
	public IplImage process(IplImage image) {

		frameBuffer = image.getBufferedImage();

		points[0].x = image.width() / 2;
		points[0].y = image.height() - 20;

		for (int i = 0; i < points.length; ++i) {
			points[i].color = frameBuffer.getRGB(points[i].x, points[i].y);
			frameBuffer.setRGB(points[0].x, points[0].y, 0x00ff22);
		}

		myService.invoke("publish", (Object) points);

		return image;

	}

	@Override
	public void imageChanged(IplImage frame) {
		// TODO Auto-generated method stub
		
	}

}
