package org.myrobotlab.control;

import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.image.Util;
import org.myrobotlab.net.BareBonesBrowserLaunch;
import org.myrobotlab.service.GUIService.UndockedWidgetWindowAdapter;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;

/**
 * @author Gro-G
 * 
 * Mmmmmm...  right click
 * 
 * References:
 * http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components-To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 * http://stackoverflow.com/questions/8080438/mouseevent-of-jtabbedpane
 * http://www.jyloo.com/news/?pubId=1315817317000
 */
public class TabControl extends JLabel implements ActionListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(TabControl.class.getCanonicalName());

	JPopupMenu popup = new JPopupMenu();
	JTabbedPane parent; 
	Container myPanel;
	String boundServiceName;
	JFrame undocked;
	TabControlWindowAdapter windowAdapter = new TabControlWindowAdapter();
	JFrame top;
	
	public class TabControlWindowAdapter extends WindowAdapter
	{		
        public void windowClosing(WindowEvent winEvt) {
            dockPanel();            
        }				
	}		
	
	/**
	 * closes window and puts the panel back into the 
	 * tabbed pane 
	 */
	public void dockPanel()
	{
		parent.add(myPanel);
		parent.setTabComponentAt(parent.getTabCount() - 1, this);
		undocked.dispose();
		undocked = null;
		//frame.pack(); - call pack
		top.pack();
	}
	
	/**
	 * undocks a tabbed panel into a JFrame 
	 */
	public void undockPanel()
	{
		parent.remove(myPanel);
		if (boundServiceName.equals(getText()))
		{
			undocked = new JFrame(boundServiceName);
		} else {
			undocked = new JFrame(boundServiceName + " " + getText());
		}

		// icon
		URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Image img = kit.createImage(url);
		undocked.setIconImage(img);
		
		undocked.getContentPane().add(myPanel);
		undocked.addWindowListener(windowAdapter);
		//undocked.setTitle(boundServiceName);
		undocked.setVisible(true);
		undocked.pack();
		top.pack();
		
	}
	

	public TabControl(JFrame top, JTabbedPane parent, Container myPanel, String boundServiceName)
	{
		this(top, parent, myPanel, boundServiceName, boundServiceName);
	}
	
	public TabControl(JFrame top, JTabbedPane parent, Container myPanel, String boundServiceName, String txt)
	{
		super(txt);
		this.parent = parent;
		this.myPanel = myPanel;
		this.boundServiceName = boundServiceName;
		this.top = top;
		
		// build menu
		JMenuItem menuItem = new JMenuItem("<html><style type=\"text/css\">a { color: #000000;text-decoration: none}</style><a href=\"http://myrobotlab.org/\">info</a></html>");
		menuItem.setActionCommand("info");
		menuItem.setIcon(Util.getScaledIcon(Util.getImage("help.png"), 0.50));
		menuItem.addActionListener(this);
		popup.add(menuItem);
		
		JMenuItem detachMenuItem = new JMenuItem("detach");
		detachMenuItem.addActionListener(this);
		detachMenuItem.setIcon(Util.getScaledIcon(Util.getImage("detach.png"), 0.50));
		//menuItem.setVisible(false);
		popup.add(detachMenuItem);
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	private void dispatchMouseEvent(MouseEvent e)
	{
	  parent.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, parent));
	}     

	@Override
	public void mouseEntered(MouseEvent e)
	{
	  dispatchMouseEvent(e);
	}     
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
        LOG.debug("mouseReleased");
    	
    	if (SwingUtilities.isRightMouseButton(e)) {
            LOG.debug("mouseReleased - right");
    		popUpTrigger(e);
    	}
	  dispatchMouseEvent(e);
	}     
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
	  dispatchMouseEvent(e);
	}     

	@Override
	public void mouseExited(MouseEvent e)
	{
	  dispatchMouseEvent(e);
	}

	
	public void mouseMoved(MouseEvent e)
	{
	  dispatchMouseEvent(e);
	}
	

	@Override
	public void mousePressed(MouseEvent e)
	{
	  dispatchMouseEvent(e);
	}

    public void popUpTrigger(MouseEvent e)
    {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

	@Override
	public void mouseDragged(MouseEvent e) {
		dispatchMouseEvent(e);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		//parent.getSelectedComponent()
		if (boundServiceName.equals(getText()))
		{
			// Service Frame
			ServiceWrapper sw = Runtime.getService(getText());
			if ("info".equals(cmd))
			{
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getShortTypeName());
				
			}  else if ("detach".equals(cmd))
			{
				undockPanel();			
			}		
		} else {
			// Sub Tabbed sub pane
			ServiceWrapper sw = Runtime.getService(boundServiceName);
			if ("info".equals(cmd))
			{
				BareBonesBrowserLaunch.openURL("http://myrobotlab.org/service/" + sw.getShortTypeName() + "#" + getText());
				
			}  else if ("detach".equals(cmd))
			{
				undockPanel();			
			}		
		}
	}    
}
	
