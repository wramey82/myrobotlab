package org.myrobotlab.framework;

public class ServiceWrapper {

	final Service service;
	//final boolean isRemote;	
	final public ServiceEnvironment host; // final immutable
	
	ServiceWrapper(Service s)
	{
		this(s, null);
	}

	ServiceWrapper(Service s, ServiceEnvironment host)
	{
		this.service = s;
		this.host = host; 
	}
	
	
	public Service get()
	{
		return service;
	}
	
	
}
