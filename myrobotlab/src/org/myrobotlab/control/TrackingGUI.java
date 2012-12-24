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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.myrobotlab.service.Tracking;
import org.myrobotlab.service.interfaces.GUI;

public class TrackingGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	JLabel cnt = new JLabel("0");
	JLabel latency = new JLabel("0");

	public TrackingGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() 
	{
		JPanel p = new JPanel();
		p.add(new JLabel("cnt "));
		p.add(cnt);
		p.add(new JLabel("latency "));
		p.add(latency);
		display.add(p);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Tracking.class);
	}

	@Override
	public void detachGUI() {
		subscribe("publishState", "getState", Tracking.class);
	}
	
	public void getState(final Tracking tracker) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {			
				
				cnt.setText(String.format("%d ",tracker.cnt));
				latency.setText(String.format("%d ms",tracker.latency));
			}
		});
	}

}
