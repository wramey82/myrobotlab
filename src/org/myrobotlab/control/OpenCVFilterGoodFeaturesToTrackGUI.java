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
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.image.OpenCVFilter;
import org.myrobotlab.image.OpenCVFilterGoodFeaturesToTrack;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.OpenCV.FilterWrapper;

public class OpenCVFilterGoodFeaturesToTrackGUI extends OpenCVFilterGUI {

	JSlider2 maxPointCount = new JSlider2(JSlider.HORIZONTAL, 0, 256, 30);
	JSlider2 minDistance = new JSlider2(JSlider.HORIZONTAL, 0, 256, 10);
	JSlider2 qualityLevel = new JSlider2(JSlider.HORIZONTAL, 0, 100, 0.05f);
	JSlider2 blockSize = new JSlider2(JSlider.HORIZONTAL, 1, 10, 3);
	AdjustSlider change = new AdjustSlider();
	
	
	public class JSlider2 extends JSlider // TODO - if worthwhile put it into control package
	{
		private static final long serialVersionUID = 1L;
		JLabel value = new JLabel();

		public JSlider2(int vertical, int i, int j, int k) {
			super (vertical, i, j, k);
			value.setText("" + k);
		}

		public JSlider2(int vertical, int i, int j, float k) {
			super (vertical, i, j, (int)k);
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
			
			if (!slider.getValueIsAdjusting())
			{
				OpenCVFilterGoodFeaturesToTrack filter = (OpenCVFilterGoodFeaturesToTrack)boundFilter.filter;
				if (slider.getName().equals("qualityLevel"))
				{
					//params[2] = slider.getValue() * 2 + 1;
					//qualityLevel.value.setText("" + (float)qualityLevel.getValue()/100);
					//filter.qualityLevel = (float)qualityLevel.getValue()/100;
				} else if (slider.getName().equals("maxPointCount")) {
					maxPointCount.value.setText("" + maxPointCount.getValue());
					filter.maxPointCount = maxPointCount.getValue();
				} else if (slider.getName().equals("minDistance")) {
					minDistance.value.setText("" + minDistance.getValue());
					filter.minDistance = minDistance.getValue();
				} else if (slider.getName().equals("blockSize")) {
					blockSize.value.setText("" + blockSize.getValue());
					filter.blockSize = blockSize.getValue();
				}
				
				myService.send(boundServiceName, "setFilterData", boundFilter);
			} // else - adjust gui text only
			
			
		}
	}


	
	public OpenCVFilterGoodFeaturesToTrackGUI(String boundFilterName,
			String boundServiceName, GUIService myService) {
		super(boundFilterName, boundServiceName, myService);

		
		maxPointCount.setName("maxPointCount"); 
		minDistance.setName("minDistance"); 
		qualityLevel.setName("qualityLevel");
		blockSize.setName("blockSize");
		
		maxPointCount.addChangeListener(change);
		minDistance.addChangeListener(change);
		qualityLevel.addChangeListener(change);
		blockSize.addChangeListener(change);
		
		gc.gridy = 0;		
		gc.gridx = 0;

		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("max points  "), gc);
		++gc.gridy;
		gc.gridwidth = 2;
		display.add(maxPointCount, gc);
		gc.gridwidth = 1;
		gc.gridx+=2;
		display.add(maxPointCount.value, gc);
		
		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("min distance"), gc);
		++gc.gridy;
		gc.gridwidth = 2;
		display.add(minDistance, gc);
		gc.gridwidth = 1;
		gc.gridx+=2;
		display.add(minDistance.value, gc);
		
		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("quality     "), gc);
		++gc.gridy;
		gc.gridwidth = 2;
		display.add(qualityLevel, gc);
		gc.gridwidth = 1;
		gc.gridx+=2;
		display.add(qualityLevel.value, gc);
		
		++gc.gridy;
		gc.gridx = 0;
		display.add(new JLabel("block size  "), gc);
		++gc.gridy;
		gc.gridwidth = 2;
		display.add(blockSize, gc);
		gc.gridwidth = 1;
		gc.gridx+=2;
		display.add(blockSize.value, gc);
		
		// set the hook
		NotifyEntry ne = new NotifyEntry("publishFilterData", myService.name, "setFilterData", new Class[]{FilterWrapper.class});
		myService.send(boundServiceName, "notify", ne);
		// thread wait?
		// send the event
		myService.send(boundServiceName, "publishFilterData", boundFilterName);
	}

	@Override
	public void apply() {
	}

	@Override
	public JPanel getDisplay() {
		return display;
	}
	
	@Override
	public void setFilterData(FilterWrapper boundFilter)
	{
		if (this.boundFilter == null)
		{
			this.boundFilter = boundFilter;			
		}

		OpenCVFilterGoodFeaturesToTrack bf = (OpenCVFilterGoodFeaturesToTrack)boundFilter.filter;
		maxPointCount.setValueIsAdjusting(true);
		minDistance.setValueIsAdjusting(true);
		qualityLevel.setValueIsAdjusting(true);
		blockSize.setValueIsAdjusting(true);

		maxPointCount.setValue(bf.maxPointCount);

		minDistance.setValue((int)bf.minDistance);
			
		qualityLevel.setValue((int)bf.qualityLevel * 100);
		qualityLevel.value.setText("" + bf.qualityLevel);
		
		blockSize.setValue((int)bf.blockSize);

		blockSize.setValueIsAdjusting(false);
		qualityLevel.setValueIsAdjusting(false);
		minDistance.setValueIsAdjusting(false);
		maxPointCount.setValueIsAdjusting(false);
	}
	
	public void updateState()
	{
		// send setState
	}


}
