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

import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvPyrDown;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterPyramidDown extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterPyramidDown.class.getCanonicalName());

	transient IplImage dst = null;
	BufferedImage frameBuffer = null;
	int filter = 7;

	public OpenCVFilterPyramidDown(OpenCV service, String name) {
		super(service, name);
	}

	int x = 0;
	int y = 0;
	int clickCounter = 0;
	int frameCounter = 0;
	Graphics g = null;
	String lastHexValueOfPoint = "";

	public void samplePoint(MouseEvent me) {
		++clickCounter;
		x = me.getPoint().x;
		y = me.getPoint().y;

	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		frameBuffer = dst.getBufferedImage(); // TODO - ran out of memory here
		++frameCounter;
		if (x != 0 && clickCounter % 2 == 0) {
			if (g == null) {
				g = frameBuffer.getGraphics();
			}

			if (frameCounter % 10 == 0) {
				lastHexValueOfPoint = Integer.toHexString(frameBuffer.getRGB(x,
						y) & 0x00ffffff);
			}
			g.setColor(Color.green);
			frameBuffer.getRGB(x, y);
			g.drawString(lastHexValueOfPoint, x, y);
		}

		return frameBuffer;
	}

	@Override
	public String getDescription() { // TODO - implement in GUI
		String desc = "The function PyrDown performs downsampling step of Gaussian pyramid"
				+ " decomposition. First it convolves source image with the specified filter and then"
				+ " downsamples the image by rejecting even rows and columns. So the destination image"
				+ " is four times smaller than the source imag";

		return desc;
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
			dst = cvCreateImage(cvSize(image.width() / 2, image.height() / 2), 8,
					image.nChannels());
		}

		cvPyrDown(image, dst, filter);

		return dst;
	}

}
