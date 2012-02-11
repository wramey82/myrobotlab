package org.myrobotlab.framework;

public class Dependency {
	
	public String organisation;
	public String module;
	public String version;

	public Dependency(String organisation, String module, String version)
	{
		this.organisation 	= organisation;
		this.module 		= module;
		this.version 		= version;
	}
}
