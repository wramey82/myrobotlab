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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Dependency;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Util;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

public class RuntimeGUI extends ServiceGUI implements ActionListener  {

	public final static Logger log = Logger.getLogger(RuntimeGUI.class
			.getCanonicalName());
	static final long serialVersionUID = 1L;

	HashMap<String, ServiceEntry> nameToServiceEntry = new HashMap<String, ServiceEntry>();

	int popupRow = 0; 
	
	JMenuItem installMenuItem = null;
	JMenuItem startMenuItem = null;
	JMenuItem upgradeMenuItem = null;
	JMenuItem releaseMenuItem = null;
	
	DefaultListModel currentServicesModel = new DefaultListModel();
	DefaultTableModel possibleServicesModel = new DefaultTableModel() {
		private static final long serialVersionUID = 1L;

		@Override
	    public boolean isCellEditable(int row, int column) {
	    return false;
	    }
	};
	CellRenderer cellRenderer = new CellRenderer();
	JTable possibleServices = new JTable(possibleServicesModel)
    {
		private static final long serialVersionUID = 1L;

		@Override
		public JToolTip createToolTip() {
			JToolTip tooltip = super.createToolTip();
			//tooltip.setFont(tooltip.getFont().deriveFont(Font. BOLD, 32));
			//tooltip.setForeground(Style.foreground);
			//tooltip.setBackground(Style.background);
			//tooltip.setOpaque(false);
			return tooltip;
		}		
		
		
        // column returns content type 
		public Class<?> getColumnClass(int column)
        {
            return getValueAt(0, column).getClass();
        }
    };
	JList currentServices = new JList(currentServicesModel);
	CurrentServicesRenderer currentServicesRenderer = new CurrentServicesRenderer();
	FilterListener filterListener = new FilterListener();
	JPopupMenu popup = new JPopupMenu();
	
	ServiceEntry releasedTarget = null;
	
