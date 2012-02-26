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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.control.ConnectDialog;
import org.myrobotlab.control.Console;
import org.myrobotlab.control.GUIServiceGUI;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.Welcome;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Utils;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.interfaces.GUI;

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
 * 		serviceGUI needs a ServiceFactory
 * 		Arduino arduin-> post back (data) --> GUI - look up serviceGUI by senders name ServiceGUI->invoke(data)
 * 
 */

public class GUIService extends GUI implements WindowListener, ActionListener, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(GUIService.class.getCanonicalName());

	public String graphXML = "";
	
	public transient JFrame frame = null;

	public transient JTabbedPane tabs = null;
	public transient JPanel panel = null;
	public transient GUIServiceGUI guiServiceGUI = null; // the tabbed panel gui of the gui service
	transient Welcome welcome = null;
	transient HashMap<String, ServiceGUI> serviceGUIMap = new HashMap<String, ServiceGUI>();		
	
	HashMap<String, JPanel> tabPanelMap = new HashMap<String, JPanel>();			
	Map<String, ServiceWrapper> sortedMap = null;
	HashMap<String, Object> commandMap = new HashMap<String, Object>(); 

	transient GridBagConstraints gc = null;
	transient public JLabel remoteStatus = new JLabel("<html><body>not connected</body></html>");

	public String remoteColorTab = "0x007000";
	public String remoteFont = "0xFFFFFF";
	
	String selectedTabTitle = null;

	
	public GUIService(String n) {
		super(n, GUIService.class.getCanonicalName());

		/*
		 * The commandMap is a list of GUIService functions which should
		 * be processed by GUIService rather than routed to control Panels
		 * TODO - dynamically generate "all" top level functions getMethods + Service ?
		 */
		
		commandMap.put("registerServicesEvent", null);
		commandMap.put("registerServices", null);
		commandMap.put("loadTabPanels", null);
		commandMap.put("registerServicesNotify", null);
		commandMap.put("notify", null);
		commandMap.put("removeNotify", null);
		commandMap.put("guiUpdated", null);
		commandMap.put("setRemoteConnectionStatus", null);

	}

	@Override
	public void loadDefaultConfiguration() {
	}

	//public HashMap<String, Integer> customWidgetPrefs = new HashMap<String, Integer>(); 
	
	public HashMap<String, ServiceGUI> getServiceGUIMap() {
		return serviceGUIMap;
	}


	public boolean preProcessHook(Message m)
	{
		if (commandMap.containsKey(m.method))
		{
			return true;
		} 
		
		ServiceGUI sg = serviceGUIMap.get(m.sender);
		if (sg == null) {
			LOG.error("attempting to update sub-gui - sender "
					+ m.sender + " not available in map " + getName());
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
	static public Object getNewInstance(String classname,
			String boundServiceName, GUI service) {
		try {
			Object[] params = new Object[2];
			params[0] = boundServiceName;
			params[1] = service;
			Class<?> c;
			c = Class.forName(classname);
			Constructor<?> mc = c.getConstructor(new Class[] { String.class,
					GUI.class });
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
	
	// FIXME - refactor name - remove kruft
	/**
	 *  a function to rebuild the GUI display.  Smaller data-exchange should be done with getState/publishState.
	 *  This can be used to rebuild the panels after a new service has been created or a foriegn set of services
	 *  has been registered.
	 */
	public void rebuild()
	{
		loadTabPanels();
	}
	
	/* (non-Javadoc)
	 * @see org.myrobotlab.service.interfaces.GUI#loadTabPanels()
	 * This is a bit "big hammer" in that it destroys all panels and rebuilds the GUI
	 * don't use except to initially build
	 */
	public JTabbedPane loadTabPanels() {
		LOG.debug("loadTabPanels");
		
		
		// detach from Services, if panels are currently attached
		Iterator<String> sgi = serviceGUIMap.keySet().iterator();
		while (sgi.hasNext()) {
			String serviceName = sgi.next();
			ServiceGUI sg = serviceGUIMap.get(serviceName);
			sg.detachGUI();
		}

		
		// begin building panels
		if (!serviceGUIMap.containsKey("welcome")) // FIXME - possible problem loading a non-Service ServiceGUI
		{
			welcome = new Welcome("",this); 
			welcome.init();
			tabs.addTab("Welcome", welcome.display);
			serviceGUIMap.put("welcome", welcome);
		}

		HashMap<String, ServiceWrapper> services = RuntimeEnvironment.getRegistry();
		LOG.info("service count " + RuntimeEnvironment.getRegistry().size());
		
		sortedMap = new TreeMap<String, ServiceWrapper>(services);

		Integer index = tabs.getTabCount() - 1;
		
		// build Service panels
		boolean createGUIServiceGUI = false;
		Iterator<String> it = sortedMap.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);

			// SW sent in registerServices - yet Service is null due to incompatible Service Types
			// FIXME - Solution ??? - send SW with "suggested type ???"  Android --becomes--> AndroidController :)
			if (sw.get() == null)
			{
				LOG.error(serviceName + " does not have a valid Service - not exported ???");
				continue;
			}
			
			// get service type class name TODO
			String serviceClassName = sw.get().getClass().getCanonicalName();
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";

			if (sw.get().getName().equals(getName())) {
				// GUIServiceGUI must be created last to ensure all routing from attachGUI is done
				LOG.debug("delaying construction my GUI " + getName() + " GUIServiceGUI ");
				createGUIServiceGUI = true;
				continue;
			}

			if (serviceGUIMap.containsKey(sw.name))
			{
				LOG.debug("not creating " + sw.name + " gui - it already exists");
				continue;
			}
			
			ServiceGUI newGUI = createTabbedPanel(serviceName, guiClass, sw);
			if (newGUI != null)
			{
				++index;
				if (sw.getAccessURL() != null) {
					tabs.setBackgroundAt(index, Color.decode(remoteColorTab));
					tabs.setForegroundAt(index, Color.decode(remoteFont));
				}

			}

		}

		// FIXME creating the ServiceGUI for "this" class if its not already created
		// FIXME - you'll need to recreate/refresh the block map since new 
		// service might have been added or dropped out !
		// POSSIBILITY is to REMOVE THEN ADD
		if (createGUIServiceGUI) // && !serviceGUIMap.containsKey(getName())
		{
			// if it already exists remove it
			if (serviceGUIMap.containsKey(getName()))
			{
				removeTab(getName());
			}
			// TODO - warning this may need more of a delay - or must "remember" notifications of attachGUI
			// going out to remote systems.
			ServiceWrapper se = services.get(getName());
			String serviceClassName = se.get().getClass().getCanonicalName();

			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";
			
			guiServiceGUI = (GUIServiceGUI)createTabbedPanel(this.getName(), guiClass, se);
			++index;

		}
				
		frame.pack();
		tabs.setSelectedIndex(0);
		
		return tabs;

	}
	
	public void removeTab(String name)
	{
		LOG.info(serviceGUIMap.size());
		JPanel tab = tabPanelMap.get(name);
		if (tab != null)
		{
			tabs.remove(tab);
			serviceGUIMap.remove(name);
			tabPanelMap.remove(name);
			LOG.info(serviceGUIMap.size());
		} else {
			LOG.error("can not removeTab " + name);
		}
	}
	
		
	public synchronized void removeAllTabPanels()
	{
		LOG.info("tab count" + tabs.getTabCount());
		while (tabs.getTabCount() > 0)
		{
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
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, ServiceWrapper sw)
	{
		ServiceGUI gui = null;
		JPanel tpanel = new JPanel();
		Service se = sw.get();
		gui = (ServiceGUI) getNewInstance(guiClass, se.getName(), this);

		if (gui != null) {
			gui.init();
			serviceGUIMap.put(serviceName, gui);
			tabPanelMap.put(serviceName, tpanel);
			gui.attachGUI();
			tpanel.add(gui.widgetFrame); 
			tabs.addTab(serviceName, tpanel);
//			customWidgetPrefs.put(se.getName(), GUI.WIDGET_PREF_TABBED);

		} else {
			LOG.warn("could not construct a " + guiClass + " object");
		}

		return gui;
	}
	
	
	/**
	 * Move Service panel into a JFrame
	 * @param boundServiceName
	 */
	public void undockPanel(String boundServiceName)
	{
		JFrame undocked = new JFrame();
		
		
		// icon
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		undocked.setIconImage(img);					
		
		ServiceGUI sg = serviceGUIMap.get(boundServiceName);
		sg.detachButton.setVisible(false);
	    tabs.remove(sg.widgetFrame.getParent()); // FIXME - memory leak tpanel parent is not disposed - put tpanel in widget

		undocked.getContentPane().add(sg.widgetFrame);
		undocked.pack();
		undocked.setVisible(true);
		undocked.setTitle(boundServiceName);
	    undocked.addWindowListener(new UndockedWidgetWindowAdapter(undocked, this, boundServiceName));
	    
	    frame.pack();		
	}
	
	/**
	 * Move Service panel back into tabbed panels
	 * @param boundServiceName
	 */
	public void dockPanel(String boundServiceName)
	{
		// add a single tabbed panel
		ServiceGUI sg = serviceGUIMap.get(boundServiceName);
		JPanel tpanel = new JPanel();
		tpanel.add(sg.widgetFrame);
		sg.detachButton.setVisible(true);
		tabs.add(boundServiceName, tpanel);		
		
		frame.pack();
	}
	
	
	public class UndockedWidgetWindowAdapter extends WindowAdapter
	{
		private String name;
		JFrame myFrame;
		GUI parent;
		
		public UndockedWidgetWindowAdapter (JFrame myFrame, GUI parent, String name)
		{
			this.myFrame = myFrame;
			this.parent = parent;
			this.name = name;
		}
		
        public void windowClosing(WindowEvent winEvt) {
            dockPanel(name);
        	myFrame.dispose();
        }		
	}

	public void display() {
		
		tabs = new JTabbedPane();
		panel = new JPanel();
		gc = new GridBagConstraints();
		frame = new JFrame();

		frame.addWindowListener(this);
		frame.setTitle("myrobotlab - " + getName());

		JTabbedPane stp = loadTabPanels();
		
		JScrollPane sp = new JScrollPane (panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		panel.add(stp, gc);

		// TODO - catch appropriate missing resource
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		frame.setIconImage(img);		
		
		JMenuBar menuBar = new JMenuBar();
	    
		// --- system ----
		JMenu system = new JMenu("system");
	    
		JMenuItem mi = new JMenuItem("save");
	    mi.addActionListener(this);
	    system.add(mi);

		mi = new JMenuItem("save as");
	    mi.addActionListener(this);
	    system.add(mi);	    
	    
		mi = new JMenuItem("load");
	    mi.addActionListener(this);
	    system.add(mi);	    

		mi = new JMenuItem("refresh");
	    mi.addActionListener(this);
	    system.add(mi);	    

		mi = new JMenuItem("connect");
	    mi.addActionListener(this);
	    system.add(mi);
	    
		//mi = new JMenuItem("console");
	    //mi.addActionListener(this);
	    //system.add(mi);

	    JMenu m = new JMenu("logging");
	    system.add(m);

	    JMenu m2 = new JMenu("level");
	    m.add(m2);

	    	ButtonGroup group = new ButtonGroup();
	    	
	    	mi = new JRadioButtonMenuItem (LOG_LEVEL_DEBUG);
		    mi.addActionListener(this);
		    group.add(mi);
		    m2.add(mi);

		    mi = new JRadioButtonMenuItem (LOG_LEVEL_INFO);
		    mi.addActionListener(this);
		    group.add(mi);
		    m2.add(mi);
		    
		    mi = new JRadioButtonMenuItem (LOG_LEVEL_WARN);
		    mi.addActionListener(this);
		    group.add(mi);
		    m2.add(mi);
		    
		    mi = new JRadioButtonMenuItem (LOG_LEVEL_ERROR);
		    mi.addActionListener(this);
		    group.add(mi);
		    m2.add(mi);
		    
		    mi = new JRadioButtonMenuItem (LOG_LEVEL_FATAL);
		    mi.addActionListener(this);
		    group.add(mi);
		    m2.add(mi);

	    m2 = new JMenu("type");
	    m.add(m2);
		    
			mi = new JCheckBoxMenuItem(LOGGING_APPENDER_NONE);
		    mi.addActionListener(this);
		    m2.add(mi);
		    
			mi = new JCheckBoxMenuItem(LOGGING_APPENDER_CONSOLE);
		    mi.addActionListener(this);
		    m2.add(mi);

			mi = new JCheckBoxMenuItem(LOGGING_APPENDER_ROLLING_FILE);
		    mi.addActionListener(this);
		    m2.add(mi);

			mi = new JCheckBoxMenuItem(LOGGING_APPENDER_SOCKET);
		    mi.addActionListener(this);
		    m2.add(mi);
		    
	    m = new JMenu("update");

	    system.add(m);
	    
			mi = new JMenuItem("check now");
		    mi.addActionListener(this);
		    m.add(mi);

		
	    /*
	    mi = new JMenuItem("","update");
	    mi.addActionListener(this);
	    system.add(mi);
	    */
	    
	    menuBar.add(system);
	    
		JMenu help = new JMenu("help");
	    JMenuItem about = new JMenuItem("about");
	    about.addActionListener(this);
	    help.add(about);
	    menuBar.add(help);
		
	    frame.setJMenuBar(menuBar);
	    frame.add(sp);
		//frame.add(panel);
		frame.setVisible(true);
		frame.pack();

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
		LOG.info("guiUpdated");
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
		//LOG.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowClosed(WindowEvent e) {
		//LOG.info("windowClosed");
	}

	// @Override - only in Java 1.6
	public void windowClosing(WindowEvent e) {
		/*
		 * TODO - at some point offer possiblities of only shutting the gui down
		 * at the moment - assume all services should be shut down.
		 */
		/*
		 * //
		 * http://download.oracle.com/javase/tutorial/uiswing/components/dialog
		 * .html LOG.info("windowClosing"); JFrame frame = new JFrame();
		 * frame.setTitle("closing gui"); JOptionPane.showInputDialog(frame,
		 * "new service name");
		 */

		// shut down all local services
		//HashMap<String, ServiceEntry> services = hostcfg.getServiceMap();
		
		HashMap<String, ServiceWrapper> services = RuntimeEnvironment.getRegistry(); 

		//ServiceEnvironment servEnv = RuntimeEnvironment.getLocalServices();
		//HashMap<String, ServiceWrapper> services = servEnv.serviceDirectory;
		
		Iterator<String> it = services.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);

			//Service local = (Service) hostcfg.getLocalServiceHandle(serviceName);
			Service service = sw.get();

			if (serviceName.compareTo(this.getName()) == 0) {
				LOG.info("momentarily skipping " + this.getName() + "....");
				continue;
			}

			if (service != null) {
				LOG.info("shutting down " + serviceName);
				service.stopService();
			} else {
				LOG.info("skipping remote service " + serviceName);

			}

		}

		// shut self down
		LOG.info("shutting down GUIService");				
		this.stopService();
		// the big hammer - TODO - close gui - allow all other services to continue
		System.exit(1); // is this correct? - or should the gui load off a different thread?
	}

	// @Override - only in Java 1.6
	public void windowDeactivated(WindowEvent e) {
		//LOG.info("windowDeactivated");
	}

	// @Override - only in Java 1.6
	public void windowDeiconified(WindowEvent e) {
		//LOG.info("windowDeiconified");
	}

	// @Override - only in Java 1.6
	public void windowIconified(WindowEvent e) {
		//LOG.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowOpened(WindowEvent e) {
		//LOG.info("windowOpened");

	}

	// TODO - refactor names
	@Override
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if ("save".equals(action))
		{
			RuntimeEnvironment.save("myrobotlab.mrl");
		} else if ("check now".equals(action))
		{
			ServiceFactory.update();
		} else if ("load".equals(action)) 
		{
			loadRuntime();
		} else if (LOG_LEVEL_DEBUG.equals(action) || 
				LOG_LEVEL_INFO.equals(action) ||
				LOG_LEVEL_WARN.equals(action) ||
				LOG_LEVEL_ERROR.equals(action) ||
				LOG_LEVEL_FATAL.equals(action))
		{
			setLogLevel(action);
		} else if ("connect".equals(action)) 
		{
			ConnectDialog dlg = new ConnectDialog(new JFrame(), "title", "message", this);
		} else if (LOGGING_APPENDER_NONE.equals(action)) 
		{
			removeAllAppenders();
		} else if (LOGGING_APPENDER_SOCKET.equals(action)) 
		{
			JCheckBoxMenuItem m = (JCheckBoxMenuItem)ae.getSource();
			if (m.isSelected()) {
				ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect to remote logging", "message", this);
				addAppender(LOGGING_APPENDER_SOCKET, dlg.host.getText(), dlg.port.getText());
			} else {
				Service.remoteAppender(LOGGING_APPENDER_SOCKET);			
			}
		} else {
			invoke(action);
		}
	}
	
	public void loadRuntime()
	{
		RuntimeEnvironment.releaseAll();
		
		// load runtime
		RuntimeEnvironment.load("myrobotlab.mrl");
		
		RuntimeEnvironment.startLocalServices();
		// Execute when button is pressed // TODO send - message

	}
	
	public void refresh()
	{
		loadTabPanels();
	}
	public void about()
	{
		String v = FileIO.getResourceFile("version.txt");
		new AboutDialog(frame, "about", 
		"<html><p align=center><a href=\"http://myrobotlab.org\">http://myrobotlab.org</a><br>version "+v+"</p><html>");		
	}
	
	public class AboutDialog extends JDialog implements ActionListener, MouseListener {

		private static final long serialVersionUID = 1L;

		public AboutDialog(JFrame parent, String title, String message) {
		    super(parent, title, true);
		    if (parent != null) {
		      Dimension parentSize = parent.getSize(); 
		      Point p = parent.getLocation(); 
		      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
		    }
		    //GridBagConstraints gc = new GridBagConstraints();
		    //JPanel messagePane = new JPanel(new GridBagLayout());
		    JPanel messagePane = new JPanel();
		    
		    JLabel pic = new JLabel();
			ImageIcon icon = Utils.getResourceIcon("mrl_logo_about_128.png");
			if (icon != null)
			{
				pic.setIcon(icon);	
			}
		    
			messagePane.add(pic);
		    
		    JLabel link = new JLabel(message);
		    link.addMouseListener(this);
		    ++gc.gridy; 
		    messagePane.add(link,gc);
		    getContentPane().add(messagePane);
		    JPanel buttonPane = new JPanel();
		    JButton button = new JButton("OK"); 
		    buttonPane.add(button); 
		    button.addActionListener(this);
		    
		    getContentPane().add(buttonPane, BorderLayout.SOUTH);
		    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		    pack(); 
		    setVisible(true);
		  }

		@Override
		  public void actionPerformed(ActionEvent e) {
		    setVisible(false); 
		    dispose(); 
		  }


		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			URI uri;
			try {
				uri = new URI("http://myrobotlab.org");
				open(uri);
			} catch (URISyntaxException error) {
				// TODO Auto-generated catch block
				error.printStackTrace();
			}
			
		}
	}		  
	
	public void stopService() {
		dispose();
		super.stopService();
	}
	
	public void dispose()
	{
		if (frame != null)
		{
			frame.dispose();
		}
	}
	
	
	@Override
	public String getToolTip() {
		return "<html>Service used to graphically display and control other services</html>";
	}

	@Override
	public void pack() {
		frame.pack();		
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
/*
	@Override
	public HashMap<String, Integer> getCustomWidgetPrefs() {
		return customWidgetPrefs;
	}
*/
	@Override
	public String getGraphXML() {
		return graphXML;
	}

	@Override
	public void setGraphXML(String xml) {
		graphXML = xml;
	}
	
	
	public void registerServicesEvent (String host, int port, Message msg)
	{
		loadTabPanels();
	}
	
	public void registerServicesEvent ()
	{
		loadTabPanels();
	}
	
	
	static public void console()
	{
		attachJavaConsole();
	}
	
	static public void attachJavaConsole()
	{
		JFrame j = new JFrame("Java Console");
		j.setSize(500, 550);
		Console c = new Console("blah");
		j.add(c.getScrollPane());
		j.setVisible(true);		
	}
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		

		/*
		MyRobot dee = new MyRobot("dee");
		dee.start();

		ServiceFactory services = new ServiceFactory("services");
		services.startService();
				
		Jython jython = new Jython("jython");
		jython.startService();

		Servo servo1 = new Servo("servo1");
		servo1.startService();
		*/

		Jython jython = new Jython("jython");
		jython.startService();
		
		Clock clock = new Clock("clock");
		clock.startService();
		
		GUIService gui2 = new GUIService("gui2");
		gui2.startService();
		gui2.display();
		
		
	}
	
}
