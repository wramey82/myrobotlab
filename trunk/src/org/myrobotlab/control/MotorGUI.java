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
import java.awt.event.ItemEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.myrobotlab.service.GUIService;

public class MotorGUI extends ServiceGUI {

	private static final long serialVersionUID = 1L;
	private JToggleButton cw = null;
	private JToggleButton stop = null;
	private JToggleButton ccw = null;
	private JSlider speed = null;
	private JLabel posValue = new JLabel("0");
	private JLabel speedValue = new JLabel("0");

	public MotorGUI(String boundServiceName, GUIService myService) {
		super(boundServiceName, myService);

		// build input begin ------------------
		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		// row 1
		gc.gridx = 0;
		gc.gridy = 0;
		input.add(getCcw(), gc);
		++gc.gridx;
		input.add(getStop(), gc);
		++gc.gridx;
		input.add(getCw(), gc);

		// row 2
		gc.gridx = 0;
		++gc.gridy;
		input.add(new JLabel("speed"), gc);

		// row 3
		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 3;
		input.add(getSpeed(), gc);
		gc.gridx = 4;
		input.add(speedValue, gc);

		// row 4
		gc.gridwidth = 1;
		gc.gridx = 0;
		++gc.gridy;
		input.add(new JLabel("pos"), gc);
		++gc.gridx;
		input.add(getPosValue(), gc);

		/*
		 * input.add(getNameValue()); input.add(new JLabel("position "));
		 * 
		 * input.add(getPosValue()); input.add(getSpeedLabel());
		 * input.add(speedValue);
		 */
		display.add(input);

		// build input end ------------------

	}

	/**
	 * This method initializes cw
	 * 
	 * @return javax.swing.JToggleButton
	 */
	private JToggleButton getCw() {
		if (cw == null) {
			cw = new JToggleButton();
			cw.setText(">>");
			cw.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					LOG.info("getCw changed state");
					int state = e.getStateChange();
					if (state == ItemEvent.SELECTED) {
						System.out.println("CW ON");
						if (ccw.isSelected()) {
							ccw.setSelected(false);
						}
						if (stop.isSelected()) {
							stop.setSelected(false);
						}
					} else {
						System.out.println("CW OFF");
					}

				}
			});
		}
		return cw;
	}

	/**
	 * This method initializes stop
	 * 
	 * @return javax.swing.JToggleButton
	 */
	private JToggleButton getStop() {
		if (stop == null) {
			stop = new JToggleButton();
			stop.setText("Stop");
			stop.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					System.out.println("itemStateChanged()"); // TODO
																// Auto-generated
																// Event stub
																// itemStateChanged()
					int state = e.getStateChange();
					if (state == ItemEvent.SELECTED) {
						System.out.println("STOP ON");
						if (ccw.isSelected()) {
							ccw.setSelected(false);
						}
						if (cw.isSelected()) {
							cw.setSelected(false);
						}
						stop.setSelected(false);
					} else {
						System.out.println("STOP OFF");
					}

				}
			});
		}
		return stop;
	}

	/**
	 * This method initializes ccw
	 * 
	 * @return javax.swing.JToggleButton
	 */
	private JToggleButton getCcw() {
		if (ccw == null) {
			ccw = new JToggleButton();
			ccw.setText("<<");
			ccw.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					int state = e.getStateChange();
					if (state == ItemEvent.SELECTED) {
						System.out.println("CCW ON");
						if (cw.isSelected()) {
							cw.setSelected(false);
						}
						if (stop.isSelected()) {
							stop.setSelected(false);
						}
					} else {
						System.out.println("CCW OFF");
					}

				}
			});
		}
		return ccw;
	}

	/**
	 * This method initializes speed
	 * 
	 * @return javax.swing.JSlider
	 */
	private JSlider getSpeed() {
		if (speed == null) {
			speed = new JSlider(0, 100, 0);
			speed.addChangeListener(new javax.swing.event.ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					speedValue.setText(speed.getValue() + "%");
					// thrower.invoke("throwInteger",new
					// Integer(speed.getValue()));
					// myService.send("logger01", "info", speed.getValue());
					// System.out.println("stateChanged()" + speed.getValue());
					// // TODO Auto-generated Event stub stateChanged()
				}
			});
		}
		return speed;
	}

	/**
	 * This method initializes posValue
	 * 
	 * @return javax.swing.JLabel
	 */
	private JLabel getPosValue() {
		if (posValue == null) {
			posValue = new JLabel();
			posValue.setText("0");
		}
		return posValue;
	}

	public void incrementPosition(Integer pos) {
		posValue.setText("" + pos);
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		sendNotifyRequest("incrementPosition", "incrementPosition", Integer.class);
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		removeNotifyRequest("incrementPosition", "incrementPosition", Integer.class);

	}

}
