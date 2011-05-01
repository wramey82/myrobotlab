package org.myrobotlab.test.junit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.JoystickService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.RecorderPlayer;
import org.myrobotlab.service.Servo;

public class ServoTest {

	@Test
	public final void testServo() {

		Servo tilt = new Servo("tilt");
		Arduino arduino01 = new Arduino("board");
		Servo pan = new Servo("pan");
		GUIService gui = new GUIService("gooey");
		RecorderPlayer recorder = new RecorderPlayer("recorder");
		JoystickService joystick = new JoystickService("joy");
		OpenCV opencv = new OpenCV("open");

		opencv.startService();
		recorder.startService();
		joystick.startService();
		arduino01.startService();
		tilt.startService();
		pan.startService();
		gui.startService();

		tilt.attach(arduino01.name, 6); // TODO - should have failed/thrown !!!
										// make bug Servo does not have a
										// analogWrite fn! out
		pan.attach(arduino01.name, 5); // TODO - allow gui to attach

		joystick.notify("getAxisR", tilt.name, "moveTo", Integer.class);
		joystick.notify("getAxisZ", pan.name, "moveTo", Integer.class);
		joystick.notify("getAxisR", recorder.name, "moveTo", Integer.class);

		/*
		 * //joystick.notify("digitalWrite", "arduino01", "digitalWrite",
		 * IOData.class.getCanonicalName()); joystick.notify("getAxisR",
		 * tilt.name, "moveTo", Integer.class.getCanonicalName());
		 * joystick.notify("getAxisZ", pan.name, "moveTo",
		 * Integer.class.getCanonicalName()); invoker.start(); joystick.start();
		 */

		// opencv.notify("publish", audio.name, "play",
		// String.class.getCanonicalName());// THE COLOR TALKER

		gui.display();

		// cal.calibrate();

		// pan.sweep();
		// tilt.sweep(sweepStart, sweepEnd, sweepDelayMS, sweepIncrement)

		/*
		 * tilt.attach(arduino01.name, 6); pan.attach(arduino01.name, 5);
		 */
		/*
		 * speech.speak("perhaps you would like some music");
		 * audio.play("/home/gperry/Desktop/jamesBond/dr-no.mp3");
		 * speech.speak("i like this music, its one of my favorites");
		 * audio.play("/home/gperry/Desktop/jamesBond/james-bond.mp3");
		 */

		try {
			while (true) {
				Thread.sleep(1000);
				recorder.play();
				recorder.saveAs("tilt.msgs");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
