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

import static com.googlecode.javacv.jna.cv.CV_BGR2GRAY;
import static com.googlecode.javacv.jna.cv.CV_CHAIN_APPROX_SIMPLE;
import static com.googlecode.javacv.jna.cv.CV_POLY_APPROX_DP;
import static com.googlecode.javacv.jna.cv.cvApproxPoly;
import static com.googlecode.javacv.jna.cv.cvContourPerimeter;
import static com.googlecode.javacv.jna.cv.cvCvtColor;
import static com.googlecode.javacv.jna.cv.cvFindContours;
import static com.googlecode.javacv.jna.cxcore.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.jna.cxcore.cvClearMemStorage;
import static com.googlecode.javacv.jna.cxcore.cvCreateImage;
import static com.googlecode.javacv.jna.cxcore.cvCreateMemStorage;
import static com.googlecode.javacv.jna.cxcore.cvGetSize;
import static com.googlecode.javacv.jna.cxcore.cvPoint;
import static com.googlecode.javacv.jna.cxcore.cvPutText;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cv;
import com.googlecode.javacv.jna.cxcore;
import com.googlecode.javacv.jna.cv.CvContour;
import com.googlecode.javacv.jna.cxcore.CvFont;
import com.googlecode.javacv.jna.cxcore.CvMemStorage;
import com.googlecode.javacv.jna.cxcore.CvPoint;
import com.googlecode.javacv.jna.cxcore.CvRect;
import com.googlecode.javacv.jna.cxcore.CvScalar;
import com.googlecode.javacv.jna.cxcore.CvSeq;
import com.googlecode.javacv.jna.cxcore.IplImage;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterFindContours extends OpenCVFilter {

	public final static Logger LOG = Logger.getLogger(OpenCVFilterFindContours.class.getCanonicalName());

	// TODO - CONSIDER NOT Publishing OpenCV.Polygon but Publish CvSeq instead
	
	IplImage dst = null;
	BufferedImage frameBuffer = null;
	Rectangle rectangle = new Rectangle();

	CvMemStorage storage = null;
	CvSeq contours;

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

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		String desc = null;

		return desc;
	}

	@Override
	public void loadDefaultConfiguration() {
		
		cfg.set("minArea", -1);
		cfg.set("maxArea", -1);
		cfg.set("useMinArea", false);
		cfg.set("useMaxArea", false);
	}

	public ArrayList<OpenCV.Polygon> polygons = new ArrayList<OpenCV.Polygon>();

	CvSeq.PointerByReference contourPointer = new CvSeq.PointerByReference();
	int sizeofCvContour = com.sun.jna.Native.getNativeSize(CvContour.ByValue.class);

	CvPoint drawPoint0 = new CvPoint(0, 0);
	CvPoint drawPoint1 = new CvPoint(0, 0);
	boolean addPolygon = false;

	boolean minArea = true;
	boolean maxArea = true;
	
	@Override
	public IplImage process(IplImage image) {


		if (image == null) {
			LOG.error("image is null");
		}

		// TODO static global class shared between filters ????
		if (storage == null) {
			storage = cvCreateMemStorage(0);
		}

		if (gray == null) {
			gray = cvCreateImage(cvGetSize(image), 8, 1);
			polyMask = cvCreateImage(cvGetSize(image), 8, 1);
			display = cvCreateImage(cvGetSize(image), 8, 3);
		}

		display = image.clone();

		if (image.nChannels == 3) {
			cvCvtColor(image, gray, CV_BGR2GRAY);
		} else {
			gray = image.clone();
		}

		cvFindContours(gray, storage, contourPointer, sizeofCvContour, 0 ,CV_CHAIN_APPROX_SIMPLE);

		// LOG.error("getStructure");
		CvSeq contour = contourPointer.getStructure();
		int cnt = 0;

		polygons.clear();

		while (contour != null) {
			if (contour.elem_size > 0) { // TODO - limit here for "TOOOO MANY !!!!"

				// LOG.error("cvApproxPoly");

				// TODO - why not enabled?
				// float area = cvContourArea( contour.getPointer(),
				// CV_WHOLE_SEQ, 0);
				// points.readField(INPUT_IMAGE_NAME);

				// LOG.error("cvDrawContours");
				// draw the polygon

				// mark centeroid
				CvRect rect = cv.cvBoundingRect(contour, 0);
				drawPoint0.x = rect.x - 1 + rect.width / 2;
				drawPoint0.y = rect.y + rect.height / 2;

				drawPoint1.x = rect.x + 1 + rect.width / 2;
				drawPoint1.y = rect.y + rect.height / 2;

				// find all the avg color of each polygon
				// cxcore.cvZero(polyMask);
				// cvDrawContours(polyMask, points, CvScalar.WHITE,
				// CvScalar.BLACK, -1, cxcore.CV_FILLED, CV_AA);

				// publish polygons
				// CvScalar avg = cxcore.cvAvg(image, polyMask); - good idea - but not implemented
				
				// size filter
				if (cfg.getBoolean("useMinArea"))
				{
					minArea = (rect.width * rect.height > cfg.getInt("minArea"))?true:false;
				}

				if (cfg.getBoolean("useMaxArea"))
				{
					maxArea = (rect.width * rect.height < cfg.getInt("maxArea"))?true:false;
				} 
				
				
				if (minArea && maxArea)
				{
					CvSeq points = cvApproxPoly(contour.getPointer(),
							sizeofCvContour, storage, CV_POLY_APPROX_DP,
							cvContourPerimeter(contour.getPointer()) * 0.02, 1);
					
					polygons.add(new OpenCV.Polygon(rect, null, 
							(cv.cvCheckContourConvexity(points) == 1) ? true:false, 
							cvPoint(rect.x + rect.width / 2, rect.y
							+ rect.height / 2), points.total));

					cvPutText(display, " " + points.total + " "
							+ (rect.x + rect.width / 2) + ","
							+ (rect.y + rect.height / 2) + " " + rect.width + "x" + rect.height + "="
							+ (rect.width * rect.height) + " " + " "
							+ cv.cvCheckContourConvexity(points), cvPoint(
							rect.x + rect.width / 2, rect.y).byValue(), font,
							CvScalar.WHITE);
				}

				minArea = true;
				maxArea = true;

				// cvPutText(display, " " + points.total + " " + (rect.x *
				// rect.height) + "c" + (int)avg.getRed() + "," +
				// (int)avg.getGreen() + "," + (int)avg.getBlue(),
				// cvPoint(rect.x + rect.width/2,rect.y).byValue(), font,
				// CV_RGB(255, 0, 0));
				// cvPutText(display, " " + points.total + " " + rect.x + "," +
				// rect.y + " "+ (rect.x * rect.height/1000) + " " +
				// OpenCVFilterAverageColor.getColorName2(avg) + " " +
				// cv.cvCheckContourConvexity(points), cvPoint(rect.x +
				// rect.width/2,rect.y).byValue(), font, CvScalar.WHITE);
				
				drawPoint0.x = rect.x;
				drawPoint0.y = rect.y;

				drawPoint1.x = rect.x + rect.width;
				drawPoint1.y = rect.y + rect.height;

				cxcore.cvDrawRect(display, drawPoint0.byValue(), drawPoint1.byValue(), CvScalar.RED, 1, 8, 0);
				
				++cnt;

				// TODO - if publish rect
				rectangle.x = rect.x;
				rectangle.y = rect.y;
				rectangle.width = rect.width;
				rectangle.height = rect.height;
				// myService.invoke("publish", (Object)rectangle);

			}
			contour = contour.h_next;
		}

		myService.invoke("publish", (Object) polygons);

		cvPutText(display, " " + cnt, cvPoint(10, 14).byValue(), font, CvScalar.RED);
		// LOG.error("x");
		cvClearMemStorage(storage);
		return display;
	}

}
