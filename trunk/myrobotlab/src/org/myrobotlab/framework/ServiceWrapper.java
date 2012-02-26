package org.myrobotlab.framework;

import java.io.Serializable;
import java.net.URL;

public class ServiceWrapper implements Serializable {

	private static final long serialVersionUID = 1L;
	public final Service service;
	public final String name; // needed for sorting
	//final boolean isRemote;	
	final public ServiceEnvironment host; // final immutable
	
	public ServiceWrapper(Service s)
	{
		this(s.getName(), s, null);
	}

	public ServiceWrapper(Service s, ServiceEnvironment host)
	{
		this(s.getName(), s, host);
	}
	
	public ServiceWrapper(String name, Service s, ServiceEnvironment host)
	{
		this.name = name;
		this.service = s;
		this.host = host; 
	}
	
	public URL getAccessURL()
	{
		return host.accessURL;
	}
	
	public Service get()
	{
		return service;
	}
	

}
