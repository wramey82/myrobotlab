package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.myrobotlab.control.widget.UndockedPanel;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Util;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Runtime;

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
public class TabControl extends JLabel implements ActionListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(TabControl.class.getCanonicalName());

	JPopupMenu popup = new JPopupMenu();
	JTabbedPane parent;
	Container myPanel;
	private String boundServiceName;// FIXME - artifact of "Service" tabs
	JFrame undocked;
	TabControlWindowAdapter windowAdapter = new TabControlWindowAdapter();
	// JFrame top;
	GUIService myService;

	String filename = null;

	public class TabControlWindowAdapter extends WindowAdapter {
		public void windowClosing(WindowEvent winEvt) {
			dockPanel();
		}
	}

	/**
	 * closes window and puts the panel back into the tabbed pane
	 */
	public void dockPanel() {
		// docking panel will move the data of the frame to serializable
		// position
		myService.undockedPanels.get(boundServiceName).savePosition(); // FIXME
																		// -
																		// very
																		// hacked
																		// !
		myService.undockedPanels.get(boundServiceName).isDocked = true;

		parent.add(myPanel);
		parent.setTabComponentAt(parent.getTabCount() - 1, this);
		undocked.dispose();
		undocked = null;

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
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {


		
		parent.remove(myPanel);
		if (boundServiceName.equals(getText())) {

			// service tabs
			undocked = new JFrame(boundServiceName);
			// check to see if this frame was positioned before
			UndockedPanel panel = null;
			if (myService.undockedPanels.containsKey(boundServiceName)) {
				// has been undocked before
				panel = myService.undockedPanels.get(boundServiceName);
				undocked.setLocation(new Point(panel.x, panel.y));
				undocked.setPreferredSize(new Dimension(panel.width, panel.height));
			} else {
				// first time undocked
				panel = new UndockedPanel(undocked);
				myService.undockedPanels.put(boundServiceName, panel);
				panel.x = undocked.getWidth();
				panel.y = undocked.getHeight();
			}

			panel.frame = undocked;
			panel.isDocked = false;

		} else {
			// sub - tabs e.g. Arduino oscope, pins, editor
			undocked = new JFrame(boundServiceName + " " + getText());
		}

		// icon
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		undocked.setIconImage(img);

		undocked.getContentPane().add(myPanel);
		undocked.addWindowListener(windowAdapter);
		// undocked.setTitle(boundServiceName);
		undocked.setVisible(true);
		undocked.pack();
		myService.getFrame().pack();
		myService.save();

			}
		});

	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, Color foreground, Color background) {
		this(gui, parent, myPanel, boundServiceName, boundServiceName, foreground, background);
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName) {
		this(gui, parent, myPanel, boundServiceName, boundServiceName, null, null);
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt, Color foreground, Color background) {
		this(gui, parent, myPanel, boundServiceName, txt);
		if (foreground != null) {
			setForeground(foreground);
		}
		if (background != null) {
			setBackground(background);
		}
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt, String filename) {
		this(gui, parent, myPanel, boundServiceName, txt);
		this.filename = filename;
	}

	public TabControl(GUIService gui, JTabbedPane parent, Container myPanel, String boundServiceName, String txt) {
		super(txt);
		this.parent = parent;
		this.myPanel = myPanel;
		this.boundServiceName = boundServiceName;
		this.myService = gui;

		// build menu
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getScaledIcon(Util.getImage("help.png"), 0.50));
		menuItem.addActionListener(this);
		popup.add(menuItem);

		JMenuItem detachMenuItem = new JMenuItem("detach");
		detachMenuItem.addActionListener(this);
		detachMenuItem.setIcon(Util.getScaledIcon(Util.getImage("detach.png"), 0.50));
		popup.add(detachMenuItem);

		JMenuItem releaseMenuItem = new JMenuItem("release");
		releaseMenuItem.addActionListener(this);
		releaseMenuItem.setIcon(Util.getScaledIcon(Util.getImage("release.png"), 0.50));
		popup.add(releaseMenuItem);

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	private void dispatchMouseEvent(MouseEvent e) {
		parent.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, parent));
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
		if (boundServiceName.equals(getText())) {
			// Service Frame
			ServiceWrapper sw = Runtime.getServiceWrapper(getText());
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getShortTypeName());

			} else if ("detach".equals(cmd)) {
				undockPanel();
			} else if ("release".equals(cmd)) {
				myService.send(Runtime.getInstance().getName(), "releaseService", boundServiceName);
			}
		} else {
			// Sub Tabbed sub pane
			ServiceWrapper sw = Runtime.getServiceWrapper(boundServiceName);
			if ("info".equals(cmd)) {
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getShortTypeName() + "#" + getText());

			} else if ("detach".equals(cmd)) {
				undockPanel();
			}
		}
	}

	public String getFilename() {
		return filename;
	}
}