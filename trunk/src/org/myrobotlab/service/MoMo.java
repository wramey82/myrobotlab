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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.SensorMonitor.Alert;
import org.myrobotlab.service.data.ColoredPoint;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

public class MoMo extends Service {

	public final static Logger LOG = Logger.getLogger(MoMo.class
			.getCanonicalName());

	public final static int IR_PIN = 1;

	/*
	 * static { Logger.getRootLogger().setLevel(Level.WARN); }
	 */

	SpeechRecognition ear = new SpeechRecognition("ear");
	RemoteAdapter remote = new RemoteAdapter("remote");
	Arduino arduino = new Arduino("arduino");
	GUIService gui = new GUIService("gui");
	// AudioFile mouth = new AudioFile("mouth");
	Speech mouth = new Speech("mouth");
	OpenCV camera = new OpenCV("camera");
	// Invoker invoker = new Invoker("invoker");
	// Servo shaker = new Servo("shaker");

	Motor left = new Motor("left");
	Motor right = new Motor("right");
	// Motor neck = new Motor("neck");

	SensorMonitor sensors = new SensorMonitor("sensors");

	public MoMo(String n) {
		this(n, null);
	}

	public MoMo(String n, String serviceDomain) {
		super(n, MoMo.class.getCanonicalName(), serviceDomain);
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public void startRobot() {

		right.startService();
		left.startService();
		// neck.start();
		arduino.startService();
		mouth.startService();
		// remote.start();
		camera.startService();
		// invoker.start();
		gui.startService();
		// ear.start();
		// shaker.start();
		// shaker.attach(arduino.name, 11);

		sensors.startService();

		mouth.getCFG().set("isATT", true);
		sensors.addAlert("200", 200, 200, Alert.BOUNDRY, Alert.STATE_LOW,
				IR_PIN);
		sensors.addAlert("300", 300, 300, Alert.BOUNDRY, Alert.STATE_LOW,
				IR_PIN);
		sensors.addAlert("400", 400, 400, Alert.BOUNDRY, Alert.STATE_LOW,
				IR_PIN);
		sensors.addAlert("500", 500, 500, Alert.BOUNDRY, Alert.STATE_LOW,
				IR_PIN);
		sensors.addAlert("600", 600, 600, Alert.BOUNDRY, Alert.STATE_LOW,
				IR_PIN);
		sensors.notify("publish", this.name, "publish", Alert.class);

		// creating static route from ear/speech recognition to special action
		ear.notify("publish", this.name, "speechToAction", String.class);

		// Motors attached to Arduino
		// left motor inverted to keep it simple for me
		// neck.attach(arduino.name, 10, 12);
		right.attach(arduino.name, 9, 8);
		// left.attach(arduino.name, 11, 13);
		left.attach(arduino.name, 10, 11);
		left.invertDirection();

		// 2 encoders attached
		// right.attachEncoder(arduino.name, 2);
		// left.attachEncoder(arduino.name, 3);

		left.stop();
		right.stop();
		// neck.stopMotor();

		// 320 x 240 is easier to work with over wireless
		camera.addFilter("PyramidDown", "PyramidDown");

		remote.startService();
		// opticalTrack();
		// moveForwardStraight();
		// mouth.play("state/looking");
		// mouth.play("state/ready");
		gui.display();

	}

	// TODO - do in Service
	public static void main(String[] args) {

		MoMo momo = new MoMo("momo");
		momo.startService();
		momo.startRobot();
	}

	int speedIncrement = 5; // TODO - put this in GUI

	public void speechToAction(String speech) {
		if (speech.compareTo("rose") == 0) {
			mouth.speak("listening");
		} else if (speech.compareTo("board") == 0) {
			// lookAtBoard();
			mouth.speak("board");
		} else if ((speech.compareTo("stop") == 0)
				|| (speech.compareTo("halt") == 0)) {
			// lookAtBoard();
			left.stopAndLock();
			right.stopAndLock();
			mouth.speak("locked");
		} else if (speech.compareTo("go") == 0) {
			left.incrementPower(0.3f);
			right.incrementPower(0.3f);
			// center();
		} else if (speech.compareTo("center") == 0) {
			// center();
			mouth.speak("center");
		} else if (speech.compareTo("camera on") == 0) {
			// cameraOn();
			mouth.speak("looking");
		} else if (speech.compareTo("camera off") == 0) {
			mouth.speak("my eyes are closed");
			// cameraOff();
		} else if (speech.compareTo("watch") == 0) {
			mouth.speak("looking");
			// filterOn();
		} else if (speech.compareTo("find") == 0) {
			// report();
		} else if (speech.compareTo("left") == 0) {
			// report();
			left.incrementPower(0.1f);
		} else if (speech.compareTo("right") == 0) {
			// report();
			right.incrementPower(0.1f);
		} else if (speech.compareTo("clear") == 0) {
			// filterOff();
		} else {
			mouth.speak("what did you say");
		}
	}

	public void opticalTrackFilter() {
		camera.addFilter("LKOpticalTrack", "LKOpticalTrack");
	}

	public void floorFindFilter() {
		camera.addFilter("Dilate", "Dilate");
		camera.addFilter("Erode", "Erode");
		camera.addFilter("Smooth", "Smooth");
		camera.addFilter("FloodFill", "FloodFill");
		camera.addFilter("SampleArray", "SampleArray");
	}

	public void movementFilter() {
		camera.addFilter("Dilate", "Dilate");
		camera.addFilter("Erode", "Erode");
		camera.addFilter("Smooth", "Smooth");
		camera.addFilter("Smooth", "Smooth");
		camera.addFilter("FGBG", "FGBG");
		camera.addFilter("FindContours", "FindContours");

	}

	public void cameraOn() {
		camera.setUseInput("camera");
		camera.capture();
	}

	public void cameraOff() {
		camera.setUseInput("null");
		camera.capture();
	}

	public void findFloor(ColoredPoint[] points) {
		LOG.info("here");
		int a = points[0].getRed();
		a = points[0].getGreen();
		a = points[0].getBlue();

		if (points[0].getRed() != 128 && points[0].getGreen() != 0
				&& points[0].getBlue() != 0) {
			stopReverseRightTurn();
		}
	}

	public void stopReverseRightTurn() {
		try {

			left.stop(); // TODO decompose
			right.stop();
			mouth.speak("stop");

			left.incrementPower(0.45f); // TODO - moveTo -3 inches
			right.incrementPower(0.45f);

			Thread.sleep(1000);
			left.stop(); // TODO decompose
			right.stop();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	int trackingPower = 1;

	public void moveForwardStraight() {
		// begin optical tracking on 1 point
		// opticalTrackFilter ();

		// add the call back
		camera.notify("publish", this.name,
				"adjustPowerMaintainStraightCourse", CvPoint2D32f.class);

	}

	CvPoint2D32f[] oldPoints = null;
	int centerX = 320 / 2;
	int centerY = 240 / 2;
	int currentX = 0;
	int currentY = 0;
	float xPower = 0;
	float maxPower = 0.4f;

	public void trackPoint(CvPoint2D32f[] points) {
		/*
		 * if (oldPoints[0] == null) { oldPoints[0] = new CvPoint2D32f();
		 * oldPoints[0].x = points[0].x; oldPoints[0].y = points[0].y; return; }
		 */
		maxPower = 0.4f;
		// oldXAverage
		// centerX = (int)oldPoints[0].x;
		// centerY = (int)oldPoints[0].y;
		currentX = (int) points[0].x();
		currentY = (int) points[0].y();

		left.setMaxPower(maxPower);
		right.setMaxPower(maxPower);

		xPower = Math.abs((float) (centerX - currentX) / 140);
		if (xPower > maxPower) {
			xPower = maxPower;
		}

		//Log.error("track " + centerX + "," + centerY + " " + currentX + "," + currentY + " speed " + xPower);

		if (centerX > currentX) {
			// left.incrementPower(-0.01f);
			// right.incrementPower(0.01f);
			LOG.error("correct Left");
			left.move(-xPower);
			right.move(xPower);
		} else if (centerX < currentX) {
			// left.incrementPower(0.01f);
			// right.incrementPower(-0.01f);
			LOG.error("correct Left");
			left.move(xPower);
			right.move(-xPower);

		} else {
			LOG.error("*****LOCKED*****");
			left.stop();
			right.stop();
		}
		oldPoints = points;
		//LOG.error("left " + left.getPower() + " right " + right.getPower());
	}

	public void watchForMovement() {
		// remove all the movement and optical tracking filters
		camera.removeFilter("Dilate");
		camera.removeFilter("Erode");
		camera.removeFilter("Smooth");
		camera.removeFilter("Smooth");
		camera.removeFilter("FGBG");
		camera.removeFilter("FindContours");

		camera.removeFilter("LKOpticalTrack");

		// remove triggers
		// camera.removeNotify(outMethod, serviceName, inMethod, paramType); //
		// TODO - make removable by query best
		camera.removeNotify("publish", this.name, "trackPoint", CvPoint2D32f[].class);

		// load the movement filter
		movementFilter();

		// set trigger
		camera.notify("publish", this.name, "foundMovement", Rectangle.class);
	}

	boolean tracking = false;

	public void foundMovement(Rectangle rect) {
		// if (!tracking)
		{
			// remove movement trigger
			camera.removeNotify("publish", this.name, "foundMovement",Rectangle.class);

			// begin tracking
			tracking = true;

			// remove filters
			// camera.removeFilters();
			camera.removeFilter("Dilate");
			camera.removeFilter("Erode");
			camera.removeFilter("Smooth");
			camera.removeFilter("Smooth");
			camera.removeFilter("FGBG");
			camera.removeFilter("FindContours");

			// add the optical track filters
			opticalTrackFilter();
			camera.invokeFilterMethod("LKOpticalTrack", "clearPoints", null);
			camera.notify("publish", this.name, "trackPoint", CvPoint2D32f[].class);

			// add a LK point in the center of the motion
			Point p = new Point(rect.x + rect.width / 2, rect.y + rect.height
					/ 2);
			Object[] params = new Object[1];
			params[0] = (Object) p;

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			camera.invokeFilterMethod("LKOpticalTrack", "samplePoint", params);

		}

	}

	public void trackPoints(CvPoint2D32f[] points) {
		LOG.error(points[0]);
	}

	public void samplePoints(CvPoint2D32f[] points) {
		LOG.warn(points[0]);
	}

	boolean beginSampled = false;
	boolean endSampled = false;
	Point[] x0 = null;
	Point[] x1 = null;

	public void sampleBeginPoints(CvPoint2D32f[] points) {
		if (!beginSampled) {
			beginSampled = true;
			x0 = new Point[points.length];
			for (int i = 0; i < points.length; ++i) {
				Point p = new Point((int) points[i].x(), (int) points[i].y());
				x0[i] = p;
			}
		}
	}

	public void sampleEndPoints(CvPoint2D32f[] points) {
		if (!endSampled) {
			endSampled = true;
			x1 = new Point[points.length];
			for (int i = 0; i < points.length; ++i) {
				Point p = new Point((int) points[i].x(), (int) points[i].y());
				x1[i] = p;
			}
		}
	}

	public void generateSketchupFile() {
		String ret = "\n";
		for (int i = 0; i < x0.length; ++i) {
			ret += "Sketchup.active_model.entities.add_line [" + x1[i].x + ", "
					+ x1[i].y + ", " + (30 - (x1[i].x - x0[i].x)) + "], ["
					+ x0[i].x + ", " + x0[i].y + ", "
					+ (30 - (x1[i].x - x0[i].x)) + "]\n";
		}
		LOG.error(ret);
	}

	public void keyCommandString(String cmd) {
		if (cmd.compareTo("Up") == 0) {
			right.incrementPower(0.1f);
			left.incrementPower(0.1f);
		} else if (cmd.compareTo("Down") == 0) {
			right.incrementPower(-0.1f);
			left.incrementPower(-0.1f);
		} else if (cmd.compareTo("Left") == 0) {
			left.incrementPower(-0.1f);
			right.incrementPower(0.1f);
		} else if (cmd.compareTo("Right") == 0) {
			right.incrementPower(-0.1f);
			left.incrementPower(0.1f);
		} else if (cmd.compareTo("Space") == 0) {
			right.stopAndLock();
			left.stopAndLock();
		} else if (cmd.compareTo("W") == 0) {
			watchForMovement();
		} else if (cmd.compareTo("U") == 0) {
			right.unLock();
			left.unLock();
		} else if (cmd.compareTo("9") == 0) {
			// shaker.invoke("moveTo",130);
		} else if (cmd.compareTo("8") == 0) {
			// shaker.invoke("moveTo",80);
		} else if (cmd.compareTo("7") == 0) {
			// shaker.invoke("moveTo",70);
		} else if (cmd.compareTo("6") == 0) {
			// shaker.invoke("moveTo",20);
		} else if (cmd.compareTo("R") == 0) {
			camera.removeNotify("publish", this.name, "sampleEndPoints",
					CvPoint2D32f[].class);
			beginSampled = false;
			camera.notify("publish", this.name, "sampleBeginPoints",
					CvPoint2D32f[].class);
		} else if (cmd.compareTo("T") == 0) {
			camera.removeNotify("publish", this.name, "sampleBeginPoints",
					CvPoint2D32f[].class);
			endSampled = false;
			camera.notify("publish", this.name, "sampleEndPoints",
					CvPoint2D32f[].class);

		} else if (cmd.compareTo("M") == 0) {
			camera.addFilter("Mouse", "Mouse");
			camera.notify("publish", this.name, "mousePoints",
					CvPoint2D32f[].class);
		} else if (cmd.compareTo("G") == 0) {
			camera.removeNotify("publish", this.name, "sampleBeginPoints",
					CvPoint2D32f[].class);
			camera.removeNotify("publish", this.name, "sampleEndPoints",
					CvPoint2D32f[].class);
			generateSketchupFile();
		}

	}

	public void mousePoints(ArrayList<CvPoint> points) {
		// b = tan(54.7 + # of pixels / 5.6) * 17
		HashMap<String, CvPoint> unique = new HashMap<String, CvPoint>();
		double Y = 0.0;
		double X = 0.0;

		String ret = "\n";

		LOG.error(points.size());
		for (int i = 0; i < points.size(); ++i) {
			CvPoint p = points.get(i);
			if (p.x() != 0 && p.x() != 319 && p.y() != 0 && p.y() != 239) {
				Y = Math.tan(0.01745 * (54.7 + (240 - p.y()) / 5.6)) * 17;
				X = Math.sqrt(Y * Y + 17 * 17)
						* Math.sin(0.01745 * -1 * (160 - p.x()) / 5.6);
				if (!unique.containsKey((int) X + "," + (int) Y)) {
					unique.put((int) X + "," + (int) Y, p);
					ret += "Sketchup.active_model.entities.add_line ["
							+ (int) X + ", " + (int) Y + ", " + 0 + "], ["
							+ (int) X + ", " + (int) Y + ", " + 8 + "]\n";
				}
				// Log.error(p.x + "," + (int)z + "," + p.y);
				// Math.sqrt(a)
			}
		}

		//Log.error(ret);
		camera.removeFilter("Mouse");
		camera.removeNotify("publish", this.name, "mousePoints",
				CvPoint2D32f[].class);

	}

	public void publish(Alert a) {
		// mouth.speak("range " + a.name);

		if (a.max == 200) {
			mouth.speak("range 5 inches");
		}

		if (a.max == 300) {
			mouth.speak("your getting closer");
		}

		if (a.max == 400) {
			mouth.speak("back off punk");
		}

		if (a.max == 500) {
			mouth.speak("i said back off");
		}

		if (a.max == 600) {
			mouth
					.speak("impact is eminant. automatic override in progress. dammit where are my wheels.");
		}
		// mouth.speak("hello");
		// LOG.error("hello");
		// right.stopAndLock();
		// left.stopAndLock();
	}

	// TODO - Remote.export(camera) ....
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
/*		
		ServiceEntry client = sdu.serviceEntryList_.get(0); // should have 1 and
															// only 1 TODO -
															// kludge - fix me
		super.registerServices(sdu);

		ServiceEntry se;
		sdu.serviceEntryList_.clear();

		se = hostcfg.getFullServiceEntry("momo");
		se.localServiceHandle = null;
		sdu.serviceEntryList_.add(se);

		se = hostcfg.getFullServiceEntry("camera");
		se.localServiceHandle = null;
		sdu.serviceEntryList_.add(se);

		se = hostcfg.getFullServiceEntry("sensors");
		se.localServiceHandle = null;
		sdu.serviceEntryList_.add(se);

		se = hostcfg.getFullServiceEntry("arduino");
		se.localServiceHandle = null;
		sdu.serviceEntryList_.add(se);

		sendServiceDirectoryUpdate("", "", client.name, sdu.hostname,
				sdu.remoteServicePort, sdu);
*/				

	}

	@Override
	public String getToolTip() {
		return "behavioral service experiment";
	}
	
}
