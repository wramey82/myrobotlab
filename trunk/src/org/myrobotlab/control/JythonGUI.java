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
import java.awt.FileDialog;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;

public class JythonGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	RSyntaxTextArea editor = new RSyntaxTextArea();
	RTextScrollPane scrollPane = null;
	JButton exec = new JButton("exec");
	JButton restart = new JButton("restart");
	EditorActionListener menuListener = new EditorActionListener();
	JLabel statusInfo = new JLabel();
	Jython myJython = null;
	// TODO - check for outside modification with lastmoddate
	File currentFile = null;
	
	public JythonGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public class EditorActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			LOG.debug("EditorActionListener.actionPerformed " + arg0);
			JMenuItem m = (JMenuItem) arg0.getSource();
			if (m.getText().equals("save")) {
				save();
			} else if (m.getText().equals("open")) {
				open();
			} else if (m.getText().equals("save as")) {
				saveAs();
			} else if (m.getText().equals("monitor")) {
				myService.send(boundServiceName, "monitorAttach");
			} else if (m.getActionCommand().equals("examples"))
			{
				editor.setText(FileIO.getResourceFile("python/examples/" + m.getText()));
			} else if (m.getActionCommand().equals("system"))
			{
				editor.setText(FileIO.getResourceFile("python/system/" + m.getText()));
			}
		}
	}


	// TODO - put in FileUtils
	void open() {
		FileDialog file = new FileDialog(myService.getFrame(), "Open File", FileDialog.LOAD);
		file.setFile("*.py"); // Set initial filename filter
		file.setVisible(true); // Blocks
		String curFile;
		if ((curFile = file.getFile()) != null) {
			String newfilename = file.getDirectory() + curFile;
			char[] data;
			// setCursor (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			File f = new File(newfilename);
			try {
				FileReader fin = new FileReader(f);
				int filesize = (int) f.length();
				data = new char[filesize];
				fin.read(data, 0, filesize);
				editor.setText(new String(data));
				statusInfo.setText("Loaded: " + newfilename);
				filename = newfilename;
			} catch (FileNotFoundException exc) {
				statusInfo.setText("File Not Found: " + newfilename);
			} catch (IOException exc) {
				statusInfo.setText("IOException: " + newfilename);
			}
			// setCursor (Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	String filename = null;

	void save() {

		if (filename == null || !(new File(filename).exists()))
		{
			saveAs();
		} else {
			writeFile(filename, editor.getText());
		}
	}

	void saveAs() {
		FileDialog fd = new FileDialog(myService.getFrame(), "Save File",FileDialog.SAVE);
		fd.setVisible(true);
		String selectedFilename = fd.getFile();
		if (selectedFilename != null)
		{
			filename = fd.getDirectory() + selectedFilename; // new selected file
		} else {
			statusInfo.setText("canceled file save");
			return;
		}
		writeFile(filename, editor.getText());
	}
	
	
	// TODO - put in fileutils
	public boolean writeFile(String filename, String data)
	{
		File f = new File(filename);		
		try {
			FileWriter fw = new FileWriter(f);
			fw.write(data, 0, data.length());
			fw.close();
			statusInfo.setText("saved: " + filename);
		} catch (IOException exc) {
			statusInfo.setText("IOException: " + filename);
			return false;
		}
		currentFile = f;
		return true;
	}

	public JMenuItem createMenuItem(String label, String actionCommand) {
		return createMenuItem(label, -1, null, actionCommand);
	}
	
	public JMenuItem createMenuItem(String label) {
		return createMenuItem(label, -1, null, null);
	}

	public JMenuItem createMenuItem(String label, int vKey, String accelerator, String actionCommand) {
		JMenuItem mi = null;
		if (vKey == -1) {
			mi = new JMenuItem(label);
		} else {
			mi = new JMenuItem(label, vKey);
		}
		
		if (actionCommand != null)
		{
			mi.setActionCommand(actionCommand);
		}

		if (accelerator != null) {
			KeyStroke ctrlCKeyStroke = KeyStroke.getKeyStroke(accelerator);
			mi.setAccelerator(ctrlCKeyStroke);
		}

		mi.addActionListener(menuListener);
		return mi;
	}

	public void init() {

		// --------- text menu begin ------------------------
		JMenuBar bar = new JMenuBar();

		// file ----------
		JMenu file = new JMenu("file");
		file.setMnemonic(KeyEvent.VK_F);
		bar.add(file);

		file.add(createMenuItem("new"));
		file.add(createMenuItem("save", KeyEvent.VK_S, "control S", null));
		file.add(createMenuItem("save as"));
		file.add(createMenuItem("open", KeyEvent.VK_O, "control O", null));
		file.addSeparator();

		// edit ---------		
		JMenu edit = new JMenu("edit");
		edit.setMnemonic(KeyEvent.VK_E);
		bar.add(edit);
		
		// examples -----
		JMenu examples = new JMenu("examples");
		examples.setMnemonic(KeyEvent.VK_X);
		
		JMenu menu = new JMenu("Arduino");
		menu.add(createMenuItem("dynamicallyLoadProgram.py","examples"));
		examples.add(menu);

		menu = new JMenu("Clock");
		menu.add(createMenuItem("inputTest.py","examples"));
		examples.add(menu);

		menu = new JMenu("OpenCV");
		menu.add(createMenuItem("faceTracking.py","examples"));
		examples.add(menu);

		menu = new JMenu("Speech");
		menu.add(createMenuItem("sayThings.py","examples"));
		examples.add(menu);

		menu = new JMenu("system");
		menu.add(createMenuItem("monitor.py","examples"));
		examples.add(menu);

		menu = new JMenu("magabot");
		menu.add(createMenuItem("magabotTest.py","examples"));
		examples.add(menu);
		
		bar.add(examples);
		
		// system -----------
		menu = new JMenu("system");
		menu.add(createMenuItem("monitor","monitor"));
		
		bar.add(menu);

		StateActionListener state = new StateActionListener();

		// make python highlighting
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		scrollPane = new RTextScrollPane(editor);

		// TODO - change border layout
		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(bar, BorderLayout.LINE_START);

		restart.addActionListener(state);
		bar.add(restart);

		exec.addActionListener(state);
		bar.add(exec);

		display.setLayout(new BorderLayout());

		JMenuBar graphicBar = new JMenuBar();
		
		// TODO pkg gui? with get gui Icon button
		JButton b = new JButton(null,FileIO.getResourceIcon("monitor.png"));
		//b.setPreferredSize(new Dimension(32,32));
		b.setMargin(new Insets(0, 0, 0, 0)); 
		b.setBorderPainted(false);
		b.setToolTipText("monitor");
		b.setBackground(new Color(0xff00ff));

		graphicBar.add(b);
		menuPanel.add(graphicBar);
		
		display.add(menuPanel, BorderLayout.PAGE_START);

		display.setPreferredSize(new Dimension(800, 600));
		display.add(scrollPane, BorderLayout.CENTER);

		display.add(statusInfo, BorderLayout.PAGE_END);
		
		
		// TODO - LOOK GOOD STUFF! 
		// FIXME - OTHER GUI's SHOULD DO THE SAME !
		myJython = (Jython) RuntimeEnvironment.getService(boundServiceName).service;

		if (myJython != null) {
			editor.setText(myJython.getScript());
		}

	}


	public class StateActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JButton button = (JButton) e.getSource();
			myService.send(boundServiceName, button.getText(), editor.getText());
		}

	}

	// TODO put in ServiceGUI framework?
	public void getState(Jython j) {
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

	/*
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.ERROR);
		
		 JFrame f = new JFrame("This is a test");
		 ServiceGUI sg = new JythonGUI("boundServiceName", null);
		 sg.init();
		 f.add(sg.display);
		 f.setVisible(true);
	}
	*/
}
