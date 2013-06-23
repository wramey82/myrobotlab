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

/*
 *  HSV changes in OpenCV -
 *  https://code.ros.org/trac/opencv/ticket/328 H is only 1-180
 *  H <- H/2 (to fit to 0 to 255)
 *  
 *  CV_HSV2BGR_FULL uses full 0 to 255 range
 */

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_RGB2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.JFrame;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterHSV extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterHSV.class.getCanonicalName());

	IplImage hsv = null;
	IplImage hue = null;
	IplImage value = null;
	IplImage saturation = null;
	IplImage mask = null;
	
	transient BufferedImage frameBuffer = null;
	
	public OpenCVFilterHSV()  {
		super();
	}
	
	public OpenCVFilterHSV(String name)  {
		super(name);
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

		frameBuffer = hsv.getBufferedImage(); // TODO - ran out of memory here
		++frameCounter;
		if (x != 0 && clickCounter % 2 == 0) {
			if (g == null) {
				g = frameBuffer.getGraphics();
			}

			if (frameCounter % 10 == 0) {
				lastHexValueOfPoint = Integer.toHexString(frameBuffer.getRGB(x, y) & 0x00ffffff);
			}
			g.setColor(Color.green);
			frameBuffer.getRGB(x, y);
			g.drawString(lastHexValueOfPoint, x, y);
		}

		return frameBuffer;
	}


	/*
	 * public void samplePoint(MouseEvent event) {
	 * 
	 * frameBuffer = hsv.getBufferedImage(); int rgb =
	 * frameBuffer.getRGB(event.getX(), event.getY()); Color c = new Color(rgb);
	 * // because of the BGR2HSV copy - it is now VSH log.error(event.getX() +
	 * "," + event.getY() + " h " + c.getBlue() + " s " + c.getGreen() + " v " +
	 * c.getRed()); }
	 */
	CvScalar hueMax = cvScalar(255.0, 0.0, 0.0, 0.0);
	CvScalar hueMin = cvScalar(255.0, 0.0, 0.0, 0.0);

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// what can you expect? nothing? - if data != null then error?

		if (hsv == null) {
			hsv = cvCreateImage(cvGetSize(image), 8, 3);
			hue = cvCreateImage(cvGetSize(image), 8, 1);
			value = cvCreateImage(cvGetSize(image), 8, 1);
			saturation = cvCreateImage(cvGetSize(image), 8, 1);
			mask = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1);
		}

		if (image == null) {
			log.error("image is null");
		}

		// CV_BGR2HSV_FULL - uses full 0-255 vs 0-180
		// CV_HSV2BGR_FULL
		cvCvtColor(image, hsv, CV_RGB2HSV);

		// cvSetImageCOI( hsv, 1);
		// cvCopy(hsv, hue );

		/*
		 * http://cgi.cse.unsw.edu.au/~cs4411/wiki/index.php?title=OpenCV_Guide#
		 * Calculating_color_histograms //Split out hue component and store in
		 * hue cxcore.cvSplit(hsv, hue, null, null, null);
		 */

		/*
		 * // good with value - cvSetImageCOI( hsv, 3); hueMin = cvScalar(254.0,
		 * 0.0, 0.0, 0.0); hueMax = cvScalar(255.0, 0.0, 0.0, 0.0);
		 */
		// hueMin = cvScalar(240.0, 240.0, 240.0, 0.0);
		// hueMax = cvScalar(255.0, 250.0, 250.0, 0.0);

		// saturation is a "wash" although it might be helpful in floor finder
		// for merging shadows back in

		// hueMin = cvScalar(175.0, 0.0, 0.0, 0.0);
		// hueMax = cvScalar(185.0, 0.0, 0.0, 0.0);

		// hueMin = cvScalar(0.0, 0.0, 0.0, 0.0);
		// hueMax = cvScalar(5.0, 0.0, 0.0, 0.0);

		// cvInRangeS(hue, hueMin, hueMax, mask);

		// return mask;

		return hsv;

	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}
