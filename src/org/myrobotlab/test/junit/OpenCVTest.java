package org.myrobotlab.test.junit;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.ColoredThingyFinder;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.SpeechRecognition;

public class OpenCVTest {

	@Test
	public final void testLoadDefaultConfiguration() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStop() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testStart() {
		// fail("Not yet implemented");
	}

	@Test
	public final void testOpenCV() {

		OpenCV opencv = new OpenCV("camera");

		Arduino arduino01 = new Arduino("board");
		Servo s1 = new Servo("tilt");
		Servo s2 = new Servo("pan");
		AudioFile audio = new AudioFile("audio");
		SpeechRecognition sr = new SpeechRecognition("recognition");
		ColoredThingyFinder test = new ColoredThingyFinder("test");
		/*
		 * 
		 * Calibrator cal = new Calibrator("cal"); cal.setServoX(s2.name);
		 * cal.setServoY(s1.name); cal.setOpenCV(opencv.name);
		 * 
		 * Invoker invoker = new Invoker("invoker"); JoystickService joystick =
		 * new JoystickService("joystick"); Speech speech = new
		 * Speech("speech");
		 */

		GUIService gui = new GUIService("gooey");
		s1.attach(arduino01.name, 6); // TODO - should have failed/thrown !!!
										// make bug Servo does not have a
										// analogWrite fn! out
		s2.attach(arduino01.name, 5); // TODO - allow gui to attach

		/*
		 * //joystick.notify("digitalWrite", "arduino01", "digitalWrite",
		 * IOData.class.getCanonicalName()); joystick.notify("getAxisR",
		 * s1.name, "moveTo", Integer.class.getCanonicalName());
		 * joystick.notify("getAxisZ", s2.name, "moveTo",
		 * Integer.class.getCanonicalName()); invoker.start(); joystick.start();
		 * cal.start();
		 */

		sr.startService();
		// opencv.notify("publish", audio.name, "play",
		// String.class.getCanonicalName());// THE COLOR TALKER
		opencv.notify("publish", test.name, "IFound", ArrayList.class);// THE COLOR TALKER
		test.notify("say", audio.name, "play", String.class);// THE
																				// COLOR
																				// TALKER

		audio.startService();
		arduino01.startService();
		s1.startService();
		s2.startService();

		opencv.startService();
		gui.startService();

		audio.play("green");

		test.setServoX(s2.name);
		test.setServoY(s1.name);
		test.setOpenCV(opencv.name);

		test.startService();
		test.sayWhatYouFound();

		gui.display();

		// cal.calibrate();

		// s2.sweep();
		// s1.sweep(sweepStart, sweepEnd, sweepDelayMS, sweepIncrement)

		/*
		 * s1.attach(arduino01.name, 6); s2.attach(arduino01.name, 5);
		 */
		/*
		 * speech.speak("perhaps you would like some music");
		 * audio.play("/home/gperry/Desktop/jamesBond/dr-no.mp3");
		 * speech.speak("i like this music, its one of my favorites");
		 * audio.play("/home/gperry/Desktop/jamesBond/james-bond.mp3");
		 */

		try {
			while (true) {
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
