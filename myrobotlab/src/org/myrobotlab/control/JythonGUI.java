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
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.interfaces.GUI;

public class JythonGUI extends ServiceGUI implements ActionListener {

	static final long serialVersionUID = 1L;

	JFrame top = myService.getFrame();
	
	RSyntaxTextArea editor = new RSyntaxTextArea();
	RTextScrollPane editorScrollPane = null;
	JTabbedPane editorTabs = new JTabbedPane();
	
	JSplitPane splitPane = null;
	
	JLabel statusInfo = new JLabel("Status:");

	// TODO - check for outside modification with lastmoddate
	File currentFile = null;
	String currentFilename = null;

	// button bar buttons
	ImageButton executeButton;
	ImageButton restartButton;

	ImageButton openFileButton;
	ImageButton saveFileButton;
	
	// consoles
	JTabbedPane consoleTabs;
	Console javaConsole = new Console();
	JTextArea jythonConsole = new JTextArea();
	JScrollPane jythonScrollPane = new JScrollPane(jythonConsole);

	// autocompletion
	CompletionProvider provider = createCompletionProvider();
	AutoCompletion ac = new AutoCompletion(provider);

	public JythonGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
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

	public void init() {

		// editor tweaks
		editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
		editorScrollPane = new RTextScrollPane(editor);
		editor.setCodeFoldingEnabled(true);
		editor.setAntiAliasingEnabled(true);

		// autocompletion
		ac.install(editor);
		ac.setShowDescWindow(true);
		
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

		/*
		// edit ---------
		JMenu edit = new JMenu("edit");
		edit.setMnemonic(KeyEvent.VK_E);
		bar.add(edit);
		*/

		// examples -----
		JMenu examples = new JMenu("examples");
		examples.setMnemonic(KeyEvent.VK_X);
		/*
		 * JMenu menu = new JMenu("arduino");
		 * menu.add(createMenuItem("dynamicallyLoadProgram.py","examples"));
		 * examples.add(menu);
		 * 
		 * menu = new JMenu("chumby");
		 * menu.add(createMenuItem("chumby.py","examples")); examples.add(menu);
		 */
		
		// TODO - dynamically build based on resources
		
		JMenu menu;
		
		/*
		JMenu menu = new JMenu("magabot");
		menu.add(createMenuItem("magabotTest.py", "examples"));
		menu.add(createMenuItem("magabotSpeechTest.py", "examples"));
		examples.add(menu);
		*/
		menu = new JMenu("basic");
		menu.add(createMenuItem("createAService.py", "examples"));
		menu.add(createMenuItem("basicPython.py", "examples"));
		examples.add(menu);
		
		menu = new JMenu("input");
		menu.add(createMenuItem("inputTest.py", "examples"));
		examples.add(menu);

		menu = new JMenu("speech");
		menu.add(createMenuItem("sayThings.py", "examples"));
		menu.add(createMenuItem("talkBack.py", "examples"));
		//menu.add(createMenuItem("faceTracking.py", "examples"));

		examples.add(menu);

		menu = new JMenu("vision");
		menu.add(createMenuItem("faceTracking.py", "examples"));
		examples.add(menu);

		/*
		menu = new JMenu("system");
		menu.add(createMenuItem("jythonConsole.py", "examples"));
		examples.add(menu);
		
		*/

		/*
		 * menu = new JMenu("mrlbots");
		 * menu.add(createMenuItem("minibot.py","examples"));
		 * examples.add(menu);
		 */

		bar.add(examples);

		// system -----------
		/*
		menu = new JMenu("system");
		menu.add(createMenuItem("jython console", "jython console"));
		*/

		display.setLayout(new BorderLayout());

		executeButton = new ImageButton("Jython", "execute", this); 
		restartButton = new ImageButton("Jython", "restart", this);
		openFileButton = new ImageButton("Jython", "open", this);;
		saveFileButton = new ImageButton("Jython", "save", this);;

		JPanel buttonBar = new JPanel();
		buttonBar.add(openFileButton);
		buttonBar.add(saveFileButton);
		buttonBar.add(restartButton);
		buttonBar.add(executeButton);

		JPanel menuPanel = new JPanel(new BorderLayout());
		menuPanel.add(bar, BorderLayout.LINE_START);
		menuPanel.add(buttonBar);

		display.add(menuPanel, BorderLayout.PAGE_START);
		display.setPreferredSize(new Dimension(800, 600));
		//display.setSize(new Dimension(800, 600));
		//display.setMinimumSize(new Dimension(800, 600));

		DefaultCaret caret = (DefaultCaret)jythonConsole.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	
		consoleTabs = new JTabbedPane();
		consoleTabs.addTab("java", javaConsole.getScrollPane());
		consoleTabs.setTabComponentAt(consoleTabs.getTabCount() - 1, new TabControl(top, consoleTabs, javaConsole.getScrollPane(), boundServiceName, "java"));
		
		consoleTabs.addTab("jython", jythonScrollPane);
		consoleTabs.setTabComponentAt(consoleTabs.getTabCount() - 1, new TabControl(top, consoleTabs, jythonScrollPane, boundServiceName, "jython"));
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleTabs);
		//new ComponentResizer(splitPane, editorScrollPane, editor);
		
