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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

import com.centralnexus.input.JoystickListener;

public class Joystick extends Service implements JoystickListener {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(Joystick.class
			.getCanonicalName());
	public com.centralnexus.input.Joystick joy;
	public final int MAX_AXES = 6;

	public final int R = 0;
	public final int U = 1;
	public final int V = 2;
	public final int X = 3;
	public final int Y = 4;
	public final int Z = 5;

	Axis axes[] = new Axis[MAX_AXES];

	public class Axis {
		public float currentValue = 0;
		public float mappedMin = 0;
		public float mappedMax = 180;

		boolean intOutput = false;

	}

	public Joystick(String n) {
		super(n, Joystick.class.getCanonicalName());

		try {
			LOG
					.info("number of joysticks reported "
							+ com.centralnexus.input.Joystick.getNumDevices());
			joy = com.centralnexus.input.Joystick.createInstance(0);
			/*
			 * for (int idx = joy.getID() + 1; idx < Joystick.getNumDevices();
			 * idx++) { //if (Joystick.isPluggedIn(idx)) { // joy2 =
			 * Joystick.createInstance(idx); //} } if (joy2 == null) { joy2 =
			 * joy; }
			 */

			for (int i = 0; i < MAX_AXES; ++i) {
				axes[i] = new Axis();
			}

			joy.addJoystickListener(this);

		} catch (IOException e) {
			LOG.error("joystick not found");
		} catch (Exception e) {
			LOG.error("joystick not found" + e.getMessage());
		} catch (UnsatisfiedLinkError e) {
			LOG.error("joystick binaries (.dll or .so) needed but not found"
					+ e.getMessage());
		}

	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("zMultiplier", 90);
		cfg.set("zOffset", 90);
		cfg.set("rMultiplier", 90);
		cfg.set("rOffset", 90);
	}

	// @Override - only in Java 1.6 - its only a single reference not all supertypes define it
	public void joystickAxisChanged(com.centralnexus.input.Joystick j) {
		LOG.info(" axis jid " + j.getID() + " dz " + j.getDeadZone() + " r "
				+ j.getR() + " u " + j.getU() + " v " + j.getV() + " x "
				+ j.getX() + " y " + j.getY() + " z " + j.getZ());

		float retval = 0;

		if (j.getZ() != axes[Z].currentValue) {
			axes[Z].currentValue = j.getZ();
			retval = cfg.getInt("zMultiplier") * axes[Z].currentValue
					+ cfg.getInt("zOffset");
			invoke("getAxisZ", (int) retval);
		}

		if (j.getR() != axes[R].currentValue) {
			axes[R].currentValue = j.getR();
			retval = cfg.getInt("rMultiplier") * axes[R].currentValue
					+ cfg.getInt("rOffset");
			invoke("getAxisR", (int) retval);
		}

	}

	/*
	 * TODO - make this an interface????? Split out for late binding and message
	 * routing
	 */

	final static public void button1() {
		LOG.info("button1");
	}

	final static public void button2() {
		LOG.info("button2");
	}

	final static public void button3() {
		LOG.info("button3");
	}

	final static public void button4() {
		LOG.info("button4");
	}

	final static public int getAxisR(Integer f) {
		LOG.info("getAxisR int " + f);
		return f;
	}

	final static public int getAxisZ(Integer f) {
		LOG.info("getAxisZ int " + f);
		return f;
	}

	// @Override - only in Java 1.6 - its only a single reference not all supertypes define it
	public void joystickButtonChanged(com.centralnexus.input.Joystick j) {

		int buttonsPressed = j.getButtons();

		if ((buttonsPressed & com.centralnexus.input.Joystick.BUTTON1) == com.centralnexus.input.Joystick.BUTTON1) {
			invoke("button1");
		}

		if ((buttonsPressed & com.centralnexus.input.Joystick.BUTTON2) == com.centralnexus.input.Joystick.BUTTON2) {
			invoke("button2");
		}

		if ((buttonsPressed & com.centralnexus.input.Joystick.BUTTON3) == com.centralnexus.input.Joystick.BUTTON3) {
			invoke("button3");
		}

		if ((buttonsPressed & com.centralnexus.input.Joystick.BUTTON4) == com.centralnexus.input.Joystick.BUTTON4) {
			invoke("button4");
		}
	}

	@Override
	public String getToolTip() {
		return "used for interfacing with a Joystick";
	}

}
