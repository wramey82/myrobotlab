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

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JButton;

import org.apache.log4j.Logger;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.FSMTest;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.VideoGUISource;

public class FSMTestGUI extends ServiceGUI implements VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(FSMTestGUI.class.toString());

	VideoWidget video0 = null;
	//VideoWidget video1 = null;
	//VideoWidget video2 = null;
	BufferedImage graph = null;
	Graphics g = null;
	
	public FSMTestGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public class StateActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			JButton button = (JButton) e.getSource();
			myService.send(boundServiceName, "heard", button.getText());			
		}
		
	}
	
	public void init() {

		StateActionListener state = new StateActionListener();
		
		JButton b = new JButton("look");
		b.addActionListener(state);
		display.add(b, gc);		
		++gc.gridx;

		b = new JButton("ball");
		b.addActionListener(state);
		display.add(b, gc);		

		gc.gridx = 0;
		++gc.gridy;
		
		video0 = new VideoWidget(boundServiceName, myService);	
		video0.init();
		display.add(video0.display, gc);		
/*		
		++gc.gridy;
		video1 = new VideoWidget(boundServiceName, myService);	
		video1.init();
		display.add(video1.display, gc);

		
		++gc.gridx;
		video2 = new VideoWidget(boundServiceName, myService);	
		video2.init();
		display.add(video2.display, gc);
*/

	}

	// TODO - com....Sensor interface
	public void displayVideo0(HashMap<String, Node> memory) {
		
		
		Iterator<String> itr = memory.keySet().iterator();
		Node unknown = memory.get(FSMTest.UNKNOWN);
		LOG.error( unknown.imageData.get(0).boundingBox);
		LOG.error( unknown.imageData.get(0).boundingBox2);
		
		while (itr.hasNext()) {
			String n = itr.next();
			Node node = memory.get(n);
			for (int i = 0; i < node.imageData.size(); ++i)
			{	
				video0.addVideoDisplayPanel(n);
				video0.displayFrame(OpenCV.publishFrame(n,node.imageData.get(i).cvCameraFrame.getBufferedImage()));
			}
		}
	}
	
	/*
	public void displayVideo1(Node node) {
		displayVideo(video1, node);
	}
	
	public void displayVideo2(Node node) {
		displayVideo(video2, node);
	}
*/
	
	public void clearVideo0 ()
	{
		video0.removeAllVideoDisplayPanels();
	}
	/*	
	
	public void displayVideo(VideoWidget v, Node node)
	{
		for (int i = 0; i < node.imageData.size(); ++i)
		{			
			v.displayFrame(OpenCV.publishFrame("unknown " + i,node.imageData.get(i).cvCameraFrame.getBufferedImage()));			
		}
	}
	
	public void displayMatchResult (IplImage img)
	{
		video2.displayFrame(OpenCV.publishFrame("matched result", img.getBufferedImage()));
	}
*/	
	@Override
	public void attachGUI() {
		video0.attachGUI();
//		video1.attachGUI();
//		video2.attachGUI();
		//sendNotifyRequest(outMethod, inMethod, parameterType)
		sendNotifyRequest("publishVideo0", "displayVideo0", HashMap.class);
		sendNotifyRequest("clearVideo0"); // FIXME - bad notation .. come on !
		//sendNotifyRequest("publishVideo1", "displayVideo1", Node.class);
		//sendNotifyRequest("publishVideo2", "displayVideo2", Node.class);
		//sendNotifyRequest("publishMatchResult", "displayMatchResult", IplImage.class);
		myService.send(boundServiceName,"attach", (Object)myService.name);
	}

	@Override
	public void detachGUI() {
		video0.detachGUI();
		removeNotifyRequest("publishVideo0", "displayVideo0", HashMap.class);
		removeNotifyRequest("clearVideo0"); // FIXME - bad notation .. come on !
		//video1.detachGUI();
		//video2.detachGUI();
		myService.send(boundServiceName,"detach");
	}


	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video0;
	}

}
