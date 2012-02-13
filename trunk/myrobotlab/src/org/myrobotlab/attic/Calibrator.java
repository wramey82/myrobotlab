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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;

import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class Calibrator extends Service {

	public final static Logger LOG = Logger.getLogger(Calibrator.class
			.getCanonicalName());
	private static final long serialVersionUID = 1L;

	String servoX = null;
	String servoY = null;
	String opencv = null;

	Point2D home = new Point2D(90, 70);

	int screenSizeX = 320; // pixels
	int screenSizeY = 240; // pixels

	CvPoint2D32f[] new_features = null;
	CvPoint2D32f[] current_features = null;
	CvPoint2D32f[] saved_features = null;

	// X Y img
	HashMap<Integer, HashMap<Integer, VisualNode>> memory = new HashMap<Integer, HashMap<Integer, VisualNode>>();

	public CvPoint2D32f[] copyArray(CvPoint2D32f[] in) {
		CvPoint2D32f[] ret = new CvPoint2D32f[in.length];
		for (int i = 0; i < in.length; ++i) {
			ret[i] = new CvPoint2D32f(in[i].x(), in[i].y());
		}

		return ret;
	}

	public class VisualNode {
		public IplImage image;
		public Date timestamp;
		public CvPoint2D32f[] registrationPoints;

		public VisualNode(IplImage image, Date timestamp,
				CvPoint2D32f[] registrationPoints) {
			this.image = image;
			this.timestamp = timestamp;
			this.registrationPoints = registrationPoints;
		}

	}

	public class Point2D {

		public int x = 0;
		public int y = 0;

		public Point2D(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public void loadImageToMemory(int x, int y, IplImage image, Date timestamp,
			CvPoint2D32f[] registrationPoints) {
		HashMap<Integer, VisualNode> mapy = null;
		if (!memory.containsKey(x)) {
			mapy = new HashMap<Integer, VisualNode>();
			memory.put(x, mapy);
		} else {
			mapy = memory.get(x);
		}

		mapy.put(y, new VisualNode(image, timestamp, registrationPoints));
		memory.put(x, mapy);
	}

	public Calibrator(String n) {
		super(n, Calibrator.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("interval", 1000);
	}

	public String setServoX(String name) {
		servoX = name;
		return servoX;
	}

	public String setServoY(String name) {
		servoY = name;
		return servoY;
	}

	public String setOpenCV(String name) {
		opencv = name;
		return opencv;
	}

	public void sleep(int millis) {
		try {

			Thread.sleep(millis); // let camera stabilize
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // wait for camera to stabalize

	}

	public void calibrate() {
		sleep(1000); // stabalize

		if (servoX == null || servoY == null || opencv == null) {
			LOG.error("servoX servoY & opencv need to be set to calibrate");
		}

		// auto configure
		// get list of servos from service cfg if not specified directly
		// get list of motors ?
		// get list of cameras - get resolutions (will invalidate lk tracking
		// point if too close to edge)

		// TODO - do NOT go home
		// send moveTo HOME command to servos
		send(servoX, "moveTo", home.x);
		send(servoY, "moveTo", home.y);
		sleep(200);
		// Thread.sleep(200); // let camera stabilize

		// ask sendBlocking last frame from camera - push into HOME "memory"
		// set camera on

		send(opencv, "setUseInput", "camera");
		send(opencv, "capture");
		send(opencv, "addFilter", "calibrator", "LKOpticalTrack");

		IplImage homeImage = null;
		while (homeImage == null) {
			homeImage = (IplImage) sendBlocking(opencv, "getLastFrame", null);
		}

		// TODO - addMsgListener
		Object[] params = new Object[4];
		params[0] = "publish";
		params[1] = this.getName();
		params[2] = "setOpticalTrackingPoints";
		params[3] = CvPoint2D32f.class.getCanonicalName();
		send(opencv, "notify", params);
		// set camera up for lk tracking

		// send notification request to publish data from lk tracking (wait)-
		// will affect global data
		// current_features = setOpticalTrackingPoints(null);
		new_features = null;
		while (new_features == null) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// saved_features = Arrays.copyOf(new_features, new_features.length);
		saved_features = copyArray(new_features);
		loadImageToMemory(home.x, home.y, homeImage, new Date(), saved_features);

		moveAndCompare(servoX, home.x);
		moveAndCompare(servoX, home.x + 3);
		moveAndCompare(servoY, home.y + 3);

		moveAndCompare(servoX, home.x);
		moveAndCompare(servoY, home.y);
		moveAndCompare(servoY, home.y + 5);

		// backplane
		// find current position
		// find offset
		// exclusive or plane - look for differences (new objects)

		// if threshold is high enough - ask
		// "What is this"
		// get input (voice) - text - email - web (google image) - google 3d
		// warehouse ??
		// serialize data/memory

	}

	/*
	 * setOpticalTrackingPoints is used to exchange data from opencv - which
	 * publishes a CvPoint2D32f array. We send it a message to notify us when it
	 * has such array. From our side it looks like we set it to null and it
	 * comes back filled
	 */
	public CvPoint2D32f[] setOpticalTrackingPoints(CvPoint2D32f[] features) {
		new_features = features;
		return new_features;
	}

	public void moveAndCompare(String servoName, Integer pos) {
		LOG.error("moving " + servoName + " " + pos);

		send(servoName, "moveTo", pos); // +1/-1

		// stabalize camera
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// current_features = setOpticalTrackingPoints (null);

		new_features = null;
		while (new_features == null) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// current_features = Arrays.copyOf(new_features, new_features.length);
		// wow - not a deep copy !
		current_features = copyArray(new_features);

		for (int i = 0; i < current_features.length; ++i) {
			CvPoint2D32f p0 = current_features[i];
			CvPoint2D32f p1 = saved_features[i];
			LOG.error((int) p0.x() + "," + (int) p0.y() + " " + (int) p1.x() + ","
					+ (int) p1.y() + " " + (int) (p0.x() - p1.x()) + " "
					+ (int) (p0.y() - p1.y()));
		}
		LOG.info("done");
		displayResults();
	}

	public SerializableImage publishFrame(String source, BufferedImage img) {
		SerializableImage si = new SerializableImage(img);
		si.source = source;
		return si;
	}

	public void displayResults() {
		IplImage displayImage = memory.get(home.x).get(home.y).image;

		Graphics2D graphics = null;
		BufferedImage frameBuffer = null;

		frameBuffer = displayImage.getBufferedImage(); // TODO - ran out of
														// memory here
		graphics = frameBuffer.createGraphics();

		for (int i = 0; i < current_features.length; ++i) {
			CvPoint2D32f p0 = current_features[i];
			CvPoint2D32f p1 = saved_features[i];

			graphics.setColor(Color.green);
			graphics.drawLine((int) p1.x(), (int) p1.y() + 1, (int) p1.x(),
					(int) p1.y() - 1);
			graphics.setColor(Color.red);
			graphics.drawLine((int) p0.x() - 1, (int) p0.y(), (int) p0.x() + 1,
					(int) p0.y());

			// LOG.error((int)p0.x + "," + (int)p0.y + " " + (int)p1.x + "," +
			// (int)p1.y + " " + (int)(p0.x - p1.x) + " " + (int)(p0.y - p1.y));
		}
		invoke("publishFrame", "cal", frameBuffer);
	}

	@Override
	public String getToolTip() {
		return "Calibrates PID";
	}

}
