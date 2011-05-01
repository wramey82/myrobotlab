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

package org.myrobotlab.service;

import static com.googlecode.javacv.jna.cv.CV_BGR2GRAY;
import static com.googlecode.javacv.jna.cv.CV_LKFLOW_PYR_A_READY;
import static com.googlecode.javacv.jna.cv.cvCalcOpticalFlowPyrLK;
import static com.googlecode.javacv.jna.cv.cvCvtColor;
import static com.googlecode.javacv.jna.cv.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_EPS;
import static com.googlecode.javacv.jna.cxcore.CV_TERMCRIT_ITER;
import static com.googlecode.javacv.jna.cxcore.cvCreateImage;
import static com.googlecode.javacv.jna.cxcore.cvGetSize;
import static com.googlecode.javacv.jna.highgui.cvCreateCameraCapture;
import static com.googlecode.javacv.jna.highgui.cvCreateFileCapture;
import static com.googlecode.javacv.jna.highgui.cvQueryFrame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.jna.cxcore.CvPoint2D32f;
import com.googlecode.javacv.jna.cxcore.CvSize;
import com.googlecode.javacv.jna.cxcore.CvTermCriteria;
import com.googlecode.javacv.jna.cxcore.IplImage;
import com.googlecode.javacv.jna.highgui.CvCapture;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import com.sun.jna.ptr.IntByReference;

/*
 *	(SOHDAR) Sparse Optical Horizontal Disparity And Ranging Service
 *  The point of this module the same as using stereopsis to get range.  
 *   
 *  This stereopsis program:
 *     This modules requires only 1 camera.
 *     The camera needs to move a known distance in a horizontal vector.
 *     Instead of every pixel being tracked cvGoodFeaturesToTrack is run to get & track a small set of features.
 *     
 *  The basic algorithm is as follows :
 *      1. Get good features set (set of good features to track with default values)
 *      2. call cvCalcOpticalFlowPyrLK on the array from good features
 *      3. verify that image is still - by validating 0 (or near 0) horizontal disparity
 *      4. move camera horizontally (tracking with cvCalcOpticalFlowPyrLK)
 *      5. stop camera
 *      6. verify image is still - use saved_features.x - current_features.x = disparity
 *      7. disparity * constant = range 
 * webcam References:
 * http://forums.sun.com/thread.jspa?threadID=247253&forumID=28 - webCam + famegrabber
 * http://forums.sun.com/thread.jspa?threadID=5258497
 * http://forums.sun.com/thread.jspa?threadID=570463&start=30 - webCam !!! 
 * http://coding.derkeiler.com/Archive/Java/comp.lang.java.programmer/2007-03/msg03186.html
 * 
 * 
 * 		SIMPLIFIED 3 D Model
 * 
 */

public class SOHDARService extends Service {

	public final static Logger LOG = Logger.getLogger(SOHDARService.class
			.getCanonicalName());

	//ArrayList<Point3f> points = new ArrayList<Point3f>();

	Thread videoThread = null;
	int frameIndex = 0;

	// log time
	long startTimeMilliseconds = 0;

	// TODO make this a video source with a VideoSource Interface
	CvCapture capture = null;

	// display
	CanvasFrame cf = null;
	Graphics2D graphics = null;
	BufferedImage frameBuffer = null;

	IplImage frame = null;
	IplImage image = null;
	IplImage grey = null;
	IplImage prev_grey = null;
	IplImage pyramid = null;
	IplImage prev_pyramid = null;
	IplImage swap_temp = null;
	IplImage eig = null;
	IplImage temp = null;
	IplImage mask = null;

	int cameraXTravel = 5; // TODO - feedback - this should come from an
							// Accelerometer or Optical Mouse or some other
							// feedback
	int cameraYTravel = 0; // TODO - feedback - this should come from an
							// Accelerometer or Optical Mouse or some other
							// feedback

	int win_size = 20;
	int maxPointCount = 30;
	float[] horizontalDisparity = new float[maxPointCount];
	IntByReference featurePointCount = new IntByReference(maxPointCount);

	CvPoint2D32f current_features[] = null;
	CvPoint2D32f previous_features[] = null;
	CvPoint2D32f saved_features[] = null;
	CvPoint2D32f swap_points[] = null;

	float distance[] = new float[maxPointCount];

	byte[] status = new byte[maxPointCount];
	float[] error = null;
	byte status_value = 0;
	int count = 0;
	int flags = 0;

	double quality = 0.01;
	double min_distance = 10;
	boolean needTrackingPoints = true;

	int featureSetDump = 0;

	public SOHDARService(String n) {
		super(n, SOHDARService.class.getCanonicalName());
	}

	public void loadDefaultConfiguration() {

		cfg.set("cameraIndex", 0);
		cfg.set("pixelsPerDegree", 7);
		// cfg.set("movieFilename", "/garden/trunk/borat.avi");
		cfg.set("performanceTiming", false);
		cfg.set("sendImage", true);
		cfg.set("useCanvasFrame", false);
	}

