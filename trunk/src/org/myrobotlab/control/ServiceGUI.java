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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.service.interfaces.GUI;

public abstract class ServiceGUI {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(ServiceGUI.class.getCanonicalName());

	public final String boundServiceName;
	final GUI myService;

	GridBagConstraints gc = new GridBagConstraints();

	// TODO - do not grab widgetFrame directly - ask for widgetDisplay()

	public JPanel widgetFrame = new JPanel(); // outside panel which looks like
												// a closeble widget - contains
												// close/detach button & title
	public JPanel menu = new JPanel();
	public JPanel display = new JPanel();
	JButton detachButton = null;
	JButton releaseServiceButton = null;
	
	public abstract void init();
	
	// index of tab in the tab panel -1 would be not displayed or displayed in custom tab
	public int myIndex = -1; 
	

	// TODO - refactor better name vs detach
	public class DetachListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			LOG.error("detach " + boundServiceName);
			HashMap<String, Boolean> cwp = myService.getCustomWidgetPrefs();
			if (!cwp.containsKey(boundServiceName) || (cwp.containsKey(boundServiceName) && cwp.get(boundServiceName) == false))
			{
				cwp.put(boundServiceName, true);
			} else {
				cwp.put(boundServiceName, false);
			}
			
			myService.loadTabPanels();
		}
		
	}

	public class ReleaseServiceListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {

			RuntimeEnvironment.release(boundServiceName);
			
			myService.loadTabPanels();
		}
		
	}
	
	
	
	public boolean isPanelTabbed()
	{
		HashMap<String, Boolean> cwp = myService.getCustomWidgetPrefs();
		return (!cwp.containsKey(boundServiceName) || (cwp.containsKey(boundServiceName) && cwp.get(boundServiceName) == false));
	}
	
	public ServiceGUI(final String boundServiceName, final GUI myService) {
		this.boundServiceName = boundServiceName;
		this.myService = myService;

		// start widget ----
		//detachButton = new JButton(getImageIcon("service_close.png"));
		detachButton = new JButton(getImageIcon("toCustom.png"));
		releaseServiceButton = new JButton(getImageIcon("service_close.png"));
		detachButton.setMargin(new Insets(0, 0, 0, 0));
		releaseServiceButton.setMargin(new Insets(0, 0, 0, 0));
		menu.add(releaseServiceButton);
		menu.add(detachButton);
		
		
		detachButton.addActionListener(new DetachListener());
		releaseServiceButton.addActionListener(new ReleaseServiceListener());
		/*
		 * JButton test = new JButton(getImageIcon("service_close.png"));
		 * test.setMargin(new Insets(0, 0, 0, 0)); menu.add(test);
		 */
		BevelBorder widgetTitle;
		widgetTitle = (BevelBorder) BorderFactory
				.createBevelBorder(BevelBorder.RAISED);
		widgetFrame.setBorder(widgetTitle);
		widgetFrame.setLayout(new GridBagLayout());

		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName);
		display.setBorder(title);

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

		// display.add(serviceDisplay, gc);

	}

	
	protected ImageIcon getImageIcon(String path) {
		ImageIcon icon = null;
		java.net.URL imgURL = getClass().getResource("/resource/" + path);
		if (imgURL != null) {
			icon = new ImageIcon(imgURL);
			return icon;
		} else {
			LOG.error("Couldn't find file: " + path);
			return null;
		}
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
		//notifyEntry.name = myService.name;
		//notifyEntry.outMethod = outMethod;
		//notifyEntry.inMethod = inMethod;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myService.name, inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.name, inMethod, null);
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
			ne = new NotifyEntry(outMethod, myService.name, inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.name, inMethod, null);
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
