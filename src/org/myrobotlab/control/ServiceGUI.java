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
import org.myrobotlab.service.GUIService;

public abstract class ServiceGUI {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(ServiceGUI.class
			.getCanonicalName());

	public final String boundServiceName;
	final GUIService myService;

	GridBagConstraints gc = new GridBagConstraints();

	// TODO - do not grap widgetFrame directly - ask for widgetDisplay()

	public JPanel widgetFrame = new JPanel(); // outside panel which looks like
												// a closeble widget - contains
												// close/detach button & title
	public JPanel menu = new JPanel();
	public JPanel display = new JPanel();

	JButton detachButton = null;
	
	public ServiceGUI() {
		this("unknown", null);
	}

	public class DetachListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			LOG.error("detach " + boundServiceName);
			HashMap<String, Boolean> cwp = myService.customWidgetPrefs;
			if (!cwp.containsKey(boundServiceName) || (cwp.containsKey(boundServiceName) && cwp.get(boundServiceName) == false))
			{
				cwp.put(boundServiceName, true);
			} else {
				cwp.put(boundServiceName, false);
			}
			
			myService.loadTabPanels();
		}
		
	}
	
	public ServiceGUI(String boundServiceName, GUIService myService) {
		this.boundServiceName = boundServiceName;
		this.myService = myService;

		// start widget ----
		detachButton = new JButton(getImageIcon("service_close.png"));
		detachButton.setMargin(new Insets(0, 0, 0, 0));
		menu.add(detachButton);
		
		
		detachButton.addActionListener(new DetachListener());
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
		java.net.URL imgURL = getClass().getResource(path);
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
	public void sendNotifyRequest(String outMethod, String inMethod, Class parameterType) 
	{
		NotifyEntry notifyEntry = new NotifyEntry();
		notifyEntry.name = myService.name;
		notifyEntry.outMethod_ = outMethod;
		notifyEntry.inMethod_ = inMethod;
		if (parameterType != null) {
			notifyEntry.paramTypes = new Class[]{parameterType};
		}
		
		myService.send(boundServiceName, "notify", notifyEntry);

	}

	// TODO - more closely model java event system with addNotification or
	// addListener
	public void removeNotifyRequest(String outMethod, String inMethod,
			Class parameterType) {
		NotifyEntry notifyEntry = new NotifyEntry();
		notifyEntry.name = myService.name;
		notifyEntry.outMethod_ = outMethod;
		notifyEntry.inMethod_ = inMethod;
		if (parameterType != null) {
			notifyEntry.paramTypes = new Class[]{parameterType};
		}
		myService.send(boundServiceName, "removeNotify", notifyEntry);

	}

	public abstract void attachGUI();

	public abstract void detachGUI();
	
	public int test (int i, double d)
	{
		int x = 0;
		return x;
	}

}
