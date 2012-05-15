package org.myrobotlab.control;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;

public class DigitalButton extends JButton{

	private static final long serialVersionUID = 1L;
	//public int ID = -1;
	public final Object parent;
	String offText = null;
	String onText = null;
	String offCMD = null;
	String onCMD = null;
	
	Color offBGColor = null;
	Color offFGColor = null;
	Color onBGColor = null;
	Color onFGColor = null;
	int type = -1;
	boolean isOn = false;

	public DigitalButton(Object parent, 
			String offText, Color offBGColor, Color offFGColor,
			String onText, Color onBGColor, Color onFGColor,
			int type) 
	{
		this(parent, offText, offText, offBGColor, offFGColor, onText, onText, onBGColor, onFGColor, type);
	}
	
	public DigitalButton(Object parent, 
			String offText, String offCMD, Color offBGColor, Color offFGColor,
			String onText, String onCMD, Color onBGColor, Color onFGColor,
			int type) 
	{
		super(offText);

		this.parent = parent;
		this.type = type;
		this.onText = onText;
		this.offText = offText;
		this.offBGColor = offBGColor;
		this.offFGColor = offFGColor;
		this.onBGColor = onBGColor;
		this.onFGColor = onFGColor;

		
		//setPreferredSize(new Dimension(35,15));
		
		setBackground(offBGColor);
		setForeground(offFGColor);
		//setOpaque(false);
		setBorder(null);
		setOpaque(true);
		setBorderPainted(false);
		//setContentAreaFilled(false);		
		//setIcon(this.offIcon);
	}
	
	public void setOn()
	{
		setBackground(onBGColor);
		setForeground(onFGColor);			
		setText(onText);
		setActionCommand(onCMD);
		isOn = true;
	}
	
	public void setOff()
	{
		setBackground(offBGColor);
		setForeground(offFGColor);
		setText(offText);		
		setActionCommand(offCMD);
		isOn = false;
	}	
	
	public void toggle()
	{
		if (isOn)
		{
			setOff();
		} else {
			setOn();
		}
	}

	public boolean isOn()
	{
		return isOn;
	}
	
}
