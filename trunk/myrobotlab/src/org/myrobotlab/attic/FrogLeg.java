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

package org.myrobotlab.attic;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.GeneticProgramming;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.SensorMonitor;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Speech;
import org.myrobotlab.service.data.Trigger;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

public class FrogLeg extends Service {

	public final static Logger log = LoggerFactory.getLogger(FrogLeg.class.getCanonicalName());
	private static final long serialVersionUID = 1L;

	public final static int IR_PIN = 1;

	RemoteAdapter remote = new RemoteAdapter("remote");
	Arduino arduino = new Arduino("arduino");
	GUIService gui = new GUIService("gui");
	// AudioFile mouth = new AudioFile("mouth");
	Speech mouth = new Speech("mouth");
	OpenCV camera = new OpenCV("camera");
	// Runtime Runtime = new Runtime("services");
	Servo hip = new Servo("hip");
	Servo knee = new Servo("knee");
	GeneticProgramming gp = new GeneticProgramming("gp");

	ArrayList<Rectangle> captureData = new ArrayList<Rectangle>();

	SensorMonitor sensors = new SensorMonitor("sensors");

	Timer timer = null;
	TimerTask timerTask = null;

	public FrogLeg(String n) {
		this(n, null);
	}

	public FrogLeg(String n, String serviceDomain) {
		super(n, FrogLeg.class.getCanonicalName(), serviceDomain);
	}

	public void startRobot() {

		// arduino.start();
		mouth.startService();
		camera.startService();
		gui.startService();
		hip.startService();
		hip.attach(arduino.getName(), 9);
		knee.startService();
		knee.attach(arduino.getName(), 10);
		gp.startService();

		gp.createGP();
		sensors.startService();

		mouth.getCFG().set("isATT", true);
		sensors.addTrigger(arduino.getName(), "200", 200, 200, Trigger.BOUNDRY, Trigger.STATE_LOW, IR_PIN);
		sensors.addTrigger(arduino.getName(), "300", 300, 300, Trigger.BOUNDRY, Trigger.STATE_LOW, IR_PIN);
		sensors.addTrigger(arduino.getName(), "400", 400, 400, Trigger.BOUNDRY, Trigger.STATE_LOW, IR_PIN);
		sensors.addTrigger(arduino.getName(), "500", 500, 500, Trigger.BOUNDRY, Trigger.STATE_LOW, IR_PIN);
		sensors.addTrigger(arduino.getName(), "600", 600, 600, Trigger.BOUNDRY, Trigger.STATE_LOW, IR_PIN);
		sensors.addListener("publish", this.getName(), "publish", Trigger.class);
		camera.addListener("publish", gp.getName(), "evalCallBack", Rectangle.class);

		// 320 x 240 is easier to work with over wireless
		// camera.addFilter("PyramidDown", "PyramidDown");

		remote.startService();
		gui.display();

		boolean goForever = true;
		while (goForever) // TODO - randome behavior - explore - map - seek
							// power
		{
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// TODO - do in Service
	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		FrogLeg frogleg = new FrogLeg("frogleg");
		frogleg.startService();
		frogleg.startRobot();
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
		camera.setInputSource("camera");
		camera.capture();
	}

	public void cameraOff() {
		camera.setInputSource("null");
		camera.capture();
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
		// camera.removeListener(outMethod, serviceName, inMethod, paramType);
		// //
		// TODO - make removable by query best
		camera.removeListener("publish", this.getName(), "trackPoint", CvPoint2D32f[].class);

		// load the movement filter
		movementFilter();

		// set trigger
		camera.addListener("publish", this.getName(), "foundMovement", Rectangle.class);
	}

	boolean tracking = false;

	public void foundMovement(Rectangle rect) {
		// if (!tracking)
		{
			// remove movement trigger
			camera.removeListener("publish", this.getName(), "foundMovement", Rectangle.class);

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
			camera.invokeFilterMethod("LKOpticalTrack", "clearPoints", (Object[]) null);
			camera.addListener("publish", this.getName(), "trackPoint", CvPoint2D32f[].class);

			// add a LK point in the center of the motion
			Point p = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
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
		log.error("{}",points[0]);
	}

	public void samplePoints(CvPoint2D32f[] points) {
		log.warn("{}",points[0]);
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

	public void keyCommand(String cmd) {
		if (cmd.compareTo("Up") == 0) {
			// Integer pos = Integer.parseInt(cmd);
			hip.invoke("move", 10);
		} else if (cmd.compareTo("Down") == 0) {
			hip.invoke("move", -10);
		} else if (cmd.compareTo("S") == 0) {
			saveCapture();
		} else if (cmd.compareTo("C") == 0) {
			startCapture();
		} else if (cmd.compareTo("K") == 0) {
			stopCapture();
		}

	}

	public void mousePoints(ArrayList<CvPoint> points) {
		// b = tan(54.7 + # of pixels / 5.6) * 17
		HashMap<String, CvPoint> unique = new HashMap<String, CvPoint>();
		double Y = 0.0;
		double X = 0.0;

		String ret = "\n";

		log.error("{}",points.size());
		for (int i = 0; i < points.size(); ++i) {
			CvPoint p = points.get(i);
			if (p.x() != 0 && p.x() != 319 && p.y() != 0 && p.y() != 239) {
				Y = Math.tan(0.01745 * (54.7 + (240 - p.y()) / 5.6)) * 17;
				X = Math.sqrt(Y * Y + 17 * 17) * Math.sin(0.01745 * -1 * (160 - p.x()) / 5.6);
				if (!unique.containsKey((int) X + "," + (int) Y)) {
					unique.put((int) X + "," + (int) Y, p);
					ret += "Sketchup.active_model.entities.add_line [" + (int) X + ", " + (int) Y + ", " + 0 + "], [" + (int) X + ", " + (int) Y + ", " + 8 + "]\n";
				}
				// Log.error(p.x + "," + (int)z + "," + p.y);
				// Math.sqrt(a)
			}
		}

		camera.removeFilter("Mouse");
		camera.removeListener("publish", this.getName(), "mousePoints", CvPoint2D32f[].class);

	}

	public void capture(Rectangle r) {
		captureData.add(new Rectangle(r));
	}

	int cnt;

	public void saveCapture() {
		++cnt;
		FileWriter outfile;
		try {
			outfile = new FileWriter("capture." + cnt + ".txt");
			PrintWriter out = new PrintWriter(outfile);
			for (int i = 0; i < captureData.size(); ++i) {
				Rectangle r = captureData.get(i);
				double x = r.x + r.width / 2;
				double y = r.y + r.height / 2;
				x = x / 320;
				y = -1 * (y / 120 - 1);
				out.write(x + " " + y + "\n");
			}

			out.close();
			outfile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void startCapture() {
		captureData.clear();
		camera.addListener("publish", this.getName(), "capture", Rectangle.class);
	}

	public void stopCapture() {
		camera.removeListener("publish", this.getName(), "capture", Rectangle.class);
	}

	@Override
	public String getDescription() {
		return "experiment in genetic programming";
	}

}
