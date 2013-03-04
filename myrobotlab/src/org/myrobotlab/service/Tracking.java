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

import java.util.ArrayList;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.service.data.Point2Df;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import static org.myrobotlab.service.OpenCV.FILTER_GOOD_FEATURES_TO_TRACK;
import static org.myrobotlab.service.OpenCV.FILTER_LK_OPTICAL_TRACK;
import static org.myrobotlab.service.OpenCV.FILTER_PYRAMID_DOWN;

public class Tracking extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Tracking.class.getCanonicalName());
	
	
	// GOOD - TODO make it better ! Encapsulate in Hash'd data structure
	// with a setSubServiceName("arduio", "x") - for foreign structures
	public String arduinoName = "arduino";
	public String xpidName = "xpid";
	public String ypidName = "ypid";
	public String xName = "x";
	public String yName = "y";
	public String opencvName = "opencv";
	
	@Element
	public int xServoPin = 13;
	@Element
	public int yServoPin = 12;
	@Element
	String serialPort = "COM12";
	
	// TODO enum - allows meta data? like description of state ???
	public final static String STATE_LK_TRACKING_POINT = "lucas kanade tracking";
	public final static String STATE_IDLE = "idle";
	public final static String STATE_NEED_TO_INITIALIZE = "initializing";
	public static final String STATUS_CALIBRATING = "calibrating";
	private static final String STATE_FINDING_GOOD_FEATURES = "finding good features";

	private String state = STATE_NEED_TO_INITIALIZE;
	
	@Element
	int xRestPos;
	@Element
	int yRestPos;
	
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
	public Point2Df targetPoint;
	
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

	// TODO - make initialization re-entrant
	boolean cameraInitialized = false;
	boolean servosInitialized = false;
	boolean arduinoInitialized = false;
	boolean systemTest = false;

	boolean interrupted = false;
	
	OpenCVData goodFeatures;

	
	public Tracking(String n) {
		super(n, Tracking.class.getCanonicalName());
	}
	
	// set SubServiceNames....
	// TODO - framework?
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
		
		if (state.equals(STATE_LK_TRACKING_POINT))
		{
			///trackLKPoint(data);
			updateTrackingPoint(data);
		} else if (state.equals(STATE_FINDING_GOOD_FEATURES))
		{
			goodFeatures = data;
			videoOff();
			setState(STATE_IDLE);
		}
		return data;
	}
	
	// blocking method
	
	// turn on the camera - but keep the video in feed off
	public void initInput()
	{
		videoOff();
		subscribe("publishOpenCVData", eye.getName(), "setOpenCVData", OpenCVData.class);
		eye.capture();
		
		setStatus("letting camera warm up and balance");
		sleep(4000);

	}
	
	public void initControl()
	{
		setStatus("initializing control");
		
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
	
	public void rest()
	{
		x.moveTo(xRestPos);
		y.moveTo(yRestPos);
	}
	
	
	
	public void initTracking() {

		setStatus("initializing tracking");
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
	
	public void getGoodFeatures()
	{
		/*
		eye.removeAllFilters();
		eye.addFilter(FILTER_PYRAMID_DOWN, FILTER_PYRAMID_DOWN);
		eye.addFilter(FILTER_GOOD_FEATURES_TO_TRACK, FILTER_GOOD_FEATURES_TO_TRACK);
		eye.setDisplayFilter(FILTER_GOOD_FEATURES_TO_TRACK);
		*/
		OpenCVData d = eye.getGoodFeatures();
		log.info("good features {}", d.keySet());
		
		SerializableImage img = d.getInputImage();
		
		//invoke("publishStatus", status);
		setState(STATE_FINDING_GOOD_FEATURES);
	}
	
	public void trackLKPoint()
	{
		trackLKPoint(null);
	}
	
	public void trackLKPoint(Point2Df targetPoint) {

		// set filters
		eye.removeAllFilters();
		eye.addFilter("pyramidDown1", "PyramidDown"); // needed ??? test
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
		videoOn();
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
	public SerializableImage publishFrame(SerializableImage image) {
		return image;
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

	// FIXME - remove OpenCV definitions
	final public void updateTrackingPoint(OpenCVData cvData) {
		
		cvData.setFilterName(FILTER_LK_OPTICAL_TRACK);
		ArrayList<Point2Df> data = cvData.getPointArray();
		if (data == null)
		{
			log.info("data arriving, but no point array - tracking point missing?");
			return;
		}
		++cnt;
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
	
	public void videoOn()
	{
		//setStatus("switching video on");
		eye.publishOpenCVData(true);
	}
	
	public void videoOff()
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
		
		tracker.trackLKPoint();
		
		log.info("here");
				
		//tracker.getGoodFeatures();

		//tracker.trackLKPoint();

	}

}
