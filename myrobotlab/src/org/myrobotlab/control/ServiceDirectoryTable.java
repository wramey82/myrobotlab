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

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ConfigurationManager;

public class ServiceDirectoryTable extends JPanel {

	static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(ServiceDirectoryTable.class.getCanonicalName()); // @jve:decl-index=0:
	JTable serviceTable = null;
	JButton refresh = null;
	static ConfigurationManager hostcfg; // @jve:decl-index=0:
	public InteractiveTableModel tableModel;
	String name;
	String hostname;
	Integer servicePort;

	public ServiceDirectoryTable(String name, String hostname, Integer port) {
		super();
		this.name = name;
		this.hostname = hostname;
		this.servicePort = port;
		hostcfg = new ConfigurationManager(hostname);
		initialize();
	}

	private void initialize() {
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		this.setSize(120, 300);
		this.setLayout(new GridBagLayout());
		this.add(new JScrollPane(getServiceTable()));
		gridBagConstraints.gridy = 1;
		this.add(getRefreshButton(serviceTable), gridBagConstraints);
	}

	private JTable getServiceTable() {
		if (serviceTable == null) {

			// sd = new ServiceDirectory(name, hostname, servicePort);
		}

		// serviceTable = new
		// JTable(sd.getVectorRows(),sd.getVectorColumnNames());
		serviceTable = new JTable();
		String[] columnNamesx = { "hostname", "port", "name", "class", "status", "category", "method", "direction", "lastModified", "dataClass" };

		tableModel = new InteractiveTableModel(columnNamesx, hostcfg.getServiceVector());
		// TableSorter sortedModel = new TableSorter( normalModel );

		serviceTable.setModel(tableModel);

		return serviceTable;
	}

	private JButton getRefreshButton(JTable serviceTable) {
		if (refresh == null) {
			refresh = new RefreshButton(this);
		}

		return refresh;
	}

	private static class RefreshButton extends JButton implements ActionListener {
		ServiceDirectoryTable parent = null;
		String text = "refresh";

		public RefreshButton(ServiceDirectoryTable jp) {
			super();
			parent = jp;
			setText(text);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			/*
			 * ServiceEntry se = new ServiceEntry(); se.category_.set("blah");
			 * parent.sd.put(se);
			 */
			parent.tableModel.set(hostcfg.getServiceVector());
		}
	}

}
