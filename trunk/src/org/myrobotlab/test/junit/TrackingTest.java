package org.myrobotlab.test.junit;

import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.TrackingService;

public class TrackingTest {

	@Test
	public final void testOpenCV() {

		OpenCV opencv = new OpenCV("camera");
		Arduino arduino01 = new Arduino("board");
		Servo tilt = new Servo("tilt");
		Servo pan = new Servo("pan");
		TrackingService tracker = new TrackingService("tracker");

		GUIService gui = new GUIService("gooey");
		tilt.attach(arduino01.name, 6); // TODO - should have failed/thrown !!!
										// make bug Servo does not have a
										// analogWrite fn! out
		pan.attach(arduino01.name, 5); // TODO - allow gui to attach

		// opencv.notify("publish", audio.name, "play",
		// String.class.getCanonicalName());// THE COLOR TALKER
		// opencv.notify("publish", test.name, "IFound",
		// ArrayList.class.getCanonicalName());// THE COLOR TALKER

		opencv.send(opencv.name, "setUseInput", "camera");
		opencv.send(opencv.name, "capture");
		opencv.send(opencv.name, "addFilter", "tracker", "LKOpticalTrack");

		/*
		 * // TODO - addMsgListener ((in/out?)method, params); Object[] params =
		 * new Object[4]; params[0] = "publish"; params[1] = tracker.name;
		 * params[2] = "setOpticalTrackingPoints"; params[3] =
		 * CvPoint2D32f.class.getCanonicalName(); opencv.send(opencv.name,
		 * "notify", params);
		 */

		// TODO - addMsgListener ((in/out?)method, params);
		Object[] params = new Object[4];
		params[0] = "publish";
		params[1] = tracker.name;
		params[2] = "center";
		params[3] = CvPoint2D32f.class.getCanonicalName();
		opencv.send(opencv.name, "notify", params);

		tracker.notify("correctX", pan.name, "move", Integer.class);
		tracker.notify("correctY", tilt.name, "move", Integer.class);

		arduino01.startService();
		tilt.startService();
		pan.startService();
		tracker.startService();

		opencv.startService();
		gui.startService();
		gui.display();

		try {
			while (true) {
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
