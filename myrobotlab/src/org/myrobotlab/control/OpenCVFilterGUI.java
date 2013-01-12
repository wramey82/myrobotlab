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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.opencv.OpenCVFilter;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.interfaces.GUI;

public abstract class OpenCVFilterGUI {
	public final static Logger log = Logger.getLogger(OpenCVFilterGUI.class.getCanonicalName());

	final String name;
	JPanel display = new JPanel(new GridBagLayout());
	final String boundServiceName;
	final GUI myGUI;
	final public GridBagConstraints gc = new GridBagConstraints();
	
	FilterWrapper boundFilter = null;


	public OpenCVFilterGUI(String boundFilterName, String boundServiceName, GUIService myGUI) {
		name = boundFilterName;
		this.boundServiceName = boundServiceName;
		this.myGUI = myGUI;

		TitledBorder title;
		title = BorderFactory.createTitledBorder(name);
		display.setBorder(title);
	}
	
	/**
	 * @param filter - initialize
	 */
	public void initFilterState(OpenCVFilter filter)
	{
		boundFilter = new FilterWrapper(name, filter);
	}
	
	public void setFilterState(OpenCVFilter filter)
	{
		myGUI.send(boundServiceName, "setFilterState", new FilterWrapper(name, filter));
	}

	public abstract void attachGUI();

	public abstract void detachGUI();

	public abstract void getFilterState(final FilterWrapper filterWrapper);

	public JPanel getDisplay() {
		return display;
	}

	public String toString() {
		return name;
	}

}
