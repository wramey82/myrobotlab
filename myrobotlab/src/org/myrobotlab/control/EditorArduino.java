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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTabbedPane;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.myrobotlab.service.interfaces.GUI;

public class EditorArduino extends Editor implements ActionListener {

	static final long serialVersionUID = 1L;

	// button bar buttons
	ImageButton compileButton;
	ImageButton uploadButton;
	ImageButton connectButton;
	ImageButton newButton;
	ImageButton openButton;
	ImageButton saveButton;
	ImageButton fullscreenButton;
	ImageButton monitorButton;

	// tool menu->methods

	// consoles
	JTabbedPane consoleTabs;

	// autocompletion
	CompletionProvider provider;
	AutoCompletion ac;

	public EditorArduino(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService, SyntaxConstants.SYNTAX_STYLE_C);

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == restartButton) {
			//performRestart();
			return;
		}
	}
	


	public void init() {
		super.init();
		
		compileButton = addImageButtonToButtonBar("Arduino","compile", this);
		uploadButton 	= addImageButtonToButtonBar("Arduino","upload", this);
		connectButton 	= addImageButtonToButtonBar("Arduino","connect", this);
		newButton 		= addImageButtonToButtonBar("Arduino","new", this);
		openButton 		= addImageButtonToButtonBar("Arduino","open", this);
		saveButton 		= addImageButtonToButtonBar("Arduino","save", this);
		fullscreenButton= addImageButtonToButtonBar("Arduino","fullscreen", this);
		monitorButton 	= addImageButtonToButtonBar("Arduino","monitor", this);
		
		buttonBar.setBackground(new Color(0,100,104));
		
		// addHelpMenuURL("help blah", "http:blahblahblah");

	}



}
