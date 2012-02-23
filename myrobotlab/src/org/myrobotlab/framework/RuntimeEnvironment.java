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
import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
//import org.myrobotlab.service.interfaces.GUI;

public class RuntimeEnvironment implements Serializable{

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(RuntimeEnvironment.class.toString());

	static private HashMap<URL, ServiceEnvironment> hosts;
	static private HashMap<String, ServiceWrapper> registry;
	
	static private boolean inclusiveExportFilterEnabled = false;
	static private boolean exclusiveExportFilterEnabled = false;
	static private HashMap<String, String> inclusiveExportFilter = new HashMap<String, String>();
	static private HashMap<String, String> exclusiveExportFilter = new HashMap<String, String>();
	
	// TODO - this should be a GUI thing only ! or getPrettyMethods or static filterMethods
	static private HashMap<String, String> hideMethods = new HashMap<String, String>(); 
		
	private static boolean needsRestart = false;
	private static boolean checkForDependencies = true; // TODO implement - Ivy related
	
	// VM Names
	public final static String DALVIK 	= "dalvik"; 
	public final static String HOTSPOT 	= "hotspot"; 

	// OS Names
	public final static String LINUX 	= "linux"; 
	public final static String MAC 		= "mac"; 
	public final static String WINDOWS	= "windows"; 
		
	public final static String UNKNOWN	= "unknown"; 
	
	private static org.myrobotlab.service.Runtime service;
	
	/**
	 * singleton instance of the runtime environment 
	 */
	private static final RuntimeEnvironment INSTANCE = new RuntimeEnvironment();
	// TODO - don't use concurrentHashMap for JVM compatibility reasons - use synchronized and/or cached instances
	// TODO wrap in service "RuntimeService"
	// TODO releaseAll = stop + unregister
	
	private RuntimeEnvironment()
	{
		hosts = new HashMap<URL, ServiceEnvironment>();			
		registry = new HashMap<String, ServiceWrapper>();
		Random rand = new Random();
		String n = "BORG " + rand.nextInt(99999);
		service = new org.myrobotlab.service.Runtime(n); 
		service.startService();
		
		hideMethods.put("main", null);
		hideMethods.put("loadDefaultConfiguration", null);
		hideMethods.put("getToolTip", null);
		hideMethods.put("run", null);
		hideMethods.put("access$0", null);
		
	}
	
	public static String getName()
	{
		return service.getName();
	}
	
	public static org.myrobotlab.service.Runtime getRuntime()
	{
		return service;
	}
	
	public static Platform getPlatform()
	{
		return new Platform(getOS(), getArch(), getBitness(), getVMName());
	}
	
	public static String getOS()
	{
		String os = System.getProperty("os.name").toLowerCase();
		if ((os.indexOf( LINUX ) >= 0))
		{
			return LINUX;
		} else if ((os.indexOf( MAC ) >= 0)) {
			return MAC;			
		} else if ((os.indexOf( "win" ) >= 0))
		{
			return WINDOWS;			
		} else {
			return UNKNOWN;
		}		
	}
	
	public static String getVMName()
	{
		String vmname = System.getProperty("java.vm.name").toLowerCase();
		
		if (vmname.equals(DALVIK))
		{
			return vmname;
		} else {
			return HOTSPOT;
		}
	}
	
	public static int getBitness()
	{
		return 32;
	}
	
