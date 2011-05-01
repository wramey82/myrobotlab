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

package org.myrobotlab.test;

import java.io.IOException;

import org.apache.log4j.Logger;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.Servo;

public class QuickTest {

	public final static Logger LOG = Logger.getRootLogger();

	public static void main(String[] args) throws IOException,
			InterruptedException {
		OpenCV opencv = new OpenCV("camera");
		GUIService gui = new GUIService("gui");
		Arduino board = new Arduino("arduino");
		Servo pan = new Servo("pan");
		Servo tilt = new Servo("tilt");

		tilt.attach(board.name, 6); // TODO - should have failed/thrown !!! make
									// bug Servo does not have a analogWrite fn!
									// out
		pan.attach(board.name, 5); // TODO - allow gui to attach

		pan.startService();
		tilt.startService();
		board.startService();
		opencv.startService();

		/*
		 * TODO - bug - did not work in gui pan.attach(5); tilt.attach(6);
		 */

		gui.startService();
		gui.display();
	}

}
