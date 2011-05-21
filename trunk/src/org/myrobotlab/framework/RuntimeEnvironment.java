package org.myrobotlab.framework;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class RuntimeEnvironment {

	public final static Logger LOG = Logger.getLogger(RuntimeEnvironment.class.toString());

	static private HashMap<URL, ServiceEnvironment> hosts;
	static private HashMap<String, ServiceWrapper> registry;

	// TODO - concurrentHashMap
	// TODO wrap in service "RuntimeService"
	// TODO releaseAll = stop + unregister
	
	public static boolean register(URL url, Service s)
	{
		return register(url, s, false);
	}
	
	public static synchronized boolean register(URL url, Service s, boolean isRemote)
	{
		if (hosts == null)
		{
			hosts = new HashMap<URL, ServiceEnvironment>();			
		}
		
		if (registry == null)
		{
			registry = new HashMap<String, ServiceWrapper>();
		}
		
		ServiceEnvironment se = null;
		if (!hosts.containsKey(url))
		{
			se = new ServiceEnvironment(url);
			hosts.put(url, se);
		} else {
			se = hosts.get(url);
		}
		
		if (se.serviceDirectory.containsKey(s.name))
		{
			LOG.error("attempting to register " + s.name + " which is already registered in " + url);
			return false;
		} else {
			ServiceWrapper sw = new ServiceWrapper(s, se); 
			se.serviceDirectory.put(s.name, sw);
			registry.put(s.name, sw);
		}
		
		return true;
	}
	
	
	
	public static void unregister(URL url, String name)
	{

		if (!registry.containsKey(name))
		{
			LOG.error("unregister " + name + " does not exist in registry");
		} else {
			registry.remove(name);
		}
		
		if (!hosts.containsKey(url))
		{
			LOG.error("unregister environment does note exist for " + url + "." + name );
			return;
		}

		ServiceEnvironment se = hosts.get(url);
		
		if (!se.serviceDirectory.containsKey(name))
		{
			LOG.error("unregister "+ name +" does note exist for " + url + "." + name );
		} else {
			se.serviceDirectory.remove(name);			
		}
		
				
	}
		
	// unregister a service environment
	public static void unregisterAll(URL url)
	{
		if (!hosts.containsKey(url))
		{
			LOG.error("unregisterAll " + url + " does not exist");
			return;
		}
		
		ServiceEnvironment se = hosts.get(url);
					
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			unregister(url, serviceName);
		}
		
	}
	
	// unregister everything
	public static void unregisterAll()
	{
		Iterator<URL> it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL se = it.next();
			unregisterAll(se);
		}
	}

	public int getServiceCount()
	{
		int cnt = 0;
		Iterator<URL> it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL sen = it.next();
			ServiceEnvironment se = hosts.get(sen);
			Iterator<String> it2 = se.serviceDirectory.keySet().iterator();
			while (it2.hasNext()) {
				String serviceName = it2.next();
				++cnt;
			}
		}
		return cnt;
	}
	
	public int getServiceEnvironmentCount()
	{
		return hosts.size();
	}
	
	
	
	public static ServiceWrapper getService(URL url, String name)
	{
		if (!hosts.containsKey(url))
		{
			LOG.error("getService environment does note exist for " + url + "." + name );
			return null;
		}
		
		ServiceEnvironment se = hosts.get(url);
		
		if (!se.serviceDirectory.containsKey(name))
		{
			LOG.error("getService "+ name +" does note exist for " + url + "." + name );
			return null;
			
		}
		
		return se.serviceDirectory.get(name); 
		
	}
	
	public static void releaseAll() /*local only?*/
	{
		LOG.debug("releaseAll");
		int cnt = 0;
		Iterator<URL> it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL sen = it.next();
			ServiceEnvironment se = hosts.get(sen);
			Iterator<String> seit = se.serviceDirectory.keySet().iterator();
			while (seit.hasNext()) {
				String serviceName = seit.next();
				ServiceWrapper sw = se.serviceDirectory.get(serviceName);
				LOG.info("stopping service "  + se.accessURL + "/" + serviceName);
				//sw.service.releaseService();
				sw.service.stopService();
			}
		}
		
		it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL sen = it.next();
			ServiceEnvironment se = hosts.get(sen);
			LOG.info("clearing environment " + se.accessURL);
			se.serviceDirectory.clear();
			//Iterator<String> seit = se.keySet().iterator();
		}

		LOG.info("clearing hosts environments");
		hosts.clear();
		
		LOG.info("clearing registry");
		registry.clear();
	}


	public static ServiceEnvironment getServiceEnvironment(URL url)
	{
		if (hosts.containsKey(url))
		{
			return hosts.get(url);
		} 
		return null;
	}
}