	/**
	 * Returns only the bitness of the JRE
	 * hooked here in-case we need to normalize
	 * @return hardware architecture
	 */
	public static String getArch()
	{
		String arch = System.getProperty("os.arch").toLowerCase(); 
		if ("i386".equals(arch) || "i686".equals(arch) || "i586".equals(arch)){
			arch = "x86"; // don't care at the moment
		}
		return arch;
	}	
	
	
	/**
	 * ONLY CALLED BY registerServices2 ... would be a bug if called from 
	 * foreign service - (no platform!) .. URL (URI) will always be null
	 * FIXME - change to register(URL url)
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
//	public static synchronized boolean register(URL url, Service s)
	// TODO more aptly named registerLocal(Service s) ?
	// FIXME - getState publish setState need to reconcile with 
	// these definitions
	public static synchronized Service register(Service s)
	{
		URL url = null; // LOCAL SERVICE !!!
		
		ServiceEnvironment se = null;
		if (!hosts.containsKey(url))
		{
			se = new ServiceEnvironment(url);
			hosts.put(url, se);
		} else {
			se = hosts.get(url);
		}
		
		if (se.serviceDirectory.containsKey(s.getName()))
		{
			LOG.error("attempting to register " + s.getName() + " which is already registered in " + url);
			service.invoke("collision", s.getName());
			return s;
		} else {
			ServiceWrapper sw = new ServiceWrapper(s, se); 
			se.serviceDirectory.put(s.getName(), sw);
			registry.put(s.getName(), sw);
			if (service != null) // !runtime
			{
					service.invoke("registered", s.getName());
			}
		}
		
		return s;
	}
	
	/**
	 * registers a ServiceEnvironment which is a complete set of Services from a
	 * foreign instance of MRL. It returns whether changes have been made.  This
	 * is necessary to determine if the register should be echoed back.
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
	public static synchronized boolean register(URL url, ServiceEnvironment s)
	{
		
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
		
		s.accessURL = url; // NEW - update 
		hosts.put(url, s);
		
		s.serviceDirectory.keySet().iterator();
		
		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			LOG.info("adding " + serviceName + " to registry");
			//s.serviceDirectory.get(serviceName).host = s;
			registry.put(serviceName, s.serviceDirectory.get(serviceName));
			service.invoke("registered", serviceName);
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
			service.invoke("released", name);
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

	
	
	/**
	 * getLocalServicesForExport returns a filtered map of Service references
	 * to export to another instance of MRL.  The objective of filtering may help resolve
	 * functionality, security, or technical issues.  For example, the Dalvik JVM
	 * can only run certain Services.  It would be error prone to export a GUIService
	 * to a jvm which does not support swing. 
	 * 
	 * Since the map of Services is made for export - it is NOT a copy but references
	 * 
	 * The filtering is done by Service Type.. although in the future it could be extended
	 * to Service.getName()
	 * 
	 * @return
	 */
	public static ServiceEnvironment getLocalServicesForExport()
	{		
		if (!hosts.containsKey(null))
		{
			LOG.error("local (null) ServiceEnvironment does not exist");
			return null;
		}

		ServiceEnvironment local = hosts.get(null);
		
		// FIXME - temporary for testing
		//if (getVMName().equals(DALVIK))
		//{
			inclusiveExportFilterEnabled = true;
			addInclusiveExportFilterServiceType("RemoteAdapter");
			addInclusiveExportFilterServiceType("SensorMonitor");
			addInclusiveExportFilterServiceType("Clock");
			addInclusiveExportFilterServiceType("Logging");
		//}
		
		if (!inclusiveExportFilterEnabled && !exclusiveExportFilterEnabled)
		{
			return local; // FIXME - still need to construct new SWs
		}
		
		// URL is null but the "acceptor" will fill in the correct URI/ID
		ServiceEnvironment export = new ServiceEnvironment(null); 	
				
		Iterator<String> it = local.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			ServiceWrapper sw = local.serviceDirectory.get(name); 
			Service s = sw.service;
			if (inclusiveExportFilterEnabled && inclusiveExportFilter.containsKey(s.getClass().getCanonicalName())) {
				LOG.debug("adding " + name + " " + s.getClass().getCanonicalName() + " to export ");
				// create new structure - otherwise it won't be correctly filtered
				ServiceWrapper sw2 = new ServiceWrapper(name, s ,export);
				export.serviceDirectory.put(name, sw2);
			} else {
				LOG.debug("adding " + name + " with name info only");
				ServiceWrapper sw2 = new ServiceWrapper(name, null ,export);
				export.serviceDirectory.put(name, sw2);				
			}
			// TODO - add exclusive list & name vs type exclusions
		}
		
