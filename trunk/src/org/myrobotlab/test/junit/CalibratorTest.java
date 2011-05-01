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

package org.myrobotlab.test.junit;

import org.junit.Test;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Calibrator;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Servo;

public class CalibratorTest {

	@Test
	public final void testServo() {

		OpenCV camera = new OpenCV("camera");
		Calibrator cal = new Calibrator("cal");
		Servo tilt = new Servo("tilt");
		Arduino arduino01 = new Arduino("board");
		Servo pan = new Servo("pan");

		GUIService gui = new GUIService("gooey");

		arduino01.startService();
		tilt.startService();
		pan.startService();
		gui.startService();
		camera.startService();
		cal.startService();

		tilt.attach(arduino01.name, 6); // TODO - should have failed/thrown !!!
										// make bug Servo does not have a
										// analogWrite fn! out
		pan.attach(arduino01.name, 5); // TODO - allow gui to attach

		gui.display();

		cal.setServoX(pan.name);
		cal.setServoY(tilt.name);
		cal.setOpenCV(camera.name);
		cal.calibrate();

		try {
			while (true) {
				Thread.sleep(10000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
