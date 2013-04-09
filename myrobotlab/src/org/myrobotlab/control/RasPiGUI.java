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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;

import org.myrobotlab.control.widget.MemoryWidget;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.RasPi;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.MemoryDisplay;
import org.slf4j.Logger;

public class RasPiGUI extends ServiceGUI implements ActionListener, MemoryDisplay {

	static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(RasPiGUI.class.getCanonicalName());

	MemoryWidget tree = new MemoryWidget(this);

	public RasPiGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
		
		display.add(tree.getDisplay());
	}

	public void init() {
	}

	public void getState(RasPi template) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

			}
		});
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", RasPiGUI.class);
		subscribe("publishNode", "publishNode", String.class, Node.class);
		subscribe("putNode", "putNode", Node.NodeContext.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", RasPiGUI.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}
	

	// FIXME !!!! SHOULD BE IN NodeGUI !!!!
	// Add a Node to the GUI - since a GUI Tree 
	// is constructed to model the memory Tree
	// this is a merge between what the user is interested in
	// and what is in memory
	// memory will grow an update the parts which a user
	// expand - perhaps configuration will allow auto-expand
	// versus user controlled expand of nodes on tree
	public void putNode(Node.NodeContext context)
	{
		tree.put(context.parentPath, context.node);
	}
	
	public void publishNode(Node.NodeContext nodeContext)
	{
		// update or add
		// remember the display node tree does not match the structure
		// of the memory tree
		String parentPath = nodeContext.parentPath;
		Node node = nodeContext.node;
		
		tree.put(parentPath, node);
		
		// FIXME - TODO - refresh Node data !!! versus re-build gui
	}

	@Override
	public void displayStatus(String status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void display(Node node) {
		// TODO Auto-generated method stub
		
	}


}
