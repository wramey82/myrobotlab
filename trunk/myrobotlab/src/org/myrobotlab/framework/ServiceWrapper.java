package org.myrobotlab.framework;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import org.myrobotlab.service.interfaces.ServiceInterface;

public class ServiceWrapper implements Serializable {

	private static final long serialVersionUID = 1L;
	public final ServiceInterface service;
	public final String name; // needed for sorting - but not normalized FYI
	final public ServiceEnvironment host; // final immutable
	
	public ServiceWrapper(ServiceInterface s)
	{
		this(s.getName(), s, null);
	}

	public ServiceWrapper(ServiceInterface s, ServiceEnvironment host)
	{
		this(s.getName(), s, host);
	}
	
	public String getName()
	{
		return name;
	}
	
	public Set<String> getNotifyListKeySet()
	{
		return service.getNotifyListKeySet();
	}
	
	public ServiceWrapper(String name, ServiceInterface s, ServiceEnvironment host)
	{
		this.name = name;
		this.service = s;
		this.host = host; 
	}
	
	public boolean isValid()
	{
		return (service != null);
	}
	
	public URL getAccessURL()
	{
		return host.accessURL;
	}
	public String getServiceType()
	{
		if (service != null)
			return service.getClass().getCanonicalName();
		else
			return "unknown";
	}
	
	public String getShortTypeName() {
		String serviceClassName = getServiceType();
		if (serviceClassName.lastIndexOf(".") != -1)
			return serviceClassName.substring(serviceClassName.lastIndexOf(".")+1);
		else 
			return serviceClassName;
	}
	
	public ServiceInterface get()
	{
		return service;
	}
	
	public ArrayList<MRLListener> getNotifyList(String key)
	{
		return service.getNotifyList(key);
	}

	public String getToolTip()
	{
		return service.getToolTip();
	}
}
