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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JList;

import org.apache.log4j.Logger;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.VideoGUISource;

public class GraphicsGUI extends ServiceGUI implements VideoGUISource {

	static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(GraphicsGUI.class.toString());

	VideoWidget video = null;
	BufferedImage graph = null;
	Graphics g = null;
	
	public GraphicsGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public void init() {

		JButton cg = new JButton("create graph");
		cg.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				myService.send(boundServiceName, "createGraph");
			}
		});
		
		video = new VideoWidget(boundServiceName, myService);	
		video.init();
		display.add(cg);
		display.add(video.display, gc);

	}

	// TODO - com....Sensor interface
	public void webCamDisplay(SerializableImage img) {
		video.webCamDisplay(img);
	}

	@Override
	public void attachGUI() {
		video.attachGUI();
		//sendNotifyRequest(outMethod, inMethod, parameterType)
		myService.send(boundServiceName,"attach", (Object)myService.name);
	}

	@Override
	public void detachGUI() {
		video.detachGUI();
		myService.send(boundServiceName,"detach");
	}

	public void createGraph (Dimension d)
	{
		graph = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		g = graph.getGraphics();
		video.webCamDisplay(graph);
	}

	// wrappers begin --------------------------
	public void drawLine(Integer x1, Integer y1, Integer x2, Integer y2)
	{
		g.drawLine(x1, y1, x2, y2);
		refreshDisplay();
		//video.webCamDisplay(graph);
	}
	
	public void drawString(String str, Integer x, Integer y)
	{
		g.drawString(str, x, y);
		//video.webCamDisplay(graph);
	}

	public void drawRect(Integer x, Integer y, Integer width, Integer height)
	{
		g.drawRect(x, y, width, height);
		//video.webCamDisplay(graph);
	}

	public void fillOval(Integer x, Integer y, Integer width, Integer height)
	{
		g.fillOval(x, y, width, height);
	}

	public void fillRect(Integer x, Integer y, Integer width, Integer height)
	{
		g.fillRect(x, y, width, height);
	}
	
	public void clearRect(Integer x, Integer y, Integer width, Integer height)
	{
		g.clearRect(x, y, width, height);
		//video.webCamDisplay(graph);
	}

	public void setColor(Color c)
	{
		g.setColor(c);
		//video.webCamDisplay(graph);
	}
	
	// wrappers end --------------------------
	
	// refresh display
	public void refreshDisplay()
	{
		video.webCamDisplay(graph);
	}
	
	// TODO - encapsulate this
	// MouseListener mouseListener = new MouseAdapter() {
	public void setCurrentFilterMouseListener() {
		MouseListener mouseListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent mouseEvent) {
				JList theList = (JList) mouseEvent.getSource();
				if (mouseEvent.getClickCount() == 2) {
					int index = theList.locationToIndex(mouseEvent.getPoint());
					if (index >= 0) {
						Object o = theList.getModel().getElementAt(index);
						System.out
								.println("Double-clicked on: " + o.toString());
					}
				}
			}
		};

		//traces.addMouseListener(mouseListener);
	}

	@Override
	public VideoWidget getLocalDisplay() {
		// TODO Auto-generated method stub
		return video;
	}

}
