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

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.myrobotlab.arduino.compiler.Target;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.Runtime;
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
	JLabel programName = new JLabel("prog1");

	// consoles
	JTabbedPane consoleTabs;

	// autocompletion
	CompletionProvider provider;
	AutoCompletion ac;
	
	Arduino myArduino = null;

	public EditorArduino(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService, SyntaxConstants.SYNTAX_STYLE_C);
		ServiceWrapper sw = Runtime.getService(boundServiceName);
		myArduino = (Arduino) sw.get();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		super.actionPerformed(event);
		Object o = event.getSource();
				
		if (o == compileButton)
		{
			myService.send(boundServiceName, "compile", programName.getText(), editor.getText());
		} else if (o == uploadButton) {
			myService.send(boundServiceName, "upload");
			return;
		} else if (o == connectButton)
		{
			myService.send(boundServiceName, "connect");
		}
	}
	
	JMenu boardsMenu = null;
	public JMenu serialDeviceMenu = null;

	public void init() {
		super.init();
		
		compileButton = addImageButtonToButtonBar("Arduino","Compile", this);
		uploadButton 	= addImageButtonToButtonBar("Arduino","Upload", this);
		connectButton 	= addImageButtonToButtonBar("Arduino","Connect", this);
		newButton 		= addImageButtonToButtonBar("Arduino","New", this);
		openButton 		= addImageButtonToButtonBar("Arduino","Open", this);
		saveButton 		= addImageButtonToButtonBar("Arduino","Save", this);
		fullscreenButton= addImageButtonToButtonBar("Arduino","Fullscreen", this);
		monitorButton 	= addImageButtonToButtonBar("Arduino","Monitor", this);
		
		buttonBar.setBackground(new Color(0,100,104));
		buttonBar.add(programName);
		
		// addHelpMenuURL("help blah", "http:blahblahblah");
		
		boardsMenu = new JMenu("Board");
		rebuildBoardsMenu(boardsMenu);
		
		serialDeviceMenu = new JMenu("Serial Device");
		
		toolsMenu.add(boardsMenu);
		toolsMenu.add(serialDeviceMenu);
		
		// add to help menu
		helpMenu.add(createMenuItem("Getting Started"));
		helpMenu.add(createMenuItem("Environment"));
		helpMenu.add(createMenuItem("Troubleshooting"));
		helpMenu.add(createMenuItem("Reference"));
		helpMenu.add(createMenuItem("Find in Reference", saveMenuMnemonic, "control+shift-F", null));
		helpMenu.add(createMenuItem("Frequently Asked Questions"));
		helpMenu.add(createMenuItem("Visit Arduino.cc"));

	}

	  public void rebuildBoardsMenu(JMenu menu) {
		    menu.removeAll();      
		    ButtonGroup group = new ButtonGroup();
		    for (Target target : myArduino.targetsTable.values()) {
		      for (String board : target.getBoards().keySet()) {
		        AbstractAction action = 
		          new AbstractAction(target.getBoards().get(board).get("name")) {
		            public void actionPerformed(ActionEvent actionevent) {
		            	log.info(String.format("switching to %s:%s",(String) getValue("target"), (String) getValue("board")));
		            	//myService.send(boundServiceName, "setPreference", "target", (String) getValue("target"));
		            	//myService.send(boundServiceName, "setPreference", "board", (String) getValue("board"));
		            	myService.send(boundServiceName, "setBoard", (String) getValue("board"));
		            }
		          };
		        action.putValue("target", target.getName());
		        action.putValue("board", board);
		        JMenuItem item = new JRadioButtonMenuItem(action);
		        if (target.getName().equals(myArduino.preferences.get("target")) &&
		            board.equals(myArduino.preferences.get("board"))) {
		          item.setSelected(true);
		        }
		        group.add(item);
		        menu.add(item);
		      }
		    }
		  }

		

}
