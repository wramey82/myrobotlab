package org.myrobotlab.framework;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

public class ServiceEnvironment implements Serializable {

	private static final long serialVersionUID = 1L;

	// access URL
	public URL accessURL;
	public HashMap<String, ServiceWrapper> serviceDirectory; // TODO make public & concurrent
			
	public ServiceEnvironment(URL url)
	{
		this.accessURL = url;
		serviceDirectory = new HashMap<String, ServiceWrapper>();
	}
}