		return export;
	}
	
	public static void addInclusiveExportFilterServiceType (String packageName, String className)
	{
		inclusiveExportFilter.put(packageName + "." + className, className);
	}

	public static void addInclusiveExportFilterServiceType (String shortClassName)
	{
		inclusiveExportFilter.put("org.myrobotlab.service." + shortClassName, shortClassName);
	}
	
	public static ServiceWrapper getService(String name)
	{

		if (!registry.containsKey(name))
		{
			LOG.debug("service " + name + " does not exist");
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

	public static boolean release(String name) /*release local Service*/
	{
		return release (null, name);
	}
	
	// FIXME - can only release local services
	public static boolean release(URL url, String name) //release service environment
	{
		ServiceWrapper sw = getService(url, name);
		if (sw != null)
		{
			if (sw.service != null) {
				sw.service.stopService(); //FIXME send message to stop ??? wait for callback?
				registry.remove(name);
				ServiceEnvironment se = hosts.get(url);
				se.serviceDirectory.remove(name);
				service.invoke("released", name);
				return true;
			}
		}
		return false;
	}
	
	
	public static void release(URL url) /*release process environment*/
	{
		ServiceEnvironment se = hosts.get(url);
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			release(url, name);
		}
	}
	
	public static void releaseAll() /*local only?*/
	{
		LOG.debug("releaseAll");
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
				if (sw.service != null)
				{
					sw.service.stopService();
				} else {
					LOG.warn("unknown type and/or remote service");
				}
			}
		}
		
		it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL sen = it.next();
			ServiceEnvironment se = hosts.get(sen);
			LOG.info("clearing environment " + se.accessURL);
			se.serviceDirectory.clear();
			//Iterator<String> seit = se.keySet().iterator();
			// FIXME - release events
		}

		LOG.info("clearing hosts environments");
		hosts.clear();
		
		LOG.info("clearing registry");
		registry.clear();
	}

	public static HashMap<String, ServiceWrapper> getRegistry()
	{
		return registry;// FIXME should be new HashMap<>
	}

	public static ServiceEnvironment getServiceEnvironment(URL url)
	{
		if (hosts.containsKey(url))
		{
			return hosts.get(url);
		} 
		return null;
	}
	
	public static HashMap<URL, ServiceEnvironment> getServiceEnvironments()
	{
		return new HashMap<URL, ServiceEnvironment> (hosts);
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
				LOG.info("saving " + sw.getName());
				se.serviceDirectory.clear();
				//Iterator<String> seit = se.keySet().iterator();
			}
			*/
			
			//out.writeObject(remote);	
			//out.writeObject(instance);
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
	public static boolean load(String filename)
	{
        try {

		       FileInputStream fis;
		       fis = new FileInputStream(filename);
		       ObjectInputStream in = new ObjectInputStream(fis);
		       //instance = (RuntimeEnvironment)in.readObject();
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
		//boolean hasGUI = false;
		//GUI gui = null;
		ServiceEnvironment se = getLocalServices(); 
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			sw.service.startService();
			/*
			if (sw.service.getClass().getSuperclass().equals(GUI.class))
			{
				gui = (GUI)sw.service;
				hasGUI = true;
			}
			*/
		}
	/*	
		if (hasGUI)
		{
			gui.display();
		}
	*/	
	}
	
	/**
	 * a method which returns a xml representation of all the listeners and routes in the
	 * runtime system
	 * @return
	 */
	public static String dumpNotifyEntries()
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append("<NotifyEntries>");
		
		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			sb.append("<service name=\""+sw.service.getName()+"\" serviceEnironment=\""+ sw.getAccessURL() +"\">");
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
					ret.add(sw.service.getName());
				}

			}

		}
		
		return ret;
	}
	/*

	Implementation - now back in Service 
	
	// new transfer state fns
	public static Service copyState (Service local, Service remote)
	{
		if (local == remote)
			return local;
		
		return local;
	}
	// the assumption is remote has been serialized and the 
	// top level fields need to be merged over
	public static Object deepCopy (Object local, Object remote)
	{
		if (local == remote)
			return local;
		
		return local;
	}
	*/
	public static boolean isMac() {
		return getOS().equals(MAC);
	}
	public static boolean isLinux() {
		return getOS().equals(LINUX);
	}

	public static boolean isWindows() {
		return getOS().equals(WINDOWS);
	}
	
	public static void requestRestart()
	{
		needsRestart = true;
	}
	
	public static boolean needsRestart()
	{
		return needsRestart;
	}

	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		RuntimeEnvironment.getService("blah");
	}

}
