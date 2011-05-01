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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.myrobotlab.comm.CommunicationManager;
import org.myrobotlab.service.GUIService;

public class Network extends JPanel {

	static final long serialVersionUID = 1L;

	protected GUIService myService;
	CommunicationManager comm = null;

	JTextField loginValue = new JTextField("");

	JTextField loginPasswordValue = new JPasswordField("blahblah");

	JTextField hostnameValue = new JTextField("", 15);

	JTextField servicePortValue = new JTextField("");

	public Network(GUIService s) {
		super();
		this.myService = s;
		initialize();
	}

	void initialize() {

		GridBagConstraints gc = new GridBagConstraints();
		gc.anchor = GridBagConstraints.WEST;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.ipadx = 5;
		// gc.gridwidth = 6;

		hostnameValue.setText(myService.getCFG("hostname"));
		servicePortValue.setText(myService.getCFG("servicePort"));

		this.setSize(300, 200);
		this.setLayout(new GridBagLayout());

		// TODO - make a way of listing connections - Connection manager et al

		comm = myService.getOutbox().getCommunicationManager(); // TODO RENAME
																// THIS WRONG
																// NAME

		gc.gridx = 0;
		++gc.gridy;
		this.add(new JLabel("service name:"), gc);
		++gc.gridx;
		this.add(hostnameValue, gc);

		gc.gridx = 0;
		++gc.gridy;
		this.add(new JLabel("service port:"), gc);
		++gc.gridx;
		this.add(servicePortValue, gc);

		gc.gridx = 0;
		++gc.gridy;
		this.add(new JLabel("login:"), gc);
		++gc.gridx;
		this.add(loginValue, gc);

		gc.gridx = 0;
		++gc.gridy;
		this.add(new JLabel("password:"), gc);
		++gc.gridx;
		this.add(loginPasswordValue, gc);

		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 2;
		JButton connectButton = new JButton("connect");
		connectButton.setActionCommand("connect");
		connectButton.addActionListener(new connect());
		this.add(connectButton, gc);

		gc.gridx = 0;
		++gc.gridy;
		this.add(myService.remoteStatus, gc);

	}

	public String setRemoteConnectionStatus(String state) {
		myService.remoteStatus.setText(state);
		return state;
	}

	// TODO - FIX THIS

	class connect implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			int port = Integer.parseInt(servicePortValue.getText());
			String t = loginValue.getText();
			myService.sendServiceDirectoryUpdate(loginValue.getText(),
					loginPasswordValue.getText(), "frogleg", hostnameValue
							.getText(), port); // TODO FIX THIS !!!
		}

	}

}
