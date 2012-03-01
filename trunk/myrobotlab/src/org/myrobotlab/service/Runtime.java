package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.apache.ivy.Main;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.cli.CommandLineParser;
import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Dependency;
import org.myrobotlab.framework.Ivy2;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.data.Style;
import org.simpleframework.xml.Element;

/**
 * 
 * Runtime is responsible for the creation and removal of all Services
 * and the associated static registries 
 * It maintains state information regarding possible & running local Services
 * It maintains state information regarding foreign Runtimes
 * It is a singleton and should be the only service of Runtime running in a
 * process
 * The host and registry maps are used in routing communication to the
 * appropriate service (be it local or remote)
 * It will be the first Service created
 * It also wraps the real JVM Runtime object
 *
 */
public class Runtime extends Service {

	private static final long serialVersionUID = 1L;
	
	static public Style style = new Style();
	
	// ---- rte members begin ----------------------------
	static private HashMap<URL, ServiceEnvironment> hosts = new HashMap<URL, ServiceEnvironment>();	;
	static private HashMap<String, ServiceWrapper> registry = new HashMap<String, ServiceWrapper>();
	
	static private boolean inclusiveExportFilterEnabled = false;
	static private boolean exclusiveExportFilterEnabled = false;
	static private HashMap<String, String> inclusiveExportFilter = new HashMap<String, String>();
	static private HashMap<String, String> exclusiveExportFilter = new HashMap<String, String>();
	
	// FIXME - this should be a GUI thing only ! or getPrettyMethods or static filterMethods
	static private HashMap<String, String> hideMethods = new HashMap<String, String>(); 
		
	private static boolean needsRestart = false;
	private static boolean checkForDependencies = true; // TODO implement - Ivy related
	
	public static final String registered = "registered"; 
	
	// VM Names
	public final static String DALVIK 	= "dalvik"; 
	public final static String HOTSPOT 	= "hotspot"; 

	// OS Names
	public final static String LINUX 	= "linux"; 
	public final static String MAC 		= "mac"; 
	public final static String WINDOWS	= "windows"; 
		
	public final static String UNKNOWN	= "unknown"; 	
	// ---- rte members end ------------------------------
	
	// ---- ServiceFactory members begin -----------------
	public final static ServiceInfo info = ServiceInfo.getInstance();

	@Element
	public String proxyHost;
	@Element
	public String proxyPort;
	@Element
	public String proxyUserName;
	@Element
	public String proxyPassword;
	@Element
	public static String ivyFileName = "ivychain.xml";
	
	static Service gui = null;
	// ---- ServiceFactory members end -----------------

	public final static Logger LOG = Logger.getLogger(Runtime.class.getCanonicalName());
	private static Runtime INSTANCE = null;

	private Runtime(String n) {
		super(n, Runtime.class.getCanonicalName());

		hideMethods.put("main", null);
		hideMethods.put("loadDefaultConfiguration", null);
		hideMethods.put("getToolTip", null);
		hideMethods.put("run", null);
		hideMethods.put("access$0", null);

		// starting this
		startService();
	}
	
	public static boolean isRuntime(Service newService)
	{
		return newService.getClass().equals(Runtime.class);
	}
	
