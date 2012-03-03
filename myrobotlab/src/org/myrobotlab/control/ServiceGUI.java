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
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Util;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public abstract class ServiceGUI {

	public final static Logger LOG = Logger.getLogger(ServiceGUI.class.getCanonicalName());

	public final String boundServiceName;
	final GUI myService;

	GridBagConstraints gc = new GridBagConstraints();
	// index of tab in the tab panel -1 would be not displayed or displayed in custom tab
	public int myIndex = -1; 

	// TODO - do not grab widgetFrame directly - ask for widgetDisplay()

	public JPanel widgetFrame = new JPanel(); // outside panel which looks like
												// a closeble widget - contains
												// close/detach button & title
	public JPanel menu = new JPanel();
	public JPanel display = new JPanel();
	public JButton detachButton = null;
	JButton releaseServiceButton = null;
	JButton help = null;
	
	public abstract void init();	

	public class DetachListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			LOG.error("undock " + boundServiceName);
			//releaseServiceButton.setVisible(false); - FIXME same functionality
			myService.undockPanel(boundServiceName);
		}
		
	}

	public class HelpListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {

			ServiceWrapper sw = Runtime.getService(boundServiceName);
			if (sw != null){
				Service s = sw.service;
				if (s != null){
					BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + s.getShortTypeName());
					return;
				}
			}
			LOG.error(boundServiceName + " service not found for help request");
		}
		
	}
	
	public class ReleaseServiceListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {

			Runtime.release(boundServiceName); // FYI - local only			
		}
		
	}
	
    public static JButton getButton(String name)
    {
    	JButton b = new JButton(Util.getScaledIcon(Util.getImage(name), 0.50));
    	b.setMargin(new Insets(0, 0, 0, 0));
		b.setOpaque(false);
		b.setContentAreaFilled(false);
		b.setBorderPainted(false);	
    	return b;
    }
		
	public ServiceGUI(final String boundServiceName, final GUI myService) {
		this.boundServiceName = boundServiceName;
		this.myService = myService;

		detachButton = getButton("detach.png");
		releaseServiceButton = getButton("release.png");
		help = getButton("help.png");
		
		menu.add(releaseServiceButton);
		menu.add(detachButton);
		menu.add(help);
			
		detachButton.addActionListener(new DetachListener());
		releaseServiceButton.addActionListener(new ReleaseServiceListener());
		help.addActionListener(new HelpListener());

		BevelBorder widgetTitle;
		widgetTitle = (BevelBorder) BorderFactory
				.createBevelBorder(BevelBorder.RAISED);
		widgetFrame.setBorder(widgetTitle);
		widgetFrame.setLayout(new GridBagLayout());

		gc.anchor = GridBagConstraints.FIRST_LINE_END;

		// place menu
		gc.gridx = 0;
		gc.gridy = 0;
		widgetFrame.add(menu, gc);
		++gc.gridy;
		widgetFrame.add(display, gc);

		display.setLayout(new GridBagLayout());

		//ConfigurationManager hostcfg = new ConfigurationManager(Service.getHostName(null));
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

	}

	/*
	 * Service functions
	 */
	public void sendNotifyRequest(String inOutMethod) 
	{
		sendNotifyRequest(inOutMethod, inOutMethod, null);
	}

	public void sendNotifyRequest(String inMethod, String outMethod) 
	{
		sendNotifyRequest(inMethod, outMethod, null);
	}
	
	public void sendNotifyRequest(String outMethod, String inMethod, Class<?> parameterType) 
	{
		NotifyEntry ne = null;
		//notifyEntry.getName() = myService.getName();
		//notifyEntry.outMethod = outMethod;
		//notifyEntry.inMethod = inMethod;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, null);
		}
		
		myService.send(boundServiceName, "notify", ne);

	}

	// TODO - more closely model java event system with addNotification or
	// addListener
	public void removeNotifyRequest(String inOutMethod) 
	{
		removeNotifyRequest(inOutMethod, inOutMethod, null);
	}

	public void removeNotifyRequest(String inMethod, String outMethod) 
	{
		removeNotifyRequest(inMethod, outMethod, null);
	}

	public void removeNotifyRequest(String outMethod, String inMethod,
			Class<?> parameterType) {

		NotifyEntry ne = null;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, null);
		}
		myService.send(boundServiceName, "removeNotify", ne);

	}

	public abstract void attachGUI();

	public abstract void detachGUI();
	
	public int test (int i, double d)
	{
		int x = 0;
		return x;
	}

}
