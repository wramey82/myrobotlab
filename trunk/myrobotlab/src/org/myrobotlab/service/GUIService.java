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

package org.myrobotlab.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.control.GUIServiceGUI;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.TabControl;
import org.myrobotlab.control.Welcome;
import org.myrobotlab.control.widget.AboutDialog;
import org.myrobotlab.control.widget.ConnectDialog;
import org.myrobotlab.control.widget.Console;
import org.myrobotlab.control.widget.UndockedPanel;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.LogAppender;
import org.myrobotlab.logging.LogLevel;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.string.Util;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

/*
 * GUI -> Look at service registry
 * GUI -> attempt to create a panel for each registered service
 * 		GUI -> create panel
 *      GUI -> panel.init(this, serviceName);
 *      	   panel.send(Notify, someoutputfn, GUIName, panel.inputfn, data);
 *  
 *       
 *       
 *       serviceName (source) --> GUI-> msg
 * Arduino arduino01 -> post message -> outbox -> outbound -> notifyList -> reference of sender? (NO) will not transport
 * across process boundry 
 * 
 * 		serviceGUI needs a Runtime
 * 		Arduino arduin-> post back (data) --> GUI - look up serviceGUI by senders name ServiceGUI->invoke(data)
 * 
 * References :
 * http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components-To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 */

