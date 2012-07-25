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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultCaret;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;

/**
 * Editor 
 * 
 * General purpose swing editor
 * TODO generalize for Jython & Arduino
 * 
 * @author GroG
 * 
 */
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

	/*
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Jython.class);
		subscribe("finishedExecutingScript");
		subscribe("publishStdOut", "getStdOut", String.class);
		// myService.send(boundServiceName, "broadcastState");
	}

	@Override
	public void detachGUI() {
		javaConsole.stopLogging();
		unsubscribe("publishStdOut", "getStdOut", String.class);
		unsubscribe("finishedExecutingScript");
		unsubscribe("publishState", "getState", Jython.class);
	}
	*/
	/**
	 * 
	 */
	public void finishedExecutingScript() {
		executeButton.deactivate();
	}

	public void init() {
		// handles basic file io and any common initialization
		super.init();
		
		// TODO - add "special Arduino components"
		compileButton = addImageButton("Arduino","compile", this);
		uploadButton 	= addImageButton("Arduino","upload", this);
		connectButton 	= addImageButton("Arduino","connect", this);
		newButton 		= addImageButton("Arduino","new", this);
		openButton 		= addImageButton("Arduino","open", this);
		saveButton 		= addImageButton("Arduino","save", this);
		fullscreenButton= addImageButton("Arduino","fullscreen", this);
		monitorButton 	= addImageButton("Arduino","monitor", this);

	}



}