		//splitPane.setLayout(new BorderLayout());
		splitPane.setDividerLocation(450);
		
		display.add(splitPane, BorderLayout.CENTER);
		display.add(statusInfo, BorderLayout.PAGE_END);
		
		//resizer.registerComponent(editor);
		//resizer.registerComponent(editorScrollPane);
	}

	public void getState(Jython j) {
		// TODO set GUI state debug from Service data

	}

	@Override
	public void attachGUI() {
		sendNotifyRequest("publishState", "getState", Jython.class);
		sendNotifyRequest("finishedExecutingScript");
		sendNotifyRequest("publishStdOut","getStdOut", String.class);		
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("publishStdOut","getStdOut", String.class);		
		removeNotifyRequest("finishedExecutingScript");
		removeNotifyRequest("publishState", "getState", Jython.class);
	}

	public void finishedExecutingScript()
	{
		executeButton.deactivate();
	}
	
	public void getStdOut (String data)
	{
		jythonConsole.append(data);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		Object o = arg0.getSource();
		if (o == restartButton) {
			restartButton.activate();
			executeButton.deactivate();
			myService.send(boundServiceName, "restart");
			return;
		} else if (o == executeButton) {
			executeButton.activate();
			restartButton.deactivate(); 
			javaConsole.startLogging(); // Hmm... noticed this is only local JVM :) the Jython console can be pushed over the network
			myService.send(boundServiceName, "attachJythonConsole");
			myService.send(boundServiceName, "exec", editor.getText());
			return;
		} else if (o == saveFileButton) {
			saveFile();
			return;
		} else if (o == openFileButton) {
			openFile();
			return;
		}

		JMenuItem m = (JMenuItem) arg0.getSource();
		if (m.getText().equals("save")) {
			saveFile();
		} else if (m.getText().equals("open")) {
			openFile();
		} else if (m.getText().equals("save as")) {
			saveAsFile();
		} else if (m.getActionCommand().equals("examples")) {
			editor.setText(FileIO.getResourceFile("python/examples/" + m.getText()));
//		} else if (m.getActionCommand().equals("system")) {
//			editor.setText(FileIO.getResourceFile("python/system/" + m.getText()));
		}
	}

	public void saveAsFile()
	{
		if (FileUtil.saveAs(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}
	
	public void saveFile()
	{
		if (FileUtil.save(top, editor.getText(), currentFilename))
			currentFilename = FileUtil.getLastFileSaved();
	}
	
	public void openFile()
	{
		String newfile = FileUtil.open(top,"*.py");
		if (newfile != null)
		{
			editor.setText(newfile);
			statusInfo.setText("Loaded: " + FileUtil.getLastFileOpened());
			return;
		}
		statusInfo.setText(FileUtil.getLastStatus());
		return;

	}
	
	private CompletionProvider createCompletionProvider() {

		// TODO -> LanguageSupportFactory.get().register(editor);

		// A DefaultCompletionProvider is the simplest concrete implementation
		// of CompletionProvider. This provider has no understanding of
		// language semantics. It simply checks the text entered up to the
		// caret position for a match against known completions. This is all
		// that is needed in the majority of cases.
		DefaultCompletionProvider provider = new DefaultCompletionProvider();

		/*
		 * try { provider.loadFromXML(new File("c.xml")); } catch (IOException
		 * e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */

		// BasicCompletion bc = new BasicCompletion(arg0, arg1, arg2, arg3);

		// Add completions for all Java keywords. A BasicCompletion is just
		// a straightforward word completion.

		provider.addCompletion(new BasicCompletion(provider, "abstract", "blah", "<html><body>hello</body></html>"));
		provider.addCompletion(new BasicCompletion(provider, "assert"));
		provider.addCompletion(new BasicCompletion(provider, "break"));
		provider.addCompletion(new BasicCompletion(provider, "case"));
		provider.addCompletion(new BasicCompletion(provider, "catch"));
		provider.addCompletion(new BasicCompletion(provider, "class"));
		provider.addCompletion(new BasicCompletion(provider, "const"));
		provider.addCompletion(new BasicCompletion(provider, "continue"));
		provider.addCompletion(new BasicCompletion(provider, "default"));
		provider.addCompletion(new BasicCompletion(provider, "do"));
		provider.addCompletion(new BasicCompletion(provider, "else"));
		provider.addCompletion(new BasicCompletion(provider, "enum"));
		provider.addCompletion(new BasicCompletion(provider, "extends"));
		provider.addCompletion(new BasicCompletion(provider, "final"));
		provider.addCompletion(new BasicCompletion(provider, "finally"));
		provider.addCompletion(new BasicCompletion(provider, "for"));
		provider.addCompletion(new BasicCompletion(provider, "goto"));
		provider.addCompletion(new BasicCompletion(provider, "if"));
		provider.addCompletion(new BasicCompletion(provider, "implements"));
		provider.addCompletion(new BasicCompletion(provider, "import"));
		provider.addCompletion(new BasicCompletion(provider, "instanceof"));
		provider.addCompletion(new BasicCompletion(provider, "interface"));
		provider.addCompletion(new BasicCompletion(provider, "native"));
		provider.addCompletion(new BasicCompletion(provider, "new"));
		provider.addCompletion(new BasicCompletion(provider, "package"));
		provider.addCompletion(new BasicCompletion(provider, "private"));
		provider.addCompletion(new BasicCompletion(provider, "protected"));
		provider.addCompletion(new BasicCompletion(provider, "public"));
		provider.addCompletion(new BasicCompletion(provider, "return"));
		provider.addCompletion(new BasicCompletion(provider, "static"));
		provider.addCompletion(new BasicCompletion(provider, "strictfp"));
		provider.addCompletion(new BasicCompletion(provider, "super"));
		provider.addCompletion(new BasicCompletion(provider, "switch"));
		provider.addCompletion(new BasicCompletion(provider, "synchronized"));
		provider.addCompletion(new BasicCompletion(provider, "this"));
		provider.addCompletion(new BasicCompletion(provider, "throw"));
		provider.addCompletion(new BasicCompletion(provider, "throws"));
		provider.addCompletion(new BasicCompletion(provider, "transient"));
		provider.addCompletion(new BasicCompletion(provider, "try"));
		provider.addCompletion(new BasicCompletion(provider, "void"));
		provider.addCompletion(new BasicCompletion(provider, "volatile"));
		provider.addCompletion(new BasicCompletion(provider, "while"));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		provider.addCompletion(new ShorthandCompletion(provider, "sysout", "System.out.println(", "System.out.println("));
		provider.addCompletion(new ShorthandCompletion(provider, "syserr", "System.err.println(", "System.err.println("));

		return provider;

	}

}
