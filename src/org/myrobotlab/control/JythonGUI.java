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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.service.Clock;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;

public class JythonGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;
	
	RSyntaxTextArea editor = null;
	RTextScrollPane scrollPane = null;
	
	public JythonGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	
	public void init() {
		
		gc.gridx = 0;
		gc.gridy = 0;

		editor = new RSyntaxTextArea();
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		scrollPane = new RTextScrollPane(editor);

		display.setLayout(new BorderLayout());
		display.setPreferredSize(new Dimension(800, 600));

		display.add(scrollPane);

		// TODO - LOOK GOOD STUFF!
		myJython = (Jython) RuntimeEnvironment.getService(boundServiceName).service;

	}

	Jython myJython = null;
	
	// TODO put in ServiceGUI framework?
	public void getState(Jython j)
	{
		// TODO set GUI state info from Service data
		
	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Clock.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", Clock.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
