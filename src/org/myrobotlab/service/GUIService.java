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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.control.GUIServiceGUI;
import org.myrobotlab.control.Network;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.ServiceTabPane;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.data.IPAndPort;

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

public class GUIService extends Service implements WindowListener, ActionListener, Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(GUIService.class.getCanonicalName());

	public JFrame frame = null;

	public ServiceTabPane tabs = new ServiceTabPane();
	public JPanel panel = new JPanel();

	public GUIServiceGUI guiServiceGUI = null; // the tabbed panel gui of the gui service
	
	Network network = null;

	HashMap<String, ServiceGUI> serviceGUIMap = new HashMap<String, ServiceGUI>();
	Map<String, ServiceEntry> sortedMap = null;

	GridBagConstraints gc = new GridBagConstraints();

	public JLabel remoteStatus = new JLabel("<html><body>not connected</body></html>");

	public GUIService(String n) {
		super(n, GUIService.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
		cfg.set("commandMap/registerServices", ""); // list of commands to
													// process directly
		cfg.set("commandMap/loadTabPanels", ""); // vs send to GUI panels
		cfg.set("commandMap/registerServicesNotify", ""); // vs send to GUI
															// panels
		cfg.set("commandMap/notify", ""); // vs send to GUI panels
		cfg.set("commandMap/removeNotify", ""); // vs send to GUI panels
		cfg.set("commandMap/guiUpdated", ""); // vs send to GUI panels
		cfg.set("commandMap/setRemoteConnectionStatus", ""); // vs send to GUI
																// panels

		cfg.set("hostname", "match01");
		cfg.set("servicePort", "3389");
		cfg.set("remoteColorTab", "0x99CC66");

		cfg.set("org.myrobotlab.service.Servo/displayOnCustomPanel",false);
		// default view
		/*
		cfg.set("org.myrobotlab.service.SpeechRecognition/displayOnCustomPanel",false);
		cfg.set("org.myrobotlab.service.SensorMonitor/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.GeneticProgramming/displayOnCustomPanel",false);
		cfg.set("org.myrobotlab.service.Servo/displayOnCustomPanel",false);
		cfg.set("org.myrobotlab.service.OpenCV/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Graphics/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.AudioFile/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Arduino/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Motor/displayOnCustomPanel", false);
		//cfg.set("org.myrobotlab.service.GUIService/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Player/displayOnCustomPanel", false);
		// cfg.set("org.myrobotlab.service.WiiDAR/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Wii/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Invoker/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.Speech/displayOnCustomPanel", false);
		// cfg.set("org.myrobotlab.service.Servo/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.SoccerGame/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.RemoteAdapter/displayOnCustomPanel", false);
		cfg.set("org.myrobotlab.service.DifferentialDrive/displayOnCustomPanel", false);
		*/

	}

	public HashMap<String, Boolean> customWidgetPrefs = new HashMap<String, Boolean>(); 
	
	public HashMap<String, ServiceGUI> getServiceGUIMap() {
		return serviceGUIMap;
	}

	// TODO - myGUI ? - send msgs too or have them executed

	@Override
	public void run() {
		try {

			HashMap<String, Object> commandMap = cfg.getMap("commandMap");
			while (isRunning) {
				// TODO command map not needed -> if
				// (serviceGUIMap.containsKey(m.sender)
				// TODO - processBlocking(msg); ???
				Message m = getMsg();
				if (commandMap.containsKey(m.method)) {
					// the GUIService should process these command directly
					process(m);
				} else {
					// let a ServiceGUI dialog process the command
					// ServiceGUI sg = serviceGUIMap.get(m.sender);
					ServiceGUI sg = serviceGUIMap.get(m.sender);
					if (sg == null) {
						LOG.error("attempting to update sub-gui - sender "
								+ m.sender + " not available in map " + name);
					} else {
						invoke(serviceGUIMap.get(m.sender), m.method, m.data);
					}
					// processBlocking(m);

				}
				// process(m); // if this is not called the service will not
				// support blocking - possible good reason not to override run
			}
		} catch (InterruptedException e) {
			LOG.error("InterruptedException");
			e.printStackTrace();
		}
	}

	int currentTab = 0;
	
	public ServiceTabPane loadTabPanels() {
		currentTab = tabs.getSelectedIndex();
		
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
		
		LOG.info("tab count" + tabs.getTabCount());
		while (tabs.getTabCount() > 0)
		{
		    tabs.remove(0);
		}
		
		// begin building panels
		network = new Network(this); // TODO - clean this up - add
										
		// TODO - throw error on name collision from client list
		tabs.addTab("communication", (JComponent) network); 

		//JPanel customPanel = new JPanel(new GridBagLayout());
		JPanel customPanel = new JPanel(new FlowLayout());
		customPanel.setPreferredSize(new Dimension(800, 600));
		//gc.anchor = GridBagConstraints.FIRST_LINE_START;

		// iterate through services list begin ------------
		HashMap<String, ServiceEntry> services = hostcfg.getServiceMap();
		sortedMap = new TreeMap<String, ServiceEntry>(services);

		int index = tabs.getTabCount() - 1;
		
		boolean createGUIServiceGUI = false;
		Iterator<String> it = sortedMap.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceEntry se = services.get(serviceName);

			// get service type class name
			String serviceClassName = se.serviceClass;
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";

			if (guiClass.compareTo("org.myrobotlab.control.GUIServiceGUI") == 0) {
				// GUIServiceGUI must be created last to ensure all routing from attachGUI is done
				LOG.debug("delaying construction of GUIServiceGUI ");
				createGUIServiceGUI = true;
				continue;
			}
			
			createTabbedPanel(serviceName, guiClass, index, customPanel, se);

		}

		// creating the ServiceGUI for "this" class
		if (createGUIServiceGUI)
		{
			// TODO - warning this may need more of a delay - or must "remember" notifications of attachGUI
			// going out to remote systems.
			ServiceEntry se = services.get(this.name);
			String serviceClassName = se.serviceClass;
			String guiClass = serviceClassName.substring(serviceClassName.lastIndexOf("."));
			guiClass = "org.myrobotlab.control" + guiClass + "GUI";
			
			guiServiceGUI = (GUIServiceGUI)createTabbedPanel(this.name, guiClass, index, customPanel, se);
		}
		
		tabs.addTab("custom", customPanel);		
		
		frame.pack();
		
		if (currentTab != -1)
		{
			tabs.setSelectedIndex(currentTab);
		} else {
			tabs.setSelectedIndex(0);
		}
		return tabs;

	}
	
	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, int index, JPanel customPanel, ServiceEntry se)
	{
		ServiceGUI gui = null;
		JPanel tpanel = new JPanel();
		gui = (ServiceGUI) getNewInstance(guiClass, se.name, this);
		if (gui != null) {
			serviceGUIMap.put(serviceName, gui);
			gui.attachGUI();

			//cfg.get(se.serviceClass + "/displayOnCustomPanel", false) ||
			 // && customWidgetPrefs.get(se.name) == true
			if (!customWidgetPrefs.containsKey(se.name) || (customWidgetPrefs.containsKey(se.name) && customWidgetPrefs.get(se.name) == false)) {
				tpanel.add(gui.widgetFrame); 
				++index;
				tabs.addTab(serviceName, tpanel);
			} else {
				//gc.gridx = 0;
				//++gc.gridy; // holey crap - there's why the flowmanager did not work
				//customPanel.add(gui.widgetFrame, gc);
				customPanel.add(gui.widgetFrame, gc);
			}

		} else {
			LOG.warn("could not construct a " + guiClass + " object");
		}

		if (se.localServiceHandle == null) {
			tabs.setBackgroundAt(index, Color.decode(cfg.get("remoteColorTab")));
		}
		
		return gui;
		
	}

	public void display() {
		if (frame == null)
		{
			frame = new JFrame();
		}

		frame.addWindowListener(this);
		frame.setTitle("myrobotlab - " + name);
		frame.setSize(150, 300);
		// Image logo = new ImageIcon("mrl_logo_small.jpg").getImage();
		// frame.setIconImage(logo);

		ServiceTabPane stp = loadTabPanels(); 
		panel.add(stp, gc);

/*
		try {
			Thread.sleep(160); // delay display - ecllipse bug? XDnD property
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
*/
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
		frame.add(this.panel);
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
		invoke("guiUpdated", 5);
	}

	// TODO ! - MAKE NULL CAPABLE BUG !
	public void guiUpdated(Integer i) {
		LOG.info("guiUpdated");
		// notification that GUI Update has occured
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
		HashMap<String, ServiceEntry> services = hostcfg.getServiceMap();
		Iterator<String> it = services.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceEntry se = services.get(serviceName);

			Service local = (Service) hostcfg.getLocalServiceHandle(serviceName);

			if (serviceName.compareTo(this.name) == 0) {
				LOG.info("momentarily skipping " + this.name + "....");
				continue;
			}

			if (local != null) {
				LOG.info("shutting down " + serviceName);
				local.stopService();
			} else {
				LOG.info("skipping remote service " + serviceName);

			}

		}

		// shut self down
		LOG.info("shutting down GUIService");
		frame.dispose();		
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
	
	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Invoker invoker = new Invoker("invoker");
		invoker.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
	}
	
	@Override
	public String getToolTip() {
		return "<html>Service used to graphically display and control other services</html>";
	}
		
}
