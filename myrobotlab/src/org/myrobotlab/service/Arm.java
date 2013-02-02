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

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Arm extends Service {

	public final static Logger log = Logger.getLogger(Arm.class.getCanonicalName());

	private static final long serialVersionUID = 1L;
	public final static int IR_PIN = 1;

	Servo shoulder = new Servo("shoulder");
	Servo elbow = new Servo("elbow");
	Servo wrist = new Servo("wrist");
	Servo hand = new Servo("hand");

	int armLength = 0;
	int formArmLength = 0;

	public Arm(String n) {
		super(n, Arm.class.getCanonicalName());
	}



	public void startRobot() {
	}

	// TODO - do in Service
	public static void main(String[] args) {

		Arm arm = new Arm("arm");
		arm.startService();
		arm.startRobot();

	}

	@Override
	public String getToolTip() {
		// TODO Auto-generated method stub
		return null;
	}

}