	public RuntimeGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {
		gc.gridx = 0;
		gc.gridy = 0;

		getCurrentServices();

		currentServices.setCellRenderer(currentServicesRenderer);

		currentServices.setFixedCellWidth(200);
		possibleServicesModel.addColumn("");
		possibleServicesModel.addColumn("");
		possibleServices.setRowHeight(24);
		//possibleServices.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		
		//possibleServices.setShowHorizontalLines(false);
		//possibleServices.setShowVerticalLines(false);
		possibleServices.setIntercellSpacing(new Dimension(0, 0));
		possibleServices.setShowGrid(false);

		//possibleServices.setGridColor(Style.possibleServicesStable);
		//possibleServices.setGridColor(Style.possibleServicesDev);
		possibleServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		TableColumn col = possibleServices.getColumnModel().getColumn(0);
		col.setPreferredWidth(150);
		possibleServices.setPreferredScrollableViewportSize(new Dimension(300, 480));
		// set map to determine what types get rendered
		possibleServices.setDefaultRenderer(ImageIcon.class, cellRenderer);
		possibleServices.setDefaultRenderer(ServiceEntry.class, cellRenderer);
		possibleServices.setDefaultRenderer(String.class, cellRenderer);
		
		/*
		possibleServices.setCellSelectionEnabled(false);
		possibleServices.setColumnSelectionAllowed(false);
		possibleServices.setRowSelectionAllowed(true);
		possibleServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		*/

		possibleServices.addMouseListener( new MouseAdapter()
		{
			// isPopupTrigger over OSs - use masks
		    public void mouseReleased(MouseEvent e)
		    {
	            log.debug("mouseReleased");
		    	
		    	if (SwingUtilities.isRightMouseButton(e)) {
		            log.debug("mouseReleased - right");
		    		popUpTrigger(e);
		    	}
		    }

		    public void popUpTrigger(MouseEvent e)
		    {
		    	log.info("******************popUpTrigger*********************");
	            JTable source = (JTable)e.getSource();
	            popupRow = source.rowAtPoint( e.getPoint() );
	            ServiceEntry c  = (ServiceEntry)possibleServicesModel.getValueAt(popupRow, 0);
	            releaseMenuItem.setVisible(false);
	            if (ServiceInfo.getInstance().hasUnfulfilledDependencies("org.myrobotlab.service." + c.type))
	            {
	            	// need to install it
	            	installMenuItem.setVisible(true);		            	
	            	startMenuItem.setVisible(false);
	            	upgradeMenuItem.setVisible(false);
	            } else {
	            	// have it
	            	installMenuItem.setVisible(false);
	            	startMenuItem.setVisible(true);
	            	if (ServiceInfo.getInstance().checkForUpgrade("org.myrobotlab.service." + c.type).size() > 0)
	            	{
	            		// has dependencies which can be upgraded
		            	upgradeMenuItem.setVisible(true);
	            	} else {
	            		// no upgrade available
		            	upgradeMenuItem.setVisible(false);
	            	}
	            }

	            int column = source.columnAtPoint( e.getPoint() );

	            if (! source.isRowSelected(popupRow))
	                source.changeSelection(popupRow, column, false, false);

	            popup.show(e.getComponent(), e.getX(), e.getY());
		    	
		    }
		    
		});
		
		
		currentServices.addMouseListener(new MouseAdapter()
		{
			// isPopupTrigger over OSs - use masks
		    public void mouseReleased(MouseEvent e)
		    {
	            log.debug("mouseReleased");
		    	
		    	if (SwingUtilities.isRightMouseButton(e)) {
		            log.debug("mouseReleased - right");
		    		popUpTrigger(e);
		    	}
		    }

		    public void popUpTrigger(MouseEvent e)
		    {
		    	log.info("******************popUpTrigger*********************");
	            JList source = (JList)e.getSource();
	            int index = source.locationToIndex(e.getPoint());
	            if (index >= 0) {
	            	releasedTarget = (ServiceEntry)source.getModel().getElementAt(index);
	            	log.info(String.format("right click on running service %s", releasedTarget.name));
	            	releaseMenuItem.setVisible(true);
	            	upgradeMenuItem.setVisible(false);
	            	installMenuItem.setVisible(false);
	            	startMenuItem.setVisible(false);
	            }
	            popup.show(e.getComponent(), e.getX(), e.getY());
	            
		    	
		    }
		    
		});
		
		
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getScaledIcon(Util.getImage("help.png"), 0.50));
		menuItem.addActionListener(this);
		popup.add(menuItem);
		
		installMenuItem = new JMenuItem("install");
		installMenuItem.addActionListener(this);
		installMenuItem.setIcon(Util.getScaledIcon(Util.getImage("install.png"), 0.50));
		//menuItem.setVisible(false);
		popup.add(installMenuItem);

		startMenuItem = new JMenuItem("start");
		startMenuItem.addActionListener(this);
		startMenuItem.setIcon(Util.getScaledIcon(Util.getImage("start.png"), 0.50));
		//menuItem.setVisible(false);
		popup.add(startMenuItem);
		
		upgradeMenuItem = new JMenuItem("upgrade");
		upgradeMenuItem.addActionListener(this);
		upgradeMenuItem.setIcon(Util.getScaledIcon(Util.getImage("upgrade.png"), 0.50));
		//menuItem.setVisible(false);
		popup.add(upgradeMenuItem);
		
		releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getScaledIcon(Util.getImage("release.png"), 0.50));
		popup.add(releaseMenuItem);
		
		