@Root
public class GUIService extends GUI implements WindowListener, ActionListener, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(GUIService.class.getCanonicalName());

	public String graphXML = "";

	public transient JFrame frame = null;

	/**
	 * class to save the position and size of undocked panels
	 * 
	 */

	@ElementMap(entry = "serviceType", value = "dependsOn", attribute = true, inline = true, required = false)
	public HashMap<String, UndockedPanel> undockedPanels = new HashMap<String, UndockedPanel>();

	/**
	 * all the panels
	 */
	public transient JTabbedPane tabs = new JTabbedPane();

	/**
	 * the GUIService's gui
	 */
	public transient GUIServiceGUI guiServiceGUI = null;
	/**
	 * welcome panel
	 */
	transient Welcome welcome = null;
	transient HashMap<String, ServiceGUI> serviceGUIMap = new HashMap<String, ServiceGUI>();

	/**
	 * hashmap "quick lookup" of panels
	 */
	transient HashMap<String, JPanel> tabPanelMap = new HashMap<String, JPanel>();
	transient Map<String, ServiceWrapper> sortedMap = null;
	HashMap<String, Object> commandMap = new HashMap<String, Object>();

	transient GridBagConstraints gc = null;

	String selectedTabTitle = null;

	public GUIService(String n) {
		super(n, GUIService.class.getCanonicalName());

		/*
		 * The commandMap is a list of GUIService functions which should be
		 * processed by GUIService rather than routed to control Panels FIXME
		 * !!! - dynamically generate "all" top level functions getMethods +
		 * Service ?
		 */

		commandMap.put("registerServicesEvent", null);
		commandMap.put("registerServices", null);
		commandMap.put("loadTabPanels", null);
		commandMap.put("registerServicesNotify", null);
		commandMap.put("addListener", null);
		commandMap.put("removeListener", null);
		commandMap.put("guiUpdated", null);
		commandMap.put("setRemoteConnectionStatus", null);

		load();

	}

	public boolean hasDisplay() {
		return true;
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public HashMap<String, ServiceGUI> getServiceGUIMap() {
		return serviceGUIMap;
	}

	public boolean preProcessHook(Message m) {
		if (commandMap.containsKey(m.method)) {
			return true;
		}

		ServiceGUI sg = serviceGUIMap.get(m.sender);
		if (sg == null) {
			log.error("attempting to update sub-gui - sender " + m.sender + " not available in map " + getName());
		} else {
			invoke(serviceGUIMap.get(m.sender), m.method, m.data);
		}

		return false;
	}

	/**
	 * method to construct ServiceGUIs - similar to the Service.getNewInstance
	 * but specifically for swing GUIs
	 * 
	 * @param classname
	 * @param boundServiceName
	 * @param service
	 * @return
	 */
	static public Object getNewInstance(String classname, String boundServiceName, GUI service) {
		try {
			Object[] params = new Object[2];
			params[0] = boundServiceName;
			params[1] = service;
			Class<?> c;
			c = Class.forName(classname);
			Constructor<?> mc = c.getConstructor(new Class[] { String.class, GUI.class });
			return mc.newInstance(params);
		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (SecurityException e) {
			logException(e);
		} catch (NoSuchMethodException e) {
			logException(e);
		} catch (IllegalArgumentException e) {
			logException(e);
		} catch (InstantiationException e) {
			logException(e);
		} catch (IllegalAccessException e) {
			logException(e);
		} catch (InvocationTargetException e) {
			logException(e);
		}
		return null;
	}

	public void buildTabPanels() {
		// add the welcome screen
		if (!serviceGUIMap.containsKey("welcome")) {
			welcome = new Welcome("", this);
			welcome.init();
			tabs.addTab("Welcome", welcome.display);
			tabs.setTabComponentAt(0, new JLabel("Welcome"));
			serviceGUIMap.put("welcome", welcome);
		}

		HashMap<String, ServiceWrapper> services = Runtime.getRegistry();
		log.info("buildTabPanels service count " + Runtime.getRegistry().size());

		sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		while (it.hasNext()) {
			String serviceName = it.next();
			addTab(serviceName);
		}

		frame.pack();
	}

	// TODO - get index based on name
	public void addTab(String serviceName) {
		// ================= begin addTab(name) =============================
		ServiceWrapper sw = Runtime.getServiceWrapper(serviceName);

		if (sw == null) {
			log.error(String.format("addTab %1$s can not proceed - %1$s does not exist in registry (yet?)", serviceName));
			return;
		}

		// SW sent in registerServices - yet Service is null due to incompatible
		// Service Types
		// FIXME - Solution ??? - send SW with "suggested type ???" Android
		// --becomes--> AndroidController :)
		if (sw.get() == null) {
			log.error(String.format("%1$s does not have a valid Service - not exported ???", serviceName));
			return;
		}

		// get service type class name TODO
		String serviceClassName = sw.get().getClass().getCanonicalName();
		String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
		guiClass = "org.myrobotlab.control" + guiClass + "GUI";

		if (serviceGUIMap.containsKey(sw.name)) {
			log.debug(String.format("not creating %1$s gui - it already exists", sw.name));
			return;
		}

		ServiceGUI newGUI = createTabbedPanel(serviceName, guiClass, sw);
		// woot - got index !
		int index = tabs.indexOfTab(serviceName) - 1;

		if (newGUI != null) {
			++index;
		}

		guiServiceGUI = (GUIServiceGUI) serviceGUIMap.get(getName());
		if (guiServiceGUI != null) {
			guiServiceGUI.rebuildGraph();
		}
		/***
		 * UNDOCKED if (undockedPanels.containsKey(serviceName)) { UndockedPanel
		 * panel = undockedPanels.get(serviceName); if (!panel.isDocked) {
		 * undockPanel(serviceName); } }
		 ***/

		// iterate through tabcomponents

		// if data - undock
		// tabs.gett
		// FIXME - put in addTab remove from here

		// for (int i = 0; i < tabs.getTabCount() - 1; ++i)
		// {
		Component c = tabs.getTabComponentAt(index);
		if (c instanceof TabControl) {
			TabControl tc = (TabControl) c;
			// String serviceName = tc.getText();
			if (undockedPanels.containsKey(serviceName)) {
				UndockedPanel up = undockedPanels.get(serviceName);
				if (!up.isDocked) {
					tc.undockPanel();
				}
			}

			// }
			log.info(c);
		}

		frame.pack();
	}

	public void removeTab(String name) {
		log.info("removeTab");
		// log.info(String.format("removeTab removing [%1$s] current tab size is %d",
		// name, serviceGUIMap.size()));

		// detaching & removing the ServiceGUI
		ServiceGUI sg = serviceGUIMap.get(name);
		if (sg != null) {
			sg.detachGUI();
		} else {
			// warn not error - because service may have been removed recently
			log.warn(String.format("%1$s was not in the serviceGUIMap - unable to preform detach", name));
		}

		// removing the tab
		JPanel tab = tabPanelMap.get(name);
		if (tab != null) {
			tabs.remove(tab);
			serviceGUIMap.remove(name);
			tabPanelMap.remove(name);

			log.info(String.format("removeTab new size %1$d", serviceGUIMap.size()));
		} else {
			log.error(String.format("can not removeTab ", name));
		}

		guiServiceGUI = (GUIServiceGUI) serviceGUIMap.get(getName());
		if (guiServiceGUI != null) {
			guiServiceGUI.rebuildGraph();
		}
		frame.pack();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.myrobotlab.service.interfaces.GUI#loadTabPanels() This is a bit
	 * "big hammer" in that it destroys all panels and rebuilds the GUI don't
	 * use except to initially build
	 * 
	 * FIXME - remove - residual kruft
	 */

	boolean test = true;

	public JTabbedPane loadTabPanels() {
		if (test) {
			buildTabPanels();
			return null;
		}

		return tabs;

	}

	public synchronized void removeAllTabPanels() {
		log.info("tab count" + tabs.getTabCount());
		while (tabs.getTabCount() > 0) {
			tabs.remove(0);
		}

	}

	/**
	 * attempts to create a new ServiceGUI and add it to the map
	 * 
	 * @param serviceName
	 * @param guiClass
	 * @param sw
	 * @return
	 */

	// public ComponentResizer resizer = new ComponentResizer();
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, ServiceWrapper sw) {
		ServiceGUI gui = null;
		ServiceInterface se = sw.get();
		if (serviceName.equals("python")) {
			log.info("here");
		}
		gui = (ServiceGUI) getNewInstance(guiClass, se.getName(), this);

		if (gui != null) {
			gui.init();
			serviceGUIMap.put(serviceName, gui);
			tabPanelMap.put(serviceName, gui.getDisplay());
			gui.attachGUI();

			tabs.addTab(serviceName, gui.getDisplay());

			if (sw.isLocal()) {
				tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(this, tabs, gui.getDisplay(), serviceName));
			} else {
				// create hash color for hsv from accessURI
				Color hsv = new Color(Color.HSBtoRGB(Float.parseFloat(String.format("0.%d", Math.abs(sw.getAccessURL().hashCode()))), 0.8f, 0.7f));
				int index = tabs.indexOfTab(serviceName);
				tabs.setBackgroundAt(index, hsv);
				tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(this, tabs, gui.getDisplay(), serviceName, Color.white, hsv));
			}

		} else {
			log.warn("could not construct a " + guiClass + " object");
		}

		return gui;
	}

	public static List<Component> getAllComponents(final Container c) {
		Component[] comps = c.getComponents();
		List<Component> compList = new ArrayList<Component>();
		for (Component comp : comps) {
			compList.add(comp);
			if (comp instanceof Container)
				compList.addAll(getAllComponents((Container) comp));
		}
		return compList;
	}

	/*
	 * public class UndockedWidgetWindowAdapter extends WindowAdapter { private
	 * String name; JFrame myFrame; GUI parent;
	 * 
	 * public UndockedWidgetWindowAdapter(JFrame myFrame, GUI parent, String
	 * name) { this.myFrame = myFrame; this.parent = parent; this.name = name; }
	 * 
	 * public void windowClosing(WindowEvent winEvt) { //dockPanel(name);
	 * myFrame.dispose(); } }
	 */

	// how to do re-entrant - reconstruct all correctly - or avoid building
	// twice ?
	// I'm going with the "easy" approach

	boolean isDisplaying = false;

	public void display() {
		if (!isDisplaying) {
			// reentrant
			if (frame != null) {
				frame.dispose();
				frame = null;
			}

			if (frame == null) {
				frame = new JFrame();
			}

			gc = new GridBagConstraints();

			frame.addWindowListener(this);
			frame.setTitle("myrobotlab - " + getName());

			buildTabPanels();

			frame.add(tabs);

			URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
			Toolkit kit = Toolkit.getDefaultToolkit();
			Image img = kit.createImage(url);
			frame.setIconImage(img);

			// menu
			frame.setJMenuBar(buildMenu());
			frame.setVisible(true);
			frame.pack();
			isDisplaying = true;
		}

	}

	private static void open(URI uri) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(uri);
			} catch (IOException e) {
				// TODO: error handling
			}
		} else {
			// TODO: error handling
		}
	}

	public void registerServicesNotify() {
		loadTabPanels();
		invoke("guiUpdated");
	}

	public void guiUpdated() {
		log.info("guiUpdated");
	}

	public String setRemoteConnectionStatus(String state) {
		welcome.setRemoteConnectionStatus(state);
		return state;
	}

	public IPAndPort noConnection(IPAndPort conn) {
		welcome.setRemoteConnectionStatus("<html><body><font color=\"red\">could not connect</font></body></html>");
		return conn;
	}

	public BufferedImage processImage(BufferedImage bi) {
		return bi;
	}

	// @Override - only in Java 1.6
	public void windowActivated(WindowEvent e) {
		// log.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowClosed(WindowEvent e) {
		// log.info("windowClosed");
	}

	// @Override - only in Java 1.6
	public void windowClosing(WindowEvent e) {
		// check for all service guis and see if its
		// ok to shutdown now
		Iterator<Map.Entry<String, ServiceGUI>> it = serviceGUIMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ServiceGUI> pairs = (Map.Entry<String, ServiceGUI>) it.next();
			String serviceName = pairs.getKey();
			if (undockedPanels.containsKey(serviceName)) {
				UndockedPanel up = undockedPanels.get(serviceName);
				if (!up.isDocked) {
					up.savePosition();
				}
			}
			pairs.getValue().isReadyForRelease();
			pairs.getValue().makeReadyForRelease();
		}

		save();

		Runtime.releaseAll();
		System.exit(1); // the Big Hamm'r
	}

	// @Override - only in Java 1.6
	public void windowDeactivated(WindowEvent e) {
		// log.info("windowDeactivated");
	}

	// @Override - only in Java 1.6
	public void windowDeiconified(WindowEvent e) {
		// log.info("windowDeiconified");
	}

	// @Override - only in Java 1.6
	public void windowIconified(WindowEvent e) {
		// log.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowOpened(WindowEvent e) {
		// log.info("windowOpened");

	}

	public String lastHost = "127.0.0.1";
	public String lastPort = "6767";

	public void about() {
		new AboutDialog(frame);
	}

	public void stopService() {
		dispose();
		super.stopService();
	}

	public void dispose() {
		if (frame != null) {
			frame.dispose();
		}
	}

	@Override
	public String getToolTip() {
		return "<html>Service used to graphically display and control other services</html>";
	}

	@Override
	public void pack() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				frame.pack();
			}
		});
	}

	@Override
	public JFrame getFrame() {
		return frame;
	}

	@Override
	public void setDstMethodName(String d) {
		guiServiceGUI.dstMethodName.setText(d);
	}

	@Override
	public void setDstServiceName(String d) {
		guiServiceGUI.dstServiceName.setText(d);
	}

	@Override
	public void setPeriod0(String s) {
		guiServiceGUI.period0.setText(s);
	}

	@Override
	public String getDstMethodName() {
		return guiServiceGUI.dstMethodName.getText();
	}

	@Override
	public String getDstServiceName() {
		return guiServiceGUI.dstServiceName.getText();
	}

	@Override
	public String getSrcMethodName() {
		return guiServiceGUI.srcMethodName.getText();
	}

	@Override
	public String getSrcServiceName() {
		return guiServiceGUI.srcServiceName.getText();
	}

	@Override
	public HashMap<String, mxCell> getCells() {
		return guiServiceGUI.serviceCells;
	}

	@Override
	public mxGraph getGraph() {
		return guiServiceGUI.graph;
	}

	@Override
	public void setSrcMethodName(String d) {
		guiServiceGUI.srcMethodName.setText(d);
	}

	@Override
	public void setSrcServiceName(String d) {
		guiServiceGUI.srcServiceName.setText(d);
	}

	@Override
	public void setArrow(String s) {
		guiServiceGUI.arrow0.setText(s);
	}

	@Override
	public void setPeriod1(String s) {
		guiServiceGUI.period1.setText(s);
	}

	@Override
	public String getGraphXML() {
		return graphXML;
	}

	@Override
	public void setGraphXML(String xml) {
		graphXML = xml;
	}

	public void registerServicesEvent(String host, int port, Message msg) {
		loadTabPanels();
	}

	public void registerServicesEvent() {
		loadTabPanels();
	}

	static public void console() {
		attachJavaConsole();
	}

	static public void attachJavaConsole() {
		JFrame j = new JFrame("Java Console");
		j.setSize(500, 550);
		Console c = new Console();
		j.add(c.getScrollPane());
		j.setVisible(true);
		c.startLogging();
	}

	/**
	 * Build the menu for display.
	 * 
	 * @return
	 */
	public JMenuBar buildMenu() {
		JMenuBar menuBar = new JMenuBar();

		// --- system ----
		JMenu systemMenu = new JMenu("system");
		JMenuItem mi;

		mi = new JMenuItem("connect");
		mi.addActionListener(this);
		systemMenu.add(mi);

		JMenu m = new JMenu("logging");
		systemMenu.add(m);

		JMenu m2 = new JMenu("level");
		m.add(m2);
		buildLogLevelMenu(m2);

		m2 = new JMenu("type");
		m.add(m2);
		buildLogAppenderMenu(m2);

		m = new JMenu("update");
		buildUpdatesMenu(m);

		systemMenu.add(m);

		systemMenu.add(buildRecordingMenu(new JMenu("recording")));

		menuBar.add(systemMenu);

		JMenu help = new JMenu("help");
		JMenuItem about = new JMenuItem("about");
		about.addActionListener(this);
		help.add(about);
		menuBar.add(help);

		return menuBar;
	}

	JMenuItem recording = new JMenuItem("start recording");
	JMenuItem loadRecording = new JMenuItem("load recording");

	public JMenu buildRecordingMenu(JMenu parentMenu) {

		recording.addActionListener(this);
		parentMenu.add(recording);

		loadRecording.addActionListener(this);
		parentMenu.add(loadRecording);

		return parentMenu;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String cmd = ae.getActionCommand();
		Object source = ae.getSource();
		if ("save".equals(cmd)) {
			Runtime.save("myrobotlab.mrl");
		} else if ("check for updates".equals(cmd)) {
			Runtime runtime = Runtime.getInstance();
			send(runtime.getName(), "checkForUpdates");
		} else if ("update all".equals(cmd)) {
			Runtime.updateAll();
		} else if (cmd.equals(LogLevel.Debug.toString()) || cmd.equals(LogLevel.Info.toString()) || cmd.equals(LogLevel.Warn.toString()) || cmd.equals(LogLevel.Error.toString())
				|| cmd.equals(LogLevel.Fatal.toString())) {
			// TODO this needs to be changed into something like tryValueOf(cmd)
			setLogLevel(LogLevel.parse(cmd));
		} else if ("connect".equals(cmd)) {
			ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect", "message", this, lastHost, lastPort);
			lastHost = dlg.host.getText();
			lastPort = dlg.port.getText();
		} else if (cmd.equals(LogAppender.None.toString())) {
			removeAllAppenders();
		} else if (cmd.equals(LogAppender.Remote.toString())) {
			JCheckBoxMenuItem m = (JCheckBoxMenuItem) ae.getSource();
			if (m.isSelected()) {
				ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect to remote logging", "message", this, lastHost, lastPort);
				lastHost = dlg.host.getText();
				lastPort = dlg.port.getText();
				addAppender(LogAppender.Remote, dlg.host.getText(), dlg.port.getText());
			} else {
				Service.remoteAppender(LogAppender.Remote);
			}
		} else if (cmd.equals(LogAppender.File.toString())) {
			addAppender(LogAppender.File);
		} else if (cmd.equals(LogAppender.None.toString())) {
			addAppender(LogAppender.None);
		} else if ("explode".equals(cmd)) {
			// display();
		} else if (source == recording) {
			if ("start recording".equals(recording.getText())) {
				startRecording();
				recording.setText("stop recording");
			} else {
				stopRecording();
				recording.setText("start recording");
			}
		} else if (source == loadRecording) {
			JFileChooser c = new JFileChooser(cfgDir);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Message files", "msg");
			c.setFileFilter(filter);
			// Demonstrate "Open" dialog:
			String filename;
			String dir;
			int rVal = c.showOpenDialog(frame);
			if (rVal == JFileChooser.APPROVE_OPTION) {
				filename = c.getSelectedFile().getName();
				dir = c.getCurrentDirectory().toString();
				loadRecording(dir + "/" + filename);
			}
			if (rVal == JFileChooser.CANCEL_OPTION) {

			}
		} else {
			invoke(Util.StringToMethodName(cmd));
		}
	}

	/**
	 * Add all options to the Software Update menu.
	 * 
	 * @param parentMenu
	 */
	private void buildUpdatesMenu(JMenu parentMenu) {
		JMenuItem mi = new JMenuItem("check for updates");
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JMenuItem("update all");
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JMenuItem("install all");
		mi.addActionListener(this);
		parentMenu.add(mi);
	}

	/**
	 * Add all options to the Log Appender menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogAppenderMenu(JMenu parentMenu) {
		Enumeration appenders = LogManager.getRootLogger().getAllAppenders();
		boolean console = false;
		boolean file = false;
		boolean remote = false;

		while (appenders.hasMoreElements()) {
			Object o = appenders.nextElement();
			if (o.getClass() == ConsoleAppender.class) {
				console = true;
			} else if (o.getClass() == FileAppender.class) {
				file = true;
			} else if (o.getClass() == SocketAppender.class) {
				remote = true;
			}

			log.info(o.getClass().toString());
		}

		JCheckBoxMenuItem mi = new JCheckBoxMenuItem(LogAppender.None.toString());
		mi.setSelected(!console && !file && !remote);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(LogAppender.Console.toString());
		mi.setSelected(console);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(LogAppender.File.toString());
		mi.setSelected(file);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(LogAppender.Remote.toString());
		mi.setSelected(remote);
		mi.addActionListener(this);
		parentMenu.add(mi);
	}

	/**
	 * Add all options to the Log Level menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogLevelMenu(JMenu parentMenu) {
		ButtonGroup logLevelGroup = new ButtonGroup();

		Logger root = Logger.getRootLogger();
		String level = root.getLevel().toString();

		JRadioButtonMenuItem mi = new JRadioButtonMenuItem(LogLevel.Debug.toString());
		mi.setSelected(("DEBUG".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(LogLevel.Info.toString());
		mi.setSelected(("INFO".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(LogLevel.Warn.toString());
		mi.setSelected(("WARN".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(LogLevel.Error.toString());
		mi.setSelected(("ERROR".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(LogLevel.Fatal.toString());
		mi.setSelected(("FATAL".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);
	}

	static public void restart(String restartScript) {
		JFrame frame = new JFrame();
		int ret = JOptionPane.showConfirmDialog(frame, "<html>New components have been added,<br>" + " it is necessary to restart in order to use them.</html>", "restart",
				JOptionPane.YES_NO_OPTION);
		if (ret == JOptionPane.OK_OPTION) {
			log.info("restarting");
			Runtime.restart(restartScript);
		} else {
			log.info("chose not to restart");
			return;
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, URISyntaxException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		/*
		 * //float x = 539248398 >> 10; float x = 10f % 539248398; log.info(x);
		 * float y = 5383823987f % 10f; log.info(y);
		 * 
		 * URI a = new URI("tcp://127.0.0.1:6767"); URI b = new
		 * URI("tcp://192.168.0.2:6767");
		 * 
		 * String c = a.toString(); String d = b.toString();
		 * 
		 * log.info(c); log.info(String.format("0.%d",Math.abs(c.hashCode())));
		 * 
		 * log.info(d); log.info(String.format("0.%d",Math.abs(d.hashCode())));
		 */
		GUIService gui2 = (GUIService) Runtime.createAndStart("gui1", "GUIService");

		// gui2.sendServiceDirectoryUpdate(login, password, name, remoteHost,
		// port, sdu) <--FIXME no sdu
		// FIXME - change to sendRegistration ....
		// gui2.sendServiceDirectoryUpdate(null, null, null, "localhost", 6767,
		// null);
		// gui2.sendServiceDirectoryUpdate(null, null, null, "10.192.198.34",
		// 6767, null);

		gui2.startService();
		// gui2.display();

		// gui2.startRecording();
		// gui2.stopRecording();

		// gui2.loadRecording(".myrobotlab/gui1_20120918052147517.msg");

	}

}