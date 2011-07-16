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
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_THRESH_BINARY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvThreshold;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterKinectDepth extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterKinectDepth.class.getCanonicalName());

	transient IplImage dst = null;
	BufferedImage frameBuffer = null;
	int filter = 7;

	public OpenCVFilterKinectDepth(OpenCV service, String name) {
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
		
		if (dst == null) {
			dst = cvCreateImage(cvSize(image.width(), image.height()), 8,
					image.nChannels());
		}

		cvThreshold(image, image, cfg.getFloat("lowThreshold"),  cfg.getFloat("highThreshold"), CV_THRESH_BINARY);

		/*
		ByteBuffer source = image.getByteBuffer(); 
		int z = source.capacity();
		ByteBuffer destination = dst.getByteBuffer(); 
		z = destination.capacity();
		
		int depth = 0;
		
		Byte b = 0xE;
		int max = 0;
		
		for (int i=0; i<image.width()*image.height(); i++) {
			
			depth = source.get(i) & 0xFF;
			depth <<= 8;
			depth = source.get(i+1) & 0xFF;
			if (depth > max) max = depth;
						    
			if (depth > 100 && depth < 400)
			{
				destination.put(i, b);
			}
		}
		*/
		
		return dst;
	}

}