/*		
		menuItem = new JMenuItem("upgrade");
		menuItem.addActionListener(this);
		menuItem.setVisible(false);
		popup.add(menuItem);
*/		
		
		getPossibleServicesThreadSafe(null);

		GridBagConstraints inputgc = new GridBagConstraints();

		JScrollPane currentServicesScrollPane = new JScrollPane(currentServices);
		JScrollPane possibleServicesScrollPane = new JScrollPane(possibleServices);

		currentServices.setVisibleRowCount(20);

		// make category filter buttons
		JPanel filters = new JPanel(new GridBagLayout());
		GridBagConstraints fgc = new GridBagConstraints();
		++fgc.gridy;
		fgc.fill = GridBagConstraints.HORIZONTAL;
		filters.add(new JLabel("category filters"), fgc);
		++fgc.gridy;
		JButton nofilter = new JButton("all");
		nofilter.addActionListener(filterListener);
		filters.add(nofilter, fgc);
		++fgc.gridy;

		String[] cats = ServiceInfo.getInstance().getUniqueCategoryNames();
		for (int j = 0; j < cats.length; ++j) {
			JButton b = new JButton(cats[j]);
			b.addActionListener(filterListener);
			filters.add(b, fgc);
			++fgc.gridy;
		}

		JPanel input = new JPanel();
		input.setLayout(new GridBagLayout());
		inputgc.anchor = GridBagConstraints.NORTH;
		inputgc.gridx = 0;
		inputgc.gridy = 1;
		input.add(filters, inputgc);
		++inputgc.gridx;
		inputgc.gridy = 0;
		input.add(new JLabel("possible services"), inputgc);
		inputgc.gridy = 1;
		//possibleServicesScrollPane.setBorder(BorderFactory.createEmptyBorder());
		input.add(possibleServicesScrollPane, inputgc);
		++inputgc.gridx;
		//input.add(getReleaseServiceButton(), inputgc);
		++inputgc.gridx;
		//input.add(getAddServiceButton(), inputgc);
		++inputgc.gridx;
		inputgc.gridy = 0;
		input.add(new JLabel("running services"), inputgc);
		inputgc.gridy = 1;
		input.add(currentServicesScrollPane, inputgc);

		TitledBorder title;
		title = BorderFactory.createTitledBorder("services");
		input.setBorder(title);

		display.add(input, gc);

		++gc.gridy;
		gc.gridx = 0;
	}

	public void getCurrentServices() {
		HashMap<String, ServiceWrapper> services = Runtime.getRegistry();

		Map<String, ServiceWrapper> sortedMap = null;
		sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);
			String shortClassName = sw.service.getShortTypeName();
			if (sw.host != null && sw.host.accessURL != null) {
				ServiceEntry se = new ServiceEntry(serviceName, shortClassName,
						true);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			} else {
				// namesAndClasses[i] = serviceName + " - " + shortClassName;
				// namesAndClasses[i] = new ServiceEntry(serviceName,
				// shortClassName);
				ServiceEntry se = new ServiceEntry(serviceName, shortClassName,
						false);
				currentServicesModel.addElement(se);
				nameToServiceEntry.put(serviceName, se);
			}
		}
	}

	
	/*
	public JButton getAddServiceButton() {
		addServiceButton = new BasicArrowButton(BasicArrowButton.EAST);
		addServiceButton.setActionCommand("install");
		addServiceButton.addActionListener(this);

		return addServiceButton;
	}
	*/

	public ServiceWrapper registered(ServiceWrapper sw) {
		String typeName;
		if (sw.service == null) {
			typeName = "unknown";
		} else {
			typeName = sw.service.getShortTypeName();
		}
		ServiceEntry newServiceEntry = new ServiceEntry(sw.name, typeName,
				(sw.host != null && sw.host.accessURL != null));
		currentServicesModel.addElement(newServiceEntry);
		nameToServiceEntry.put(sw.name, newServiceEntry);
		myService.addTab(sw.name);
		return sw;
	}

	public ServiceWrapper released(ServiceWrapper sw) {
		// FIXME - bug if index is moved before call back is processed

		myService.removeTab(sw.name);// FIXME will bust when service == null
		if (nameToServiceEntry.containsKey(sw.name)) {
			currentServicesModel.removeElement(nameToServiceEntry.get(sw.name));
		} else {
			log.error(sw.name
					+ " released event - but could not find in currentServiceModel");
		}
		// myService.loadTabPanels();
		return sw;
	}

	/*
	public JButton getReleaseServiceButton() {
		releaseServiceButton = new BasicArrowButton(BasicArrowButton.WEST);
		releaseServiceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ServiceEntry oldService = (ServiceEntry) currentServices.getSelectedValue();
				myService.send(boundServiceName, "releaseService", oldService.name);
			}

		});

		return releaseServiceButton;
	}
	*/
	
	@Override
	public void attachGUI() {
		subscribe("resolveSuccess", "resolveSuccess", String.class);
		subscribe("resolveError", "resolveError", String.class);
		subscribe("resolveBegin", "resolveBegin", String.class);
		subscribe("resolveEnd", "resolveEnd");
		
		subscribe("registered", "registered", ServiceWrapper.class);
		subscribe("released", "released", ServiceWrapper.class);
		subscribe("failedDependency", "failedDependency", String.class);
		subscribe("proposedUpdates", "proposedUpdates",
				ServiceInfo.class);
	}

	@Override
	public void detachGUI() {
		unsubscribe("resolveSuccess", "resolveSuccess", String.class);
		unsubscribe("resolveError", "resolveError", String.class);
		unsubscribe("resolveBegin", "resolveBegin", String.class);
		unsubscribe("resolveEnd", "resolveEnd");
		
		unsubscribe("registered", "registered", ServiceWrapper.class);
		unsubscribe("released", "released", ServiceWrapper.class);
		unsubscribe("failedDependency", "failedDependency", String.class);
		unsubscribe("proposedUpdates", "proposedUpdates", ServiceInfo.class);
	}

	public void failedDependency(String dep) {
		JOptionPane.showMessageDialog(null,
				"<html>Unable to load Service...<br>" + dep + "</html>",
				"Error", JOptionPane.ERROR_MESSAGE);
	}

	class ServiceEntry {
		public String name;
		public String type;
		public boolean loaded = false;
		public boolean isRemote = false;

		ServiceEntry(String name, String type, boolean isRemote) {
			this.name = name;
			this.type = type;
			this.isRemote = isRemote;
		}

		public String toString() {
			return type;
		}
	}

	class CurrentServicesRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;

		public CurrentServicesRenderer() {
			setOpaque(true);
			setIconTextGap(12);
		}

		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			//log.info("getListCellRendererComponent - begin");
			ServiceEntry entry = (ServiceEntry) value;

			setText("<html><font color=#" + Style.listBackground + ">" + entry.name
					+ "</font></html>");

			//ImageIcon icon = Util.getScaledIcon(Util.getImage((entry.type + ".png").toLowerCase(), "unknown.png"), 0.50);
			ImageIcon icon = Util.getScaledIcon(Util.getImage((entry.type + ".png"), "unknown.png"), 0.50);
			setIcon(icon);

			if (isSelected) {
				setBackground(Style.listHighlight);
				setForeground(Style.listBackground);
			} else {
				setBackground(Style.listBackground);
				setForeground(Style.listForeground);
			}

			//log.info("getListCellRendererComponent - end");
			return this;
		}
	}


	/**
	 * Swing is not thread-safe we
	 * need to wrap the swing calls into a runnable and post them !
	 * http://stackoverflow
	 * .com/questions/4547113/jlist-setlistdata-threading-issues Thank you
	 * Robert & Stack-overflow for something which I was tearing my hair out for
	 * a day !!!
	 */
	public void getPossibleServicesThreadSafe(String filter) {
		Runnable worker = new PossibleServicesRunnable(filter);
		// FIXED - a new AWT Thread is spawned off to do the rendering
		SwingUtilities.invokeLater(worker);
	}

	class PossibleServicesRunnable implements Runnable {
		private String filter;

		public PossibleServicesRunnable(String filter) {
			this.filter = filter;
		}

		public void run() {

			for (int i = possibleServicesModel.getRowCount(); i > 0; --i) {
				possibleServicesModel.removeRow(i - 1);
			}

			possibleServicesModel.getRowCount();

			String[] sscn = Runtime.getServiceShortClassNames(filter);
			ServiceEntry[] ses = new ServiceEntry[sscn.length];
			ServiceEntry se = null;

			for (int i = 0; i < ses.length; ++i) {
				log.info(i);
				se = new ServiceEntry(null, sscn[i], false);

				possibleServicesModel.addRow(new Object[] {se,""});
			}

			possibleServicesModel.fireTableDataChanged();
			possibleServices.invalidate();
		}

	}

	class CellRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean selected, boolean focused, int row,
				int column) {

			//Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			//setBorder(BorderFactory.createEmptyBorder());
			//log.info(value.getClass().getCanonicalName());
			
			setEnabled(table == null || table.isEnabled()); 
			ServiceInfo info = ServiceInfo.getInstance();
			ServiceEntry entry = (ServiceEntry)table.getValueAt(row, 0);
			// FIXME - "org.myrobotlab.service." +  is not allowed "anywhere" - cept in overloaded methods
			boolean available = !(info.hasUnfulfilledDependencies ("org.myrobotlab.service." + entry.type));
			boolean upgradeAvailable = false;
			
			String upgradeString = "<html><h6>upgrade<br>";
			List<Dependency> deps = info.checkForUpgrade("org.myrobotlab.service." + entry.type);
			if (deps.size() > 0)
			{
				upgradeAvailable = true;
				for (int i=0; i < deps.size(); ++i)
				{
					upgradeString += deps.get(i).module + " " + deps.get(i).version;
					
					if (i < deps.size() -1)
					{
						upgradeString += "<br>";
					}
					
				}
				upgradeString += "</h6></html>";
			}
			
			if (value.getClass().equals(ServiceEntry.class))
			{
				setHorizontalAlignment(JLabel.LEFT);
				setIcon(Util.getScaledIcon(Util.getImage( (entry.type +
						 ".png"), "unknown.png"), 0.50));
				setText(entry.type);
				//setToolTipText("<html><body bgcolor=\"#E6E6FA\">" + entry.type+ " <a href=\"http://myrobotlab.org\">blah</a></body></html>");
				
			} else if (value.getClass().equals(String.class))  {
				setIcon(null);
				setHorizontalAlignment(JLabel.LEFT);

				if (!available)
				{
					setText("<html><h6>not<br>installed&nbsp;</h6></html>");
				} else {
					if (upgradeAvailable)
					{
						setText(upgradeString);
					} else {
						setText("<html><h6>latest&nbsp;</h6></html>");
					}
				}				

			} else {
				log.error("unknown class");
			}
					
			if (possibleServices.isRowSelected(row)) { 
				setBackground(Style.listHighlight);
				setForeground(Style.listForeground); 
			 } else {
			
				if (!available) {
					setForeground(Style.listForeground);
					setBackground(Style.possibleServicesNotInstalled); 
				} else {
					if (upgradeAvailable)
					{
						//Component c = super.getTableCellRendererComponent(table, value, selected, focused, row, column-1);
						//c.setForeground(Style.foreground);
						//c.setForeground(Style.possibleServicesUpdate);
						setForeground(Style.listForeground);
						setBackground(Style.possibleServicesUpdate);						
					} else {
						setForeground(Style.listForeground);
						setBackground(Style.possibleServicesStable);
					}
				}
			 }
			


			//setBorder(BorderFactory.createEmptyBorder());
			
			return this;
		}
	}
	
	class FilterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent cmd) {
			log.info(cmd.getActionCommand());
			if ("all".equals(cmd.getActionCommand())) {
				getPossibleServicesThreadSafe(null);
			} else {
				getPossibleServicesThreadSafe(cmd.getActionCommand());
			}
		}

	}

	/**
	 * event method which is called when a "check for updates" request has new
	 * ServiceInfo data from the repo
	 * 
	 * @param si
	 * @return
	 */
	public ServiceInfo proposedUpdates(ServiceInfo si) {
		getPossibleServicesThreadSafe(null);
		return si;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		ServiceEntry c  = (ServiceEntry)possibleServicesModel.getValueAt(popupRow, 0);
		String cmd = event.getActionCommand();
		Object o = event.getSource();
		if (releaseMenuItem == o)
		{
			myService.send(boundServiceName, "releaseService", releasedTarget.name);
			return;
		}
		
		if ("info".equals(cmd))
		{
			BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + c.type);
			
		}  else if ("install".equals(cmd))
		{
			int selectedRow = possibleServices.getSelectedRow();

			String newService = ((ServiceEntry) possibleServices.getValueAt(selectedRow, 0)).toString();
			ServiceInfo serviceInfo = ServiceInfo.getInstance();
			String fullTypeName = "org.myrobotlab.service." + newService;
			if (serviceInfo.hasUnfulfilledDependencies(fullTypeName)) {
				// dependencies needed !!!
				String msg = "<html>This Service has dependencies which are not yet loaded,<br>"
						+ "do you wish to download them now?";
				JOptionPane.setRootFrame(myService.getFrame());
				int result = JOptionPane.showConfirmDialog(
						(Component) null, msg, "alert",
						JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.CANCEL_OPTION) {
					return;
				}

				myService.send(Runtime.getInstance().getName(), "update", "org.myrobotlab.service." + c.type);						
			} else {
				// no unfulfilled dependencies - good to go
				addNewService(newService);
			}

		} else if ("start".equals(cmd)) {
			int selectedRow = possibleServices.getSelectedRow();
			String newService = ((ServiceEntry) possibleServices.getValueAt(selectedRow, 0)).toString();
			addNewService(newService);
		} else if ("upgrade".equals(cmd)) {
			myService.send(Runtime.getInstance().getName(), "update", "org.myrobotlab.service." + c.type);						
		} else {
			log.error("unknown command " + cmd);
		}
		
		// end actionCmd

	}
	
	ProgressDialog progressDialog = null;
	
	public void addNewService(String newService)
	{
		JFrame frame = new JFrame();
		frame.setTitle("add new service");
		String name = JOptionPane.showInputDialog(frame,
				"new service name");

		if (name != null && name.length() > 0) {
			myService.send(boundServiceName, "createAndStart", name,
					newService);

		}
	}
	
	JDialog updateDialog = null;
	List<String> resolveErrors = null;

	public String resolveBegin (String className) // per dependency module
	{
		// FIXME - start dialog - warn previously internet connection necessary
		// no proxy
		if (progressDialog == null)
		{
			progressDialog = new ProgressDialog(myService.getFrame());
			progressDialog.setVisible(true);
		}
		
		resolveErrors = null;
		progressDialog.addInfo("checking for latest version of " + className);
		progressDialog.addInfo("attempting to retrieve " + className + " info");

		return className;
	}
	
	
	public List<String> resolveError (List<String> errors)
	{
		if (progressDialog == null)
		{
			progressDialog = new ProgressDialog(myService.getFrame());
		}
		// FIXME - dialog - there are errors which - cancel? or terminate
		resolveErrors = errors;
		progressDialog.addErrorInfo("ERROR - " + errors);
		//JOptionPane.showMessageDialog(myService.getFrame(), "could not resolve", "error " + errors, JOptionPane.ERROR_MESSAGE);
		return resolveErrors;
	}
	
	public String resolveSuccess (String className)
	{
		progressDialog.addInfo("installed " + className);
		return className;
	}

	
	public void resolveEnd ()
	{
		progressDialog.addInfo("finished processing updates ");
//		progressDialog.setVisible(false);
//		progressDialog.dispose();  FIXME - needs an "OK" to terminate

		if (resolveErrors != null)
		{
			log.info("there were errors");
			progressDialog.addErrorInfo("there were errors " + resolveErrors);
		} else {
			progressDialog.finished();
			GUIService.restart();
		}
	}

}