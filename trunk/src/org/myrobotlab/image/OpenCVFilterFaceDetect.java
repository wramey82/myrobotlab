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

import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JTextField;

import org.apache.log4j.Logger;


import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;


public class OpenCVFilterFaceDetect extends OpenCVFilter {

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterFaceDetect.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	int convert = CV_BGR2HSV; // TODO - convert to all schemes
	JFrame myFrame = null;
	JTextField pixelsPerDegree = new JTextField("8.5"); // TODO - needs to pull
														// from SOHDARService
														// configuration

	public OpenCVFilterFaceDetect(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		return buffer.getBufferedImage();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.myrobotlab.image.OpenCVFilter#process(com.googlecode.javacv.cpp.opencv_core
	 * .IplImage, java.util.HashMap)
	 * 
	 * void cvErode( const CvArr* A, CvArr* C, IplConvKernel* B=0, int
	 * iterations=1 ); A Source image. C Destination image. B Structuring
	 * element used for erosion. If it is NULL, a 3×3 rectangular structuring
	 * element is used. iterations Number of times erosion is applied. The
	 * function cvErode erodes the source image using the specified structuring
	 * element B that determines the shape of a pixel neighborhood over which
	 * the minimum is taken:
	 * 
	 * C=erode(A,B): C(x,y)=min((x',y') in B(x,y))A(x',y') The function supports
	 * the in-place mode when the source and destination pointers are the same.
	 * Erosion can be applied several times iterations parameter. Erosion on a
	 * color image means independent transformation of all the channels.
	 */

	// Create memory for calculations
	CvMemStorage storage = null; // TODO - was static

	// Create a new Haar classifier
	CvHaarClassifierCascade cascade = null; // TODO - was static
	int scale = 1;
	CvPoint pt1 = new CvPoint(0, 0);
	CvPoint pt2 = new CvPoint(0, 0);
	CvPoint ch1 = new CvPoint(0, 0);
	CvPoint ch2 = new CvPoint(0, 0);
	CvPoint centeroid = new CvPoint(0, 0);
	int i;

	@Override
	public IplImage process(IplImage img) {


		if (cascade == null) {
			cascade = new CvHaarClassifierCascade(cvLoad("haarcascades/haarcascade_frontalface_alt.xml"));
			//cascade = new CvHaarClassifierCascade(cvLoad("haarcascades/haarcascade_eye.xml"));
			
			if (cascade == null) {
				LOG.error("Could not load classifier cascade");
				return img;
			}
		}

		// Allocate the memory storage TODO make this globalData
		if (storage == null) {
			storage = cvCreateMemStorage(0);
		}

		// Clear the memory storage which was used before
		cvClearMemStorage(storage);

		// Find whether the cascade is loaded, to find the faces. If yes, then:
		if (cascade != null) {

			// There can be more than one face in an image. So create a growable
			// sequence of faces.
			// Detect the objects and store them in the sequence

//			CvSeq faces = cvHaarDetectObjects(img, cascade, storage, 1.1, 2,
//					CV_HAAR_DO_CANNY_PRUNING, cvSize(40, 40));

			CvSeq faces = cvHaarDetectObjects(img, cascade, storage, 1.1, 2,
					CV_HAAR_DO_CANNY_PRUNING);
			
			// Loop the number of faces found.
			for (i = 0; i < (faces != null ? faces.total() : 0); i++) {
				// Create a new rectangle for drawing the face
				// CvRect r = (CvRect)cvGetSeqElem( faces, i );
				CvRect r = new CvRect(cvGetSeqElem(faces, i));

				// Find the dimensions of the face,and scale it if necessary
				pt1.x(r.x() * scale);
				pt2.x((r.x() + r.width()) * scale);
				pt1.y(r.y() * scale);
				pt2.y((r.y() + r.height()) * scale);

				// Draw the rectangle in the input image
				cvRectangle(img, pt1, pt2, CV_RGB(255, 0, 0), 3, 8, 0);
				centeroid.x(r.x() + r.width() * scale / 2);
				centeroid.y(r.y() + r.height() * scale / 2);
/*				ch1.x = centeroid.x + 1;
				ch1.y = centeroid.y;
				ch2.x = centeroid.x - 1;
				ch2.y = centeroid.y;*/
				cvDrawLine(img, centeroid, centeroid, CV_RGB(255, 0, 0), 3, 8, 0);
				myService.invoke("publish", centeroid);
			}
		}

		buffer = img;
		return buffer;
	}

}
