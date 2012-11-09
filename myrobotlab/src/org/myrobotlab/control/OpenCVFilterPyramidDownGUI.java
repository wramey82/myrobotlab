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


package org.myrobotlab.control;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV.FilterWrapper;

public class OpenCVFilterPyramidDownGUI extends OpenCVFilterGUI {

	public OpenCVFilterPyramidDownGUI(String boundFilterName,
			String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

	}

	// @Override
	public void attachGUI() {
		log.debug("attachGUI");

	}

	// @Override
	public void detachGUI() {
		log.debug("detachGUI");

	}

	public JPanel getDisplay() {
		log.debug("display");
		return display;

	}

	public void apply() {
		log.debug("apply");

	}

	@Override
	public void setFilterData(FilterWrapper filter) {
		// TODO Auto-generated method stub
		
	}

}
