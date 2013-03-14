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

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.cvAvg;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawRect;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

@Root
public class OpenCVFilterAverageColor extends OpenCVFilter {

	private static final long serialVersionUID = 1L;
	
	public final static Logger log = Logger
			.getLogger(OpenCVFilterAverageColor.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	@Element
	int convert = CV_BGR2HSV; // TODO - convert to all schemes
	JFrame myFrame = null;
	JTextField pixelsPerDegree = new JTextField("8.5"); // TODO - needs to pull
	// from SOHDARService
	// configuration

	CvPoint startPoint = new CvPoint(180, 120);
	CvScalar fillColor = cvScalar(0.0, 0.0, 0.0, 1.0);
	CvScalar lo_diff = CV_RGB(20.0, 20.0, 20.0);// cvScalar(20, 0.0, 0.5, 1.0);
	CvScalar up_diff = CV_RGB(20.0, 20.0, 20.0);

	Graphics2D graphics = null;

	CvRect roi = cvRect(100, 40, 200, 100);
	CvPoint p0 = new CvPoint(100, 40);
	CvPoint p1 = new CvPoint(200, 100);

	CvScalar avg = null;
	String colorName = "";
	String lastColorName = "";

	int roiX = 0;
	int roiY = 0;
	int roiWidth = 100;
	int roiHeight = 100;

	int red = 0;
	int green = 0;
	int blue = 0;

	int filterFrameCnt = 0;

	boolean makeGrid = true;
	boolean publishColorName = false;

	CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);

	final static String[][][] colorNameCube = {
			{ { "black", "navy", "blue" }, { "green", "teal", "bondi blue" },
					{ "lime", "persian green", "aqua" } },
			{ { "maroon", "purple", "amethyst" },
					{ "olive", "gray", "sky blue" },
					{ "brown", "aquamarine", "pale blue" } },
			{ { "red", "rose", "fushia" }, { "persimmon", "pink", "plum" },
					{ "yellow", "apricot", "white" } } };

	public static String getRGBColorName(CvScalar c) {
		String ret = "";
		int red = (int) c.red();
		int green = (int) c.green();
		int blue = (int) c.blue();

		// 63 < divisor < 85
		red = red / 64 - 1;
		green = green / 64 - 1;
		blue = blue / 64 - 1;

		if (red < 1)
			red = 0;
		if (green < 1)
			green = 0;
		if (blue < 1)
			blue = 0;

		ret = colorNameCube[red][green][blue];
		return ret;
	}

	public OpenCVFilterAverageColor(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		/*
		 * graphics = bi.createGraphics(); graphics.setColor(Color.green);
		 * graphics.drawString("R/H " + (int)avg.getRed() + " G/S " +
		 * (int)avg.getGreen() + " B/V " + (int)avg.getBlue(), 120, 120);
		 */

		makeGrid = true;
		if (makeGrid && colorGrid != null) {
			CvScalar coi = null;
			for (int x = 0; x < (image.width() / roiWidth); ++x) {
				for (int y = 0; y < (image.height() / roiHeight); ++y) {
					coi = colorGrid[x][y];
					poi.x(x * roiWidth);
					poi.y(y * roiHeight);
					cvDrawRect(image, cvPoint(x * roiWidth, y * roiHeight)
							, cvPoint(x * roiWidth + roiWidth,
							y * roiHeight + roiHeight),
							coi, 1, 1, 0);
					if (lastColorName != getColorName(coi)) {
						cvPutText(image, getColorName(coi).substring(0, 2), poi
								, font, CV_RGB(255, 255, 255));
					}
					lastColorName = getColorName(coi);

				}
			}
		}

		cvPutText(image, colorName, cvPoint(10, 14), font, CV_RGB(
				255, 0, 0));
		cvPutText(image, filterFrameCnt + " " + (int) avg.red() + " "
				+ (int) avg.green() + " " + (int) avg.blue(), cvPoint(10,
				28), font, CV_RGB(255, 0, 0));
		// cvPutText(image, red + " " + green + " "
		// + blue, cvPoint(10, 42), font, CV_RGB(255, 0, 0));
		/*
		 * CvPoint[] pts = new CvPoint[4]; pts[0] = cvPoint(0, 0); pts[1] =
		 * cvPoint(0, 10); pts[2] = cvPoint(10, 10); pts[3] = cvPoint(1, 0);
		 */
		fillColor = cvScalar(30.0, 120.0, 70.0, 1.0);
		// cvFillConvexPoly( image, pts, 4, fillColor, CV_AA, 0 );
		// cvFillPoly(image, pts, 4, contours, cvScalar(255.0, 255.0, 255.0,
		// 0.0), 8, 0)
		cvDrawRect(image, cvPoint(roiX, roiY), cvPoint(
				roiX + roiWidth, roiY + roiHeight), fillColor
				, 1, 1, 0);

		return image.getBufferedImage();
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	CvScalar[][] colorGrid = null;
	CvRect troi = null;
	CvPoint poi = null;

	@Override
	public IplImage process(IplImage image) {
		// cvCvtColor(image, buffer, CV_BGR2HSV);
		roi = null;
		roiX = 170;
		roiY = 110;
		roiWidth = 40;
		roiHeight = 40;
		makeGrid = true;
		roi = cvRect(roiX, roiY, roiWidth, roiHeight);

		++filterFrameCnt;

		if (poi == null) {
			poi = cvPoint(roiX, roiY);
			troi = cvRect(roiX, roiY, roiWidth, roiHeight);
		}

		publishColorName = true;

		if (roi == null) {
			cvResetImageROI(image);
		} else {
			if (makeGrid) {
				if (colorGrid == null) {
					colorGrid = new CvScalar[image.width() / roiWidth + 1][image.height()
							/ roiHeight + 1];
				}
				for (int x = 0; x < (image.width() / roiWidth); ++x) {
					for (int y = 0; y < (image.height() / roiHeight); ++y) {

						troi.x(x * roiWidth);
						troi.y(y * roiHeight);
						cvSetImageROI(image, troi);
						avg = cvAvg(image, null);
						cvResetImageROI(image);
						colorGrid[x][y] = avg;
						/*
						 * if (publishColorName &&
						 * colorName.compareTo(lastColorName) != 0) {
						 * myService.invoke("publish", colorName); }
						 */
						lastColorName = colorName;

					}

				}

			}

			cvSetImageROI(image, roi);
			avg = cvAvg(image, null);
			cvResetImageROI(image);
			colorName = getColorName(avg);
			if (publishColorName && colorName.compareTo(lastColorName) != 0) {
				myService.invoke("publish", colorName);
			}
			lastColorName = colorName;
		}

		return image;
	}

	static public String getColorName2(CvScalar color) {
		int red = (int) color.red();
		int green = (int) color.green();
		int blue = (int) color.blue();

		// 63 < divisor < 85
		red = red / 64 - 1;
		green = green / 64 - 1;
		blue = blue / 64 - 1;

		if (red < 1)
			red = 0;
		if (green < 1)
			green = 0;
		if (blue < 1)
			blue = 0;

		return colorNameCube[red][green][blue];

	}

	public String getColorName(CvScalar color) {
		red = (int) color.red();
		green = (int) color.green();
		blue = (int) color.blue();

		// 63 < divisor < 85
		red = red / 64 - 1;
		green = green / 64 - 1;
		blue = blue / 64 - 1;

		if (red < 1)
			red = 0;
		if (green < 1)
			green = 0;
		if (blue < 1)
			blue = 0;

		return colorNameCube[red][green][blue];

	}
}