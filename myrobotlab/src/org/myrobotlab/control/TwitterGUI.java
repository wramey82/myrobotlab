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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Twitter;
import org.myrobotlab.service._TemplateService;
import org.myrobotlab.service.interfaces.GUI;
import org.slf4j.Logger;

public class TwitterGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(_TemplateServiceGUI.class.getCanonicalName());
	
	JPasswordField consumerKey = new JPasswordField("XXXXXX",20);
	JPasswordField consumerSecret = new JPasswordField("XXXXXX",20);
	JPasswordField accessToken = new JPasswordField("XXXXXX",20);
	JPasswordField accessTokenSecret = new JPasswordField("XXXXXX",20);
	JButton configure = new JButton("set keys");
	Twitter twitter = null;
	
	public TwitterGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		JPanel west = new JPanel(new GridLayout(2,4));
		west.add(new JLabel("consumer key"));
		west.add(consumerKey);
		west.add(new JLabel("consumer secret"));
		west.add(consumerSecret);
		west.add(new JLabel("access token"));
		west.add(accessToken);
		west.add(new JLabel("access token secret"));
		west.add(accessTokenSecret);
		west.add(configure);
		//display.setLayout(new BorderLayout());
		display.add(west);
	}

	public void init() {
		
	}

	public void getState(final Twitter twitter) {
		this.twitter = twitter;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				consumerKey.setText(twitter.consumerKey);
				consumerSecret.setText(twitter.consumerSecret);
				accessToken.setText(twitter.accessToken);
				accessTokenSecret.setText(twitter.accessTokenSecret);
				
			}
		});
	}
	
	public void setState()
	{
		myService.send(boundServiceName, "setState", twitter);
	}
	

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", _TemplateService.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", _TemplateService.class);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == configure) {
			myService.send(boundServiceName, "setSecurity", new String(consumerKey.getPassword()),  new String(consumerSecret.getPassword()), new String(accessToken.getPassword()), new String(accessTokenSecret.getPassword()));
		}
		
		// TODO Auto-generated method stub

	}
	

}
