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
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Service;
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
public class Editor extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	final static int fileMenuMnemonic = KeyEvent.VK_F;
	static final int saveMenuMnemonic = KeyEvent.VK_S;
	static final int openMenuMnemonic = KeyEvent.VK_O;
	static final int examplesMenuMnemonic = KeyEvent.VK_X;

	final JFrame top;

	final RSyntaxTextArea editor;
	JScrollPane editorScrollPane;
	final JTabbedPane editorTabs;

	JSplitPane splitPane;

	final JLabel statusLabel;
	final JLabel status;

	// TODO - check for outside modification with lastmoddate
	File currentFile;
	String currentFilename;

	JMenuBar menuBar;
	
	JMenu fileMenu = null;
	JMenu editMenu = null;
	JMenu examplesMenu = null;
	JMenu toolsMenu = null;
	JMenu helpMenu = null;
	
	// button bar buttons
	JPanel buttonBar;
	ImageButton executeButton;
	ImageButton restartButton;
	ImageButton openFileButton;
	ImageButton saveFileButton;	

	// consoles
	JTabbedPane consoleTabs;
	final Console javaConsole;
	final JTextArea jythonConsole;
	final JScrollPane jythonScrollPane;

	// autocompletion
	final CompletionProvider provider;
	final AutoCompletion ac;
	
	String syntaxStyle;
	
	/**
	 * Constructor
	 * 
	 * @param boundServiceName
	 * @param myService
	 */
	public Editor(final String boundServiceName, final GUI myService, String syntaxStyle) {
		super(boundServiceName, myService);

		this.syntaxStyle = syntaxStyle;
		
		javaConsole = new Console();  // FIXME - rename log console
		jythonConsole = new JTextArea();
		jythonScrollPane = new JScrollPane(jythonConsole);

		provider = createCompletionProvider();
		ac = new AutoCompletion(provider);

		// FYI - files are on the "Arduino" service not on the GUI - these potentially are remote objects
		currentFile = null;
		currentFilename = null;

		editor = new RSyntaxTextArea();
		editorScrollPane = null;
		editorTabs = new JTabbedPane();

		splitPane = null;

		statusLabel = new JLabel("Status:");
		status = new JLabel("");
		top = myService.getFrame();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == restartButton) {
			performRestart();
			return;
		} else if (o == executeButton) {
			performExecute();
			return;
		} else if (o == saveFileButton) {
			saveFile();
			return;
		} else if (o == openFileButton) {
			openFile();
			return;
		}

		if (!(o instanceof JMenuItem)) {
			return;
		}
		JMenuItem m = (JMenuItem) o;
		if (m.getText().equals("save")) {
			saveFile();
		} else if (m.getText().equals("open")) {
			openFile();
		} else if (m.getText().equals("save as")) {
			saveAsFile();
		} else if (m.getActionCommand().equals("examples")) {
			editor.setText(FileIO.getResourceFile(String.format("python/examples/%1$s", m.getText())));
		}
	}

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

	public void finishedExecutingScript() {
		executeButton.deactivate();
	}

	public void getState(Service j) {
		// TODO set GUI state debug from Service data

	}

	public void getStdOut(String data) {
		jythonConsole.append(data);
	}

	/**
	 * 
	 */
	public void init() {
		display.setLayout(new BorderLayout());
		display.setPreferredSize(new Dimension(800, 600));

		// default text based menu
		display.add(createMenuPanel(), BorderLayout.PAGE_START);

		DefaultCaret caret = (DefaultCaret) jythonConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		splitPane = createMainPane();

		display.add(splitPane, BorderLayout.CENTER);
		
		JPanel s = new JPanel();
		s.add(statusLabel);
		//s.add(comp)
		display.add(statusLabel, BorderLayout.PAGE_END);
	}

	/**
	 * Build the main portion of the view.
	 * 
	 * @return
	 */
	JSplitPane createMainPane() {
		JSplitPane pane = new JSplitPane();

		consoleTabs = createTabsPane();
		editorScrollPane = createEditorPane();

		pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane,
				consoleTabs);
		pane.setDividerLocation(450);

		return pane;
	}

	/**
	 * Build the editor pane.
	 * 
	 * @return
	 */
	JScrollPane createEditorPane() {
		editor.setSyntaxEditingStyle(syntaxStyle);
		editor.setCodeFoldingEnabled(true);
		editor.setAntiAliasingEnabled(true);

		// autocompletion
		ac.install(editor);
		ac.setShowDescWindow(true);

		return new RTextScrollPane(editor);
	}


	JMenu createFileMenu() {
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(fileMenuMnemonic);
		fileMenu.add(createMenuItem("New"));
		fileMenu.add(createMenuItem("Save", saveMenuMnemonic, "control S", null));
		fileMenu.add(createMenuItem("Save As"));
		fileMenu.add(createMenuItem("Open", openMenuMnemonic, "control O", null));
		fileMenu.addSeparator();
		return fileMenu;
	}

	JMenu createEditMenu() {
		editMenu = new JMenu("Edit");
		editMenu.add(createMenuItem("Undo"));
		editMenu.add(createMenuItem("Redo"));
		editMenu.addSeparator();
		editMenu.add(createMenuItem("Cut"));
		editMenu.add(createMenuItem("Copy"));
		//editMenu.add(createMenuItem("save", saveMenuMnemonic, "control S", null));
		editMenu.addSeparator();
		editMenu.add(createMenuItem("Format"));
		return editMenu;
	}

	JMenu createExamplesMenu() {
		// TODO - dynamically build based on resources
		examplesMenu = new JMenu("Examples");
		examplesMenu.setMnemonic(examplesMenuMnemonic);

		JMenu menu;
		menu = new JMenu("Arduino");
		menu.add(createMenuItem("arduinoBasic.py", "examples"));
		menu.add(createMenuItem("arduinoInput.py", "examples"));
		menu.add(createMenuItem("arduinoOutput.py", "examples"));
		menu.add(createMenuItem("arduinoServo.py", "examples"));
		examplesMenu.add(menu);

		menu = new JMenu("Basic");
		menu.add(createMenuItem("createAService.py", "examples"));
		menu.add(createMenuItem("basicPython.py", "examples"));
		examplesMenu.add(menu);

		menu = new JMenu("Input");
		menu.add(createMenuItem("inputTest.py", "examples"));
		examplesMenu.add(menu);

		menu = new JMenu("Speech");
		menu.add(createMenuItem("sayThings.py", "examples"));
		menu.add(createMenuItem("talkBack.py", "examples"));
		examplesMenu.add(menu);

		menu = new JMenu("Vision");
		menu.add(createMenuItem("faceTracking.py", "examples"));
		examplesMenu.add(menu);
		
		return examplesMenu;
	}

	JMenu createToolsMenu() {
		toolsMenu = new JMenu("Tools");
		return toolsMenu;
	}

	JMenu createHelpMenu() {
		helpMenu = new JMenu("Help");
		return helpMenu;
	}
	
	
	CompletionProvider createCompletionProvider() {
		// TODO -> LanguageSupportFactory.get().register(editor);

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		return new JavaCompletionProvider();
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @return
	 */
	JMenuItem createMenuItem(String label) {
		return createMenuItem(label, -1, null, null);
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @param actionCommand
	 * @return
	 */
	JMenuItem createMenuItem(String label, String actionCommand) {
		return createMenuItem(label, -1, null, actionCommand);
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @param vKey
	 * @param accelerator
	 * @param actionCommand
	 * @return
	 */
	JMenuItem createMenuItem(String label, int vKey,
			String accelerator, String actionCommand) {
		JMenuItem mi = null;
		if (vKey == -1) {
			mi = new JMenuItem(label);
		} else {
			mi = new JMenuItem(label, vKey);
		}

		if (actionCommand != null) {
			mi.setActionCommand(actionCommand);
		}

		if (accelerator != null) {
			KeyStroke ctrlCKeyStroke = KeyStroke.getKeyStroke(accelerator);
			mi.setAccelerator(ctrlCKeyStroke);
		}

		mi.addActionListener(this);
		return mi;
	}

	
	JPanel createMenuPanel() {
		menuBar = createTopMenuBar();
		buttonBar = createTopButtonBar();

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(menuBar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		return menuPanel;
	}

	JTabbedPane createTabsPane() {
		JTabbedPane pane = new JTabbedPane();
		pane.addTab("java", javaConsole.getScrollPane());
		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl(top,
				pane, javaConsole.getScrollPane(), boundServiceName, "java"));

		pane.addTab("jython", jythonScrollPane);
		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl(top,
				pane, jythonScrollPane, boundServiceName, "jython"));

		return pane;
	}

	JPanel createTopButtonBar() {
		buttonBar = new JPanel(); // returns empty - no common set - change in future?
		/*
		executeButton = new ImageButton("Jython", "execute", this);
		restartButton = new ImageButton("Jython", "restart", this);
		openFileButton = new ImageButton("Jython", "open", this);
		saveFileButton = new ImageButton("Jython", "save", this);
		


		buttonBar.add(openFileButton);
		buttonBar.add(saveFileButton);
		buttonBar.add(restartButton);
		buttonBar.add(executeButton);
		
		buttonBar.setBackground(new Color(0,100,104));
		*/
		return buttonBar;
	}

	/**
	 * Build up the top text menu bar.
	 * 
	 * @return the menu bar filled with the top-level options.
	 */
	JMenuBar createTopMenuBar() {
		menuBar = new JMenuBar();		

		menuBar.add(createFileMenu());
		menuBar.add(createEditMenu());
		menuBar.add(createExamplesMenu());
		menuBar.add(createToolsMenu());
		menuBar.add(createHelpMenu());

		return menuBar;
	}

	/**
	 * 
	 */
	void openFile() {
		// TODO does this need to be closed?
		String newfile = FileUtil.open(top, "*.py");
		if (newfile != null) {
			editor.setText(newfile);
			statusLabel.setText("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		statusLabel.setText(FileUtil.getLastStatus());
		return;
	}

	/**
	 * Perform an execute action.
	 */
	void performExecute() {
		executeButton.activate();
		restartButton.deactivate();
		javaConsole.startLogging(); // Hmm... noticed this is only local JVM
									// :) the Jython console can be pushed
									// over the network
		myService.send(boundServiceName, "attachJythonConsole");
		myService.send(boundServiceName, "exec", editor.getText());
	}

	/**
	 * Perform the restart action.
	 */
	void performRestart() {
		restartButton.activate();
		executeButton.deactivate();
		myService.send(boundServiceName, "restart");
	}

	/**
	 * 
	 */
	void saveAsFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.saveAs(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}

	/**
	 * 
	 */
	void saveFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.save(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}
	
	public ImageButton addImageButtonToButtonBar(String resourceDir, String name, ActionListener al)
	{
		ImageButton ret = new ImageButton(resourceDir,name, al);
		buttonBar.add(ret);
		return ret;
	}
	
	public void setStatus(String s)
	{
		status.setText(s);
	}

}
