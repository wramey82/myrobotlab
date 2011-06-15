package org.myrobotlab.framework;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.myrobotlab.service.interfaces.GUI;

public class RuntimeEnvironment implements Serializable{

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(RuntimeEnvironment.class.toString());

	static private HashMap<URL, ServiceEnvironment> hosts;
	static private HashMap<String, ServiceWrapper> registry;
	
	// TODO - this should be a GUI thing only ! or getPrettyMethods or static filterMethods
	static private HashMap<String, String> hideMethods = new HashMap<String, String>(); 
	
	private static boolean initialized = false;
	
	//static private HashMap<String, String> environmentColor;

	// TODO - concurrentHashMap
	// TODO wrap in service "RuntimeService"
	// TODO releaseAll = stop + unregister
	
	
	public static synchronized boolean register(URL url, Service s)
	{
		if (!initialized)
		{
			init();
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
	
	public static synchronized boolean register(URL url, ServiceEnvironment s)
	{
		if (!initialized)
		{
			init();
		}
		
		if (!hosts.containsKey(url))
		{
			LOG.info("adding new ServiceEnvironment " + url);
		} else {
			ServiceEnvironment se = hosts.get(url);
			
			if (se.serviceDirectory.size() == s.serviceDirectory.size())
			{
				boolean equal = true;
				
				s.serviceDirectory.keySet().iterator();				
				Iterator<String> it = s.serviceDirectory.keySet().iterator();
				while (it.hasNext()) {
					String serviceName = it.next();
					if (!se.serviceDirectory.containsKey(serviceName))
					{
						equal = false;
						break;
					}
				}
				
				if (equal)
				{
					LOG.info("ServiceEnvironment " + url + " already exists - with same count and names");
					return false;
				}
				
			}
			
			
			LOG.info("replacing ServiceEnvironment " + url);			
		}
		
		hosts.put(url, s);
		
		s.serviceDirectory.keySet().iterator();
		
		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			LOG.info("adding " + serviceName + " to registry");
			registry.put(serviceName, s.serviceDirectory.get(serviceName));
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
	

	public static ServiceEnvironment getLocalServices()
	{
		if (!hosts.containsKey(null))
		{
			LOG.error("local (null) ServiceEnvironment does not exist");
			return null;
		}
		
		return hosts.get(null);		
	}
	
	public static ServiceWrapper getService(String name)
	{
		if (!registry.containsKey(name))
		{
			LOG.error("service " + name + " does not exist");
			return null;
		}
		
		return registry.get(name);
		
	}
	
	// get Service from specific Service Environment - null is typically local
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

	public static HashMap<String, ServiceWrapper> getRegistry()
	{
		return registry;
	}

	public static ServiceEnvironment getServiceEnvironment(URL url)
	{
		if (hosts.containsKey(url))
		{
			return hosts.get(url);
		} 
		return null;
	}

	public static synchronized void init()
	{
		hosts = new HashMap<URL, ServiceEnvironment>();			
		registry = new HashMap<String, ServiceWrapper>();

		hideMethods.put("main", null);
		hideMethods.put("loadDefaultConfiguration", null);
		hideMethods.put("getToolTip", null);
		hideMethods.put("run", null);
		hideMethods.put("access$0", null);
		
		initialized = true;

	}
	
	public static HashMap<String, MethodEntry> getMethodMap (String serviceName)
	{
		if (!registry.containsKey(serviceName))
		{
			LOG.error(serviceName + " not in registry - can not return method map");
			return null;
		}
		
		HashMap<String, MethodEntry> ret = new HashMap<String, MethodEntry>(); 
		ServiceWrapper sw = registry.get(serviceName);
		
		Class<?> c = sw.service.getClass();
		Method[] methods = c.getDeclaredMethods();
		
		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			//Class<?>[] paramTypes = m.getParameterTypes();
			//Class<?> returnType = m.getReturnType();

			if (!hideMethods.containsKey(m.getName()))
			{
				MethodEntry me = new MethodEntry();
				me.name = m.getName();
				me.parameterTypes = m.getParameterTypes();
				me.returnType = m.getReturnType();
				String s = MethodEntry.getSignature(me.name, me.parameterTypes, me.returnType);
				ret.put(s, me);
			}


		}
		
		return ret;
	}

	public static boolean save (String filename) 
	{
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {

		//ServiceEnvironment se = getLocalServices();
			
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);

			/*
			Iterator<String> it = se.serviceDirectory.keySet().iterator();
			while (it.hasNext()) {
				String sen = it.next();
				ServiceWrapper sw = se.serviceDirectory.get(sen);
				LOG.info("saving " + sw.name);
				se.serviceDirectory.clear();
				//Iterator<String> seit = se.keySet().iterator();
			}
			*/
			
			//out.writeObject(remote);
			out.writeObject(RuntimeEnvironment.hosts);
			out.writeObject(RuntimeEnvironment.registry);
			out.writeObject(RuntimeEnvironment.hideMethods);
		} catch (FileNotFoundException e) {
			LOG.error(Service.stackToString(e));
			return false;
		} catch (IOException e) {
			LOG.error(Service.stackToString(e));
			return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public static boolean load (String filename)
	{
        try {

		       FileInputStream fis;
		       fis = new FileInputStream(filename);
		       ObjectInputStream in = new ObjectInputStream(fis);
		       RuntimeEnvironment.hosts = (HashMap<URL, ServiceEnvironment>)in.readObject();
		       RuntimeEnvironment.registry = (HashMap<String, ServiceWrapper>)in.readObject();
		       RuntimeEnvironment.hideMethods = (HashMap<String, String>)in.readObject();
		       in.close();

	        } catch (Exception ex) {
				LOG.error(Service.stackToString(ex));
				return false;
			}
	        
	        return true;
	}
	
	public static void startLocalServices()
	{
		boolean hasGUI = false;
		GUI gui = null;
		ServiceEnvironment se = getLocalServices(); 
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			sw.service.startService();
			if (sw.service.getClass().getSuperclass().equals(GUI.class))
			{
				gui = (GUI)sw.service;
				hasGUI = true;
			}
		}
		
		if (hasGUI)
		{
			gui.display();
		}
		
	}
	
	public static String dumpNotifyEntries()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("<NotifyEntries>");
		
		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			sb.append("<service name=\""+sw.service.name+"\" serviceEnironment=\""+ sw.getAccessURL() +"\">");
			Iterator<String> nit = sw.service.getOutbox().notifyList.keySet().iterator();
			while (nit.hasNext()) {
				String n = nit.next();
				sb.append("<notify map=\""+n+"\">");
				ArrayList<NotifyEntry> nes = sw.service.getOutbox().notifyList.get(n);
				for (int i = 0; i < nes.size(); ++i)
				{
					NotifyEntry ne = nes.get(i);
					sb.append("<notifyEntry outMethod=\"" + ne.outMethod + "\" name=\""+ne.name+"\" inMethod=\""+ne.outMethod+"\" />");
				}
				sb.append("</notify>");
			}
			sb.append("</service>");
		}
		
		sb.append("</NotifyEntries>");
		
		return sb.toString();
	}
	
	public static Vector<String> getServicesFromInterface(String interfaceName) {
		Vector<String> ret = new Vector<String>(); 
		
		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			Class<?> c = sw.service.getClass();
			
			Class<?> [] interfaces = c.getInterfaces();
			
			for (int i = 0; i < interfaces.length; ++i) {
				Class<?> m = interfaces[i];

				if (m.getCanonicalName().equals(interfaceName))
				{
					ret.add(sw.service.name);
				}

			}

		}
		
		return ret;
	}
	
}
