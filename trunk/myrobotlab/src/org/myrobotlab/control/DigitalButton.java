package org.myrobotlab.control;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class DigitalButton extends JButton{

	private static final long serialVersionUID = 1L;
	private int ID = -1; 
	ImageIcon offIcon = null;
	ImageIcon onIcon = null;
	String type = null;
	
	public DigitalButton(int ID, ImageIcon offIcon, ImageIcon onIcon, String type) 
	{
		super();

		this.ID = ID;
		this.type = type;
		this.onIcon = onIcon;
		this.offIcon = offIcon;
		
		// image button properties
		setOpaque(false);
		setBorderPainted(false);
		setContentAreaFilled(false);
		
		setIcon(this.offIcon);
	}

}
