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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public class RuntimeGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	
	Runtime myRuntime = null;
	
	public RuntimeGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;
        
        // reuse gc
        gc.gridx = 0;
        gc.gridy = 0;
         
        myRuntime = (Runtime)RuntimeEnvironment.getService(boundServiceName).service;
		
	}

	
	public void getState(Runtime c)
	{
	}


	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <- Class.forName(type)
	@Override
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Runtime.class); 
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", Runtime.class);
	}

}
