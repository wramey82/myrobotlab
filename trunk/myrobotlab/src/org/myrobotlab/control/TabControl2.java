package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * @author Gro-G
 * 
 *         Mmmmmm... right click
 * 
 *         References:
 *         http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components
 *         -To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 *         http://stackoverflow.com/questions/8080438/mouseevent-of-jtabbedpane
 *         http://www.jyloo.com/news/?pubId=1315817317000
 */
public class TabControl2 extends JLabel implements ActionListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(TabControl2.class);

	JPopupMenu popup = new JPopupMenu(); // owns it
	JTabbedPane tabs;  // the tabbed pane this tab control belongs to
	Container myPanel; // service gui panel
	JFrame undocked;  // the undocked frame when panel is undocked - when docked this is null
	GUIService myService;

	JMenuItem allowExportMenuItem;

	JMenuItem hide;

	/**
	 * closes window and puts the panel back into the tabbed pane
	 */
	public void dockPanel() {
		String label = getText();
		// docking panel will move the data of the frame to serializable
		// position
		// FIXME - very hacked
		myService.undockedPanels.get(label).savePosition();

		tabs.add(myPanel);
		tabs.setTabComponentAt(tabs.getTabCount() - 1, this);
		
		log.info("{}",tabs.indexOfTab(label));
		
		if (undocked != null) {
			undocked.dispose();
			undocked = null;
		}

		// frame.pack(); - call pack
		myService.getFrame().pack();
		myService.save();
	}

	/**
	 * undocks a tabbed panel into a JFrame FIXME - NORMALIZE - there are
	 * similar methods in GUIService FIXME - there needs to be clear pattern
	 * replacement - this is a decorator - I think... (also it will always be
	 * Swing)
	 * 
	 */
	public void undockPanel() {
		
		//myService.undockPanel(boundServiceName);
		//return;
		myService.undockPanel(getText());

	}



	public TabControl2(GUIService gui, JTabbedPane tabs, Container myPanel, String label) {
		super(label);
		this.tabs = tabs;
		this.myPanel = myPanel;
		this.myService = gui;

		Container c = getParent();
		// build menu
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getImageIcon("help.png"));
		menuItem.addActionListener(this);
		popup.add(menuItem);

		JMenuItem undockMenuItem = new JMenuItem("undock");
		undockMenuItem.addActionListener(this);
		undockMenuItem.setIcon(Util.getImageIcon("undock.png"));
		popup.add(undockMenuItem);

		JMenuItem releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getImageIcon("release.png"));
		popup.add(releaseMenuItem);

		allowExportMenuItem = new JMenuItem("prevent export");
		allowExportMenuItem.setActionCommand("prevent export");
		allowExportMenuItem.addActionListener(this);
		allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
		popup.add(allowExportMenuItem);

		hide = new JMenuItem("hide");
		hide.setActionCommand("hide");
		hide.addActionListener(this);
		hide.setIcon(Util.getImageIcon("hide.png"));
		popup.add(hide);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		//this(gui, parent, myPanel, boundServiceName, txt);
	}

	private void dispatchMouseEvent(MouseEvent e) {
		tabs.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, tabs));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		log.debug("mouseReleased");

		if (SwingUtilities.isRightMouseButton(e)) {
			log.debug("mouseReleased - right");
			popUpTrigger(e);
		}
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (myService != null) {
			myService.lastTabVisited = this.getText();
		}
		dispatchMouseEvent(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	public void mouseMoved(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	public void popUpTrigger(MouseEvent e) {
		popup.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		dispatchMouseEvent(e);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		// parent.getSelectedComponent()
		String label = getText();
		if (label.equals(getText())) {
			// Service Frame
			ServiceInterface sw = Runtime.getService(getText());
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName());

			} else if ("undock".equals(cmd)) {
				undockPanel();
			} else if ("release".equals(cmd)) {
				myService.send(Runtime.getInstance().getName(), "releaseService", label);
			} else if ("prevent export".equals(cmd)) {
				myService.send(label, "allowExport", false);
				allowExportMenuItem.setIcon(Util.getImageIcon("allowExport.png"));
				allowExportMenuItem.setActionCommand("allow export");
				allowExportMenuItem.setText("allow export");
			} else if ("allow export".equals(cmd)) {
				myService.send(label, "allowExport", true);
				allowExportMenuItem.setIcon(Util.getImageIcon("preventExport.png"));
				allowExportMenuItem.setActionCommand("prevent export");
				allowExportMenuItem.setText("prevent export");
			} else if ("hide".equals(cmd)) {
				//myService.send(label, "hide", true);
				myService.hidePanel(label);
			}
		} else {
			// Sub Tabbed sub pane
			ServiceInterface sw = Runtime.getService(label);
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getSimpleName() + "#" + getText());

			} else if ("undock".equals(cmd)) {
				undockPanel();
			}
		}
	}

}
