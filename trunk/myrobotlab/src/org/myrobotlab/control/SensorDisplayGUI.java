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

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.myrobotlab.control.widget.RadarWidget;
import org.myrobotlab.service.GUIService;

public class SensorDisplayGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	public SensorDisplayGUI(final String boundServiceName, final GUIService myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		RadarWidget radar = new RadarWidget();
		// radar.show();
		new Thread(radar).start();
		JLabel screen = new JLabel();
		screen.setSize(320, 240);

		display.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.ipady = 160;
		gc.ipadx = 160;

		display.add(radar, gc);

	}

	// TODO - normalize - this fn is copy pasted
	protected ImageIcon createImageIcon(String path, String description) {
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL, description);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}

}
