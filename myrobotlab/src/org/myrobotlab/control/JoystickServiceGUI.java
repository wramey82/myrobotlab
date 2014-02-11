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
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.myrobotlab.service.GUIService;

public class JoystickServiceGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	private JComboBox joystickIndex = null;
	private JTextField rMultiplier = null;
	private JTextField zMultiplier = null;
	private JTextField rOffset = null;
	private JTextField zOffset = null;

	public JoystickServiceGUI(final String boundServiceName, final GUIService myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		/* TODO - getCFG(boundService, name) */

		// joystick # begin ----------------
		Vector<Integer> idx = new Vector<Integer>();
		// joystickIndex.addElement("0");
		idx.addElement(1); // TODO - getCFG(boundService) - even on service -
							// JoystickService.setIndex(newIndex) which changes
							// config too
		idx.addElement(2);
		idx.addElement(3);
		idx.addElement(4);

		joystickIndex = new JComboBox(idx);

		// axis begin ----------------
		JPanel axis = new JPanel();
		axis.setLayout(new GridBagLayout());
		// GridBagConstraints inputgc = new GridBagConstraints();

		TitledBorder title;
		title = BorderFactory.createTitledBorder("axis multiplier/offset");
		axis.setBorder(title);

		// TODO - configuration getCFG(boundServiceName, name) - block?
		rMultiplier = new JTextField(5);
		rMultiplier.setHorizontalAlignment(JTextField.RIGHT);
		rMultiplier.setText("90");

		zMultiplier = new JTextField(5);
		zMultiplier.setHorizontalAlignment(JTextField.RIGHT);
		zMultiplier.setText("90");

		rOffset = new JTextField(5);
		rOffset.setHorizontalAlignment(JTextField.RIGHT);
		rOffset.setText("0");

		zOffset = new JTextField(5);
		zOffset.setHorizontalAlignment(JTextField.RIGHT);
		zOffset.setText("0");

		axis.add(new JLabel("R Axis"));
		axis.add(rMultiplier);
		axis.add(rOffset);

		axis.add(new JLabel("Z Axis"));
		axis.add(zMultiplier);
		axis.add(zOffset);

		display.add(joystickIndex);
		display.add(axis);

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
