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
import static org.myrobotlab.service.OpenCV.FILTER_BACKGROUND_SUBTRACTOR_MOG2;
import static org.myrobotlab.service.OpenCV.FILTER_DILATE;
import static org.myrobotlab.service.OpenCV.FILTER_ERODE;
import static org.myrobotlab.service.OpenCV.FILTER_FIND_CONTOURS;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FILTER_PYRAMID_DOWN;
import static org.myrobotlab.service.OpenCV.FOREGROUND;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterBackgroundSubtractorMOG2;
import org.myrobotlab.service.data.Point2Df;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

// TODO - attach() ???  Static name peer key list ???

public class Tracking extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Tracking.class.getCanonicalName());
	
	// static Service.requestName();
	// GOOD - TODO make it better ! Encapsulate in Hash'd data structure
	// with a setSubServiceName("arduio", "x") - for foreign structures
	public String arduinoName = "arduino";
	public String xpidName = "xpid";
	public String ypidName = "ypid";
	public String xName = "x";
	public String yName = "y";
	public String opencvName = "opencv";
	//public String processorName = "processor";
	
	@Element
	public int xServoPin = 13;
	@Element
	public int yServoPin = 12;
	@Element
	String serialPort = "COM12";
	
	long lastTimestamp = 0;
	long waitInterval = 5000;
	int lastNumberOfObjects = 0;
	
	// TODO enum - allows meta data? like description of state ???
	public static final String STATE_INITIALIZING_INPUT = "initializing input";
	public static final String STATE_INITIALIZING_CONTROL = "initializing control";
	public static final String STATE_INITIALIZING_TRACKING = "initializing tracking";
	
	public final static String STATE_LK_TRACKING_POINT = "lucas kanade tracking";
	public final static String STATE_IDLE = "idle";
	public final static String STATE_NEED_TO_INITIALIZE = "initializing";
	public static final String STATUS_CALIBRATING = "calibrating";
	public static final String STATE_FINDING_GOOD_FEATURES = "finding good features";
	public static final String STATE_LEARNING_BACKGROUND = "learning background";
	public static final String STATE_SEARCH_FOREGROUND = "search foreground";
	public static final String STATE_SEARCHING_FOREGROUND = "searching foreground";
	public static final String STATE_WAITING_FOR_OBJECTS_TO_STABILIZE = "waiting for objects to stabilize";
	public static final String STATE_WAITING_FOR_OBJECTS_TO_DISAPPEAR =  "waiting for objects to disappear";
	public static final String STATE_STABILIZED = "stabilized";

	// memory constants
	public final static String LOCATION_X = "LOCATION_X";
	public final static String LOCATION_Y = "LOCATION_Y";

	private String state = STATE_NEED_TO_INITIALIZE;
	
	@Element
	int xRestPos;
	@Element
	int yRestPos;
	
	// FIXME - HOW TO HANDLE !?!?!?
	// peer services
	transient PID xpid, ypid;
	transient OpenCV eye;
	transient Arduino arduino;
	transient Servo x, y;

	// statistics
	public int updateModulus = 20;
	public long cnt = 0;
	public long latency = 0;

	// MRL points
	public Point2Df lastPoint;
	
	// "center" set points
	@Element
	double xSetpoint = 0.5;
	@Element
	double ySetpoint = 0.5;

	// servo related
	int currentXServoPos;
	int currentYServoPos;
	private int lastXServoPos;
	private int lastYServoPos;
	
	// tracking variables
	private Integer xmin;
	private Integer xmax;
	private Integer ymin;
	private Integer ymax;
	private double computeX;
	private double computeY;
	
	OpenCVData goodFeatures;

	
	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
	}
	
	// set SubServiceNames....
	// TODO - framework? requestService(name, type)
	// TODO renameService()  globalRename(name, newName)
	public void createAndStartSubServices() {
		xpid = (PID) Runtime.createAndStart(xpidName, "PID");
		ypid = (PID) Runtime.createAndStart(ypidName, "PID");
		eye = (OpenCV) Runtime.createAndStart(opencvName, "OpenCV");
		x = (Servo) Runtime.createAndStart(xName, "Servo");
		y = (Servo) Runtime.createAndStart(yName, "Servo");
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino");
	}
	
	// the big kahuna input feed
	public OpenCVData setOpenCVData (OpenCVData data)
	{
		//log.info("data from opencv - state {}", state);
		if (STATE_IDLE.equals(state))
		{
			// we are idle - might as well do something
			// FIXME - reduce to nothing if done again
			// FIXME - NON-RENTRANT
			setForegroundBackgroundFilter();
			// TODO - begin searching for new things !!!!
		} else if (STATE_LK_TRACKING_POINT.equals(state))
		{
			// check non-blocking queue for better tracking point !!!
			updateTrackingPoint(data);	
		} else if (STATE_LEARNING_BACKGROUND.equals(state))
		{
			waitInterval = 3000;
			waitForObjects(data);
		} else if (STATE_SEARCHING_FOREGROUND.equals(state))
		{
			waitInterval = 3000;
			waitForObjects(data);
		}
		return data;
	}
	
	// ----------- init routines begin ---------------
	public void initInput()
	{
		setState(STATE_INITIALIZING_INPUT);
//		videoOff(); eye data
		subscribe("publishOpenCVData", eye.getName(), "setOpenCVData", OpenCVData.class);
		eye.capture();
		
		setStatus("letting camera warm up and balance");
		sleep(2000);

	}
	
	public void initControl()
	{
		setState(STATE_INITIALIZING_CONTROL);
		
		arduino.setBoard("atmega328");
		arduino.setSerialDevice(serialPort); // TODO throw event if no serial connection
		Service.sleep(500);

		arduino.servoAttach("x", xServoPin);
		arduino.servoAttach("y", yServoPin);

		Service.sleep(500);

		x.moveTo(xRestPos);
		y.moveTo(yRestPos);

		Service.sleep(500);

		x.moveTo(xRestPos + 5);
		y.moveTo(yRestPos + 5);

		Service.sleep(500);
		
		x.moveTo(xRestPos);
		y.moveTo(yRestPos);
		
		currentXServoPos = xRestPos;
		currentYServoPos = yRestPos;
		lastXServoPos = xRestPos;
		lastYServoPos = yRestPos;
	}
		
	public void initTracking() {

		setState(STATE_INITIALIZING_TRACKING);
		// set initial Kp Kd Ki - TODO - use values derived from calibration
		xpid.setPID(10.0, 5.0, 1.0);
		xpid.setControllerDirection(PID.DIRECTION_DIRECT);
		xpid.setMode(PID.MODE_AUTOMATIC);
		xpid.setOutputRange(-10, 10); // <- not correct - based on maximum
		xpid.setSampleTime(30);

		ypid.setPID(10.0, 5.0, 1.0);
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

	}
	
	// ----------- init routines end ---------------

	public void rest()
	{
		x.moveTo(xRestPos);
		y.moveTo(yRestPos);
	}
	

	/*  FOUND/FIND BETTER TRACKING POINTS - 
	 * needs to be done  in the cortex
	 
	public void getGoodFeatures()
	{
		
		eye.removeAllFilters();
		eye.addFilter(FILTER_PYRAMID_DOWN, FILTER_PYRAMID_DOWN);
		eye.addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
		eye.setDisplayFilter(FILTER_GOOD_FEATURES_TO_TRACK);
		
		OpenCVData d = eye.getGoodFeatures();
		log.info("good features {}", d.keySet());
		
		SerializableImage img = d.getInputImage();
		
		//invoke("publishStatus", status);
		setState(STATE_FINDING_GOOD_FEATURES);
	}
	*/
	
	// ------------------- tracking & detecting methods begin ---------------------
	public void trackLKPoint()
	{
		trackLKPoint(null);
	}
	
	// TODO - put in OpenCV as a Composite Filter
	public void trackLKPoint(Point2Df targetPoint) {

		// set filters
		eye.removeAllFilters();
		eye.addFilter(FILTER_PYRAMID_DOWN, FILTER_PYRAMID_DOWN); // needed ??? test
		eye.addFilter(FILTER_LK_OPTICAL_TRACK, FILTER_LK_OPTICAL_TRACK);
		eye.setDisplayFilter(FILTER_LK_OPTICAL_TRACK);

		// FIXME - clear points
		// eye.invokeFilterMethod("clearPoints", method, params)
		
		// set point
		if (targetPoint != null)
		{
			eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", targetPoint.x, targetPoint.y);
		}

		setState(STATE_LK_TRACKING_POINT);
//		videoOn();
	}

	public void setForegroundBackgroundFilter() {

		// set filters
		eye.removeAllFilters();
		eye.addFilter(FILTER_PYRAMID_DOWN);
		eye.addFilter(FILTER_BACKGROUND_SUBTRACTOR_MOG2);
		eye.addFilter(FILTER_ERODE);
		eye.addFilter(FILTER_DILATE);
		eye.addFilter(FILTER_FIND_CONTOURS);

		((OpenCVFilterBackgroundSubtractorMOG2)eye.getFilter(FILTER_BACKGROUND_SUBTRACTOR_MOG2)).learn();

		setState(STATE_LEARNING_BACKGROUND);
//		videoOn();
	}

	public void learnBackground() {

		((OpenCVFilterBackgroundSubtractorMOG2)eye.getFilter(FILTER_BACKGROUND_SUBTRACTOR_MOG2)).learn();

		setState(STATE_LEARNING_BACKGROUND);
//		videoOn();
	}
	
	public void searchForeground() {

		((OpenCVFilterBackgroundSubtractorMOG2)eye.getFilter(FILTER_BACKGROUND_SUBTRACTOR_MOG2)).search();

		setState(STATE_SEARCHING_FOREGROUND);
//		videoOn();
	}
	
	double sizeIndexForBackgroundForegroundFlip = 0.10;
	
	public void waitForObjects(OpenCVData data)
	{
		data.setFilterName(FILTER_FIND_CONTOURS);
		ArrayList<Rectangle> objects = data.getBoundingBoxArray();
		int numberOfNewObjects = (objects == null)?0:objects.size();
		
		// if I'm not currently learning the background and
		// countour == background ??
		// set state to learn background
		if (!STATE_LEARNING_BACKGROUND.equals(state) && numberOfNewObjects == 1)
		{
			SerializableImage img = data.getImage();
			if (img == null)
			{
				log.error("here");
				return;
			}
			double width = img.getWidth();
			double height = img.getHeight();
			
			Rectangle rect = objects.get(0);
			
			//publish(data.getImages());
			
			if ((width - rect.width)/width < sizeIndexForBackgroundForegroundFlip && (height - rect.height)/height < sizeIndexForBackgroundForegroundFlip)
			{
				learnBackground();
				setStatus(String.format("%s - object found was nearly whole view - foreground background flip", state));
			}
			
		}
		
		if (numberOfNewObjects != lastNumberOfObjects)
		{
			setStatus(String.format("%s - unstable change from %d to %d objects - reset clock - was stable for %d ms limit is %d ms", state, lastNumberOfObjects, numberOfNewObjects ,System.currentTimeMillis() -lastTimestamp, waitInterval));
			lastTimestamp = System.currentTimeMillis();
		}
		
		if (waitInterval < System.currentTimeMillis() - lastTimestamp)
		{
			setLocation(data);
			// number of objects have stated the same
			if (STATE_LEARNING_BACKGROUND.equals(state))
			{
				if (numberOfNewObjects == 0)
				{
					// process background
					data.putAttribute(BACKGROUND);
					invoke("toProcess", data);
					// ready to search foreground
					searchForeground();
				}
			} else {
				
				// stable state changes with # objects
				//setState(STATE_STABILIZED);
				//log.info("number of objects {}",numberOfNewObjects);
				// TODO - SHOULD NOT PUT IN MEMORY -
				// LET OTHER THREAD DO IT
				if (numberOfNewObjects > 0)
				{
					data.putAttribute(FOREGROUND);
					invoke("toProcess", data);
				}// else TODO - processBackground(data) <- on a regular interval (addToSet) !!!!!!
			}
		}
		
		lastNumberOfObjects = numberOfNewObjects;
		
	}
	
	// TODO - enhance with location - not just heading
	// TODO - array of attributes expanded Object[] ... ???
	// TODO - use GEOTAG - LAT LONG ALT DIRECTION LOCATION CITY GPS TIME OFFSET
	public OpenCVData setLocation(OpenCVData data)
	{
		data.setX(currentXServoPos);
		data.setY(currentYServoPos);
		return data;
	}

	// ------------------- tracking & detecting methods end ---------------------

	public void setIdle()
	{
		setState(STATE_IDLE);
	}

	public void setState(String newState) {
		state = newState;
		setStatus(state);
	}
	
	public void setStatus(String status) {
		log.info(status);
		invoke("publishStatus", status);
	}

	// ---------------  publish methods begin ----------------------------
	public OpenCVData toProcess (OpenCVData data){
		return data;
	}
	
	public SerializableImage publishFrame(SerializableImage image) {
		return image;
	}

	// ubermap !!!
	public void publish(HashMap<String,SerializableImage> images) {
		for (Map.Entry<String,SerializableImage> o : images.entrySet())
		{
			//Map.Entry<String,SerializableImage> pairs = o;
			log.info(o.getKey());
			publish(o.getValue());
		}
	}
	
	public void publish(SerializableImage image) {
		invoke("publishFrame", image);
	}

	public String publishStatus(String status) {
		return status;
	}
	
	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation";
	}

	// ---------------  publish methods end ----------------------------


	// FIXME - lost tracking event !!!!
	final public void updateTrackingPoint(OpenCVData cvData) {
		
		// extract tracking info
		cvData.setFilterName(FILTER_LK_OPTICAL_TRACK);
		ArrayList<Point2Df> data = cvData.getPointArray();
		
		if (data == null)
		{
			// lost track event ?!?
			log.info("data arriving, but no point array - tracking point missing?");
			return;
		}
		++cnt;
		Point2Df targetPoint;
		
		if (data.size() > 0) {
			targetPoint = data.get(0);
			// describe this time delta
			latency = System.currentTimeMillis() - targetPoint.timestamp; 
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
	
	// ----------------- required config & necessary important global switches begin --------------------------
	public void startVideoStream()
	{
		//setStatus("switching video on");
		eye.publishOpenCVData(true);
	}
	
	public void stopVideoStream()
	{
		//setStatus("switching video off");
		eye.publishOpenCVData(false);
	}
	

	public void setRestPosition(int xpos, int ypos) {
		this.xRestPos = xpos;
		this.yRestPos = ypos;
	}

	public void setCameraIndex(int i) {
		eye.setCameraIndex(i);
	}

	public void setYServoPin(int y) {
		yServoPin = y;
	}

	public void setXServoPin(int x) {
		xServoPin = x;
	}

	public void setSerialPort(String portName) {
		serialPort = portName;
	}
	

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Tracking tracker = new Tracking("tracking");
		tracker.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		tracker.setRestPosition(90, 5);
		tracker.setSerialPort("COM12");
		tracker.setXServoPin(13);
		tracker.setYServoPin(12);
		tracker.setCameraIndex(1);
		
		tracker.initTracking();
		tracker.initControl();
		tracker.initInput();
		
		tracker.startVideoStream();
		
		tracker.trackLKPoint();
		
		
		tracker.setIdle();
		
		tracker.startVideoStream();
		
//		tracker.learnBackGround();
		//tracker.searchForeground();
		
		tracker.trackLKPoint(new Point2Df(50,50));
		
		log.info("here");
				
		//tracker.getGoodFeatures();

		//tracker.trackLKPoint();

	}

}
