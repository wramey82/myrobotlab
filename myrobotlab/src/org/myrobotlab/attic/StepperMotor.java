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

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class StepperMotor extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(StepperMotor.class
			.getCanonicalName());
	public final static String DIRECTION_CCW = "CCW"; // TODO - should this be
														// in config?
	public final static String DIRECTION_CW = "CW";

	// private StepperMotorConfig config;

	public StepperMotor(String name) {
		super(name, StepperMotor.class.getCanonicalName());
		// config.getName() = name; TODO WHY HURT YOURSELF?
	}

	public void loadDefaultConfiguration() {
	}

	public void goCW() {
		LOG.debug("goCW");
	}

	public void goCW(int step) {
		LOG.debug("goCW");
	}

	public void goCCW(int step) {
		LOG.debug("goCCW");
	}

	public void goCCW() {
		LOG.debug("goCCW");
	}

	public void stopMotor() {
		LOG.debug("stopMotor");
	}

	@Override
	public String getToolTip() {
		return "<html>stepper motor service (not implemented)</html>";
	}
	

}