	public static Runtime getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new Runtime("BORG " + new Random().nextInt(99999));
		}
		return INSTANCE;		
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "Runtime singleton service";
	}
	
	//---------- Java Runtime wrapper functions begin --------	
	public int exec (String[] params)
	{
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		try {
			Process p = r.exec(params);
			return p.exitValue();
		} catch (IOException e) {
			logException(e);
		}
		
		return 0;
	}
		
	// dorky pass-throughs to the real JVM Runtime
	public static final long getTotalMemory()
	{
	    return java.lang.Runtime.getRuntime().totalMemory();
	}

	public static final long getFreeMemory()
	{
	    return java.lang.Runtime.getRuntime().freeMemory();
	}

	public static final int availableProcessors()
	{
	    return java.lang.Runtime.getRuntime().availableProcessors();
	}

	public static final void exit(int status)
	{
	    java.lang.Runtime.getRuntime().exit(status);
	}

	public static final void gc()
	{
	    java.lang.Runtime.getRuntime().gc();
	}
	
	public static final void loadLibrary(String filename)
	{
	    java.lang.Runtime.getRuntime().loadLibrary(filename);
	}
	
	//---------- Java Runtime wrapper functions end   --------
	
	//-------------pass through begin -------------------
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
			if (INSTANCE != null)
			{
				INSTANCE.invoke("collision", s.getName());
			}
			return s;
		} else {
			ServiceWrapper sw = new ServiceWrapper(s, se); 
			se.serviceDirectory.put(s.getName(), sw);
			registry.put(s.getName(), sw);
			if (INSTANCE != null)
			{
				INSTANCE.invoke("registered", sw);
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
			INSTANCE.invoke("registered", s.serviceDirectory.get(serviceName));
		}

		
		return true;
	}	
	
	/*  FIXME - possibly needed when the intent is to
	 * remove the registration of a foreign Service
	 * 
	 */
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
			INSTANCE.invoke("released", se.serviceDirectory.get(name));
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
				INSTANCE.invoke("released", se.serviceDirectory.get(name));
				se.serviceDirectory.remove(name);				
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
				LOG.info("saving " + sw.service.getName());
				out.writeObject(sw);
				//Iterator<String> seit = se.keySet().iterator();
			}
			*/
			
			//out.writeObject(remote);	
			//out.writeObject(instance);
			out.writeObject(hosts);
			
			out.writeObject(registry);
			out.writeObject(hideMethods);
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
		       hosts = (HashMap<URL, ServiceEnvironment>)in.readObject();
		       registry = (HashMap<String, ServiceWrapper>)in.readObject();
		       hideMethods = (HashMap<String, String>)in.readObject();
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

	// ---------------- callback events begin -------------	
	/**
	 * registration event
	 * @param name - the name of the Service which was successfully registered
	 * @return
	 */
	public ServiceWrapper registered (ServiceWrapper sw)
	{
		return sw;
	}
	
	/**
	 * release event 
	 * @param name - the name of the Service which was successfully released
	 * @return
	 */
	public ServiceWrapper released (ServiceWrapper sw)
	{
		return sw;
	}
	
	
	/**
	 * collision event - when a registration is attempted but there is a 
	 * name collision
	 * @param name - the name of the two Services with the same name
	 * @return
	 */
	public String collision (String name)
	{
		return name;
	}
	// ---------------- callback events end   -------------	
	
	// ---------------- ServiceFactory begin --------------

	static void help() {
		System.out.println("Runtime " + version());
		System.out.println("-h       			# help ");
		System.out.println("-list        		# list services");
		System.out.println("-logToConsole       # redirects logging to console");
		System.out.println("-logLevel        	# log level [DEBUG | INFO | WARNING | ERROR | FATAL]");
		System.out.println("-service [Service Name] [Service] ...");
		System.out.println("example:");
		System.out.println(helpString);
	}

	static String version() {
		String v = FileIO.getResourceFile("version.txt");
		System.out.println(v);
		return v;
	}

	static String helpString = "java -Djava.library.path=./bin org.myrobotlab.service.Runtime -service services Runtime gui GUIService -logLevel DEBUG -logToConsole";

	
	public final static void invokeCMDLine(CMDLine cmdline) {

		if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
			help();
			return;
		}

		if (cmdline.containsKey("-v") || cmdline.containsKey("--version")) {
			version();
			return;
		}

		System.out.println("service count "
				+ cmdline.getArgumentCount("-service") / 2);

		if (cmdline.getArgumentCount("-service") > 0
				&& cmdline.getArgumentCount("-service") % 2 == 0) {

			for (int i = 0; i < cmdline.getArgumentCount("-service"); i += 2) {
								
				LOG.info("attempting to invoke : org.myrobotlab.service."
						+ cmdline.getSafeArgument("-service", i + 1, "") + " named " +
				 cmdline.getSafeArgument("-service", i, ""));

				Service s = Runtime.create(
						cmdline.getSafeArgument("-service", i, ""),
						cmdline.getSafeArgument("-service", i + 1, ""));
				
				s.startService();
				
				// if the service has a display
				// delay the display untill all Services have
				// been created
				if (s.hasDisplay())
				{
					gui = s;
				}
			}
			// if the system is going to have a display
			// display it
			if (gui != null) {
				gui.display();
			}

		} else if (cmdline.hasSwitch("-list")) {
			System.out.println(getServiceShortClassNames());

		} else {
			help();
			return;
		}
	}

	static public String[] getServiceShortClassNames() {
		return getServiceShortClassNames(null);
	}
	
	static public String[] getServiceShortClassNames(String filter) {
		return ServiceInfo.getShortClassNames(filter);
	}

	
	/**	
	 * 
	 * initially I thought that is would be a good idea to dynamically load Services
	 * and append their definitions to the class path.
	 * This would "theoretically" be done with ivy to get/download the appropriate 
	 * dependent jars from the repo.  Then use a custom ClassLoader to load the new
	 * service.
	 * 
	 * Ivy works for downloading the appropriate jars & artifacts
	 * However, the ClassLoader became very problematic
	 * 
	 * There is much mis-information around ClassLoaders.  The most knowledgeable article
	 * I have found has been this one :
	 * http://blogs.oracle.com/sundararajan/entry/understanding_java_class_loading
	 * 
	 * Overall it became a huge PITA with really very little reward.
	 * The consequence is all Services' dependencies and categories are defined here
	 * rather than the appropriate Service class.
	 * 
	 * @return
	 */
	
	/**
	 * @param name - name of Service to be removed and whos resources will be released
	 */
	static public void releaseService(String name) {
		Runtime.release(name);
	}

	
	/**
	 * this "should" be the gateway function to a MyRobotLab instance
	 * going through this main will allow the see{@link}MyRobotLabClassLoader 
	 * to load the appropriate classes and give access to the addURL to allow dynamic
	 * additions of new modules without having to restart.
	 * 
	 * TODO :   -cmd <method> invokes the appropriate static method e.g. -cmd setLogLevel DEBUG
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		URL url = null;
		try {
			 url = new URL ("http://0.0.0.0:0");
		} catch (MalformedURLException e2) {
			Service.logException(e2);
		}
		
		System.out.println(url.getHost());
		System.out.println(url.getPort());
		
		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		try {
			if (cmdline.containsKey("-logToConsole"))
			{
				addAppender(LOGGING_APPENDER_CONSOLE);
				setLogLevel(LOG_LEVEL_DEBUG);
			} else if (cmdline.containsKey("-logToRemote")) {
				String host = cmdline.getSafeArgument("-logToRemote", 0, "localhost");
				String port = cmdline.getSafeArgument("-logToRemote", 1, "4445"); 
				addAppender(LOGGING_APPENDER_SOCKET, host, port);
				setLogLevel(LOG_LEVEL_DEBUG);
			} else {			
				addAppender(LOGGING_APPENDER_ROLLING_FILE);
				setLogLevel(LOG_LEVEL_WARN);
			}
						
			if (cmdline.containsKey("-logLevel"))
			{
				setLogLevel(cmdline.getSafeArgument("-logLevel", 0, "DEBUG"));
			}

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND
			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work
			
			if (cmdline.containsKey("-update"))
			{
				// force all updates
				update();
				return;
			} else {
				invokeCMDLine(cmdline);
			}
		} catch (Exception e) {
			LOG.error(Service.stackToString(e));
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	
	static public Service createAndStart (String name, String type)
	{
		Service s = create(name, type);
		if (s == null)
		{
			LOG.error("cannot start service " + name);
			return null;
		}
		s.startService();
		return s;
	}
	

	static public synchronized Service create(String name, String type) {
		return create(name, "org.myrobotlab.service.", type);
	}

	/**
	 * @param name - name of Service
	 * @param pkgName - package of Service in case Services are created in different packages
	 * @param type - type of Service
	 * @return
	 */
	static public synchronized Service create(String name, String pkgName,
			String type) {
		try {
			LOG.debug("Runtime.create - Class.forName");
			// get String Class
			String typeName = pkgName + type;
			//Class<?> cl = Class.forName(typeName);
			//Class<?> cl = Class.forName(typeName, false, ClassLoader.getSystemClassLoader());
			return createService(name, typeName);
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	
	/**
	 * update - force Ivy to check for all dependencies of all possible
	 * Services - Ivy will attempt to check & fufill dependencies by downloading
	 * jars from the repo 
	 */
	static public void update()
	{
		  Iterator<String> it = ServiceInfo.getKeySet().iterator();
		  while (it.hasNext()) {
		        String s = it.next();
		        getDependencies(s);
		    }
		  // TODO if (Ivy2.hasNewDependencies()) - schedule restart
	}
	
	/** 
	 * gets the dependencies of a Service using Ivy
	 * interfaces with Ivy using its command parameters
	 * @param fullTypeName
	 */
	static public boolean getDependencies(String fullTypeName)
	{
		LOG.debug("getDependencies " + fullTypeName);
		boolean ret = true;
		try {
			// use Ivy standalone			
			// Main.main(cmd.toArray(new String[cmd.size()]));
			// Method getDependencies = cls.getMethod("getDependencies");
			// Programmatic use of Ivy
			// https://cwiki.apache.org/IVY/programmatic-use-of-ivy.html
			ArrayList<Dependency> d = ServiceInfo.getDependencies(fullTypeName);

			if (d != null)
			{
				LOG.info(fullTypeName + " found " + d.size() + " dependencies");
				for (int i=0; i < d.size(); ++i)
				{					
					Dependency dep = d.get(i);					
					
					ArrayList<String> cmd = new ArrayList<String>();
					
					cmd.add("-cache");
					cmd.add(".ivy");
	
					cmd.add("-retrieve");
					cmd.add("libraries/[type]/[artifact].[ext]");
	
					cmd.add("-settings");
					//cmd.add("ivysettings.xml");
					cmd.add(ivyFileName);
	
					//cmd.add("-cachepath");
					//cmd.add("cachefile.txt");					
					
					cmd.add("-dependency");
					cmd.add(dep.organisation); // org
					cmd.add(dep.module); 		// module		
					cmd.add(dep.version); 	// version
					
					cmd.add("-confs");
					String confs = "runtime,"+Runtime.getArch()+"."+
							Runtime.getBitness()+"." + 
							Runtime.getOS();
					cmd.add(confs);
					
					CommandLineParser parser = Main.getParser();
					
					try {
						Ivy2.run(parser, cmd.toArray(new String[cmd.size()]));
						ResolveReport report = Ivy2.getReport();
			            if (report.hasError()) {
			            	ret = false;
			                // System.exit(1);
			            	LOG.error("Ivy resolve error");
			            	// invoke Dependency Error - 
			            	List<String> l = report.getAllProblemMessages();
			            	for (int j = 0; j < l.size(); ++j)
			            	{
				            	
				    			if (INSTANCE != null)
				    			{
				    				INSTANCE.invoke("failedDependency", l.get(j));
				    			}
			            		LOG.error(l.get(j));
			            	}
			            }
					} catch (Exception e)
					{
						logException(e);
					}
					
					// local config - 
					
					// if the Service is downloaded we have to dynamically 
					// load the classes - if we are not going to restart
					// http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
				}
			} else {
				if (d == null)
				{
					LOG.info(fullTypeName + " returned no dependencies");
				}
			}
		} catch (Exception e) {
			Service.logException(e);
			ret = false;
		}		
		
		return ret;
	}
	
	/**
	 * publishing point of Ivy sub system - sends even failedDependency when the
	 * retrieve report for a Service fails
	 * @param dep
	 * @return
	 */
	public String failedDependency(String dep)
	{
		return dep;
	}
	
	/**
	 * @param name
	 * @param cls
	 * @return
	 */
	static public synchronized Service createService(String name, String fullTypeName) {
		LOG.debug("Runtime.createService");
		if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) //|| !cls.isInstance(Service.class)) \
		{
			LOG.error(fullTypeName + " not a type or " + name + " not defined ");
			return null;
		}
		
		ServiceWrapper sw = Runtime.getService(name);
		if (sw != null) {
			LOG.debug("service " + name + " already exists");
			return sw.service;
		}
				
		File ivysettings = new File(ivyFileName);
		if (ivysettings.exists())
		{
			if (!getDependencies(fullTypeName))
			{
				LOG.error("failed dependencies");
				return null;
			}
			// TODO - if (Ivy2.newDependencies()) - schedule restart
		} else {
			LOG.debug(ivyFileName + " not available - will not manage dependencies");
		}

		try {
			
			// TODO - determine if there have been new classes added from ivy			
			LOG.debug("ABOUT TO LOAD CLASS");

			// MyRobotLabClassLoader loader = (MyRobotLabClassLoader)ClassLoader.getSystemClassLoader(); 
			// loader.addURL((new File("libraries/jar/RXTXcomm.jar").toURI().toURL()));
			// ClassLoader Info
			LOG.info("loader for this class " + Runtime.class.getClassLoader().getClass().getCanonicalName());
			LOG.info("parent " + Runtime.class.getClassLoader().getParent().getClass().getCanonicalName());
			LOG.info("system class loader " + ClassLoader.getSystemClassLoader());
			LOG.info("parent should be null" + ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
			LOG.info("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
			LOG.info("thread context parent " + Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
			//MyRobotLabClassLoader loader = (MyRobotLabClassLoader) = Thread.currentThread().getContextClassLoader();
			LOG.info("refreshing classloader");
			//MyRobotLabClassLoader.refresh();
//			Class<?> cls = Class.forName(fullTypeName);
//			Class<?> cls = MyRobotLabClassLoader.getClassLoader().loadClass(fullTypeName);
//			Class<?> cls = Runtime.class.getClassLoader().loadClass(fullTypeName);
			Class<?> cls = Class.forName(fullTypeName);
 			Constructor<?> constructor = cls.getConstructor(new Class[] { String.class });

			// create an instance
			Object newService = constructor.newInstance(new Object[] { name });
			LOG.info("returning " + fullTypeName);
			return (Service)newService;
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	// ---------------- ServiceFactory end   --------------
}
