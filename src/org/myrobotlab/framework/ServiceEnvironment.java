package org.myrobotlab.framework;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;

public class ServiceEnvironment implements Serializable {

	private static final long serialVersionUID = 1L;

	// access URL
	public URL accessURL;
	public HashMap<String, ServiceWrapper> serviceDirectory; // TODO make public & concurrent
			
	ServiceEnvironment(URL url)
	{
		this.accessURL = url;
		serviceDirectory = new HashMap<String, ServiceWrapper>();
	}
	/*
	
	public boolean containsKey (String key)
	{
		return serviceDirectory.containsKey(key);
	}
	
	public ServiceWrapper get(String name)
	{
		return serviceDirectory.get(name);
	}
	
	public ServiceWrapper put(String name, ServiceWrapper sw)
	{
		return serviceDirectory.put(name, sw);
	}
	
	public ServiceWrapper remove(String name)
	{
		return serviceDirectory.remove(name);
	}
	
	public Set<String> keySet()
	{
		return serviceDirectory.keySet();
	}
	
	public URL getURL()
	{
		return accessURL;
	}
	
	public void clear()
	{
		serviceDirectory.clear();
	}
	*/
}
