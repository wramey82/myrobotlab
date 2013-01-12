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
import javax.swing.JTextField;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterMatchTemplateGUI extends OpenCVFilterGUI {

	JButton button = new JButton("smooth me");
	JTextField kernel = new JTextField("3");

	public OpenCVFilterMatchTemplateGUI(String boundFilterName, String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);
		display.add(button);
		display.add(kernel);
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getFilterState(FilterWrapper filterWrapper) {
		// TODO Auto-generated method stub
		
	}

}
