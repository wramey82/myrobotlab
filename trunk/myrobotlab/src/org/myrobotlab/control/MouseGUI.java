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

//raver1975 was here!

package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Mouse;
import org.myrobotlab.service._TemplateService;
import org.myrobotlab.service.interfaces.GUI;
import org.slf4j.Logger;

public class MouseGUI extends ServiceGUI implements ActionListener, MouseMotionListener,MouseListener,MouseWheelListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(MouseGUI.class.getCanonicalName());

	VideoWidget video0 = null;
	JTextField status = new JTextField("", 20);
	JLabel x = new JLabel("0");
	JLabel y = new JLabel("0");
	
	public MouseGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		video0 = new VideoWidget(boundServiceName, myService, false);
		//video0.setNormalizedSize(400,400);
		video0.init();
		video0.getDisplay().addMouseListener(this);
		display.setLayout(new BorderLayout());
		display.add(video0.getDisplay(), BorderLayout.CENTER);
		//display.add(status, BorderLayout.SOUTH);
	}

	public void getState(_TemplateService template) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

			}
		});
	}
	
	public void displayFrame(SerializableImage img) {
		video0.displayFrame(img);
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", _TemplateService.class);
		subscribe("publishDisplay", "displayFrame", SerializableImage.class);
		video0.attachGUI(); // default attachment
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", _TemplateService.class);
		unsubscribe("publishDisplay", "displayFrame", SerializableImage.class);
		video0.detachGUI(); // default attachment
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	

}
