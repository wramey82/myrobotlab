package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;
import org.myrobotlab.net.BareBonesBrowserLaunch;

public class AboutDialog extends JDialog implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	public AboutDialog(JFrame parent, String title, String message) {
	    super(parent, title, true);
	    if (parent != null) {
	      Dimension parentSize = parent.getSize(); 
	      Point p = parent.getLocation(); 
	      setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
	    }

	    JPanel messagePane = new JPanel();
	    
	    JLabel pic = new JLabel();
		ImageIcon icon = Util.getResourceIcon("mrl_logo_about_128.png");
		if (icon != null)
		{
			pic.setIcon(icon);	
		}
	    
		messagePane.add(pic);
	    
	    JLabel link = new JLabel(message);
	    link.addMouseListener(this);
	    messagePane.add(link);
	    getContentPane().add(messagePane);
	    
	    JPanel buttonPane = new JPanel();
	   
	    JButton button = new JButton("OK"); 
	    buttonPane.add(button); 
	    button.addActionListener(this);
	    
	    button = new JButton("WTF GroG, it \"no-worky\"!"); 
	    buttonPane.add(button); 
	    button.addActionListener(this);

	    button = new JButton("I feel lucky, give me the bleeding edge !"); 
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
			BareBonesBrowserLaunch.openURL("http://myrobotlab.org");
	}
}		  