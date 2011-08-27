package org.myrobotlab.memory;

import java.io.Serializable;
import java.util.Date;

public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	public int ID = 0;
	public Date timestamp = null;
	public String word = null;	
	public VisualData imageData = null;
	
	public Node () 
	{
		imageData = new VisualData();
	}
}
