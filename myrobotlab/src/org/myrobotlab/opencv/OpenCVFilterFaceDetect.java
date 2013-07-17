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
 import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
 import static com.googlecode.javacv.cpp.opencv_imgproc.CV_HAAR_DO_CANNY_PRUNING;
 import static com.googlecode.javacv.cpp.opencv_imgproc.cvHaarDetectObjects;
 import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
 import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
 import static com.googlecode.javacv.cpp.opencv_core.cvCreateMemStorage;
 import static com.googlecode.javacv.cpp.opencv_core.cvDrawLine;
 import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
 import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
 import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
 import static com.googlecode.javacv.cpp.opencv_core.cvSize;
 import com.googlecode.javacv.cpp.opencv_imgproc.CvHaarClassifierCascade;
 import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
 import com.googlecode.javacv.cpp.opencv_core.CvPoint;
 import com.googlecode.javacv.cpp.opencv_core.CvRect;
 import com.googlecode.javacv.cpp.opencv_core.CvSeq;
 import com.googlecode.javacv.cpp.opencv_core.IplImage;
 */

import static com.googlecode.javacv.cpp.opencv_core.cvClearMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateMemStorage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSeqElem;
import static com.googlecode.javacv.cpp.opencv_core.cvLoad;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_FIND_BIGGEST_OBJECT;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_objdetect;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

public class OpenCVFilterFaceDetect extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterFaceDetect.class.getCanonicalName());

	CvMemStorage storage = null;
	public CvHaarClassifierCascade cascade = null; // TODO - was static
	public String cascadeDir = "haarcascades";
	public String cascadeFile = "haarcascade_frontalface_alt2.xml";
	// public String cascadePath = "haarcascades/haarcascade_mcs_lefteye.xml";
	// public String cascadePath =
	// "haarcascades/haarcascade_mcs_eyepair_big.xml";

	int i;

	public OpenCVFilterFaceDetect() {
		super();
	}

	public OpenCVFilterFaceDetect(String name) {
		super(name);
	}

	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {

		if (data != null) {
			ArrayList<Rectangle> bb = data.getBoundingBoxArray();
			if (bb != null) {
				BufferedImage bi = image.getBufferedImage();
				Graphics2D g2d = bi.createGraphics();
				g2d.setColor(Color.RED);
				for (int i = 0; i < bb.size(); ++i) {
					Rectangle rect = bb.get(i);
					g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
				}
				
				return bi;
			}
		}
		
		return image.getBufferedImage();
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// Clear the memory storage which was used before
		cvClearMemStorage(storage);

		// Find whether the cascade is loaded, to find the faces. If yes, then:
		if (cascade != null) {

			// CvSeq faces = cvHaarDetectObjects(image, cascade, storage, 1.1,
			// 1, CV_HAAR_DO_ROUGH_SEARCH | CV_HAAR_FIND_BIGGEST_OBJECT);
			CvSeq faces = cvHaarDetectObjects(image, cascade, storage, 1.1, 1, CV_HAAR_DO_CANNY_PRUNING | CV_HAAR_FIND_BIGGEST_OBJECT);

			if (faces != null) {
				ArrayList<Rectangle> bb = new ArrayList<Rectangle>();
				// Loop the number of faces found.
				for (i = 0; i < faces.total(); i++) {

					CvRect r = new CvRect(cvGetSeqElem(faces, i));

					Rectangle rect = new Rectangle(r.x(), r.y(), r.width(), r.height());
					bb.add(rect);
				}

				data.put(bb);
			}
		} else {
			cascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, cascadeFile)));
		}

		return image;
	}

	@Override
	public void imageChanged(IplImage image) {
		// Allocate the memory storage TODO make this globalData
		if (storage == null) {
			storage = cvCreateMemStorage(0);
		}

		if (cascade == null) {
			// Preload the opencv_objdetect module to work around a known bug.
			Loader.load(opencv_objdetect.class);

			cascade = new CvHaarClassifierCascade(cvLoad(String.format("%s/%s", cascadeDir, cascadeFile)));
			// cascade = new
			// CvHaarClassifierCascade(cvLoad("haarcascades/haarcascade_eye.xml"));

			if (cascade == null) {
				log.error("Could not load classifier cascade");
			}
		}

	}

}
