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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;

public class JythonGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;
	
	RSyntaxTextArea editor = null;
	RTextScrollPane scrollPane = null;
	
	public JythonGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	JButton exec = new JButton("exec");
	//JMenu fileMenu = new JMenu("file");
	//JMenuBar menuBar = new JMenuBar();
	
	EditorActionListener al = new EditorActionListener();
	
	public class EditorActionListener implements ActionListener
	{
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			LOG.debug("EditorActionListener.actionPerformed " + arg0);
			JMenuItem m = (JMenuItem)arg0.getSource();
			if (m.getText().equals("save"))
			{
				save();
			} else if (m.getText().equals("open"))
			{
				open();
			}
		}		
	}	
	
	JLabel statusInfo = new JLabel();

	// TODO - put in FileUtils
	  void open () {
		    FileDialog file = new FileDialog (myService.getFrame(), "Open File", FileDialog.LOAD);
		    file.setFile ("*.java;*.txt");  // Set initial filename filter
		    file.setVisible(true); // Blocks
		    String curFile;
		    if ((curFile = file.getFile()) != null) {
		      String filename = file.getDirectory() + curFile;
		      char[] data;
		      //setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		      File f = new File (filename);
		      try {
		        FileReader fin = new FileReader (f);
		        int filesize = (int)f.length();
		        data = new char[filesize];
		        fin.read (data, 0, filesize);
		        editor.setText (new String (data));
		        statusInfo.setText ("Loaded: " + filename);
		      } catch (FileNotFoundException exc) {
		        statusInfo.setText ("File Not Found: " + filename);
		      } catch (IOException exc) {
		        statusInfo.setText ("IOException: " + filename);
		      }
		      //setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		    }
		  }

	
	 void save () {
		    FileDialog file = new FileDialog (myService.getFrame(), "Save File", FileDialog.SAVE);
		    file.setVisible(true);
		    String curFile;
		    if ((curFile = file.getFile()) != null) {
		      String filename = file.getDirectory() + curFile;// + "1";
		      //setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		      File f = new File (filename);
		      try {
		        FileWriter fw = new FileWriter (f);
		        String text = editor.getText();
		        int textsize = text.length();
		        fw.write (editor.getText(), 0, textsize);
		        fw.close ();
		        statusInfo.setText ("Saved: " + filename);
		      } catch (IOException exc) {
		        statusInfo.setText ("IOException: " + filename);
		      }
		      //setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		    }
	 }
	
	public JMenuItem createMenuItem(String label)
	{
		JMenuItem mi = new JMenuItem(label);
		mi.addActionListener(al);
		return mi;
	}
	
	public void init() {

		/*
        ImageIcon iconNew = new ImageIcon(getClass().getResource("new.png"));
        ImageIcon iconOpen = new ImageIcon(getClass().getResource("open.png"));
        ImageIcon iconSave = new ImageIcon(getClass().getResource("save.png"));
        ImageIcon iconExit = new ImageIcon(getClass().getResource("exit.png"));
        */

		
		JMenuBar bar = new JMenuBar();
	    
	    // file
		JMenu file = new JMenu("file");
	    file.setMnemonic('f');
	    bar.add(file);
	    
	    file.add(createMenuItem("new"));
	    file.add(createMenuItem("save"));
	    file.add(createMenuItem("save as"));
	    file.add(createMenuItem("open"));
	    file.addSeparator();

	    // edit
		JMenu edit = new JMenu("edit");
		edit.setMnemonic('e');
	    bar.add(edit);

	    // examples
		JMenu examples = new JMenu("examples");
		examples.setMnemonic('x');		
	    examples.add(createMenuItem("Arduino"));
	    examples.add(createMenuItem("OpenCV"));
	    examples.add(createMenuItem("Clock"));
	    examples.add(createMenuItem("Speech"));
	    bar.add(examples);
		
		
		StateActionListener state = new StateActionListener();

		gc.gridx = 0;
		gc.gridy = 0;

		exec.addActionListener(state);
		
		editor = new RSyntaxTextArea();
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		scrollPane = new RTextScrollPane(editor);

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(bar, BorderLayout.LINE_START);
		menuPanel.add(exec);
		
		
		display.setLayout(new BorderLayout());
		
		display.add(menuPanel, BorderLayout.PAGE_START);		

		display.setPreferredSize(new Dimension(800, 600));
		display.add(scrollPane, BorderLayout.CENTER);

		// TODO - LOOK GOOD STUFF!
		myJython = (Jython) RuntimeEnvironment.getService(boundServiceName).service;
		
		if (myJython != null)
		{
			editor.setText(myJython.getScript());
		}

	}

	Jython myJython = null;
	
	public class StateActionListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			JButton button = (JButton) e.getSource();
			myService.send(boundServiceName, button.getText(), editor.getText());			
		}
		
	}
		
	
	// TODO put in ServiceGUI framework?
	public void getState(Jython j)
	{
		// TODO set GUI state debug from Service data
		
	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Jython.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishState", "getState", Jython.class);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
