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

import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterKinectInterleave extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterKinectInterleave.class.getCanonicalName());

	transient IplImage dst = null;
	transient IplImage src = null;
	BufferedImage frameBuffer = null;
	int filter = 7;

	public OpenCVFilterKinectInterleave(VideoProcessor vp, String name, HashMap<String, IplImage> source,  String sourceKey)  {
		super(vp, name, source, sourceKey);
	}

	int x = 0;
	int y = 0;
	int clickCounter = 0;
	int frameCounter = 0;
	Graphics g = null;
	String lastHexValueOfPoint = "";

	public void samplePoint(Integer inX, Integer inY) {
		++clickCounter;
		x = inX;
		y = inY;

	}

	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {
		return image.getBufferedImage();
		/*
		 * frameBuffer = dst.getBufferedImage(); // TODO - ran out of memory
		 * here ++frameCounter; if (x != 0 && clickCounter % 2 == 0) { if (g ==
		 * null) { g = frameBuffer.getGraphics(); }
		 * 
		 * if (frameCounter % 10 == 0) { lastHexValueOfPoint =
		 * Integer.toHexString(frameBuffer.getRGB(x, y) & 0x00ffffff); }
		 * g.setColor(Color.green); frameBuffer.getRGB(x, y);
		 * g.drawString(lastHexValueOfPoint, x, y); }
		 * 
		 * return frameBuffer;
		 */
	}


	// CvScalar min = cvScalar(cfg.getFloat("hueMin"), 0.0, 0.0, 0.0);
	// CvScalar max = cvScalar(cfg.getFloat("hueMax"), 1000.0, 0.0, 0.0);

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		if (image.nChannels() == 3) // rgb
		{
			return image;
		}

		if (dst == null) {
			// dst = cvCreateImage(cvSize(image.width(), image.height()),
			// image.depth(),image.nChannels());
			// dst = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
			src = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
			dst = cvCreateImage(cvSize(image.width(), image.height()), 8, 1);
		}

		// cvConvertScale(image, src, 1, 0);
		// cvThreshold(dst, dst, 30, 255, CV_THRESH_BINARY);

		CvScalar min = cvScalar(0.0, 0.0, 0.0, 0.0);
		CvScalar max = cvScalar(10000, 0.0, 0.0, 0.0);

		cvInRangeS(image, min, max, dst);

		// cvCvtColor
		/*
		 * ByteBuffer source = image.getByteBuffer(); int z = source.capacity();
		 * ByteBuffer destination = dst.getByteBuffer(); z =
		 * destination.capacity();
		 * 
		 * int depth = 0;
		 * 
		 * Byte b = 0xE; int max = 0;
		 * 
		 * for (int i=0; i<image.width()*image.height(); i++) {
		 * 
		 * depth = source.get(i) & 0xFF; depth <<= 8; depth = source.get(i+1) &
		 * 0xFF; if (depth > max) max = depth;
		 * 
		 * if (depth > 100 && depth < 400) { destination.put(i, b); } }
		 */

		return dst;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}