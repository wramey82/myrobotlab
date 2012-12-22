package org.myrobotlab.control;

import java.awt.Point;
import java.io.Serializable;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class UndockedPanel implements Serializable
{
	public final static Logger log = Logger.getLogger(UndockedPanel.class.getCanonicalName());

	private static final long serialVersionUID = 1L;		
	@Element
	public int x;
	@Element
	public int y;
	@Element
	public int width;
	@Element
	public int height;
	@Element
	public boolean isDocked = false;
	
	transient public JFrame frame;
	
	public UndockedPanel()
	{
	}
	
	public UndockedPanel (JFrame f)
	{
		this.frame = f;
	}
	
	public void savePosition()
	{
		if (frame == null)
		{
			log.error("frame is null");
			return;
		}
		Point point = frame.getLocation();
		x = point.x;
		y = point.y;
		width = frame.getWidth();
		height = frame.getHeight();
	}
	
}