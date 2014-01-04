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

import java.awt.BorderLayout;
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
import org.apache.log4j.LogManager;
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
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.string.Util;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

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

	transient public final static Logger log = LoggerFactory.getLogger(GUIService.class.getCanonicalName());

	public String graphXML = "";

	public transient JFrame frame = null;

	@Element(required = false)
	public String lastTabVisited;
	@Element
	public String lastHost = "127.0.0.1";
	@Element
	public String lastPort = "6767";

	/**
	 * class to save the position and size of undocked panels
	 * 
	 */
	@ElementMap(entry = "serviceType", value = "dependsOn", attribute = true, inline = true, required = false)
	transient public HashMap<String, UndockedPanel> undockedPanels = new HashMap<String, UndockedPanel>();

	/**
	 * all the panels
	 */
	public transient JTabbedPane tabs = new JTabbedPane();

	transient JMenuItem recording = new JMenuItem("start recording");
	transient JMenuItem loadRecording = new JMenuItem("load recording");

	boolean test = true;

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
	transient Map<String, ServiceInterface> sortedMap = null;

	transient GridBagConstraints gc = null;

	String selectedTabTitle = null;
	boolean isDisplaying = false;
	transient JLabel status = new JLabel("status");

	public GUIService(String n) {
		super(n);
		load();// <-- HA was looking all over for it
	}

	public boolean hasDisplay() {
		return true;
	}

	public HashMap<String, ServiceGUI> getServiceGUIMap() {
		return serviceGUIMap;
	}

	public boolean preProcessHook(Message m) {
		// FIXME - problem with collisions of this service's methods
		// and dialog methods ?!?!?

		// if the method name is == to a method in the GUIService
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}

		// otherwise send the message to the dialog with the senders name
		ServiceGUI sg = serviceGUIMap.get(m.sender);
		if (sg == null) {
			log.error("attempting to update sub-gui - sender " + m.sender + " not available in map " + getName());
		} else {
			// FIXME - NORMALIZE - Instantiator or Service - not both !!!
			//Instantiator.invokeMethod(serviceGUIMap.get(m.sender), m.method, m.data);
			invokeOn(serviceGUIMap.get(m.sender), m.method, m.data);
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
	// FIXME - use instanciator !!!
	static public Object getNewInstance(String classname, String boundServiceName, GUI service) {
		try {
			Object[] params = new Object[2];
			params[0] = boundServiceName;
			params[1] = service;
			Class<?> c;
			c = Class.forName(classname);
			Constructor<?> mc = c.getConstructor(new Class[] { String.class, GUI.class });
			return mc.newInstance(params);
		} catch (Exception e) {
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

		HashMap<String, ServiceInterface> services = Runtime.getRegistry();
		log.info("buildTabPanels service count " + Runtime.getRegistry().size());

		sortedMap = new TreeMap<String, ServiceInterface>(services);
		Iterator<String> it = sortedMap.keySet().iterator();
		synchronized (sortedMap) { // FIXED YAY !!!!
			while (it.hasNext()) {
				String serviceName = it.next();
				addTab(serviceName);
			}
		}

		frame.pack();
	}

	// TODO - get index based on name
	public void addTab(String serviceName) {
		// ================= begin addTab(name) =============================
		ServiceInterface sw = Runtime.getService(serviceName);

		if (sw == null) {
			log.error(String.format("addTab %1$s can not proceed - %1$s does not exist in registry (yet?)", serviceName));
			return;
		}

		// SW sent in registerServices - yet Service is null due to incompatible
		// Service Types
		// FIXME - Solution ??? - send SW with "suggested type ???" Android
		// --becomes--> AndroidController :)
		if (sw == null) {
			log.error(String.format("%1$s does not have a valid Service - not exported ???", serviceName));
			return;
		}

		// get service type class name TODO
		String serviceClassName = sw.getClass().getCanonicalName();
		String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
		guiClass = "org.myrobotlab.control" + guiClass + "GUI";

		if (serviceGUIMap.containsKey(sw.getName())) {
			log.debug(String.format("not creating %1$s gui - it already exists", sw.getName()));
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
			log.info(c.toString());
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
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, ServiceInterface sw) {
		ServiceGUI gui = null;
		ServiceInterface se = sw;
		if (serviceName.equals("python")) {
			log.info("here");
		}
		gui = (ServiceGUI) getNewInstance(guiClass, se.getName(), this);

		if (gui == null) {
			log.warn("could not construct a " + guiClass + " object - creating generic template");
			gui = (ServiceGUI) getNewInstance("org.myrobotlab.control._TemplateServiceGUI", se.getName(), this);
		}

		gui.init();
		serviceGUIMap.put(serviceName, gui);
		// FIXME - add method gui.setService(registry.get(boundServiceName))
		tabPanelMap.put(serviceName, gui.getDisplay());
		gui.attachGUI();

		// TODO - all auto-subscribtions could be done here
		subscribe("publishStatus", se.getName(), "getStatus", String.class);

		tabs.addTab(serviceName, gui.getDisplay());

		if (sw.isLocal()) {
			tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(this, tabs, gui.getDisplay(), serviceName));
		} else {
			// create hash color for hsv from accessURI
			// Color hsv = new
			// Color(Color.HSBtoRGB(Float.parseFloat(String.format("0.%d",
			// Math.abs(sw.getAccessURL().hashCode()))), 0.8f, 0.7f));
			Color hsv = getColorFromURI(sw.getHost());
			int index = tabs.indexOfTab(serviceName);
			tabs.setBackgroundAt(index, hsv);
			tabs.setTabComponentAt(tabs.getTabCount() - 1, new TabControl(this, tabs, gui.getDisplay(), serviceName, Color.white, hsv));
		}

		return gui;
	}

	public static Color getColorFromURI(Object uri) {
		StringBuffer sb = new StringBuffer(String.format("%d", Math.abs(uri.hashCode())));
		Color c = new Color(Color.HSBtoRGB(Float.parseFloat("0." + sb.reverse().toString()), 0.8f, 0.7f));
		return c;
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

			// FIXME - deprecate
			gc = new GridBagConstraints();

			frame.addWindowListener(this);
			frame.setTitle("myrobotlab - " + getName() + " " + Runtime.getVersion());

			buildTabPanels();

			JPanel main = new JPanel(new BorderLayout());
			main.add(tabs, BorderLayout.CENTER);
			main.add(status, BorderLayout.SOUTH);
			status.setOpaque(true);

			frame.add(main);

			URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
			Toolkit kit = Toolkit.getDefaultToolkit();
			Image img = kit.createImage(url);
			frame.setIconImage(img);

			// menu
			frame.setJMenuBar(buildMenu());
			frame.setVisible(true);
			frame.pack();
			if (tabPanelMap.containsKey(lastTabVisited)) {
				try {
					tabs.setSelectedComponent(tabPanelMap.get(lastTabVisited));
				} catch (Exception e) {
					Logging.logException(e);
				}
			}

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
	public String getDescription() {
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

		JMenuItem save = new JMenuItem("save");
		save.setActionCommand("save");
		systemMenu.add(save);
		save.addActionListener(this);

		JMenuItem load = new JMenuItem("load");
		load.setActionCommand("load");
		systemMenu.add(load);
		load.addActionListener(this);

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
		// get local runtime instance
		Runtime runtime = Runtime.getInstance();
		if ("save".equals(cmd)) {
			Runtime.saveAll();
		} else if ("load".equals(cmd)) {
			Runtime.loadAll();
		} else if ("install latest".equals(cmd)) {
			runtime.updateAll();
		} else if (cmd.equals(Level.DEBUG) || cmd.equals(Level.INFO) || cmd.equals(Level.WARN) || cmd.equals(Level.ERROR) || cmd.equals(Level.FATAL)) {
			// TODO this needs to be changed into something like tryValueOf(cmd)
			Logging logging = LoggingFactory.getInstance();
			logging.setLevel(cmd);
		} else if ("connect".equals(cmd)) {
			ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect", "message", this, lastHost, lastPort);
			lastHost = dlg.host.getText();
			lastPort = dlg.port.getText();
		} else if (cmd.equals(Appender.NONE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.removeAllAppenders();
		} else if (cmd.equals(Appender.REMOTE)) {
			JCheckBoxMenuItem m = (JCheckBoxMenuItem) ae.getSource();
			if (m.isSelected()) {
				ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect to remote logging", "message", this, lastHost, lastPort);
				lastHost = dlg.host.getText();
				lastPort = dlg.port.getText();
				Logging logging = LoggingFactory.getInstance();
				logging.addAppender(Appender.REMOTE, dlg.host.getText(), dlg.port.getText());
			} else {
				Logging logging = LoggingFactory.getInstance();
				logging.removeAppender(Appender.REMOTE);
			}
		} else if (cmd.equals(Appender.FILE)) { // FIXME - refactor it all out
												// (it recovered from enums !
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.FILE);
		} else if (cmd.equals(Appender.NONE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.NONE);
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
		JMenuItem mi = new JMenuItem("install latest");
		mi = new JMenuItem("install latest");
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

		JCheckBoxMenuItem mi = new JCheckBoxMenuItem(Appender.NONE);
		mi.setSelected(!console && !file && !remote);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.CONSOLE);
		mi.setSelected(console);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.FILE);
		mi.setSelected(file);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.REMOTE);
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

		String level = LoggingFactory.getInstance().getLevel();

		JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Level.DEBUG);
		mi.setSelected(("DEBUG".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.INFO);
		mi.setSelected(("INFO".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.WARN);
		mi.setSelected(("WARN".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.ERROR);
		mi.setSelected(("ERROR".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.FATAL); // TODO - deprecate to WTF
													// :)
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

	public void getStatus(Status inStatus) {
		/*
		 * if (msg.startsWith("error")) { status.setOpaque(true);
		 * status.setForeground(Color.white); status.setBackground(Color.red); }
		 * else if (msg.startsWith("error")) { status.setOpaque(true);
		 * status.setForeground(Color.white);
		 * status.setBackground(Color.yellow); } else {
		 * status.setForeground(Color.black); status.setOpaque(false); }
		 */

		if (inStatus.isError()) {
			status.setOpaque(true);
			status.setForeground(Color.white);
			status.setBackground(Color.red);
		} else if (inStatus.isWarn()) {
			status.setOpaque(true);
			status.setForeground(Color.white);
			status.setBackground(Color.yellow);
		} else {
			status.setForeground(Color.black);
			status.setOpaque(false);
		}

		status.setText(inStatus.detail);
	}

	public static void main(String[] args) throws ClassNotFoundException, URISyntaxException {
		LoggingFactory.getInstance().configure();
		Logging logging = LoggingFactory.getInstance();
		logging.setLevel(Level.INFO);
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

		gui2.startService();
		// gui2.display();

		// gui2.startRecording();
		// gui2.stopRecording();

		// gui2.loadRecording(".myrobotlab/gui1_20120918052147517.msg");

	}

}
