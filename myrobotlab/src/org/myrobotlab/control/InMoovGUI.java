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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.InMoov;
import org.slf4j.Logger;

public class InMoovGUI extends ServiceGUI implements ActionListener {


	HashSet<String> handTemplate = new HashSet<String>(Arrays.asList("%s.%s", "%s.%sHand","%s.%sHand.index", "%s.%sHand.majeure", "%s.%sHand.ringFinger", "%s.%sHand.pinky", "%s.%sHand.thumb", "%s.%sHand.wrist"));
	HashSet<String> armTemplate = new HashSet<String>(Arrays.asList("%s.%s", "%s.%Arm","%s.%sArm.bicep", "%s.%sArm.rotate", "%s.%sArm.shoulder", "%s.%sArm.omoplate"));
	HashSet<String> headTemplate = new HashSet<String>(Arrays.asList("%s.%s", "%s.%Arm","%s.%sArm.bicep", "%s.%sArm.rotate", "%s.%sArm.shoulder", "%s.%sArm.omoplate"));

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoovGUI.class.getCanonicalName());

	JLayeredPane imageMap;
	
	JButton leftHand = new JButton("start left hand");
	JButton leftArm = new JButton("hide left arm");

	JButton rightHand = new JButton("hide right hand");
	JButton rightArm = new JButton("hide right arm");
	
	JButton head = new JButton("hide head");
	private String defaultLeftPort;
	private String defaultRightPort;

	public InMoovGUI(final String boundServiceName, final GUIService myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		display.setLayout(new BorderLayout());
		
		imageMap = new JLayeredPane();
		imageMap.setPreferredSize(new Dimension(692, 688));
		
		JLabel image = new JLabel();
		ImageIcon dPic = Util.getImageIcon("InMoov/body.png"); 
		image.setIcon(dPic);
		Dimension s = image.getPreferredSize();
		image.setBounds(0, 0, s.width, s.height);
		imageMap.add(image, new Integer(1));

		JPanel controls = new JPanel(new GridLayout(6, 1));
		
		controls.add(leftHand);
		controls.add(leftArm);
		controls.add(head);
		controls.add(rightHand);
		controls.add(rightArm);
		
		display.add(controls, BorderLayout.EAST);
		++gc.gridy;
		display.add(imageMap, BorderLayout.CENTER);
		
		
		leftHand.addActionListener(this);
		leftArm.addActionListener(this);
		rightHand.addActionListener(this);
		rightArm.addActionListener(this);
		head.addActionListener(this);
	}

	public void getState(InMoov moov) {

	}

	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <-
	// Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", InMoovGUI.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", InMoovGUI.class);
	}

	
	public String getPort(String side){
		if ("left".equals(side) && defaultLeftPort == null){
			defaultLeftPort = JOptionPane.showInputDialog(getDisplay(), "left COM port");
			return defaultLeftPort;
		}
		
		if ("right".equals(side) && defaultRightPort == null){
			defaultRightPort = JOptionPane.showInputDialog(getDisplay(), "right COM port");
			return defaultRightPort;
		}
		
		if ("left".equals(side)){
			return defaultLeftPort;
		}
		
		if ("right".equals(side)){
			return defaultRightPort;
		}
		
		return null;			
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == leftHand){
			if ("start left hand".equals(leftHand.getText())){
				String port = getPort("left");
				log.info("starting left hand with port {}", port);
				send("startLeftHand", port);
				leftHand.setText("hide left hand");
				return;
			}
			if ("hide left hand".equals(leftHand.getText())){
				
				for (String s : handTemplate) {
				    myService.hidePanel(String.format(s,boundServiceName,"left"));
				}
				
			}
		} else if  (o == rightHand){
			if ("hide right hand".equals(rightHand.getText())){
				
				for (String s : handTemplate) {
				    myService.hidePanel(String.format(s,boundServiceName,"right"));
				}
				
			}
		} else if  (o == leftArm){
			if ("hide left arm".equals(leftArm.getText())){
				
				for (String s : armTemplate) {
				    myService.hidePanel(String.format(s,boundServiceName,"left"));
				}
				
			}
		} else if  (o == rightArm){
			if ("hide right arm".equals(rightArm.getText())){
				
				for (String s : armTemplate) {
				    myService.hidePanel(String.format(s,boundServiceName,"right"));
				}
				
			}
		}

	}

}
