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

import static com.googlecode.javacv.jna.cv.cvCvtColor;
import static com.googlecode.javacv.jna.cxcore.CV_AA;
import static com.googlecode.javacv.jna.cxcore.CV_L1;
import static com.googlecode.javacv.jna.cxcore.CV_RGB;
import static com.googlecode.javacv.jna.cxcore.IPL_DEPTH_32F;
import static com.googlecode.javacv.jna.cxcore.IPL_DEPTH_8U;
import static com.googlecode.javacv.jna.cxcore.cvAbsDiff;
import static com.googlecode.javacv.jna.cxcore.cvCircle;
import static com.googlecode.javacv.jna.cxcore.cvClearMemStorage;
import static com.googlecode.javacv.jna.cxcore.cvCreateImage;
import static com.googlecode.javacv.jna.cxcore.cvCreateMemStorage;
import static com.googlecode.javacv.jna.cxcore.cvCvtScale;
import static com.googlecode.javacv.jna.cxcore.cvLine;
import static com.googlecode.javacv.jna.cxcore.cvMerge;
import static com.googlecode.javacv.jna.cxcore.cvNorm;
import static com.googlecode.javacv.jna.cxcore.cvPoint;
import static com.googlecode.javacv.jna.cxcore.cvRect;
import static com.googlecode.javacv.jna.cxcore.cvReleaseImage;
import static com.googlecode.javacv.jna.cxcore.cvResetImageROI;
import static com.googlecode.javacv.jna.cxcore.cvSetImageROI;
import static com.googlecode.javacv.jna.cxcore.cvSize;
import static com.googlecode.javacv.jna.cxcore.cvZero;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import com.googlecode.javacv.jna.cv;
import com.googlecode.javacv.jna.cxcore.CvMemStorage;
import com.googlecode.javacv.jna.cxcore.CvPoint;
import com.googlecode.javacv.jna.cxcore.CvRect;
import com.googlecode.javacv.jna.cxcore.CvScalar;
import com.googlecode.javacv.jna.cxcore.CvSeq;
import com.googlecode.javacv.jna.cxcore.CvSize;
import com.googlecode.javacv.jna.cxcore.IplImage;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterMotionTemplate extends OpenCVFilter {

	public final static Logger LOG = Logger
			.getLogger(OpenCVFilterMotionTemplate.class.getCanonicalName());

	// various tracking parameters (in seconds)
	final double MHI_DURATION = 1;
	final double MAX_TIME_DELTA = 0.5;
	final double MIN_TIME_DELTA = 0.05;
	// number of cyclic frame buffer used for motion detection
	// (should, probably, depend on FPS)
	final int N = 4;

	// ring image buffer
	IplImage[] buf = null;
	int last = 0;

	// temporary images
	IplImage mhi = null; // MHI
	IplImage orient = null; // orientation
	IplImage mask = null; // valid orientation mask
	IplImage segmask = null; // motion segmentation map
	CvMemStorage storage = null; // temporary storage

	IplImage motion = null;

	// parameters:
	// img - input video frame
	// dst - resultant motion picture
	// args - optional parameters
	void update_mhi(IplImage img, IplImage dst, int diff_threshold) {
		double timestamp = 0.0;
		// TODO FIX double timestamp = (double)clock()/CLOCKS_PER_SEC; // get
		// current time in seconds
		CvSize size = cvSize(img.width, img.height); // get current frame size
		int i, idx1 = last, idx2;
		IplImage silh;
		CvSeq seq;
		CvRect comp_rect;
		double count;
		double angle;
		CvPoint center;
		double magnitude;
		CvScalar color;

		// allocate images at the beginning or
		// reallocate them if the frame size is changed
		if (mhi == null || mhi.width != size.width || mhi.height != size.height) {
			if (buf == null) {
				buf = new IplImage[10];// IplImage.create(arg0, arg1, arg2,
										// arg3);
			}

			for (i = 0; i < N; i++) {
				if (buf[i] != null) {
					cvReleaseImage(buf[i].pointerByReference());
				}
				buf[i] = cvCreateImage(size.byValue(), IPL_DEPTH_8U, 1);
				cvZero(buf[i]);
			}
			if (mhi != null) {
				cvReleaseImage(mhi.pointerByReference());
			}
			if (orient != null) {
				cvReleaseImage(orient.pointerByReference());
			}
			if (segmask != null) {
				cvReleaseImage(segmask.pointerByReference());
			}
			if (mask != null) {
				cvReleaseImage(mask.pointerByReference());
			}

			mhi = cvCreateImage(size.byValue(), IPL_DEPTH_32F, 1);
			cvZero(mhi); // clear MHI at the beginning
			orient = cvCreateImage(size.byValue(), IPL_DEPTH_32F, 1);
			segmask = cvCreateImage(size.byValue(), IPL_DEPTH_32F, 1);
			mask = cvCreateImage(size.byValue(), IPL_DEPTH_8U, 1);
		}

		cvCvtColor(img, buf[last], cv.CV_BGR2GRAY); // convert frame to
													// grayscale

		idx2 = (last + 1) % N; // index of (last - (N-1))th frame
		last = idx2;

		silh = buf[idx2];
		cvAbsDiff(buf[idx1], buf[idx2], silh); // get difference between frames

		cv.cvThreshold(silh, silh, diff_threshold, 1, cv.CV_THRESH_BINARY); // and
																			// threshold
																			// it
		cv.cvUpdateMotionHistory(silh, mhi, timestamp, MHI_DURATION); // update
																		// MHI

		// convert MHI to blue 8u image
		cvCvtScale(mhi, mask, 255. / MHI_DURATION, (MHI_DURATION - timestamp)
				* 255. / MHI_DURATION);
		cvZero(dst);
		cvMerge(mask, null, null, null, dst);

		// calculate motion gradient orientation and valid orientation mask
		cv.cvCalcMotionGradient(mhi, mask, orient, MAX_TIME_DELTA,
				MIN_TIME_DELTA, 3);

		if (storage == null)
			storage = cvCreateMemStorage(0);
		else
			cvClearMemStorage(storage);

		// segment motion: get sequence of motion components
		// segmask is marked motion components map. It is not used further
		seq = cv.cvSegmentMotion(mhi, segmask, storage, timestamp,
				MAX_TIME_DELTA);

		// iterate through the motion components,
		// One more iteration (i == -1) corresponds to the whole image (global
		// motion)
		for (i = -1; i < seq.total; i++) {
			comp_rect = null;
			if (i < 0) { // case of the whole image
				comp_rect = cvRect(0, 0, size.width, size.height);
				color = CV_RGB(255, 255, 255);
				magnitude = 100;
			} else { // i-th motion component
			// TODO - fix cv.CvConnectedComp connected_comp = new
			// cv.CvConnectedComp(cvGetSeqElem( seq, i ));
			// TODO -fix comp_rect = connected_comp.rect;
				if (comp_rect.width + comp_rect.height < 100) // reject very
																// small
																// components
					continue;
				color = CV_RGB(255, 0, 0);
				magnitude = 30;
			}

			// select component ROI
			cvSetImageROI(silh, comp_rect.byValue());
			cvSetImageROI(mhi, comp_rect.byValue());
			cvSetImageROI(orient, comp_rect.byValue());
			cvSetImageROI(mask, comp_rect.byValue());

			// calculate orientation
			angle = cv.cvCalcGlobalOrientation(orient, mask, mhi, timestamp,
					MHI_DURATION);
			angle = 360.0 - angle; // adjust for images with top-left origin

			count = cvNorm(silh, null, CV_L1, null); // calculate number of
														// points within
														// silhouette ROI

			cvResetImageROI(mhi);
			cvResetImageROI(orient);
			cvResetImageROI(mask);
			cvResetImageROI(silh);

			// check for the case of little motion
			if (count < comp_rect.width * comp_rect.height * 0.05)
				continue;

			// draw a clock with arrow indicating the direction
			center = cvPoint((comp_rect.x + comp_rect.width / 2),
					(comp_rect.y + comp_rect.height / 2));

			// cvCircle( dst, center, cvRound(magnitude*1.2), color, 3, CV_AA, 0
			// );
			cvCircle(dst, center.byValue(), (int) (magnitude * 1.2), color
					.byValue(), 3, CV_AA, 0);
			cvLine(dst, center.byValue(), cvPoint((int) (center.x + magnitude
					* Math.cos(angle * Math.PI / 180)),
					(int) (center.y - magnitude
							* Math.sin(angle * Math.PI / 180))), color
					.byValue(), 3, CV_AA, 0);
		}
	}

	public OpenCVFilterMotionTemplate(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {

		return null;
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

	@Override
	public IplImage process(IplImage image) {

		// what can you expect? nothing? - if data != null then error?
		if (motion == null) {
			motion = cvCreateImage(cvSize(image.width, image.height), 8, 3);
			cvZero(motion);
			motion.origin = image.origin;
		}

		update_mhi(image, motion, 30);

		return motion;
	}

}
