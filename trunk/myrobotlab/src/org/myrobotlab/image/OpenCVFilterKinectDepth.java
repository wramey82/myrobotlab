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

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterKinectDepth extends OpenCVFilter {

	// useful data for the kinect is 632 X 480 - 8 pixels on the right edge are not good data
	// http://groups.google.com/group/openkinect/browse_thread/thread/6539281cf451ae9e?pli=1
		
	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(OpenCVFilterKinectDepth.class.getCanonicalName());

	transient IplImage dst = null;
	transient IplImage src = null;
	transient IplImage mask = null;
	BufferedImage frameBuffer = null;
	int filter = 7;
	boolean createMask = false;

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

	public void createMask()
	{
		createMask = true;
	}
	
	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return image.getBufferedImage();
	}

	// TODO - provide "Link" to myrobotlab.org/OpenCVFilterKinectDepth (javadoc link) - NON javadoc - link to javadoc through name!
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

//	CvScalar min = cvScalar(cfg.getFloat("hueMin"), 0.0, 0.0, 0.0);
//	CvScalar max = cvScalar(cfg.getFloat("hueMax"), 1000.0, 0.0, 0.0);

	
	
	@Override
	public IplImage process(IplImage image) {
		
		IplImage kinectDepth = getIplImage("kinectDepth");
		
		// allowing publish & fork
		if (dst == null || dst.width() != image.width() || dst.nChannels() != image.nChannels()) {
			dst = cvCreateImage(cvSize(kinectDepth.width() / 2, kinectDepth.height() / 2), kinectDepth.depth(),
					kinectDepth.nChannels());
		}

		cvPyrDown(kinectDepth, dst, filter);
		myService.invoke("publishFrame", "kinectDepth", dst.getBufferedImage());
		// end fork
		
		return image;
		
		/*
		// check for depth ! 1 ch 16 depth - if not format error & return
		if (image.nChannels() != 1 || image.depth() != 16)
		{
			LOG.error("image is not a kinect depth image");
			return image;
		}
		
		if (dst == null) {
			//dst = cvCreateImage(cvSize(image.width(), image.height()), image.depth(),image.nChannels());
			//dst = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
			src = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
			dst = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
		}

		cvConvertScale(image, src, 1, 0);
		//cvThreshold(dst, dst, 30, 255, CV_THRESH_BINARY);
		
		CvScalar min = cvScalar(30000, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(150000, 0.0, 0.0, 0.0);

		cvInRangeS(image, min, max, dst);
		
		createMask = true;
		if (createMask)
		{
			if (mask == null)
			{
				mask = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);	
			}
			cvCopy(dst, mask, null);
			myService.setMask(this.getName(), mask);
			createMask = false;
		}
		//cvCvtColor
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
		
		//return dst;
	}

}
