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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.myrobotlab.openni.Points3DPanel;
import org.myrobotlab.openni.PointsShape;
import org.myrobotlab.service.PointCloud2;
import org.myrobotlab.service.data.SensorData;
import org.myrobotlab.service.interfaces.GUI;

public class PointCloud2GUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	PointsShape ptsShape = new PointsShape();
	Points3DPanel panel3d = new Points3DPanel(ptsShape);
	JButton captureButton = new JButton("capture");
	JButton recordButton = new JButton("record");
	JButton playbackButton = new JButton("playback");
	JButton pointCloudButton = new JButton("point cloud");
	JButton depthCloudButton = new JButton("depth");
	JButton imageCloudButton = new JButton("image");

	JPanel eastPanel = new JPanel();

	public PointCloud2GUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		// openni = (OpenNI)Runtime.getService(boundServiceName).service;

		display.setLayout(new BorderLayout());
		display.add(panel3d, BorderLayout.CENTER);

		eastPanel.setLayout(new GridLayout(6, 1));
		eastPanel.add(captureButton);
		eastPanel.add(recordButton);
		eastPanel.add(playbackButton);
		eastPanel.add(pointCloudButton);
		eastPanel.add(depthCloudButton);
		eastPanel.add(imageCloudButton);

		display.add(eastPanel, BorderLayout.EAST);

		captureButton.addActionListener(this);
		recordButton.addActionListener(this);
		playbackButton.addActionListener(this);
		/*
		 * JPanel viewer = new JPanel(); viewer.setLayout(new BorderLayout());
		 * viewer.add(panel3d, BorderLayout.CENTER);
		 */

	}

	public void getState(PointCloud2 openni) {
		if (openni != null) {

		}

	}

	public void publishFrame(SensorData kd) {
		ptsShape.updateDepthCoords(kd);
	}

	@Override
	public void attachGUI() {
		// subscribe & ask for the initial state of the service
		subscribe("publishState", "getState", PointCloud2.class);
		// subscribe("publishDisplay", "publishDisplay", ShortBuffer.class);
		subscribe("publishFrame", "publishFrame", SensorData.class);
		myService.send(boundServiceName, "publishState");

	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", PointCloud2.class);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == captureButton) {
			if (captureButton.getText().equals("capture")) {
				myService.send(boundServiceName, "capture");
				captureButton.setText("stop capture");
			} else {
				myService.send(boundServiceName, "stopCapture");
				captureButton.setText("capture");
			}
		} else if (o == recordButton) {
			if (recordButton.getText().equals("record")) {
				myService.send(boundServiceName, "record");
				recordButton.setText("stop recording");
			} else {
				myService.send(boundServiceName, "stopRecording");
				recordButton.setText("record");
			}
		} else if (o == playbackButton) {
			if (playbackButton.getText().equals("playback")) {
				myService.send(boundServiceName, "playback");
				playbackButton.setText("stop playback");
			} else {
				myService.send(boundServiceName, "stopPlayback");
				playbackButton.setText("playback");
			}
		}
	}

}
