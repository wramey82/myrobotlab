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

import static org.myrobotlab.service.OpenCV.BACKGROUND;
import static org.myrobotlab.service.OpenCV.FILTER_DETECTOR;
import static org.myrobotlab.service.OpenCV.FILTER_DILATE;
import static org.myrobotlab.service.OpenCV.FILTER_ERODE;
import static org.myrobotlab.service.OpenCV.FILTER_FACE_DETECT;
import static org.myrobotlab.service.OpenCV.FILTER_FIND_CONTOURS;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FOREGROUND;
import static org.myrobotlab.service.OpenCV.PART;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.data.Rectangle;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

// TODO - attach() ???  Static name peer key list ???

public class Tracking extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Tracking.class.getCanonicalName());

	// static reqest api of Service Names & Types !
	// GOOD - TODO make it better ! Encapsulate in Hash'd data structure
	// with a setSubServiceName("arduio", "x") - for foreign structures
	public String arduinoName = "arduino";
	public String xpidName = "xpid";
	public String ypidName = "ypid";
	public String xName = "x";
	public String yName = "y";
	public String opencvName = "opencv";

	public ArrayList<OpenCVFilter> preFilters = new ArrayList<OpenCVFilter>();

	long lastTimestamp = 0;
	long waitInterval = 5000;
	int lastNumberOfObjects = 0;

	// Tracking states
	public final static String STATE_LK_TRACKING_POINT = "state lucas kanade tracking";
	public final static String STATE_IDLE = "state idle";
	public final static String STATE_NEED_TO_INITIALIZE = "state initializing";
	public static final String STATUS_CALIBRATING = "state calibrating";
	public static final String STATE_FINDING_GOOD_FEATURES = "state finding good features";
	public static final String STATE_LEARNING_BACKGROUND = "state learning background";
	public static final String STATE_SEARCH_FOREGROUND = "state search foreground";
	public static final String STATE_SEARCHING_FOREGROUND = "state searching foreground";
	public static final String STATE_WAITING_FOR_OBJECTS_TO_STABILIZE = "state waiting for objects to stabilize";
	public static final String STATE_WAITING_FOR_OBJECTS_TO_DISAPPEAR = "state waiting for objects to disappear";
	public static final String STATE_STABILIZED = "state stabilized";
	public static final String STATE_FACE_DETECT = "state face detect";

	// memory constants

	private String state = STATE_NEED_TO_INITIALIZE;

	@Element
	int xRestPos = 90;
	@Element
	int yRestPos = 90;

	// ------ PEER SERVICES BEGIN------
	// peer services are always transient (i think)
	transient public PID xpid;
	transient public PID ypid;
	transient public OpenCV eye;
	transient public Arduino arduino;
	transient public Servo x;
	transient public Servo y;
	// ------ PEER SERVICES END------

	// statistics
	public int updateModulus = 20;
	public long cnt = 0;
	public long latency = 0;

	// MRL points
	public Point2Df lastPoint = new Point2Df();

	// internal servo related
	private int currentXServoPos;
	private int currentYServoPos;
	private int lastXServoPos;
	private int lastYServoPos;

	// tracking variables
	private Integer xmin;
	private Integer xmax;
	private Integer ymin;
	private Integer ymax;
	private double computeX;
	private double computeY;

	// ----- INITIALIZATION DATA BEGIN -----
	@Element
	public double xSetpoint = 0.5;
	@Element
	public double ySetpoint = 0.5;

	// ----- INITIALIZATION DATA END -----

	public String LKOpticalTrackFilterName;
	public String FaceDetectFilterName;

	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
	}

	// DATA WHICH MUST BE SET BEFORE ATTACH METHODS !!!! - names must be set of
	// course !
	// com port
	// IMPORTANT CONCEPT - the Typed function should have ALL THE BUSINESS LOGIC
	// TO ATTACH
	// NON ANYWHERE ELSE !!
	public void startService() {
		super.startService();

		try {

			info("attaching eye");

			// cleansest simplest solution - create and start - as always
			// startService needs to be re-entrant !
			arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino");
			xpid = (PID) Runtime.createAndStart(xpidName, "PID");
			ypid = (PID) Runtime.createAndStart(ypidName, "PID");
			x = (Servo) Runtime.createAndStart(xName, "Servo");
			y = (Servo) Runtime.createAndStart(yName, "Servo");

			ypid.startService();
			xpid.startService();
			x.startService();
			y.startService();
			// put servos in rest position
			rest();

			eye = (OpenCV) Runtime.createAndStart(opencvName, "OpenCV");
			// subscribe("publishOpenCVData", eye.getName(), "setOpenCVData");
			eye.addListener("publishOpenCVData", getName(),"setOpenCVData");
			LKOpticalTrackFilterName = String.format("%s.%s", eye.getName(), FILTER_LK_OPTICAL_TRACK);
			FaceDetectFilterName = String.format("%s.%s", eye.getName(), FILTER_FACE_DETECT);
			setDefaultPreFilters();

			// values are cached for speed optimization
			xmin = x.getPositionMin();
			xmax = x.getPositionMax();
			ymin = y.getPositionMin();
			ymax = y.getPositionMax();

			eye.broadcastState();
			sleep(20); // cheesy way to keep the gui from crashing
			arduino.broadcastState();
			sleep(20);
			y.broadcastState();
			sleep(20);
			x.broadcastState();
			sleep(20);
			xpid.broadcastState();
			sleep(20);
			ypid.broadcastState();
			sleep(20);

		} catch (Exception e) {
			error(String.format("could not start tracking %s", e.getMessage()));
		}

	}

	public boolean connect(String port) {
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino");
		arduino.connect(port);
		arduino.broadcastState();
		return arduino.isConnected();
	}

	/**
	 * call back of all video data video calls this whenever a frame is
	 * processed
	 * 
	 * @param data
	 * @return
	 */
	public OpenCVData setOpenCVData(OpenCVData data) {
		// log.info("data from opencv - state {}", state);
		if (STATE_IDLE.equals(state)) {
			// we are idle - might as well do something
			// FIXME - reduce to nothing if done again
			// FIXME - NON-RENTRANT
			setForegroundBackgroundFilter();
			// TODO - begin searching for new things !!!!
		} else if (STATE_LK_TRACKING_POINT.equals(state)) {

			// extract tracking info
			data.setFilterName(LKOpticalTrackFilterName);
			Point2Df targetPoint = data.getFirstPoint();
			if (targetPoint != null) {
				updateTrackingPoint(targetPoint);
			}

		} else if (STATE_LEARNING_BACKGROUND.equals(state)) {
			waitInterval = 3000;
			waitForObjects(data);
		} else if (STATE_SEARCHING_FOREGROUND.equals(state)) {
			waitInterval = 3000;
			waitForObjects(data);
		} else if (STATE_FACE_DETECT.equals(state)) {
			// check for bounding boxes
			data.setFilterName(FaceDetectFilterName);
			ArrayList<Rectangle> bb = data.getBoundingBoxArray();

			if (bb != null && bb.size() > 0) {
				// find centroid of first bounding box
				lastPoint.x = bb.get(0).x + bb.get(0).width / 2;
				lastPoint.y = bb.get(0).y + bb.get(0).height / 2;
				updateTrackingPoint(lastPoint);
			}
			// if bounding boxes & no current tracking points
			// set set of tracking points in square - search for eyes?
			// find average point ?

		} else {
			error("recieved opencv data but unknown state");
		}
		return data;
	}

	// begin attach points -----------------

	public void attachServos(int xpin, int ypin) {
		info("attaching servos");
		
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino");
		if (!arduino.isConnected())
		{
			error("arduino must be connected before attaching servos!");
		}

		// notice only attach
		x = (Servo) Runtime.create(xName, "Servo");
		y = (Servo) Runtime.create(yName, "Servo");

		// this needs to be connected to process the comand
		arduino.servoAttach(xName, xpin);
		arduino.servoAttach(yName, ypin);

		x.broadcastState();
		y.broadcastState();

	}

	public void setServoLimits(int xmin, int xmax, int ymin, int ymax) {
		log.info(String.format("setServoLimits %d %d %d %d", xmin, xmax, ymin, ymax));
		x.setPositionMin(xmin);
		x.setPositionMax(xmax);
		y.setPositionMin(ymin);
		y.setPositionMax(ymax);
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		x.broadcastState();
		y.broadcastState();
	}

	public void setPIDDefaults() {

		setXPID(5.0, 5.0, 0.1, PID.DIRECTION_DIRECT, PID.MODE_AUTOMATIC, -10, 10, 30, 0.5);
		setYPID(5.0, 5.0, 0.1, PID.DIRECTION_DIRECT, PID.MODE_AUTOMATIC, -10, 10, 30, 0.5);
	}

	public PID setXPID(double Kp, double Ki, double Kd, int direction, int mode, int minOutput, int maxOutput, int sampleTime, double setPoint) {
		// notice - this is just create - start needs to be in startService
		xpid = (PID) Runtime.create(xpidName, "PID");
		xpid.setPID(Kp, Ki, Kd);
		xpid.setControllerDirection(direction);
		xpid.setMode(mode);
		xpid.setOutputRange(minOutput, maxOutput); // <- not correct - based on
													// maximum
		xpid.setSampleTime(sampleTime);
		// set center
		xpid.setSetpoint(setPoint);
		return xpid;
	}

	public PID setYPID(double Kp, double Ki, double Kd, int direction, int mode, int minOutput, int maxOutput, int sampleTime, double setPoint) {
		// notice - this is just create - start needs to be in startService
		ypid = (PID) Runtime.create(ypidName, "PID");
		ypid.setPID(Kp, Ki, Kd);
		ypid.setControllerDirection(direction);
		ypid.setMode(mode);
		ypid.setOutputRange(minOutput, maxOutput); // <- not correct - based on
													// mayimum
		ypid.setSampleTime(sampleTime);
		// set center
		ypid.setSetpoint(setPoint);
		return ypid;
	}

	public void rest() {
		log.info("rest");
		x.moveTo(xRestPos);
		currentXServoPos = xRestPos;
		lastXServoPos = xRestPos;

		y.moveTo(yRestPos);
		currentYServoPos = yRestPos;
		lastYServoPos = yRestPos;
	}

	// ------------------- tracking & detecting methods begin
	// ---------------------

	public void startLKTracking() {
		log.info("startLKTracking");

		eye.clearFilters();

		for (int i = 0; i < preFilters.size(); ++i) {
			eye.addFilter(preFilters.get(i));
		}

		eye.addFilter(FILTER_LK_OPTICAL_TRACK, FILTER_LK_OPTICAL_TRACK);
		eye.setDisplayFilter(FILTER_LK_OPTICAL_TRACK);

		eye.capture();
		eye.publishOpenCVData(true);

		setState(STATE_LK_TRACKING_POINT);
	}

	public void stopLKTracking() {
		eye.clearFilters();
		setState(STATE_IDLE);
	}

	public void trackPoint(float x, float y) {

		if (!STATE_LK_TRACKING_POINT.equals(state)) {
			startLKTracking();
		}

		eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
	}

	// GAAAAAAH figure out if (int , int) is SUPPORTED WOULD YA !
	public void trackPoint(int x, int y) {

		if (!STATE_LK_TRACKING_POINT.equals(state)) {
			startLKTracking();
		}
		eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
	}

	public void reset() {
		// TODO - reset pid values
		// clear filters
		eye.clearFilters();
		// reset position
		rest();
	}

	// reset better ?
	public void clearTrackingPoints() {
		eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "clearPoints");
		// reset position
		rest();
	}

	public void setForegroundBackgroundFilter() {
		eye.clearFilters();
		for (int i = 0; i < preFilters.size(); ++i) {
			eye.addFilter(preFilters.get(i));
		}
		eye.addFilter(FILTER_DETECTOR);
		eye.addFilter(FILTER_ERODE);
		eye.addFilter(FILTER_DILATE);
		eye.addFilter(FILTER_FIND_CONTOURS);

		((OpenCVFilterDetector) eye.getFilter(FILTER_DETECTOR)).learn();

		setState(STATE_LEARNING_BACKGROUND);
	}

	public void learnBackground() {

		((OpenCVFilterDetector) eye.getFilter(FILTER_DETECTOR)).learn();

		setState(STATE_LEARNING_BACKGROUND);
	}

	public void searchForeground() {

		((OpenCVFilterDetector) eye.getFilter(FILTER_DETECTOR)).search();

		setState(STATE_SEARCHING_FOREGROUND);
	}

	double sizeIndexForBackgroundForegroundFlip = 0.10;

	public void waitForObjects(OpenCVData data) {
		data.setFilterName(FILTER_FIND_CONTOURS);
		ArrayList<Rectangle> objects = data.getBoundingBoxArray();
		int numberOfNewObjects = (objects == null) ? 0 : objects.size();

		// if I'm not currently learning the background and
		// countour == background ??
		// set state to learn background
		if (!STATE_LEARNING_BACKGROUND.equals(state) && numberOfNewObjects == 1) {
			SerializableImage img = data.getImage();
			if (img == null) {
				log.error("here");
				return;
			}
			double width = img.getWidth();
			double height = img.getHeight();

			Rectangle rect = objects.get(0);

			// publish(data.getImages());

			if ((width - rect.width) / width < sizeIndexForBackgroundForegroundFlip && (height - rect.height) / height < sizeIndexForBackgroundForegroundFlip) {
				learnBackground();
				info(String.format("%s - object found was nearly whole view - foreground background flip", state));
			}

		}

		if (numberOfNewObjects != lastNumberOfObjects) {
			info(String.format("%s - unstable change from %d to %d objects - reset clock - was stable for %d ms limit is %d ms", state, lastNumberOfObjects, numberOfNewObjects,
					System.currentTimeMillis() - lastTimestamp, waitInterval));
			lastTimestamp = System.currentTimeMillis();
		}

		if (waitInterval < System.currentTimeMillis() - lastTimestamp) {
			setLocation(data);
			// number of objects have stated the same
			if (STATE_LEARNING_BACKGROUND.equals(state)) {
				if (numberOfNewObjects == 0) {
					// process background
					// data.putAttribute(BACKGROUND);
					data.putAttribute(PART, BACKGROUND);
					invoke("toProcess", data);
					// ready to search foreground
					searchForeground();
				}
			} else {

				// stable state changes with # objects
				// setState(STATE_STABILIZED);
				// log.info("number of objects {}",numberOfNewObjects);
				// TODO - SHOULD NOT PUT IN MEMORY -
				// LET OTHER THREAD DO IT
				if (numberOfNewObjects > 0) {
					data.putAttribute(PART, FOREGROUND);
					invoke("toProcess", data);
				}// else TODO - processBackground(data) <- on a regular interval
					// (addToSet) !!!!!!
			}
		}

		lastNumberOfObjects = numberOfNewObjects;

	}

	// TODO - enhance with location - not just heading
	// TODO - array of attributes expanded Object[] ... ???
	// TODO - use GEOTAG - LAT LONG ALT DIRECTION LOCATION CITY GPS TIME OFFSET
	public OpenCVData setLocation(OpenCVData data) {
		data.setX(currentXServoPos);
		data.setY(currentYServoPos);
		return data;
	}

	// ------------------- tracking & detecting methods end
	// ---------------------

	public void setIdle() {
		setState(STATE_IDLE);
	}

	public void setState(String newState) {
		state = newState;
		info(state);
	}

	// --------------- publish methods begin ----------------------------
	public OpenCVData toProcess(OpenCVData data) {
		return data;
	}

	public SerializableImage publishFrame(SerializableImage image) {
		return image;
	}

	// ubermap !!!
	public void publish(HashMap<String, SerializableImage> images) {
		for (Map.Entry<String, SerializableImage> o : images.entrySet()) {
			// Map.Entry<String,SerializableImage> pairs = o;
			log.info(o.getKey());
			publish(o.getValue());
		}
	}

	public void publish(SerializableImage image) {
		invoke("publishFrame", image);
	}

	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation";
	}

	// --------------- publish methods end ----------------------------

	// FIXME - NEED A lost tracking event !!!!
	// FIXME - this is WAY TO OPENCV specific !
	// OpenCV should have a publishTrackingPoint method !
	// This should be updateTrackingPoint(Point2Df) & perhaps Point3Df :)
	final public void updateTrackingPoint(Point2Df targetPoint) {

		++cnt;

		// describe this time delta
		latency = System.currentTimeMillis() - targetPoint.timestamp;
		log.info(String.format("pt %s", targetPoint));

		xpid.setInput(targetPoint.x);
		ypid.setInput(targetPoint.y);

		// TODO - work on removing currentX/YServoPos - and use the servo's
		// directly ???
		// if I'm at my min & and the target is further min - don't compute
		// pid
		if ((currentXServoPos <= xmin && xSetpoint - targetPoint.x < 0) || (currentXServoPos >= xmax && xSetpoint - targetPoint.x > 0)) {
			error(String.format("%d x limit out of range", currentXServoPos));
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

		if ((currentYServoPos <= ymin && ySetpoint - targetPoint.y < 0) || (currentYServoPos >= ymax && ySetpoint - targetPoint.y > 0)) {
			error(String.format("%d y limit out of range", currentYServoPos));
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

		if (cnt % updateModulus == 0) {
			broadcastState(); // update graphics ?
			info(String.format("computeX %f computeY %f", computeX, computeY));
		}

	}

	public void setRestPosition(int xpos, int ypos) {
		this.xRestPos = xpos;
		this.yRestPos = ypos;
	}

	public void setCameraIndex(int i) {
		if (eye == null) {
			eye = (OpenCV) Runtime.create(opencvName, "OpenCV");
		}
		eye.setCameraIndex(i);
	}

	public void setXMinMax(int xmin, int xmax) {
		this.xmin = xmin;
		this.xmax = xmax;
	}

	public void setYMinMax(int ymin, int ymax) {
		this.ymin = ymin;
		this.ymax = ymax;
	}

	public void faceDetect() {
		// eye.addFilter("Gray"); needed ?
		eye.clearFilters();
		
		log.info("starting faceDetect");

		for (int i = 0; i < preFilters.size(); ++i) {
			eye.addFilter(preFilters.get(i));
		}

		// TODO single string static
		eye.addFilter(FILTER_FACE_DETECT);
		eye.setDisplayFilter(FILTER_FACE_DETECT);

		eye.capture();
		eye.publishOpenCVData(true);

		// wrong state
		setState(STATE_FACE_DETECT);

	}

	public void clearFilters() {
		eye.clearFilters();
	}

	public void test() {
		for (int i = 0; i < 1000; ++i) {
			// invoke("trackPoint", 0.5, 0.5);
			// faceDetect();
			trackPoint();
			// trackPoint(0.5f,0.5f);
			setForegroundBackgroundFilter();
			learnBackground();
			searchForeground();
			clearFilters();
		}
	}

	public void trackPoint() {
		trackPoint(0.5f, 0.5f);
	}

	public void addPreFilter(OpenCVFilter filter) {
		preFilters.add(filter);
	}

	public void clearPreFilters(OpenCVFilter filter) {
		preFilters.clear();
	}

	public void setDefaultPreFilters() {
		if (preFilters.size() == 0) {
			OpenCVFilterPyramidDown pd = new OpenCVFilterPyramidDown("PyramidDown");
			preFilters.add(pd);
		}
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Tracking tracker = new Tracking("tracking");
		tracker.connect("COM12");
		tracker.attachServos(3, 6);
		tracker.setRestPosition(90, 90);
		// tracker.setServoLimits(0, 180, 0, 180);
		tracker.setPIDDefaults();
		tracker.startService();

		Python python = new Python("python");
		python.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		tracker.faceDetect();

		// tracker.startLKTracking();

		// tracker.setCameraIndex(1);
		// tracker.test();
		/*
		 * tracker.setRestPosition(90, 5); tracker.setSerialPort("COM12");
		 * tracker.setServoPins(13, 12); tracker.setCameraIndex(1);
		 */

		/*
		 * tracker.startLKTracking();
		 * 
		 * tracker.invoke("trackPoint", 100, 100); tracker.invoke("trackPoint",
		 * 0.5f, 0.5f);
		 * 
		 * 
		 * tracker.setIdle(); tracker.startVideoStream();
		 * tracker.stopVideoStream(); tracker.startVideoStream();
		 */

		// tracker.learnBackGround();
		// tracker.searchForeground();

		log.info("here");

		// tracker.getGoodFeatures();

		// tracker.trackLKPoint();

	}

}
