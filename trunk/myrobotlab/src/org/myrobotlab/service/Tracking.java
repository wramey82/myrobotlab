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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.tracking.ControlSystem;

public class Tracking extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Tracking.class.getCanonicalName());

	// TODO - Avoidance / Navigation Service
	// ground plane
	// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision

	/*
	 * TODO - Calibrate - do good features points - select closest to center -
	 * record point - move 1 x -> record delta & latency -> move to edge -> take
	 * multiple samples for non-linear control TODO - do not use CVPoint nor AWT
	 * Points !
	 */

	// TODO center vs - lock
	/*
	 * 
	 * There is several kinds of tracking - centering on a point creating a
	 * bounding box - keeping as many points as possible within the bounding box
	 * getting a set of points - filtering/eliminating stragglers & setting new
	 * points
	 * 
	 * motion establishes initial ROI - publishes ROI tracker? gets ROI sends
	 * ROI to opencv and requests for set of points within ROI set of tracking
	 * points are published iterative tracking - points or camera is in motion
	 * points which do not move together are eliminated
	 * 
	 * if a set of points reach a threshold - tracker attempts to center the
	 * centeroid of points
	 * 
	 * 
	 * parameters: DeadZone
	 * 
	 * 
	 * also - you may or may not know where the servos are to keep it
	 * generalized - assume you don't although there would be advantages if
	 * "something" knew where they were... relative v.s. absolute coordinates
	 * 
	 * BIG JUMP assess error recalculate multiplication factor small correction
	 * 
	 * CONCEPT - calibrate with background - background should have its own set
	 * of key/tracking points
	 * 
	 * Better head gimble - less jumping around
	 * 
	 * Single formula to trac because the X Y access of the servos do not
	 * necessarily align with the X Y axis of opencv - so the ration to move to
	 * the right may correspond to -3X +1Y to move along the opencv X axis
	 * 
	 * 
	 * Background set array of points & saved picture
	 * 
	 * NEEDS needs to be smoother - less jerky needs to create new points needs
	 * to make a roi from motion needs to focus at the top of the motion
	 */

	/*
	 * TODO - abstract to support OpenNI ObjectFinder finder = new
	 * ObjectFinder(); ObjectTracker tracker = new ObjectTracker();
	 */
	transient OpenCV opencv;
	transient ControlSystem control = new ControlSystem();

	public static final String STATUS_IDLE = "IDLE";
	public static final String STATUS_CALIBRATING = "CALIBRATING";

	public String status = STATUS_IDLE;

	// statistics
	public int updateModulus = 100;
	public long cnt = 0;
	public long latency = 0;

	boolean tracking = true;

	float XMultiplier = (float) 0.15; // 0.14 ~ 0.16 PID
	float YMultiplier = (float) 0.15;

	public Point2Df lastPoint;
	public Point2Df targetPoint;

	public ArrayList<Point2Df> points = new ArrayList<Point2Df>();

	public Rectangle deadzone = new Rectangle();

	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public boolean calibrate() {
		/*
		 * if (opencv == null) { log.error("must set an object finder"); return
		 * false; }
		 * 
		 * if (!control.isReady()) { log.error("control system is not ready");
		 * return false; }
		 * 
		 * control.center();
		 */
		// clear filters
		opencv.removeAllFilters();

		// get good features
		opencv.addFilter("pd", "PyramidDown");
		opencv.addFilter("gft", "GoodFeaturesToTrack");
		// opencv.publishFilterData("gft"); FIXME - doesnt work yet
		opencv.setDisplayFilter("gft"); // <-- IMPORTANT this sets the frame
										// which will get published - you can
										// select off of any named filter which
										// you want data from
		setStatus("letting camera warm up and balance");
		opencv.capture();

		// pause - warm up camera
		setStatus("letting camera warm up and balance");
		sleep(2000);

		// set the message route - TODO - encapsulate this in an OpenCV method?
		subscribe("publish", opencv.getName(), "GoodFeaturesToTrack", double[].class);
		subscribe("publishFrame", opencv.getName(), "setOpenCVImage", SerializableImage.class);

		// TODO - these are nice methods - need to incorporate a framework which
		// supports them
		double[] goodfeatures = GoodFeaturesToTrack();
		SerializableImage goodfeaturesImage = getOpenCVImageData();
		goodfeaturesImage.source = "GoodFeaturesToTrack";

		targetPoint = findPointFarthestFromCenter(goodfeatures);

		invoke("publishFrame", goodfeaturesImage);

		int width = goodfeaturesImage.getImage().getWidth();
		int height = goodfeaturesImage.getImage().getHeight();
		setStatus(String.format("setting LK tracking point at %s - %d, %d", targetPoint.toString(), (int) (targetPoint.x * width), (int) (targetPoint.y * height)));
		// find closes to the center

		// set tracking point there

		// set filters
		opencv.removeAllFilters();
		opencv.addFilter("pyramidDown1", "PyramidDown"); // needed ??? test
		opencv.addFilter("lkOpticalTrack1", "LKOpticalTrack");
		opencv.setDisplayFilter("lkOpticalTrack1");

		// set point
		opencv.invokeFilterMethod("lkOpticalTrack1", "samplePoint", targetPoint.x, targetPoint.y);

		SerializableImage lk = getOpenCVImageData();
		lk.source = "LKOpticalTrack";
		invoke("publishFrame", goodfeaturesImage);

		// sendBlockingWithTimeout(1000, name, method, data)
		// opencv.setCameraIndex(1);

		// subscribe
		subscribe("publish", opencv.getName(), "updateTrackingPoint", Point2Df.class);

		// don't move - calculate error & latency

		// move minimum amount (int)

		// determine difference - > build PID map or use PID

		return true;
	}

	public SerializableImage publishFrame(SerializableImage image) {
		return image;
	}

	float targetDistance = 0.0f;
	Point2Df goodFeaturePoint = null;

	/**
	 * @param data
	 *            - input data of good features
	 * @return a point on the edge of the view farthest from center
	 */
	Point2Df findPointFarthestFromCenter(double[] data) {
		float farthestX = 0.0f;
		float farthestY = 0.0f;

		float distance = 0.0f;
		targetDistance = 0.0f;
		int index = 0;

		for (int i = 0; i < data.length / 2; ++i) {
			distance = (float) Math.sqrt(Math.pow((0.5 - data[i]), 2) + Math.pow((0.5 - data[i + 1]), 2));
			if (distance > targetDistance) {
				targetDistance = distance;
				index = i;
			}

		}

		Point2Df p = new Point2Df((float) data[index], (float) data[index + 1]);
		log.info(String.format("findPointFarthestFromCenter %s", p));
		return p;
	}

	public double[] GoodFeaturesToTrack(double[] data) {
		opencvData.add(data);
		return data;
	}

	BlockingQueue<double[]> opencvData = new LinkedBlockingQueue<double[]>();
	BlockingQueue<SerializableImage> opencvImageData = new LinkedBlockingQueue<SerializableImage>();
	boolean interrupted = false;

	// TODO - bundle epi-filter & config data with this method in OpenCV for
	// those who what to block on a method
	public double[] GoodFeaturesToTrack() {
		double[] goodfeatures = null;
		try {
			opencvData.clear();
			while (!interrupted) {
				goodfeatures = opencvData.take();
				return goodfeatures;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public SerializableImage setOpenCVImage(SerializableImage data) {
		opencvImageData.add(data);
		return data;
	}

	public SerializableImage getOpenCVImageData() {
		SerializableImage image = null;
		try {
			opencvData.clear();
			while (!interrupted) {
				image = opencvImageData.take();
				return image;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	// FIXME - remove OpenCV definitions
	final public void updateTrackingPoint(Point2Df pt) {
		++cnt;

		if (cnt % updateModulus == 0) {
			broadcastState();
		}

		latency = System.currentTimeMillis() - pt.timestamp;

		log.debug(String.format("pt %s", pt));
		/*
		 * trackX((int)pt.x); trackY((int)pt.y);
		 */
	}

	public void setStatus(String status) {
		log.info(status);
		invoke("publishStatus", status);
	}

	public String publishStatus(String status) {
		return status;
	}

	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation - full PID not implemented yet :P";
	}

	// TODO - support interfaces
	/*
	 * public boolean attach (String serviceName, Object...data) {
	 * log.info(String.format("attaching %s", serviceName)); ServiceWrapper sw =
	 * Runtime.getServiceWrapper(serviceName); if (sw == null) {
	 * log.error(String.format("could not attach % - not found in registry",
	 * serviceName)); return false; } if
	 * (sw.getServiceType().equals("org.myrobotlab.service.OpenCV")) {
	 * subscribe("publish", serviceName, "updateTrackingPoint", Point2Df.class);
	 * return true; }
	 * 
	 * log.error(String.format("%s - don't know how to attach %s", getName(),
	 * serviceName)); return false; }
	 */

	public void attachObjectTracker(OpenCV opencv) {
		this.opencv = opencv;
	}

	public void attachControlX(Servo servo) {
		control.setServoX(servo);
	}

	public void attachControlY(Servo servo) {
		control.setServoY(servo);
	}

	public static void main(String[] args) {

		// ground plane
		// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
		// radio lab - map cells location cells yatta yatta
		// lkoptical disparity motion Time To Contact
		// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);

		/*
		 * IplImage imgA = cvLoadImage( "hand0.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		 * IplImage imgB = cvLoadImage( "hand1.jpg", CV_LOAD_IMAGE_GRAYSCALE);
		 * try { ObjectFinder of = new ObjectFinder(imgA); of.find(imgB); }
		 * catch (Exception e) { // TODO Auto-generated catch block
		 * logException(e); }
		 */

		OpenCV opencv = (OpenCV) Runtime.createAndStart("opencv", "OpenCV");
		opencv.startService();

		// opencv.startService();
		// opencv.addFilter("PyramidDown1", "PyramidDown");
		// opencv.addFilter("KinectDepthMask1", "KinectDepthMask");
		// opencv.addFilter("InRange1", "InRange");
		// opencv.setFrameGrabberType("camera");
		// opencv.grabberType = "com.googlecode.javacv.OpenCVFrameGrabber";
		// opencv.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		// opencv.grabberType = "com.googlecode.javacv.FFmpegFrameGrabber";

		// opencv.getDepth = true; // FIXME DEPRICATE ! no longer needed
		// opencv.capture();

		/*
		 * Arduino arduino = new Arduino("arduino"); arduino.startService();
		 * 
		 * Servo pan = new Servo("pan"); pan.startService();
		 * 
		 * Servo tilt = new Servo("tilt"); tilt.startService();
		 */

		// FIXME way to test with faux controlsystem

		/*
		 * Arduino mega = (Arduino)Runtime.createAndStart("mega", "Arduino");
		 * mega.setBoard(Arduino.BOARD_TYPE_ATMEGA2560);
		 * mega.setSerialDevice("COM9", 57600, 8, 1, 0);
		 * 
		 * Servo pan = (Servo)Runtime.createAndStart("pan", "Servo"); Servo tilt
		 * = (Servo)Runtime.createAndStart("tilt", "Servo");
		 * 
		 * mega.servoAttach("pan", 32); mega.servoAttach("tilt", 6);
		 */
		Tracking tracker = new Tracking("tracking");
		tracker.startService();

		tracker.attachObjectTracker(opencv);
		// tracker.attachControlX(pan);
		// tracker.attachControlY(tilt);

		// IPCamera ip = new IPCamera("ip");
		// ip.startService();
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		tracker.calibrate();
		// opencv.addFilter("pyramdDown", "PyramidDown");
		// opencv.addFilter("floodFill", "FloodFill");

		// opencv.capture();

	}

}
