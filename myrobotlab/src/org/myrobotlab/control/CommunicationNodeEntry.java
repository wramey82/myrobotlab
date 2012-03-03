package org.myrobotlab.control;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.myrobotlab.image.Util;

public class CommunicationNodeEntry extends JPanel {

	private static final long serialVersionUID = 1L;

	String host;
	ImageIcon image;

	  public CommunicationNodeEntry(String host, String imagePath) {
	    this.host = host;
	    this.image = Util.getResourceIcon("c0.png");
	  }

	  public String getTitle() {
	    return host;
	  }

	  public ImageIcon getImage() {
	    return image;
	  }

	  // Override standard toString method to give a useful result
	  public String toString() {
	    return host;
	  }	
}
