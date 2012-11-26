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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.ServoController;

public class ServoGUI extends ServiceGUI implements ActionListener, MouseListener {

	public final static Logger log = Logger.getLogger(ServoGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	JLabel boundPos = null;

	AttachButton attachButton = null;
	JSlider slider = new JSlider(0, 180, 90);

	BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
	BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);

	JComboBox controller = new JComboBox();
	JComboBox pin = null;

	DefaultComboBoxModel controllerModel = new DefaultComboBoxModel();

	// TODO - sync initially by requesting entire Servo service object - can you
	// get cfg? that way?
	JTextField posMin = new JTextField("0");
	JTextField posMax = new JTextField("180");

	Servo myServo = null;
	
	private class SliderListener implements ChangeListener
	{
		public void stateChanged(javax.swing.event.ChangeEvent e) {
			boundPos.setText("" + slider.getValue());

			if (myService != null) {
				myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
			} else {
				log.error("can not send message myService is null");
			}
		}
	}
	
	SliderListener sliderListener = new SliderListener();

	public ServoGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		myServo = (Servo) Runtime.getServiceWrapper(boundServiceName).service;
	}

	public void init() {

		left.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setValue(slider.getValue() - 1);
			}
		});
		right.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider.setValue(slider.getValue() + 1);
			}
		});

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;

		input.add(slider, gc);
		slider.addChangeListener(sliderListener);

		++gc.gridx;
		input.add(new JLabel(" "));
		++gc.gridx;
		boundPos = new JLabel("90");

		input.add(boundPos, gc);

		gc.gridwidth = 2;
		gc.gridx = 1;
		++gc.gridy;
		input.add(left, gc);
		++gc.gridx;

		input.add(right, gc);
		++gc.gridx;

		gc.gridx = 0;
		++gc.gridy;

		JPanel control = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		attachButton = new AttachButton();
		attachButton.setText("attach");
		control.add(attachButton, gc);
		++gc.gridx;

		control.add(controller, gc);

		++gc.gridx;
		control.add(new JLabel("pin"), gc); // TODO build pin arrangement for
											// Arduino - getValidPinsForServo in
											// Interface

		// FIXME - controller selection - generates a getPins() which populates
		// pin JComboBox
		Vector<Integer> p = new Vector<Integer>();
		p.addElement(1);
		p.addElement(2);
		p.addElement(3);
		p.addElement(4);
		p.addElement(5);
		p.addElement(6);
		p.addElement(7);
		p.addElement(8);
		p.addElement(9);
		p.addElement(10);
		p.addElement(11);
		p.addElement(12);
		p.addElement(13);

		pin = new JComboBox(p);

		++gc.gridx;
		control.add(pin, gc);

		display.add(control);
		display.add(input);

		gc.gridx = 0;
		++gc.gridy;

		JPanel limits = new JPanel();
		limits.add(new UpdateLimits());
		limits.add(new JLabel("min "));
		limits.add(posMin);
		limits.add(new JLabel(" max "));
		limits.add(posMax);

		display.add(limits, gc);

		// http://stackoverflow.com/questions/6205433/jcombobox-focus-and-mouse-click-events-not-working
		// jComboBox1.getEditor().getEditorComponent().addMouseListener(...);
		// have to add mouse listener to the MetalComboButton embedded in the
		// JComboBox
		Component[] comps = controller.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].addMouseListener(this); // JComboBox composite listener -
												// have to get all the sub
												// components
			/*
			 * comps[i].addMouseListener(new MouseAdapter() { public void
			 * mouseClicked(MouseEvent me) { System.out.println("clicked"); }
			 * });
			 */
		}
		// controller.getEditor().getEditorComponent().addMouseListener(this);
		controller.addActionListener(this);
		controller.setModel(controllerModel);

		refreshControllers();
	}

	public void refreshControllers() {
		// FIXME - would newing? a new DefaultComboBoxModel be better?
		controllerModel.removeAllElements();
		Vector<String> v = Runtime.getServicesFromInterface(ServoController.class.getCanonicalName()); // FIXME
																										// -
																										// getLocalRelative
																										// to
																										// the
																										// Servo
		for (int i = 0; i < v.size(); ++i) {
			controllerModel.addElement(v.get(i));
		}

		controller.invalidate();
		// if isAttached() - select the correct one
	}
	
	
/*
	private JSlider getAnalogValue() {
		if (slider == null) {
			slider = new JSlider(0, 180, 90);
			slider.addChangeListener(new ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					boundPos.setText("" + slider.getValue());

					if (myService != null) {
						myService.send(boundServiceName, "moveTo", Integer.valueOf(slider.getValue()));
					} else {
						log.error("can not send message myService is null");
					}
				}
			});

		}
		return slider;
	}
*/
	private class AttachButton extends JButton implements ActionListener {
		private static final long serialVersionUID = 1L;

		public AttachButton() {
			super();
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (getText().equals("attach")) {
				setText("detach");
				myService.send(controller.getSelectedItem().toString(), "servoAttach", boundServiceName, pin.getSelectedItem());
			} else {
				setText("attach");
				myService.send(controller.getSelectedItem().toString(), "servoDetach", boundServiceName);
			}
		}
	}

	private class UpdateLimits extends JButton implements ActionListener {
		private static final long serialVersionUID = 1L;

		public UpdateLimits() {
			super();
			setText("update limits");
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			myService.send(boundServiceName, "setPositionMin", Integer.parseInt(posMin.getText()));
			myService.send(boundServiceName, "setPositionMax", Integer.parseInt(posMax.getText()));
		}
	}

	public void getState(final Servo servo) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				controller.setSelectedItem(servo.getControllerName());
				pin.setSelectedItem(servo.getPin());
				if (servo.isAttached()) {
					attachButton.setText("detach");
				} else {
					attachButton.setText("attach");
				}
				
				if (servo.getPosition() == null)
				{
					boundPos.setText("");
				} else {
					boundPos.setText(servo.getPosition().toString());
					slider.removeChangeListener(sliderListener);
					slider.setValue(servo.getPosition());
					slider.addChangeListener(sliderListener);
				}
				posMin.setText(servo.getPositionMin().toString());
				posMax.setText(servo.getPositionMax().toString());
			}
		});
	}

	public void attachGUI() {
		subscribe("publishState", "getState", Servo.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Servo.class);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == controller) {
			log.info("here");
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("clicked");

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("entered");

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("exited");

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("pressed");
		Object o = arg0.getSource();
		log.info(o);

		refreshControllers();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		log.info("released");
	}

}