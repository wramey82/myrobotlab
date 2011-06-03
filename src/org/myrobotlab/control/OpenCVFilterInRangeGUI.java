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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.image.OpenCVFilter;
import org.myrobotlab.service.GUIService;

public class OpenCVFilterInRangeGUI extends OpenCVFilterGUI {

	// TODO - look into binding with CFG - although will only work locally

	
	JCheckBox useHue = new JCheckBox();
	JSlider2 hueMin = new JSlider2(JSlider.VERTICAL, 0, 256, 0);
	JSlider2 hueMax = new JSlider2(JSlider.VERTICAL, 0, 256, 256);
	
	JCheckBox useSaturation = new JCheckBox();
	JSlider2 satMin = new JSlider2(JSlider.VERTICAL, 0, 256, 0);
	JSlider2 satMax = new JSlider2(JSlider.VERTICAL, 0, 256, 256);

	JCheckBox useValue = new JCheckBox();
	JSlider2 valMin = new JSlider2(JSlider.VERTICAL, 0, 256, 0);
	JSlider2 valMax = new JSlider2(JSlider.VERTICAL, 0, 256, 256);
	
	public class JSlider2 extends JSlider
	{
		private static final long serialVersionUID = 1L;
		JLabel value = new JLabel();

		public JSlider2(int vertical, int i, int j, int k) {
			super (vertical, i, j, k);
			value.setText("" + k);
		}
				
	}
	
	
	public class AdjustSlider implements ChangeListener
	{

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider2 slider = (JSlider2) e.getSource();
			Object[] params = new Object[3];
			params[0] = name;
			params[1] = slider.getName();
			params[2] = slider.getValue();
			myService.send(boundServiceName, "setFilterCFG", params);
			slider.value.setText("" + slider.getValue());
		}
	}

	public class AdjustCheckBox implements ChangeListener
	{

		@Override
		public void stateChanged(ChangeEvent e) {
			JCheckBox t = (JCheckBox) e.getSource();
			Object[] params = new Object[3];
			params[0] = name;
			params[1] = t.getName();
			if (t.getModel().isSelected())
			{
				params[2] = true;
			} else {
				params[2] = false;				
			}
			myService.send(boundServiceName, "setFilterCFG", params);
		}
	}
	
	AdjustSlider change = new AdjustSlider();
	AdjustCheckBox checkBoxChange = new AdjustCheckBox();
	
	public OpenCVFilterInRangeGUI(String boundFilterName,
			String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

		hueMin.setName("hueMin"); 
		hueMax.setName("hueMax");
		satMin.setName("saturationMin");
		satMax.setName("saturationMax");
		valMin.setName("valueMin");
		valMax.setName("valueMax");
		useHue.setName("useHue");
		useSaturation.setName("useSaturation");
		useValue.setName("useValue");
		
		hueMin.addChangeListener(change);
		hueMax.addChangeListener(change);
		valMin.addChangeListener(change);
		valMax.addChangeListener(change);
		satMin.addChangeListener(change);
		satMax.addChangeListener(change);
		
		useHue.addChangeListener(checkBoxChange);
		useSaturation.addChangeListener(checkBoxChange);
		useValue.addChangeListener(checkBoxChange);
		
		
		TitledBorder title;
		JPanel j = new JPanel(new GridBagLayout());
		title = BorderFactory.createTitledBorder("hue");
		j.setBorder(title);
		gc.gridx = 0;
		gc.gridy = 0;
		j.add(new JLabel("enable"), gc);
		++gc.gridx;
		j.add(useHue, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(new JLabel("  min max"), gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(hueMin, gc);
		++gc.gridx;
		j.add(hueMax, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(hueMin.value, gc);
		++gc.gridx;
		j.add(hueMax.value, gc);
		display.add(j);

		j = new JPanel(new GridBagLayout());
		title = BorderFactory.createTitledBorder("saturation");
		j.setBorder(title);
		gc.gridx = 0;
		gc.gridy = 0;
		j.add(new JLabel("enable"), gc);
		++gc.gridx;
		j.add(useSaturation, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(new JLabel("  min max"), gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(satMin, gc);
		++gc.gridx;
		j.add(satMax, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(satMin.value, gc);
		++gc.gridx;
		j.add(satMax.value, gc);		
		display.add(j);
		 
		j = new JPanel(new GridBagLayout());
		title = BorderFactory.createTitledBorder("value");
		j.setBorder(title);
		gc.gridx = 0;
		gc.gridy = 0;
		j.add(new JLabel("enable"), gc);
		++gc.gridx;
		j.add(useValue, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(new JLabel(" min max"), gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(valMin, gc);
		++gc.gridx;
		j.add(valMax, gc);
		++gc.gridy;
		gc.gridx=0;
		j.add(valMin.value, gc);
		++gc.gridx;
		j.add(valMax.value, gc);		
		display.add(j);
		
	}

	@Override
	public void apply() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JPanel getDisplay() {
		// TODO Auto-generated method stub
		return display;
	}

	@Override
	public void setFilterData(OpenCVFilter filter) {
		// TODO Auto-generated method stub
		
	}

}
