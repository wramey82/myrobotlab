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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.CommunicationNodeEntry;
import org.myrobotlab.control.widget.CommunicationNodeList;
import org.myrobotlab.net.CommData;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.interfaces.GUI;

public class RemoteAdapterGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;
	JLabel numClients = new JLabel("0");
	
	CommunicationNodeList list = new CommunicationNodeList();

	public RemoteAdapterGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;
		display.add(new JLabel("number of connections :"), gc);
		++gc.gridy;
		display.add(new JLabel("last activity : "), gc);
		++gc.gridy;
		display.add(new JLabel("number of messages : "), gc);
		++gc.gridy;
		// list.setPreferredSize(new Dimension(arg0, arg1))
		gc.gridwidth = 4;
		gc.fill = GridBagConstraints.HORIZONTAL;
		display.add(list, gc);
		
		updateNodeList(null);

	}

	public void updateNodeList(RemoteAdapter remote) {
		if (remote != null)
		{
			
			Communicator cm = remote.getComm().getComm();
			
			HashMap<URI, CommData> clients = cm.getClients();
			
			for (Map.Entry<URI,CommData> o : clients.entrySet())
			{
				//Map.Entry<String,SerializableImage> pairs = o;
				URI uri = o.getKey();
				CommData data = o.getValue();
				list.model.add(0, (Object) new CommunicationNodeEntry(uri, data));
			}
			
			numClients.setText(String.format("%d",clients.size()));

		}
	}

	public void getState(final RemoteAdapter remote) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

					updateNodeList(remote);
			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RemoteAdapter.class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RemoteAdapter.class);
	}

}