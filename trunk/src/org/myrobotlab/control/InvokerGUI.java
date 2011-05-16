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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicArrowButton;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Invoker;

public class InvokerGUI extends ServiceGUI {

	public final static Logger LOG = Logger.getLogger(InvokerGUI.class.getCanonicalName());
	static final long serialVersionUID = 1L;

	JList possibleServices;
	JList currentServices;
	BasicArrowButton addServiceButton = null;
	BasicArrowButton removeServiceButton = null;
	String level[] = { "DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
	JComboBox logLevel = new JComboBox(level);
	
	// TODO - widgetize the "possible services" list

	public InvokerGUI(String name, GUIService myService) {
		super(name, myService);

		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());

		gc.gridx = 0;
		gc.gridy = 0;

		possibleServices = new JList(Invoker.getServiceShortClassNames());

		HashMap<String, ServiceEntry> services = myService.getHostCFG().getServiceMap();
		Map<String, ServiceEntry> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceEntry>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		String[] namesAndClasses = new String[sortedMap.size()];
		int i = 0;
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceEntry se = services.get(serviceName);
			String shortClassName = se.serviceClass.substring(se.serviceClass
					.lastIndexOf(".") + 1);
			namesAndClasses[i] = serviceName + " - " + shortClassName;
			++i;
		}

		currentServices = new JList(namesAndClasses);

		GridBagConstraints inputgc = new GridBagConstraints();
		inputgc.anchor = GridBagConstraints.FIRST_LINE_START;

		JScrollPane currentServicesScrollPane = new JScrollPane(currentServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(
				possibleServices);

		currentServices.setVisibleRowCount(20);
		possibleServices.setVisibleRowCount(20);
		input.add(possibleServicesScrollPane, inputgc);
		input.add(getRemoveServiceButton(), inputgc);
		input.add(getAddServiceButton(), inputgc);
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("local services - current");
		input.setBorder(title);

		display.add(input, gc);
		
		++gc.gridy;
		gc.gridx = 0;
		JPanel debug = new JPanel();
		title = BorderFactory.createTitledBorder("logging");
		debug.setBorder(title);
		
		debug.add(logLevel);
		logLevel.addItemListener(new LogLevel());
	    display.add(debug, gc);
	}
	
	public class LogLevel implements ItemListener {

		@Override
		public void itemStateChanged(ItemEvent e) {
	        String str = (String)logLevel.getSelectedItem();
	        myService.send(boundServiceName, "setLogLevel", str);			
		}
		
	}
	

	public JButton getAddServiceButton() {
		addServiceButton = new BasicArrowButton(BasicArrowButton.EAST);
		addServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFrame frame = new JFrame();
				frame.setTitle("add new service");
				String name = JOptionPane.showInputDialog(frame,
						"new service name");
				if (name != null) {
					String newService = (String) possibleServices.getSelectedValue();
					myService.send(boundServiceName, "addService", newService,name);
					// this is asynchronous - the service will be created later
					// - Especially on a remote process
					// it would be nice to momentarily block on this call !!!
					myService.loadTabPanels();
				}

			}

		});

		return addServiceButton;
	}

	public JButton getRemoveServiceButton() {
		removeServiceButton = new BasicArrowButton(BasicArrowButton.WEST);
		removeServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String oldService = (String) currentServices.getSelectedValue();
				myService.send(boundServiceName, "removeService", oldService);
				myService.loadTabPanels();
			}

		});

		return removeServiceButton;
	}

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}
	
}