	// TODO - put in Service ! - load into Service Directory !!
	final public void pause(Integer length) {
		try {
			Thread.sleep(length);
		} catch (InterruptedException e) {
			return;
		}
	}

	@Override
	public void startService() {
		VideoProcessor v1 = new VideoProcessor();
		videoThread = new Thread(v1);
		videoThread.start();
		super.startService();
	}

	@Override
	public void stopService() {
		videoThread.interrupt();
		videoThread = null;
		super.stopService();
	}

	public void logTime(String tag) // TODO - this should be a library function
									// service.util.PerformanceTimer()
	{
		if (startTimeMilliseconds == 0) {
			startTimeMilliseconds = System.currentTimeMillis();
		}
		if (cfg.getBoolean("performanceTiming")) {
			LOG.info("performance clock :"
					+ (System.currentTimeMillis() - startTimeMilliseconds
							+ " ms " + tag));
		}
	}

	public SerializableImage sendImage(IplImage img) {
		SerializableImage si = new SerializableImage(img.getBufferedImage());
		return si;
	}

	public SerializableImage sendImage(BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		return si;
	}

	class VideoProcessor implements Runnable {
		public void run() {
			if (cfg.getBoolean("useCanvasFrame")) {
				// cf = new CanvasFrame(false);
			}
			try {

				LOG.info("capture camera index " + cfg.get("cameraIndex"));

				if (cfg.get("movieFilename").length() > 0) {
					capture = cvCreateFileCapture(cfg.get("movieFilename"));
				} else {
					capture = cvCreateCameraCapture(cfg.getInt("cameraindex"));
				}

				featurePointCount.setValue(maxPointCount);
				CvSize cvWinSize = new CvSize(win_size, win_size);
				CvTermCriteria termCrit = new CvTermCriteria(CV_TERMCRIT_ITER
						| CV_TERMCRIT_EPS, 20, 0.03);

				while (isRunning) {
					++frameIndex;

					logTime("start");

					frame = cvQueryFrame(capture);

					logTime("read");

					if (frame != null) {

						if (image == null) {
							/* allocate all the buffers */
							image = cvCreateImage(cvGetSize(frame), 8, 3);
							image.origin = frame.origin;
							grey = cvCreateImage(cvGetSize(frame), 8, 1);
							prev_grey = cvCreateImage(cvGetSize(frame), 8, 1);
							pyramid = cvCreateImage(cvGetSize(frame), 8, 1);
							prev_pyramid = cvCreateImage(cvGetSize(frame), 8, 1);
							// mask = cvCreateImage( cvGetSize(frame), 8, 1 );
							mask = null;
							current_features = CvPoint2D32f
									.createArray(maxPointCount);
							previous_features = CvPoint2D32f
									.createArray(maxPointCount);
							saved_features = CvPoint2D32f
									.createArray(maxPointCount);
							flags = 0;
							eig = cvCreateImage(cvGetSize(grey), 32, 1);
							temp = cvCreateImage(cvGetSize(grey), 32, 1);
						}

						cvCvtColor(frame, grey, CV_BGR2GRAY);
						logTime("grey copy and convert");

						if (needTrackingPoints) // warm up camera
						{

							cvGoodFeaturesToTrack(grey, eig, temp,
									current_features, featurePointCount,
									quality, min_distance, mask, 3, 0, 0.04);

							/*
							 * void cvGoodFeaturesToTrack( const CvArr* image,
							 * CvArr* eigImage, - eigen results CvArr*
							 * tempImage, CvPoint2D32f* corners, int*
							 * corner_count, input & output double
							 * quality_level, range 0 - 1 (.1 - .01) double
							 * min_distance, const CvArr* mask = NULL, mask -
							 * array of points to avoid int block_size = 3, int
							 * use_harris = 0, double k = 0.4 );
							 */
							count = featurePointCount.getValue(); // TODO - YOU
																	// MUST
																	// check
																	// validity
																	// at each
																	// iteration
							needTrackingPoints = false;
							LOG.info("good features found "
									+ featurePointCount.getValue() + " points");

						} else if (count > 0) // weird logic - but guarantees a
												// swap after features are found
						{

							cvCalcOpticalFlowPyrLK(prev_grey, grey,
									prev_pyramid, pyramid, previous_features,
									current_features, count, cvWinSize
											.byValue(), 3, status, error,
									termCrit.byValue(), flags);

							flags |= CV_LKFLOW_PYR_A_READY;

							frameBuffer = frame.getBufferedImage(); // TODO -
																	// ran out
																	// of memory
																	// here
							graphics = frameBuffer.createGraphics();
							graphics.setColor(Color.red);

							int validPoints = 0;

							// calculate Z or calculate Distance
							float Z = 0;
							for (int i = 0; i < count; ++i) {

								if (status[i] == 1) {
									++validPoints;
									if (graphics != null) {
										// cross-hairs
										graphics
												.drawLine(
														(int) current_features[i].x - 1,
														(int) current_features[i].y,
														(int) current_features[i].x + 1,
														(int) current_features[i].y);
										graphics
												.drawLine(
														(int) current_features[i].x,
														(int) current_features[i].y - 1,
														(int) current_features[i].x,
														(int) current_features[i].y + 1);
										Z = cameraXTravel
												/ (float) Math
														.toRadians(horizontalDisparity[i]
																/ cfg
																		.getFloat("pixelsPerDegree"));
										// float b = (float)Math.toRadians(a);
										// distance[i] = (int)(cameraXTravel /
										// (Math.sin(b)));
										distance[i] = (int) Z;
										if (distance[i] > 300)
											distance[i] = -1.0f;
										// graphics.drawString((int)current_features[i].x
										// + "," + (int)current_features[i].y
										// +"," + (int)distance[i] + "|" +
										// saved_features[i].x + "|" +
										// current_features[i].x + " " +
										// horizontalDisparity[i],
										// (int)current_features[i].x,
										// (int)current_features[i].y);
										graphics
												.drawString(
														(int) current_features[i].x
																+ ","
																+ (int) current_features[i].y
																+ ","
																+ (int) distance[i],
														(int) current_features[i].x,
														(int) current_features[i].y);
									}
								}
							}

							if (graphics != null) {
								graphics.drawString("v " + validPoints, 0, 0);
							}

							logTime("valid points " + validPoints);
							logTime("calculate " + count);
							logTime("circles");

						}

						logTime("calculate " + count);

						swap_temp = prev_grey;
						prev_grey = grey;
						grey = swap_temp;

						swap_temp = prev_pyramid;
						prev_pyramid = pyramid;
						pyramid = swap_temp;

						swap_points = previous_features;
						previous_features = current_features;
						current_features = swap_points;

						// load them into a optical tracking

						// move the camera
						buildMesh();

						// determine the disparity of each

						// compute the distance
						if (cfg.getBoolean("useCanvasFrame")) {
							cf.showImage(frameBuffer);
						}
						logTime("show");

					}

					if (cfg.getBoolean("sendImage") && frameBuffer != null) {
						invoke("sendImage", frameBuffer);
					}

					if (graphics != null) {
						graphics.dispose();
					}

					// frameBuffer = null;
					frame = null; // done with current frame
					graphics = null;
					startTimeMilliseconds = 0;

				} // while (isRunning)

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}

	public void saveFeatures() {
		LOG.info("saveFeatures");
		for (int i = 0; i < count; ++i) {
			// TODO - if y > threshold - invalidate disparity
			saved_features[i].x = current_features[i].x;
			saved_features[i].y = current_features[i].y;
		}

	}

	public void dumpFeatureData() {
		LOG.info("dumpFeatureData");

		float xFactor = .001f;
		float yFactor = -.001f;
		float zFactor = .001f;

		StringBuffer data = new StringBuffer();
		for (int i = 0; i < count; ++i) {
			horizontalDisparity[i] = current_features[i].x
					- saved_features[i].x;
			data.append((xFactor * current_features[i].x) + ","
					+ (yFactor * current_features[i].y) + ","
					+ (zFactor * distance[i]) + "\n");
		}

		++featureSetDump;
		saveBufferedImage(frameBuffer, "feature." + featureSetDump + ".jpg");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream("feature." + featureSetDump + ".txt");
			// OutputStreamWriter out = new OutputStreamWriter(fos);
			// out.write(data.toString());
			for (int i = 0; i < data.length(); ++i)
				fos.write(data.charAt(i));
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static void saveBufferedImage(BufferedImage newImg, String filename) {
		saveBufferedImage(newImg, filename, null);
	}

	static void saveBufferedImage(BufferedImage newImg, String filename,
			String format) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(newImg, "jpg", baos);
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(baos.toByteArray());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void compareFeatures() {
		LOG.info("compareFeatures");
		for (int i = 0; i < count; ++i) {
			horizontalDisparity[i] = current_features[i].x
					- saved_features[i].x;
			// LOG.info(horizontalDisparity[i]);
		}

	}

	public void getFeatures() {
		LOG.info("getFeatures");
		for (int i = 0; i < maxPointCount; ++i) {
			horizontalDisparity[i] = 0;
			// LOG.info(horizontalDisparity[i]);
		}
		needTrackingPoints = true;
	}

	public CvPoint2D32f[] goodFeaturesToTrack(IplImage grey) {
		/* automatic initialization */
		if (eig == null) {
			eig = cvCreateImage(cvGetSize(grey), 32, 1);
			temp = cvCreateImage(cvGetSize(grey), 32, 1);
		}

		cvGoodFeaturesToTrack(grey, eig, temp, current_features,
				featurePointCount, quality, min_distance, null, 3, 0, 0.04);
		LOG.error(featurePointCount.getValue());

		/*
		 * for (int i = 0; i < goodFeaturePoints.length; ++i) {
		 * LOG.error(goodFeaturePoints[i]); }
		 */
		needTrackingPoints = false;
		return current_features;

	}

	public void init3D() {

		// mainDisplay.add("Center", canvas3D);

	}

	public void buildMesh() {
		// frameBuffer
	}

	@Override
	public String getToolTip() {
		return "<html>experimental service generating depth maps with horizontal disparity from track points</html>";
	}

}
