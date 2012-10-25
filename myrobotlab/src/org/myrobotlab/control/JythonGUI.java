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
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.ui.autocomplete.MRLCompletionProvider;

/**
 * Jython GUI
 * 
 * @author SwedaKonsult
 * 
 */
public class JythonGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;
	private final static int fileMenuMnemonic = KeyEvent.VK_F;
	private static final int saveMenuMnemonic = KeyEvent.VK_S;
	private static final int openMenuMnemonic = KeyEvent.VK_O;
	private static final int examplesMenuMnemonic = KeyEvent.VK_X;

	final JFrame top;

	final RSyntaxTextArea editor;
	JScrollPane editorScrollPane;
	final JTabbedPane editorTabs;

	JSplitPane splitPane;

	final JLabel statusInfo;

	// TODO - check for outside modification with lastmoddate
	File currentFile;
	String currentFilename;

	// button bar buttons
	ImageButton executeButton;
	ImageButton stopButton;

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

	/**
	 * Constructor
	 * 
	 * @param boundServiceName
	 * @param myService
	 */
	public JythonGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);

		javaConsole = new Console();
		jythonConsole = new JTextArea();
		jythonScrollPane = new JScrollPane(jythonConsole);

		// autocompletion - in the constructor so that they can be declared final
		provider = createCompletionProvider();
		ac = new AutoCompletion(provider);

		currentFile = null;
		currentFilename = null;

		editor = new RSyntaxTextArea();
		editorScrollPane = null;
		editorTabs = new JTabbedPane();

		splitPane = null;

		statusInfo = new JLabel("Status:");
		top = myService.getFrame();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Object o = arg0.getSource();
		if (o == stopButton) {
			performStop();
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
			editor.setText(FileIO.getResourceFile(String.format("Jython/examples/%1$s", m.getText())));
			editor.setCaretPosition(0);
		}
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Jython.class);
		subscribe("finishedExecutingScript");
		subscribe("publishStdOut", "getStdOut", String.class);
		myService.send(boundServiceName, "attachJythonConsole");
		// myService.send(boundServiceName, "broadcastState");
	}

	@Override
	public void detachGUI() {
		javaConsole.stopLogging();
		unsubscribe("publishState", "getState", Jython.class);
		unsubscribe("finishedExecutingScript");
		unsubscribe("publishStdOut", "getStdOut", String.class);
	}

	/**
	 * 
	 */
	public void finishedExecutingScript() {
		executeButton.deactivate();
		stopButton.deactivate();
	}

	/**
	 * 
	 * @param j
	 */
	public void getState(Jython j) {
		// TODO set GUI state debug from Service data

	}

	/**
	 * 
	 * @param data
	 */
	public void getStdOut(String data) {
		jythonConsole.append(data);
	}

	/**
	 * 
	 */
	public void init() {
		display.setLayout(new BorderLayout());
		display.setPreferredSize(new Dimension(800, 600));

		// --------- text menu begin ------------------------
		JPanel menuPanel = createMenuPanel();

		display.add(menuPanel, BorderLayout.PAGE_START);

		DefaultCaret caret = (DefaultCaret) jythonConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		splitPane = createMainPane();

		display.add(splitPane, BorderLayout.CENTER);
		display.add(statusInfo, BorderLayout.PAGE_END);
	}

	/**
	 * Build the main portion of the view.
	 * 
	 * @return
	 */
	private JSplitPane createMainPane() {
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
	private JScrollPane createEditorPane() {
		// editor tweaks
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		editor.setCodeFoldingEnabled(true);
		editor.setAntiAliasingEnabled(true);

		// autocompletion
		ac.install(editor);
		ac.setShowDescWindow(true);

		return new RTextScrollPane(editor);
	}

	/**
	 * Fill up the examples menu with submenu items.
	 * 
	 * @param examples
	 */
	private void createExamplesMenu(JMenu examples) {
		// FIXME - dynamically build based on resources
		JMenu menu;
		menu = new JMenu("Arduino");
		menu.add(createMenuItem("arduinoInput.py", "examples"));
		menu.add(createMenuItem("arduinoOutput.py", "examples"));
		menu.add(createMenuItem("arduinoLoopback.py", "examples"));
		examples.add(menu);

		menu = new JMenu("Python");
		menu.add(createMenuItem("createAService.py", "examples"));
		menu.add(createMenuItem("basicPython.py", "examples"));
		menu.add(createMenuItem("panTilt.py", "examples"));
		examples.add(menu);

		menu = new JMenu("Ser");
		menu.add(createMenuItem("createAService.py", "examples"));
		
		menu = new JMenu("input");
		menu.add(createMenuItem("inputTest.py", "examples"));
		examples.add(menu);

		menu = new JMenu("Speech");
		menu.add(createMenuItem("sayThings.py", "examples"));
		menu.add(createMenuItem("talkBack.py", "examples"));
		examples.add(menu);

		menu = new JMenu("Vision");
		menu.add(createMenuItem("faceTracking.py", "examples"));
		menu.add(createMenuItem("colorTracking.py", "examples"));
		menu.add(createMenuItem("lkOpticalTrack.py", "examples"));
		examples.add(menu);
	}

	/**
	 * Fill up the file menu with submenu items.
	 * 
	 * @param fileMenu
	 */
	private void createFileMenu(JMenu fileMenu) {
		fileMenu.add(createMenuItem("new"));
		fileMenu.add(createMenuItem("save", saveMenuMnemonic, "control S", null));
		fileMenu.add(createMenuItem("save as"));
		fileMenu.add(createMenuItem("open", openMenuMnemonic, "control O", null));
		fileMenu.addSeparator();
	}

	/**
	 * 
	 * @return
	 */
	private CompletionProvider createCompletionProvider() {
		// TODO -> LanguageSupportFactory.get().register(editor);

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		return new MRLCompletionProvider();
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @return
	 */
	private JMenuItem createMenuItem(String label) {
		return createMenuItem(label, -1, null, null);
	}

	/**
	 * Helper function to create a menu item.
	 * 
	 * @param label
	 * @param actionCommand
	 * @return
	 */
	private JMenuItem createMenuItem(String label, String actionCommand) {
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
	private JMenuItem createMenuItem(String label, int vKey,
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

	/**
	 * Build the top menu panel.
	 * 
	 * @return
	 */
	private JPanel createMenuPanel() {
		JMenuBar menuBar = createTopMenuBar();
		JPanel buttonBar = createTopButtonBar();

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(menuBar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		return menuPanel;
	}

	/**
	 * Build the tabs pane.
	 * 
	 * @return
	 */
	private JTabbedPane createTabsPane() {
		JTabbedPane pane = new JTabbedPane();
		pane.addTab("java", javaConsole.getScrollPane());
		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl(top,
				pane, javaConsole.getScrollPane(), boundServiceName, "java"));

		pane.addTab("jython", jythonScrollPane);
		pane.setTabComponentAt(pane.getTabCount() - 1, new TabControl(top,
				pane, jythonScrollPane, boundServiceName, "jython"));

		return pane;
	}

	/**
	 * Build up the top button menu bar.
	 * 
	 * @return
	 */
	private JPanel createTopButtonBar() {
		executeButton = new ImageButton("Jython", "execute", this);
		stopButton = new ImageButton("Jython", "stop", this);
		openFileButton = new ImageButton("Jython", "open", this);
		;
		saveFileButton = new ImageButton("Jython", "save", this);
		;

		JPanel buttonBar = new JPanel();
		buttonBar.add(openFileButton);
		buttonBar.add(saveFileButton);
		buttonBar.add(stopButton);
		buttonBar.add(executeButton);

		return buttonBar;
	}

	/**
	 * Build up the top text menu bar.
	 * 
	 * @return the menu bar filled with the top-level options.
	 */
	private JMenuBar createTopMenuBar() {
		JMenuBar menuBar = new JMenuBar();		// file ----------
		JMenu fileMenu = new JMenu("file");
		menuBar.add(fileMenu);
		fileMenu.setMnemonic(fileMenuMnemonic);
		createFileMenu(fileMenu);

		// examples -----
		JMenu examples = new JMenu("examples");
		menuBar.add(examples);
		examples.setMnemonic(examplesMenuMnemonic);
		createExamplesMenu(examples);

		return menuBar;
	}

	/**
	 * 
	 */
	private void openFile() {
		// TODO does this need to be closed?
		String newfile = FileUtil.open(top, "*.py");
		if (newfile != null) {
			editor.setText(newfile);
			statusInfo.setText("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		statusInfo.setText(FileUtil.getLastStatus());
		return;
	}

	/**
	 * Perform an execute action.
	 */
	private void performExecute() {
		executeButton.activate();
		stopButton.deactivate();
		javaConsole.startLogging(); // Hmm... noticed this is only local JVM
									// :) the Jython console can be pushed
									// over the network
		myService.send(boundServiceName, "exec", editor.getText());
	}

	/**
	 * Perform the restart action.
	 */
	private void performStop() {
		stopButton.activate();
		//executeButton.deactivate();
		myService.send(boundServiceName, "stop");
		myService.send(boundServiceName, "attachJythonConsole");
	}

	/**
	 * 
	 */
	private void saveAsFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.saveAs(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}

	/**
	 * 
	 */
	private void saveFile() {
		// TODO do we need to handle errors with permissions?
		if (FileUtil.save(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}

}
