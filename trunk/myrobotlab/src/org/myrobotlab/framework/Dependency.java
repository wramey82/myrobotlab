package org.myrobotlab.framework;

import org.simpleframework.xml.Element;

public class Dependency {
	
	@Element
	public String organisation;
	@Element
	public String module;
	@Element
	public String version;
	@Element
	public boolean resolved = false;
	@Element
	public boolean released = true;
	
	public Dependency()
	{		
	}
	
	public Dependency(String organisation, String module, String version, boolean released)
	{
		this.organisation 	= organisation;
		this.module 		= module;
		this.version 		= version;
		this.released 		= released;
	}
}
