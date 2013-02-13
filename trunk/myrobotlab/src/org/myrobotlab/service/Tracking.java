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

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.data.Point2Df;

public class Tracking extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Tracking.class.getCanonicalName());

	// References :
	// image stitching - http://www.youtube.com/watch?v=a5OK6bwke3I using surf -
	// with nice readout of fps, matches, surf pts etc

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


	// TODO - Avoidance / Navigation Service
	// ground plane
	// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
	// radio lab - map cells location cells yatta yatta
	// lkoptical disparity motion Time To Contact
	// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance
	
	transient PID xpid, ypid;
	transient OpenCV opencv;
	// transient ControlSystem control = new ControlSystem();
	transient Arduino arduino;
	transient Servo x, y;

	public static final String STATUS_IDLE = "IDLE";
	public static final String STATUS_CALIBRATING = "CALIBRATING";

	public String status = STATUS_IDLE;

	// statistics
	public int updateModulus = 20;
	public long cnt = 0;
	public long latency = 0;

	boolean tracking = true;

	float XMultiplier = (float) 0.15; // 0.14 ~ 0.16 PID
	float YMultiplier = (float) 0.15;

	public Point2Df lastPoint;
	public Point2Df targetPoint;

	public ArrayList<Point2Df> points = new ArrayList<Point2Df>();

	public Rectangle deadzone = new Rectangle();

	int currentXServoPos;
	int currentYServoPos;

	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
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
		opencv.addFilter("pyramidDown", "PyramidDown");
		opencv.addFilter("goodFeaturesToTrack", "GoodFeaturesToTrack");
		// opencv.publishFilterData("gft"); FIXME - doesnt work yet
		opencv.setDisplayFilter("goodFeaturesToTrack"); // <-- IMPORTANT this
														// sets the frame
		// which will get published - you can
		// select off of any named filter which
		// you want data from
		setStatus("letting camera warm up and balance");
		opencv.capture();

		// pause - warm up camera
		setStatus("letting camera warm up and balance");
		sleep(4000);

		// set the message route - TODO - encapsulate this in an OpenCV method?

		// TODO - these are nice methods - need to incorporate a framework which
		// supports them
		ArrayList<Point2Df> goodfeatures = GoodFeaturesToTrack();
		SerializableImage goodfeaturesImage = getOpenCVImageData();
		goodfeaturesImage.source = "goodFeaturesToTrack";

		// test node
		Node node = new Node();
		node.put("goodfeatures", goodfeatures);
		node.put("goodfeaturesImage", goodfeaturesImage);
		
		// FIXME - NOT EDGE 8% away ? AND HIGH GoodFeaturesTrak value !!!
		targetPoint = findPoint(goodfeatures, DIRECTION_CLOSEST_TO_CENTER, 0.5);

		invoke("publishFrame", goodfeaturesImage);

		int width = goodfeaturesImage.getImage().getWidth();
		int height = goodfeaturesImage.getImage().getHeight();
		setStatus(String.format("setting LK tracking point at %s - %d, %d", targetPoint.toString(), (int) (targetPoint.x * width), (int) (targetPoint.y * height)));
		// find closes to the center

		// set tracking point there
		// initialize setpoints - set output range
		initTracking();

		// set filters
		opencv.removeAllFilters();
		opencv.addFilter("pyramidDown1", "PyramidDown"); // needed ??? test
		opencv.addFilter("lkOpticalTrack", "LKOpticalTrack");
		opencv.setDisplayFilter("lkOpticalTrack");

		sleep(500); // allow the filters to fall into place

		// set point
		opencv.invokeFilterMethod("lkOpticalTrack", "samplePoint", targetPoint.x, targetPoint.y);

		SerializableImage lk = getOpenCVImageData();
		lk.source = "lkOpticalTrack";
		invoke("publishFrame", goodfeaturesImage);

		// sendBlockingWithTimeout(1000, name, method, data)
		// opencv.setCameraIndex(1);

		// subscribe
		subscribe("publish", opencv.getName(), "updateTrackingPoint", ArrayList.class);

		// don't move - calculate error & latency

		// move minimum amount (int)

		// determine difference - > build PID map or use PID

		return true;
	}

	public SerializableImage publishFrame(SerializableImage image) {
		return image;
	}

	double targetDistance = 0.0f;
	Point2Df goodFeaturePoint = null;

	// directional constants
	final static public String DIRECTION_FARTHEST_FROM_CENTER = "DIRECTION_FARTHEST_FROM_CENTER";
	final static public String DIRECTION_CLOSEST_TO_CENTER = "DIRECTION_CLOSEST_TO_CENTER";
	final static public String DIRECTION_FARTHEST_LEFT = "DIRECTION_FARTHEST_LEFT";
	final static public String DIRECTION_FARTHEST_RIGHT = "DIRECTION_FARTHEST_RIGHT";
	final static public String DIRECTION_FARTHEST_TOP = "DIRECTION_FARTHEST_TOP";
	final static public String DIRECTION_FARTHEST_BOTTOM = "DIRECTION_FARTHEST_BOTTOM";

	Point2Df findPoint(ArrayList<Point2Df> data, String direction) {
		return findPoint(data, direction, null);
	}

	/**
	 * @param data
	 *            - input data of good features
	 * @return a point on the edge of the view farthest from center
	 */
	Point2Df findPoint(ArrayList<Point2Df> data, String direction, Double minValue) {

		double distance = 0;
		int index = 0;

		if (data == null || data.size() == 0) {
			log.error("no data");
			return null;
		}

		if (minValue == null) {
			minValue = 0.0;
		}

		if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
			targetDistance = 1;
		} else {
			targetDistance = 0;
		}

		for (int i = 0; i < data.size(); ++i) {
			Point2Df point = data.get(i);

			if (DIRECTION_FARTHEST_FROM_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_LEFT.equals(direction)) {
				distance = point.x;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_RIGHT.equals(direction)) {
				distance = point.x;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_TOP.equals(direction)) {
				distance = point.y;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_BOTTOM.equals(direction)) {
				distance = point.y;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			}

		}

		Point2Df p = data.get(index);
		log.info(String.format("findPointFarthestFromCenter %s", p));
		return p;
	}

	// call back for point data
	public ArrayList<Point2Df> GoodFeaturesToTrack(ArrayList<Point2Df> data) {
		opencvData.add(data);
		return data;
	}

	BlockingQueue<ArrayList<Point2Df>> opencvData = new LinkedBlockingQueue<ArrayList<Point2Df>>();
	BlockingQueue<SerializableImage> opencvImageData = new LinkedBlockingQueue<SerializableImage>();
	boolean interrupted = false;

	private double computeX;

	private double computeY;

	private int lastXServoPos;

	private int lastYServoPos;

	// TODO - data structure which bundles the frame with the data ! - very
	// helpful
	// TODO - bundle epi-filter & config data with this method in OpenCV for
	// those who what to block on a method
	public ArrayList<Point2Df> GoodFeaturesToTrack() {
		ArrayList<Point2Df> goodfeatures = null;
		try {
			opencvData.clear();
			subscribe("publish", opencv.getName(), "GoodFeaturesToTrack", double[].class);
			while (!interrupted) {
				goodfeatures = opencvData.take();
				unsubscribe("publish", opencv.getName(), "GoodFeaturesToTrack", double[].class);
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
			subscribe("publishFrame", opencv.getName(), "setOpenCVImage", SerializableImage.class);
			while (!interrupted) {
				image = opencvImageData.take();
				unsubscribe("publishFrame", opencv.getName(), "setOpenCVImage", SerializableImage.class);
				return image;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void setStatus(String status) {
		log.info(status);
		invoke("publishStatus", status);
	}

	/**
	 * publishing point for information to be sent to the gui
	 * 
	 * @param status
	 *            - status info
	 * @return
	 */
	public String publishStatus(String status) {
		return status;
	}

	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation";
	}

	// GOOD - make it better ! Encapsulate in Hash'd data structure
	// with a setSubServiceName("arduio", "x") - for foreign structures
	public String arduinoName = "arduino";
	public String xpidName = "xpid";
	public String ypidName = "ypid";
	public String xName = "x";
	public String yName = "y";
	public String opencvName = "opencv";

	private Integer xmin;

	private Integer xmax;

	private Integer ymin;

	private Integer ymax;

	// set SubServiceNames....
	// TODO - framework?
	public void createAndStartSubServices() {
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino");
		xpid = (PID) Runtime.createAndStart(xpidName, "PID");
		ypid = (PID) Runtime.createAndStart(ypidName, "PID");
		opencv = (OpenCV) Runtime.createAndStart(opencvName, "OpenCV");
		x = (Servo) Runtime.createAndStart(xName, "Servo");
		y = (Servo) Runtime.createAndStart(yName, "Servo");
	}

	public void initTracking() {
		// init arduino :P - work on interface on how-to do all on default yet
		// allow
		// access to control specifics - e.g. work on this main - work on
		// Runtime script & work on InMoov - with the little amount of effort &
		// code
		// x.moveTo(90);
		// y.moveTo(5);

		currentXServoPos = x.getPosition();
		currentYServoPos = y.getPosition();
		lastXServoPos = currentXServoPos;
		lastYServoPos = currentYServoPos;

		// set initial Kp Kd Ki - TODO - use values derived from calibration
		xpid.setPID(10, 5, 1);
		xpid.setControllerDirection(PID.DIRECTION_DIRECT);
		xpid.setMode(PID.MODE_AUTOMATIC);
		xpid.setOutputRange(-10, 10); // <- not correct - based on maximum
		xpid.setSampleTime(30);

		ypid.setPID(10, 5, 1);
		ypid.setControllerDirection(PID.DIRECTION_DIRECT);
		ypid.setMode(PID.MODE_AUTOMATIC);
		ypid.setOutputRange(-10, 10);
		ypid.setSampleTime(30);

		// set center
		xpid.setSetpoint(xSetpoint);
		ypid.setSetpoint(ySetpoint);

		xmin = x.getPositionMin();
		xmax = x.getPositionMax();

		ymin = y.getPositionMin();
		ymax = y.getPositionMax();
		// initialize - end ----------

	}

	double xSetpoint = 0.5;
	double ySetpoint = 0.5;

	// FIXME - remove OpenCV definitions
	final public void updateTrackingPoint(ArrayList<Point2Df> data) {
		++cnt;
		if (data.size() > 0) {
			targetPoint = data.get(0);
			latency = System.currentTimeMillis() - targetPoint.timestamp; // describe
																			// this
																			// time
																			// delta
			log.debug(String.format("pt %s", targetPoint));

			xpid.setInput(targetPoint.x);
			ypid.setInput(targetPoint.y);

			// TODO - work on removing currentX/YServoPos - and use the servo's
			// directly ???
			// if I'm at my min & and the target is further min - don't compute
			// pid
			if ((currentXServoPos == xmin && xSetpoint - targetPoint.x < 0) || (currentXServoPos == xmax && xSetpoint - targetPoint.x > 0)) {
				log.warn("at x limit, and target is outside of limit");
			} else {

				if (xpid.compute()) {
					computeX = xpid.getOutput();
					currentXServoPos += (int) computeX;
					if (currentXServoPos != lastXServoPos) {
						x.moveTo(currentXServoPos);
						currentXServoPos = x.getPosition();
						lastXServoPos = currentXServoPos;
					}
					// TODO - canidate for "move(int)" ?

				} else {
					log.warn("x data under-run");
				}
			}

			if ((currentYServoPos == ymin && ySetpoint - targetPoint.y < 0) || (currentYServoPos == ymax && ySetpoint - targetPoint.y > 0)) {
				log.warn("at y limit, and target is outside of limit");
			} else {
				if (ypid.compute()) {
					computeY = ypid.getOutput();
					currentYServoPos += (int) computeY;
					if (currentYServoPos != lastYServoPos) {
						y.moveTo(currentYServoPos);
						currentYServoPos = y.getPosition();
						lastYServoPos = currentYServoPos;
					}
				} else {
					log.warn("y data under-run");
				}
			}
			
			lastPoint = targetPoint;

		}

		if (cnt % updateModulus == 0) {
			broadcastState(); // update graphics ?
			log.error(String.format("%f %f", computeX, computeY));
		}

	}

	public static void main(String[] args) {

		// ground plane
		// http://stackoverflow.com/questions/6641055/obstacle-avoidance-with-stereo-vision
		// radio lab - map cells location cells yatta yatta
		// lkoptical disparity motion Time To Contact
		// https://www.google.com/search?aq=0&oq=opencv+obst&gcx=c&sourceid=chrome&ie=UTF-8&q=opencv+obstacle+avoidance

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// appropriate way to set sub-services -
		// 1. query ? if they exist by name - if not create them ? single
		// Runtime.createAndStart
		// setNames option to set the names before creation ?
		// a framework method - createSubServices ? - or delayed until "used" ?
		// requested at use everytime ? - refactor getService

		// stitched frame map - visual memory
		// at least 2 overlap good features
		// polygon shape of keypoints (registration) - match shape - size -
		// scale - scale is proportional to position

		// coordinates - relative heading
		//

		Tracking tracker = new Tracking("tracking");
		tracker.startService();

		tracker.arduino.setBoard("atmega328");
		tracker.arduino.setSerialDevice("COM12", 57600, 8, 1, 0);
		Service.sleep(500);

		tracker.arduino.servoAttach("x", 13);
		tracker.arduino.servoAttach("y", 12);

		tracker.x.moveTo(90);
		tracker.y.moveTo(5);

		Service.sleep(500);

		tracker.x.moveTo(100);
		tracker.y.moveTo(5);

		Service.sleep(500);

		tracker.x.moveTo(90);
		tracker.y.moveTo(5);

		tracker.opencv.setCameraIndex(1);

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		tracker.calibrate();

	}

}
