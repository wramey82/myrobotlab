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
import static org.myrobotlab.service.OpenCV.FILTER_FIND_CONTOURS;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FILTER_PYRAMID_DOWN;
import static org.myrobotlab.service.OpenCV.FOREGROUND;
import static org.myrobotlab.service.OpenCV.PART;

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
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.opencv.OpenCVFilterDetector;
import org.myrobotlab.opencv.OpenCVFilterFlip;
import org.myrobotlab.opencv.OpenCVFilterPyramidDown;
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
		
	public ArrayList<OpenCVFilter> additionalFilters = new ArrayList<OpenCVFilter>();
	
	long lastTimestamp = 0;
	long waitInterval = 5000;
	int lastNumberOfObjects = 0;
	
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
	public Point2Df lastPoint;

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
	public Integer xPin;
	@Element
	public Integer yPin;
	@Element
	String serialPort;
	// "center" set points
	@Element
	public double xSetpoint = 0.5;
	@Element
	public double ySetpoint = 0.5;

	// ----- INITIALIZATION DATA END -----

	
	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
	}
	
	// DATA WHICH MUST BE SET BEFORE ATTACH METHODS !!!! - names must be set of course !
	// com port
	// IMPORTANT CONCEPT - the Typed function should have ALL THE BUSINESS LOGIC TO ATTACH 
	// NON ANYWHERE ELSE !!
	public void startService()
	{	super.startService();
	
		boolean startup = true;
		
		startup &= attach(eye);
		startup &= attach(arduino, serialPort);
		startup &= attachServos(x, xPin, y, yPin);
		startup &= attachPIDs(xpid, ypid);
		
		if (startup) {
			info("tracking ready");
		} else {
			error("tracking could not initialize properly");
		}
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
	// begin attach points -----------------
	// FIXME - make re-entrant !!!
	public boolean attach(OpenCV opencv)
	{
		if (eye != null)
		{
			log.info("eye already assigned");
			return true;
		}
		info("attaching eye");
		eye = (OpenCV) Runtime.createAndStart(opencvName, "OpenCV", opencv);
		subscribe("publishOpenCVData", eye.getName(), "setOpenCVData", OpenCVData.class);
		//eye.capture();
		eye.broadcastState();
		return true;
	}
	
	public boolean attach(Arduino duino)
	{
		return attach(duino, serialPort);
	}
	// TODO - check all service - check for validity of system !!
	public boolean attach(Arduino duino, String inSerialPort)
	{
		if (arduino != null)
		{
			log.info("arduino already attached");
			return true;
		}
		serialPort = inSerialPort;
		
		info("attaching Arduino");
		if (duino!= null)
		{
			arduinoName = duino.getName();
		}
		arduino = (Arduino) Runtime.createAndStart(arduinoName, "Arduino", duino);
		
		if (!arduino.isConnected())
		{
			if (serialPort == null)
			{
				error("no serial port specified for Arduino");
				return false;
			}
			arduino.setSerialDevice(serialPort);
		}
		
		Service.sleep(500);
		
		if (!arduino.isConnected())
		{
			error("Arduino is not connected!");
			return false;
		}
		
		arduino.broadcastState();
		return true;
	} 
	
	// TODO - this should resolve the difference between default and externally supplied
	// servos - NEEDS OVERLOADING (no pins)
	public boolean attachServos(Servo inX, Integer inXPin, Servo inY, Integer inYPin)
	{
		// FIXME - SEE IF NOT CONNECTED IF NOT CONNECTED ERROR - LOGIC SHOULD BE IN SERVOS!!!!!
	
		info("attaching servos");
		if (arduino == null)
		{
			error("Arduino must be attached first, before servos !");
			return false;
		}
		
		/*
		TODO - finish table - find pattern
		states
		default init no data    				x == servox && x == null && inXPin == null
		data from file							x == servox && x == null && inXPin != null		INVALID IF - inXPin == null
		replace possible valid with different  	x != null && x != servox  && servox != null && (InXPin == null || InXPin != null)
		
		*/
		
		if (inXPin == null || inYPin == null && (inX == null|| inY == null))
		{
			error("servo pins must be set before attaching servos!");
		}
		
		if (inX != null)
		{
			xName = inX.getName();
		}
		if (inY != null)
		{
			yName = inY.getName();
		}
		
		xPin = inXPin;
		yPin = inYPin;
		
		
		x = (Servo) Runtime.createAndStart(xName, "Servo", inX);
		y = (Servo) Runtime.createAndStart(yName, "Servo", inY);
		
		if (xmin != null)
		{
			x.setPositionMin(xmin);
		}
		
		if (xmax != null)
		{
			x.setPositionMax(xmax);
		}
		
		if (ymin != null)
		{
			y.setPositionMin(ymin);
		}
		
		if (ymax != null)
		{
			y.setPositionMax(ymax);
		}
		
		arduino.servoAttach(xName, xPin);
		arduino.servoAttach(yName, yPin);
		
		x.moveTo(xRestPos);
		y.moveTo(yRestPos);

		// shake and be ALIVE !
		
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

		// save limits for next time
		xmin = x.getPositionMin();
		xmax = x.getPositionMax();

		ymin = y.getPositionMin();
		ymax = y.getPositionMax();
		
		x.broadcastState();
		y.broadcastState();

		return true;
	}
	
	public boolean attachPIDs(PID inXpid, PID inYpid)
	{
		info("attaching pid");
		if (inXpid != null)
		{
			xpidName = inXpid.getName();
		}
		if (inYpid != null)
		{
			ypidName = inYpid.getName();
		}
		
		if (xpid != null || ypid != null && (inXpid == null || inYpid == null))
		{
			info("xpid or ypid already set - must unset it first");
			return true;
		}
		
		xpid = (PID) Runtime.createAndStart(xpidName, "PID", inXpid);
		ypid = (PID) Runtime.createAndStart(ypidName, "PID", inYpid);

		xpid.setPID(10.0, 5.0, 1.0);
		xpid.setControllerDirection(PID.DIRECTION_DIRECT);
		xpid.setMode(PID.MODE_AUTOMATIC);
		xpid.setOutputRange(-10, 10); // <- not correct - based on maximum
		xpid.setSampleTime(30);
		// set center
		xpid.setSetpoint(xSetpoint);

		ypid.setPID(10.0, 5.0, 1.0);
		ypid.setControllerDirection(PID.DIRECTION_DIRECT);
		ypid.setMode(PID.MODE_AUTOMATIC);
		ypid.setOutputRange(-10, 10);
		ypid.setSampleTime(30);
		// set center
		ypid.setSetpoint(ySetpoint);

		xpid.broadcastState();
		ypid.broadcastState();
		return true;
	}

	public void rest()
	{
		x.moveTo(xRestPos);
		y.moveTo(yRestPos);
	}
	
	
	// ------------------- tracking & detecting methods begin ---------------------

	public void startLKTracking()
	{
		// set filters
		eye.clearFilters();
		eye.addFilter("PyramidDown"); // FIXME - remove this and have user (or helper method) add it to custom filter
		for (int i = 0; i < additionalFilters.size(); ++i)
		{
			eye.addFilter(additionalFilters.get(i));
		}
		eye.addFilter(FILTER_LK_OPTICAL_TRACK, FILTER_LK_OPTICAL_TRACK);
		eye.setDisplayFilter(FILTER_LK_OPTICAL_TRACK);

		eye.capture();
		eye.publishOpenCVData(true);
		
		setState(STATE_LK_TRACKING_POINT);
		
	}
	
	public void stopLKTracking()
	{
		eye.clearFilters();
		setState(STATE_IDLE);
	}
	
	public void trackPoint(float x, float y) {

		if (!STATE_LK_TRACKING_POINT.equals(state))
		{
			startLKTracking();
		}

		eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
	}
	
	// GAAAAAAH figure out if (int , int) is SUPPORTED WOULD YA !
	public void trackPoint(int x, int y) {

		if (!STATE_LK_TRACKING_POINT.equals(state))
		{
			startLKTracking();
		}
		eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "samplePoint", x, y);
	}

	public void clearTrackingPoints()
	{
		 eye.invokeFilterMethod(FILTER_LK_OPTICAL_TRACK, "clearPoints");
	}
	
	public void setForegroundBackgroundFilter() {

		// set filters
		eye.clearFilters();
		eye.addFilter(FILTER_PYRAMID_DOWN);
		for (int i = 0; i < additionalFilters.size(); ++i)
		{
			eye.addFilter(additionalFilters.get(i));
		}
		eye.addFilter(FILTER_DETECTOR);
		eye.addFilter(FILTER_ERODE);
		eye.addFilter(FILTER_DILATE);
		eye.addFilter(FILTER_FIND_CONTOURS);

		((OpenCVFilterDetector)eye.getFilter(FILTER_DETECTOR)).learn();

		setState(STATE_LEARNING_BACKGROUND);
	}

	public void learnBackground() {

		((OpenCVFilterDetector)eye.getFilter(FILTER_DETECTOR)).learn();

		setState(STATE_LEARNING_BACKGROUND);
	}
	
	public void searchForeground() {

		((OpenCVFilterDetector)eye.getFilter(FILTER_DETECTOR)).search();

		setState(STATE_SEARCHING_FOREGROUND);
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
				info(String.format("%s - object found was nearly whole view - foreground background flip", state));
			}
			
		}
		
		if (numberOfNewObjects != lastNumberOfObjects)
		{
			info(String.format("%s - unstable change from %d to %d objects - reset clock - was stable for %d ms limit is %d ms", state, lastNumberOfObjects, numberOfNewObjects ,System.currentTimeMillis() -lastTimestamp, waitInterval));
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
					//data.putAttribute(BACKGROUND);
					data.putAttribute(PART, BACKGROUND);
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
					data.putAttribute(PART, FOREGROUND);
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
		info(state);
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

	
	@Override
	public String getToolTip() {
		return "proportional control, tracking, and translation";
	}

	// ---------------  publish methods end ----------------------------


	// FIXME - lost tracking event !!!!
	// FIXME - this is WAY TO OPENCV specific !
	// OpenCV should have a publishTrackingPoint method !
	// This should be updateTrackingPoint(Point2Df) & perhaps Point3Df :)
	final public void updateTrackingPoint(OpenCVData cvData) {
		
		// extract tracking info
		cvData.setFilterName(String.format("%s.%s", eye.getName(),FILTER_LK_OPTICAL_TRACK));
		ArrayList<Point2Df> data = cvData.getPoints();
		
		if (data == null)
		{
			// lost track event ?!?
//			log.info("data arriving, but no point array - tracking point missing?");
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
		}

		if (cnt % updateModulus == 0) {
			broadcastState(); // update graphics ?
			log.error(String.format("%f %f", computeX, computeY));
		}

	}	

	public void setRestPosition(int xpos, int ypos) {
		this.xRestPos = xpos;
		this.yRestPos = ypos;
	}

	public void setCameraIndex(int i) {
		if (eye == null)
		{
			eye = (OpenCV)Runtime.create(opencvName, "OpenCV");
		}
		eye.setCameraIndex(i);
	}

	public void setServoPins(Integer x, Integer y) {
		xPin = x;
		yPin = y;
	}

	public void setSerialPort(String portName) {
		serialPort = portName;
	}
	
	public void setXMinMax(int xmin, int xmax)
	{
		this.xmin = xmin;
		this.xmax = xmax;
	}
	public void setYMinMax(int ymin, int ymax)
	{
		this.ymin = ymin;
		this.ymax = ymax;
	}
	
	public void faceDetect()
	{
		OpenCVFilterPyramidDown py = new OpenCVFilterPyramidDown();
		eye.addFilter("PyramidDown"); // FIXME - add to additional filters !
		eye.addFilter("Gray"); // FIXME - add to additional filters !
		for (int i = 0; i < additionalFilters.size(); ++i)
		{
			eye.addFilter(additionalFilters.get(i));
		}
		eye.addFilter("FaceDetect", "FaceDetect");
		eye.setDisplayFilter("FaceDetect");

		//wrong state
		setState(STATE_LK_TRACKING_POINT);

	}
	
	public void clearFilters()
	{
		eye.clearFilters();
	}
	

	public void test()
	{
		for (int i = 0; i < 1000; ++i)
		{
			//invoke("trackPoint", 0.5, 0.5);
			//faceDetect();
			trackPoint();
			//trackPoint(0.5f,0.5f);
			setForegroundBackgroundFilter();
			learnBackground();
			searchForeground();
			clearFilters();
		}
	}
	
	public void trackPoint() {
		trackPoint(0.5f,0.5f);	
	}

	public void addFilter(OpenCVFilter filter)
	{
		additionalFilters.add(filter);
	}
	
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Tracking tracker = new Tracking("tracking");
		tracker.startService();
		/*
		OpenCV cv = new OpenCV("cv");
		
		tracker.attach("cv");
		
		Speech mouth = new Speech("mouth");
		mouth.subscribe("publishStatus", tracker.getName(), "speak", String.class);
		mouth.startService();
		tracker.startService();
		
		*/
		
		OpenCVFilterFlip flip = new OpenCVFilterFlip();
		tracker.addFilter(flip);
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

		tracker.startLKTracking();
		
		//tracker.setCameraIndex(1);
		//tracker.test();
		/*
		tracker.setRestPosition(90, 5);
		tracker.setSerialPort("COM12");
		tracker.setServoPins(13, 12);
		tracker.setCameraIndex(1);
		*/
	

		/*
		tracker.startLKTracking();
		
		tracker.invoke("trackPoint", 100, 100);
		tracker.invoke("trackPoint", 0.5f, 0.5f);
		
		
		tracker.setIdle();
		tracker.startVideoStream();
		tracker.stopVideoStream();
		tracker.startVideoStream();
		
*/		
		
//		tracker.learnBackGround();
		//tracker.searchForeground();
		
		
		log.info("here");
				
		//tracker.getGoodFeatures();

		//tracker.trackLKPoint();

	}

}
