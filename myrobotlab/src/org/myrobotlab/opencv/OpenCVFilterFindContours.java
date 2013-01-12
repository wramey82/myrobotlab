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

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvDrawRect;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_POLY_APPROX_DP;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvApproxPoly;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvBoundingRect;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCheckContourConvexity;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvContourPerimeter;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindContours;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterFindContours extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(OpenCVFilterFindContours.class.getCanonicalName());

	// TODO - CONSIDER NOT Publishing OpenCV.Polygon but Publish CvSeq instead

	IplImage dst = null;
	BufferedImage frameBuffer = null;
	Rectangle rectangle = new Rectangle();

	CvMemStorage cvStorage = null;

	IplImage gray = null;
	IplImage polyMask = null;
	// IplImage threshold = null;
	IplImage display = null;

	int thresholdValue = 0;

	CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);

	public OpenCVFilterFindContours(OpenCV service, String name) {
		super(service, name);
	}

	// TODO - display various parts based on cfg
	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		frameBuffer = display.getBufferedImage(); // TODO - ran out of memory
													// here
		return frameBuffer;
	}

	/*
	@Override
	public void loadDefaultConfiguration() {

		cfg.set("minArea", 150);
		cfg.set("maxArea", -1);
		cfg.set("useMinArea", true);
		cfg.set("useMaxArea", false);
	}
*/
	public ArrayList<OpenCV.Polygon> polygons = new ArrayList<OpenCV.Polygon>();

	CvSeq contourPointer = new CvSeq();
	// int sizeofCvContour =
	// com.sun.jna.Native.getNativeSize(CvContour.ByValue.class);

	CvPoint drawPoint0 = new CvPoint(0, 0);
	CvPoint drawPoint1 = new CvPoint(0, 0);
	boolean addPolygon = false;

	boolean minArea = true;
	boolean maxArea = true;

	@Override
	public IplImage process(IplImage image) {

		if (image == null) {
			log.error("image is null");
		}

		// TODO static global class shared between filters ????
		if (cvStorage == null) {
			cvStorage = cvCreateMemStorage(0);
		}

		if (gray == null) {
			gray = cvCreateImage(cvGetSize(image), 8, 1);
			polyMask = cvCreateImage(cvGetSize(image), 8, 1);
			display = cvCreateImage(cvGetSize(image), 8, 3);
		}

		display = image.clone();

		if (image.nChannels() == 3) {
			cvCvtColor(image, gray, CV_BGR2GRAY);
		} else {
			gray = image.clone();
		}

		cvFindContours(gray, cvStorage, contourPointer, Loader.sizeof(CvContour.class), 0, CV_CHAIN_APPROX_SIMPLE);
		// new cvFindContours(gray, cvStorage, contourPointer,
		// Loader.sizeof(CvContour.class), CV_RETR_LIST,
		// CV_CHAIN_APPROX_SIMPLE);
		// old cvFindContours(gray, cvStorage, contourPointer, sizeofCvContour,
		// 0 ,CV_CHAIN_APPROX_SIMPLE);

		// log.error("getStructure");
		CvSeq contour = contourPointer;
		int cnt = 0;

		polygons.clear();

		while (contour != null && !contour.isNull()) {
			if (contour.elem_size() > 0) { // TODO - limit here for
											// "TOOOO MANY !!!!"

				// log.error("cvApproxPoly");

				// TODO - why not enabled?
				// float area = cvContourArea( contour.getPointer(),
				// CV_WHOLE_SEQ, 0);
				// points.readField(INPUT_IMAGE_NAME);

				// log.error("cvDrawContours");
				// draw the polygon

				// mark centeroid
				CvRect rect = cvBoundingRect(contour, 0);
				drawPoint0.x(rect.x() - 1 + rect.width() / 2);
				drawPoint0.y(rect.y() + rect.height() / 2);

				drawPoint1.x(rect.x() + 1 + rect.width() / 2);
				drawPoint1.y(rect.y() + rect.height() / 2);

				// find all the avg color of each polygon
				// cxcore.cvZero(polyMask);
				// cvDrawContours(polyMask, points, CvScalar.WHITE,
				// CvScalar.BLACK, -1, cxcore.CV_FILLED, CV_AA);

				// publish polygons
				// CvScalar avg = cxcore.cvAvg(image, polyMask); - good idea -
				// but not implemented

				// size filter
				if (cfg.getBoolean("useMinArea")) {
					minArea = (rect.width() * rect.height() > cfg.getInt("minArea")) ? true : false;
				}

				if (cfg.getBoolean("useMaxArea")) {
					maxArea = (rect.width() * rect.height() < cfg.getInt("maxArea")) ? true : false;
				}

				if (minArea && maxArea) {
					CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class), cvStorage, CV_POLY_APPROX_DP, cvContourPerimeter(contour) * 0.02, 1);

					polygons.add(new OpenCV.Polygon(rect, null, (cvCheckContourConvexity(points) == 1) ? true : false, cvPoint(rect.x() + rect.width() / 2,
							rect.y() + rect.height() / 2), points.total()));

					cvPutText(display, " " + points.total() + " " + (rect.x() + rect.width() / 2) + "," + (rect.y() + rect.height() / 2) + " " + rect.width() + "x" + rect.height()
							+ "=" + (rect.width() * rect.height()) + " " + " " + cvCheckContourConvexity(points), cvPoint(rect.x() + rect.width() / 2, rect.y()), font,
							CvScalar.WHITE);
				}

				minArea = true;
				maxArea = true;

				// cvPutText(display, " " + points.total() + " " + (rect.x() *
				// rect.height()) + "c" + (int)avg.getRed() + "," +
				// (int)avg.getGreen() + "," + (int)avg.getBlue(),
				// cvPoint(rect.x() + rect.width()/2,rect.y()), font,
				// CV_RGB(255, 0, 0));
				// cvPutText(display, " " + points.total() + " " + rect.x() +
				// "," +
				// rect.y() + " "+ (rect.x() * rect.height()/1000) + " " +
				// OpenCVFilterAverageColor.getColorName2(avg) + " " +
				// cv.cvCheckContourConvexity(points), cvPoint(rect.x() +
				// rect.width()/2,rect.y()), font, CvScalar.WHITE);

				drawPoint0.x(rect.x());
				drawPoint0.y(rect.y());

				drawPoint1.x(rect.x() + rect.width());
				drawPoint1.y(rect.y() + rect.height());

				cvDrawRect(display, drawPoint0, drawPoint1, CvScalar.RED, 1, 8, 0);

				++cnt;

				// TODO - if publish rect
				rectangle.x = rect.x();
				rectangle.y = rect.y();
				rectangle.width = rect.width();
				rectangle.height = rect.height();
				// myService.invoke("publish", (Object)rectangle);

			}
			contour = contour.h_next();
		}

		myService.invoke("publish", (Object) polygons);

		cvPutText(display, " " + cnt, cvPoint(10, 14), font, CvScalar.RED);
		// log.error("x");
		cvClearMemStorage(cvStorage);
		return display;
	}

	@Override
	public void imageChanged(IplImage frame) {
		// TODO Auto-generated method stub
		
	}

}
