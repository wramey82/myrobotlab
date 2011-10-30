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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.control.GUIServiceGUI;
import org.myrobotlab.control.Network;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.ServiceTabPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
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
 * 		serviceGUI needs an invoker
 * 		Arduino arduin-> post back (data) --> GUI - look up serviceGUI by senders name ServiceGUI->invoke(data)
 * 
 */

public class GUIService extends GUI implements WindowListener, ActionListener, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(GUIService.class.getCanonicalName());

	public String graphXML = "";
	
	public transient JFrame frame = null;

	public transient ServiceTabPane tabs = null;
	public transient JPanel panel = null;
	public transient GUIServiceGUI guiServiceGUI = null; // the tabbed panel gui of the gui service
	transient Network network = null;
	transient HashMap<String, ServiceGUI> serviceGUIMap = null;
	
	Map<String, ServiceWrapper> sortedMap = null;
	HashMap<String, Object> commandMap = new HashMap<String, Object>(); 

	transient GridBagConstraints gc = null;
	transient public JLabel remoteStatus = new JLabel("<html><body>not connected</body></html>");

	public String remoteColorTab = "0x99DD66";
	
	int currentTab = 0;
	String selectedTabTitle = null;
	HashMap<String, Integer> titleToTabIndexMap = new HashMap<String, Integer>(); 

	
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

	public HashMap<String, Boolean> customWidgetPrefs = new HashMap<String, Boolean>(); 
	
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
					+ m.sender + " not available in map " + name);
		} else {
			invoke(serviceGUIMap.get(m.sender), m.method, m.data);
		}
		
		return false;
	}
		
	/**
	 *  a function to rebuild the GUI display.  Smaller data-exchange should be done with getState/publishState.
	 *  This can be used to rebuild the panels after a new service has been created or a foriegn set of services
	 *  has been registered.
	 */
	public void rebuild()
	{
		loadTabPanels();
	}
	
	public ServiceTabPane loadTabPanels() {
		LOG.debug("loadTabPanels");
		currentTab = tabs.getSelectedIndex();
		if (currentTab > 0)
		{
			selectedTabTitle = tabs.getTitleAt(currentTab);
		}
		
		// detach if panels are currently attached
		Iterator<String> sgi = serviceGUIMap.keySet().iterator();
		while (sgi.hasNext()) {
			String serviceName = sgi.next();
			ServiceGUI sg = serviceGUIMap.get(serviceName);
			sg.detachGUI();
		}

		// TODO - 2 x bugs 
		// 1. not synchronized - put in synchronized block?
		// 2. called too many times
		// tabs.removeAll(); - will explode too
		// http://code.google.com/p/myrobotlab/issues/detail?id=1
		/*
		LOG.info("tab count" + tabs.getTabCount());
		while (tabs.getTabCount() > 0)
		{
		    tabs.remove(0);
		}
		*/
		removeAllTabPanels();
		
		// begin building panels
		network = new Network("communication",this); // TODO - clean this up - add
		network.init();
										
		// TODO - throw error on name collision from client list
		//tabs.addTab("communication", (JComponent) network);
		tabs.addTab("communication", network.display);

		JPanel customPanel = new JPanel(new FlowLayout());
		customPanel.setPreferredSize(new Dimension(800, 600));

		HashMap<String, ServiceWrapper> services = RuntimeEnvironment.getRegistry();
		LOG.info("service count " + RuntimeEnvironment.getRegistry().size());
		
		sortedMap = new TreeMap<String, ServiceWrapper>(services);

		Integer index = tabs.getTabCount() - 1;
		
		boolean createGUIServiceGUI = false;
		Iterator<String> it = sortedMap.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper se = services.get(serviceName);

			// get service type class name
			String serviceClassName = se.get().getClass().getCanonicalName();
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";

			//if (guiClass.compareTo("org.myrobotlab.control.GUIServiceGUI") == 0) {
			if (se.get().name.equals(name)) {
				// GUIServiceGUI must be created last to ensure all routing from attachGUI is done
				LOG.debug("delaying construction my GUI " + name + " GUIServiceGUI ");
				createGUIServiceGUI = true;
				continue;
			}
			
			ServiceGUI newGUI = createTabbedPanel(serviceName, guiClass, customPanel, se);
			if (newGUI != null)
			{
				if (newGUI.isPanelTabbed())
				{
					++index;
					titleToTabIndexMap.put(serviceName, index);
					if (se.getAccessURL() != null) {
						tabs.setBackgroundAt(index, Color.decode(remoteColorTab));
					}

				}
			}

		}

		// creating the ServiceGUI for "this" class
		if (createGUIServiceGUI)
		{
			// TODO - warning this may need more of a delay - or must "remember" notifications of attachGUI
			// going out to remote systems.
			ServiceWrapper se = services.get(this.name);
			String serviceClassName = se.get().getClass().getCanonicalName();
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";
			
			guiServiceGUI = (GUIServiceGUI)createTabbedPanel(this.name, guiClass, customPanel, se);
			++index;
			titleToTabIndexMap.put("custom", index);

		}
		
		tabs.addTab("custom", customPanel);		
		++index;
		titleToTabIndexMap.put("custom", index);
		
		frame.pack();
		
		// attempt to select the previously selected tab
		
		if (selectedTabTitle != null && titleToTabIndexMap.containsKey(selectedTabTitle))
		{
			int newPos = titleToTabIndexMap.get(selectedTabTitle);
			tabs.setSelectedIndex(newPos);
		}
		/*
		if (currentTab != -1)
		{
			tabs.setSelectedIndex(currentTab);
		} else {
			tabs.setSelectedIndex(0);
		}
		*/
		
		return tabs;

	}
	
	public synchronized void removeAllTabPanels()
	{
		//tabs.removeAll();
		LOG.info("tab count" + tabs.getTabCount());
		while (tabs.getTabCount() > 0)
		{
		    tabs.remove(0);
		}

	}
	
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, JPanel customPanel, ServiceWrapper sw)
	{
		ServiceGUI gui = null;
		JPanel tpanel = new JPanel();
		Service se = sw.get();
		gui = (ServiceGUI) getNewInstance(guiClass, se.name, this);
		if (gui != null) {
			gui.init();
			serviceGUIMap.put(serviceName, gui);
			gui.attachGUI();

			if (!customWidgetPrefs.containsKey(se.name) || (customWidgetPrefs.containsKey(se.name) && customWidgetPrefs.get(se.name) == false)) {
				tpanel.add(gui.widgetFrame); 
				tabs.addTab(serviceName, tpanel);
			} else {
				customPanel.add(gui.widgetFrame);
			}

		} else {
			LOG.warn("could not construct a " + guiClass + " object");
		}

		return gui;
	}

	public void display() {
		
		tabs = new ServiceTabPane();
		panel = new JPanel();
		serviceGUIMap = new HashMap<String, ServiceGUI>();		
		gc = new GridBagConstraints();
		frame = new JFrame();

		frame.addWindowListener(this);
		frame.setTitle("myrobotlab - " + name);

		ServiceTabPane stp = loadTabPanels();
		
		JScrollPane sp = new JScrollPane (panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		panel.add(stp, gc);

		// TODO - catch appropriate missing resource
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		frame.setIconImage(img);		
		
		JMenuBar menuBar = new JMenuBar();
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
		network.setRemoteConnectionStatus(state);
		return state;
	}

	public IPAndPort noConnection(IPAndPort conn) {
		network.setRemoteConnectionStatus("<html><body><font color=\"red\">could not connect</font></body></html>");
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
			ServiceWrapper se = services.get(serviceName);

			//Service local = (Service) hostcfg.getLocalServiceHandle(serviceName);
			Service service = se.get();

			if (serviceName.compareTo(this.name) == 0) {
				LOG.info("momentarily skipping " + this.name + "....");
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

	@Override
	public void actionPerformed(ActionEvent arg0) {
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
			ImageIcon icon = FileIO.getResourceIcon("mrl_logo_about_128.png");
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

	@Override
	public HashMap<String, Boolean> getCustomWidgetPrefs() {
		return customWidgetPrefs;
	}

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
	

	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		Clock clock = new Clock("clock");
		clock.startService();
				
		GUIService gui2 = new GUIService("gui2");
		
		gui2.notify("registerServices", gui2.name, "registerServicesEvent");
		//gui2.notify("registerServices", gui2.name, "registerServicesEvent", String.class, Integer.class, Message.class);

		gui2.startService();
		gui2.display();
		
		
	}
	
}
