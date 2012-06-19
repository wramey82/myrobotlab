package org.myrobotlab.service;

import java.io.FileInputStream;
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

import org.apache.log4j.Logger;
import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.*;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;

/**
 * 
 * Runtime is responsible for the creation and removal of all Services and the
 * associated static regestries It maintains state information regarding
 * possible & running local Services It maintains state information regarding
 * foreign Runtimes It is a singleton and should be the only service of Runtime
 * running in a process The host and registry maps are used in routing
 * communication to the appropriate service (be it local or remote) It will be
 * the first Service created It also wraps the real JVM Runtime object
 * 
 */
public class Runtime extends Service {

	private static final long serialVersionUID = 1L;

	// ---- rte members begin ----------------------------
	static private HashMap<URL, ServiceEnvironment> hosts = new HashMap<URL, ServiceEnvironment>();;
	static private HashMap<String, ServiceWrapper> registry = new HashMap<String, ServiceWrapper>();

	static private boolean inclusiveExportFilterEnabled = false;
	static private boolean exclusiveExportFilterEnabled = false;
	static private HashMap<String, String> inclusiveExportFilter = new HashMap<String, String>();
	static private HashMap<String, String> exclusiveExportFilter = new HashMap<String, String>();

	// FIXME - this should be a GUI thing only ! or getPrettyMethods or static
	// filterMethods
	static private HashMap<String, String> hideMethods = new HashMap<String, String>();

	private static boolean needsRestart = false;
	private static boolean checkForDependencies = true; // TODO implement - Ivy
														// related


	// ---- rte members end ------------------------------

	// ---- Runtime members begin -----------------
	public final static ServiceInfo serviceInfo = ServiceInfo.getInstance();

	@Element
	public String proxyHost;
	@Element
	public String proxyPort;
	@Element
	public String proxyUserName;
	@Element
	public String proxyPassword;

	static ServiceInterface gui = null;
	// ---- Runtime members end -----------------

	public final static Logger log = Logger.getLogger(Runtime.class.getCanonicalName());
	private static Runtime INSTANCE = null;

	/**
	 * Constructor.
	 * 
	 * @param n
	 */
	private Runtime(String n) {
		super(n, Runtime.class.getCanonicalName());

		hideMethods.put("main", null);
		hideMethods.put("loadDefaultConfiguration", null);
		hideMethods.put("getToolTip", null);
		hideMethods.put("run", null);
		hideMethods.put("access$0", null);

		// load the current set of possible service
		serviceInfo.getLocalServiceData();

		// starting this
		startService();
	}

	// TODO - put this method in ServiceInterface
	/**
	 * 
	 * @param newService
	 * @return
	 */
	public static boolean isRuntime(Service newService) {
		return newService.getClass().equals(Runtime.class);
	}

	/**
	 * Get a handle to this singleton.
	 * @return
	 */
	public static Runtime getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Runtime("BORG " + new Random().nextInt(99999));
		}
		return INSTANCE;
	}

	/**
	 * FYI - if stopServices deos not remove INSTANCE - it is not re-entrant in junit tests
	 */
	@Override
	public void stopService() {
		super.stopService();
		INSTANCE = null;
	}

	/**
	 * 
	 */
	@Override
	public void loadDefaultConfiguration() {

	}

	/**
	 * 
	 */
	@Override
	public String getToolTip() {
		return "Runtime singleton service";
	}

	// ---------- Java Runtime wrapper functions begin --------
	/**
	 * 
	 * @param params
	 * @return
	 */
	public int exec(String[] params) {
		java.lang.Runtime r = java.lang.Runtime.getRuntime();
		try {
			Process p = r.exec(params);
			return p.exitValue();
		} catch (IOException e) {
			logException(e);
		}

		return 0;
	}

	/**
	 * dorky pass-throughs to the real JVM Runtime
	 * 
	 * @return
	 */
	public static final long getTotalMemory() {
		return java.lang.Runtime.getRuntime().totalMemory();
	}

	/**
	 * 
	 * @return
	 */
	public static final long getFreeMemory() {
		return java.lang.Runtime.getRuntime().freeMemory();
	}

	/**
	 * 
	 * @return
	 */
	public static final int availableProcessors() {
		return java.lang.Runtime.getRuntime().availableProcessors();
	}

	/**
	 * 
	 * @param status
	 */
	public static final void exit(int status) {
		java.lang.Runtime.getRuntime().exit(status);
	}

	/**
	 * 
	 */
	public static final void gc() {
		java.lang.Runtime.getRuntime().gc();
	}

	/**
	 * 
	 * @param filename
	 */
	public static final void loadLibrary(String filename) {
		java.lang.Runtime.getRuntime().loadLibrary(filename);
	}

	// ---------- Java Runtime wrapper functions end --------

	/**
	 * ONLY CALLED BY registerServices2 ... would be a bug if called from
	 * foreign service - (no platform!) .. URL (URI) will always be null FIXME -
	 * change to register(URL url)
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
	// public static synchronized boolean register(URL url, Service s)
	// TODO more aptly named registerLocal(Service s) ?
	// FIXME - getState publish setState need to reconcile with
	// these definitions
	public static synchronized Service register(Service s, URL url) {
		// URL url = null; // LOCAL SERVICE !!!

		ServiceEnvironment se = null;
		if (!hosts.containsKey(url)) {
			se = new ServiceEnvironment(url);
			hosts.put(url, se);
		} else {
			se = hosts.get(url);
		}

		if (se.serviceDirectory.containsKey(s.getName())) {
			log.error("attempting to register " + s.getName() + " which is already registered in " + url);
			if (INSTANCE != null) {
				INSTANCE.invoke("collision", s.getName());
			}
			return s;
		} else {
			ServiceWrapper sw = new ServiceWrapper(s, se);
			se.serviceDirectory.put(s.getName(), sw);
			registry.put(s.getName(), sw);
			if (INSTANCE != null) {
				INSTANCE.invoke("registered", sw);
			}
		}

		return s;
	}

	/**
	 * registers a ServiceEnvironment which is a complete set of Services from a
	 * foreign instance of MRL. It returns whether changes have been made. This
	 * is necessary to determine if the register should be echoed back.
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
	public static synchronized boolean register(URL url, ServiceEnvironment s) {

		if (!hosts.containsKey(url)) {
			log.info("adding new ServiceEnvironment " + url);
		} else {
			ServiceEnvironment se = hosts.get(url);

			if (se.serviceDirectory.size() == s.serviceDirectory.size()) {
				boolean equal = true;

				s.serviceDirectory.keySet().iterator();
				Iterator<String> it = s.serviceDirectory.keySet().iterator();
				while (it.hasNext()) {
					String serviceName = it.next();
					if (!se.serviceDirectory.containsKey(serviceName)) {
						equal = false;
						break;
					}
				}

				if (equal) {
					log.info("ServiceEnvironment " + url + " already exists - with same count and names");
					return false;
				}

			}

			log.info("replacing ServiceEnvironment " + url);
		}

		s.accessURL = url; // NEW - update
		hosts.put(url, s);

		s.serviceDirectory.keySet().iterator();

		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			log.info("adding " + serviceName + " to registry");
			// s.serviceDirectory.get(serviceName).host = s;
			registry.put(serviceName, s.serviceDirectory.get(serviceName));
			INSTANCE.invoke("registered", s.serviceDirectory.get(serviceName));
		}

		return true;
	}

	/*
	 * FIXME - possibly needed when the intent is to remove the registration of
	 * a foreign Service
	 */
	public static void unregister(URL url, String name) {

		if (!registry.containsKey(name)) {
			log.error("unregister " + name + " does not exist in registry");
		} else {
			registry.remove(name);
		}

		if (!hosts.containsKey(url)) {
			log.error("unregister environment does note exist for " + url + "." + name);
			return;
		}

		ServiceEnvironment se = hosts.get(url);

		if (!se.serviceDirectory.containsKey(name)) {
			log.error("unregister " + name + " does note exist for " + url + "." + name);
		} else {
			INSTANCE.invoke("released", se.serviceDirectory.get(name));
			se.serviceDirectory.remove(name);
		}

	}

	/**
	 * unregister a service environment
	 * 
	 * @param url
	 */
	public static void unregisterAll(URL url) {
		if (!hosts.containsKey(url)) {
			log.error("unregisterAll " + url + " does not exist");
			return;
		}

		ServiceEnvironment se = hosts.get(url);

		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			unregister(url, serviceName);
		}

	}

	/**
	 * unregister everything
	 */
	public static void unregisterAll() {
		Iterator<URL> it = hosts.keySet().iterator();
		while (it.hasNext()) {
			URL se = it.next();
			unregisterAll(se);
		}
	}

	/**
	 * 
	 * @return
	 */
	public int getServiceCount() {
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

	/**
	 * 
	 * @return
	 */
	public int getServiceEnvironmentCount() {
		return hosts.size();
	}

	/**
	 * 
	 * @return
	 */
	public static ServiceEnvironment getLocalServices() {
		if (!hosts.containsKey(null)) {
			log.error("local (null) ServiceEnvironment does not exist");
			return null;
		}

		return hosts.get(null);
	}

	/**
	 * getLocalServicesForExport returns a filtered map of Service references to
	 * export to another instance of MRL. The objective of filtering may help
	 * resolve functionality, security, or technical issues. For example, the
	 * Dalvik JVM can only run certain Services. It would be error prone to
	 * export a GUIService to a jvm which does not support swing.
	 * 
	 * Since the map of Services is made for export - it is NOT a copy but
	 * references
	 * 
	 * The filtering is done by Service Type.. although in the future it could
	 * be extended to Service.getName()
	 * 
	 * @return
	 */
	public static ServiceEnvironment getLocalServicesForExport() {
		if (!hosts.containsKey(null)) {
			log.error("local (null) ServiceEnvironment does not exist");
			return null;
		}

		ServiceEnvironment local = hosts.get(null);

		// FIXME - temporary for testing
		// if (getVMName().equals(DALVIK))
		// {
		inclusiveExportFilterEnabled = true;
		addInclusiveExportFilterServiceType("RemoteAdapter");
		addInclusiveExportFilterServiceType("SensorMonitor");
		addInclusiveExportFilterServiceType("Clock");
		addInclusiveExportFilterServiceType("Logging");
		addInclusiveExportFilterServiceType("Jython");
		addInclusiveExportFilterServiceType("GUIService");
		addInclusiveExportFilterServiceType("Runtime");
		// }

		if (!inclusiveExportFilterEnabled && !exclusiveExportFilterEnabled) {
			return local; // FIXME - still need to construct new SWs
		}

		// URL is null but the "acceptor" will fill in the correct URI/ID
		ServiceEnvironment export = new ServiceEnvironment(null);

		Iterator<String> it = local.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			ServiceWrapper sw = local.serviceDirectory.get(name);
			// Service s = (Service)sw.get();
			// s.getClass()
			if (inclusiveExportFilterEnabled && inclusiveExportFilter.containsKey(sw.getServiceType())) {
				log.debug("adding " + name + " " + sw.getServiceType() + " to export ");
				// create new structure - otherwise it won't be correctly
				// filtered
				ServiceWrapper sw2 = new ServiceWrapper(name, sw.get(), export);
				export.serviceDirectory.put(name, sw2);
			} else {
				log.debug("adding " + name + " with name info only");
				ServiceWrapper sw2 = new ServiceWrapper(name, null, export);
				export.serviceDirectory.put(name, sw2);
			}
			// TODO - add exclusive list & name vs type exclusions
		}

		return export;
	}

	/**
	 * 
	 * @param packageName
	 * @param className
	 */
	public static void addInclusiveExportFilterServiceType(String packageName, String className) {
		inclusiveExportFilter.put(packageName + "." + className, className);
	}

	/**
	 * 
	 * @param shortClassName
	 */
	public static void addInclusiveExportFilterServiceType(String shortClassName) {
		inclusiveExportFilter.put("org.myrobotlab.service." + shortClassName, shortClassName);
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static ServiceWrapper getService(String name) {

		if (!registry.containsKey(name)) {
			log.debug("service " + name + " does not exist");
			return null;
		}

		return registry.get(name);

	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static boolean release(String name) /* release local Service */
	{
		log.info("releasing service " + name);
		ServiceWrapper sw = getService(name);
		if (sw != null) {
			URL url = sw.getAccessURL();
			if (sw.isValid()) {
				if (url == null) {
					sw.get().stopService();// if its a local Service shut it
											// down
				}				
				ServiceEnvironment se = hosts.get(url);
				INSTANCE.invoke("released", se.serviceDirectory.get(name));
				registry.remove(name);
				se.serviceDirectory.remove(name);
				if (se.serviceDirectory.size() == 0)
				{
					log.info("service directory empty - removing host");
					hosts.remove(se); // TODO - invoke message
				}
				log.info("released " + name);
				return true;
			}
		} else {
			log.error("no service wrapper for " + name);
		}
		return false;
		
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static boolean release(URL url) /* release process environment */
	{
		log.info("releasing url " + url);
		boolean ret = true;
		ServiceEnvironment se = hosts.get(url);
		if (se != null)
		{
			String[] services = (String[])se.serviceDirectory.keySet().toArray(new String[se.serviceDirectory.keySet().size()]);
			String runtimeName = null;
			for (int i = 0; i < services.length; ++i)
			{
				ServiceInterface service =  registry.get(services[i]).get();
				if ("Runtime".equals(service.getShortTypeName()))
				{
					runtimeName = service.getName();
					log.info("delaying release of Runtime " + runtimeName);
					continue;
				}
				ret &= release(services[i]);
			}
			
			if (runtimeName != null)
			{
				ret &= release(runtimeName);
			}
		} else {
			log.warn("attempt to release " + url + " not successful - it does not exist");
			return false;
		}
		
		return ret;
	}

	/**
	 * 
	 * release all local services
	 * 
	 * FIXME - there "should" be an order to releasing the correct way would be
	 * to save the Runtime for last and broadcast all the services being
	 * released
	 */
	public static void releaseAll() /* local only? */
	{
		log.debug("releaseAll");
		
		// FIXME - release all by calling sub methods & normalize code

		// FIXME - this is a bit of a lie
		// broadcasting the info all services are released before releasing them
		// but you can't send the info if everything has been released :P

		ServiceEnvironment se = hosts.get(null); // local services only
		Iterator<String> seit = se.serviceDirectory.keySet().iterator();
		while (seit.hasNext()) {
			String serviceName = seit.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			INSTANCE.invoke("released", se.serviceDirectory.get(serviceName));
		}

		seit = se.serviceDirectory.keySet().iterator();
		while (seit.hasNext()) {
			String serviceName = seit.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			log.info("stopping service " + se.accessURL + "/" + serviceName);

			if (sw.service != null) {
				sw.service.stopService();
			} else {
				log.warn("unknown type and/or remote service");
			}
		}

		log.info("clearing hosts environments");
		hosts.clear();

		log.info("clearing registry");
		registry.clear();
	}

	/**
	 * 
	 * @return
	 */
	public static HashMap<String, ServiceWrapper> getRegistry() {
		return registry;// FIXME should return copy
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static ServiceEnvironment getServiceEnvironment(URL url) {
		if (hosts.containsKey(url)) {
			return hosts.get(url); // FIXME should return copy
		}
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public static HashMap<URL, ServiceEnvironment> getServiceEnvironments() {
		return new HashMap<URL, ServiceEnvironment>(hosts);
	}

	/**
	 * 
	 * @param serviceName
	 * @return
	 */
	public static HashMap<String, MethodEntry> getMethodMap(String serviceName) {
		if (!registry.containsKey(serviceName)) {
			log.error(serviceName + " not in registry - can not return method map");
			return null;
		}

		HashMap<String, MethodEntry> ret = new HashMap<String, MethodEntry>();
		ServiceWrapper sw = registry.get(serviceName);

		Class<?> c = sw.service.getClass();
		Method[] methods = c.getDeclaredMethods();

		for (int i = 0; i < methods.length; ++i) {
			Method m = methods[i];
			// Class<?>[] paramTypes = m.getParameterTypes();
			// Class<?> returnType = m.getReturnType();

			if (!hideMethods.containsKey(m.getName())) {
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

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public static boolean save(String filename) {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {

			// ServiceEnvironment se = getLocalServices();

			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);

			/*
			 * Iterator<String> it = se.serviceDirectory.keySet().iterator();
			 * while (it.hasNext()) { String sen = it.next(); ServiceWrapper sw
			 * = se.serviceDirectory.get(sen); log.info("saving " +
			 * sw.service.getName()); out.writeObject(sw); //Iterator<String>
			 * seit = se.keySet().iterator(); }
			 */

			// out.writeObject(remote);
			// out.writeObject(instance);
			out.writeObject(hosts);

			out.writeObject(registry);
			out.writeObject(hideMethods);
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static boolean load(String filename) {
		try {

			FileInputStream fis;
			fis = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fis);
			// instance = (RuntimeEnvironment)in.readObject();
			hosts = (HashMap<URL, ServiceEnvironment>) in.readObject();
			registry = (HashMap<String, ServiceWrapper>) in.readObject();
			hideMethods = (HashMap<String, String>) in.readObject();
			in.close();

		} catch (Exception e) {
			Service.logException(e);
			return false;
		}

		return true;
	}

	/**
	 * 
	 */
	public static void startLocalServices() {
		// boolean hasGUI = false;
		// GUI gui = null;
		ServiceEnvironment se = getLocalServices();
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			sw.service.startService();
			/*
			 * if (sw.service.getClass().getSuperclass().equals(GUI.class)) {
			 * gui = (GUI)sw.service; hasGUI = true; }
			 */
		}
		/*
		 * if (hasGUI) { gui.display(); }
		 */
	}

	/**
	 * a method which returns a xml representation of all the listeners and
	 * routes in the runtime system
	 * 
	 * @return
	 */
	public static String dumpNotifyEntries() {
		StringBuffer sb = new StringBuffer();

		sb.append("<NotifyEntries>");

		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			sb.append("<service name=\"" + sw.getName() + "\" serviceEnironment=\"" + sw.getAccessURL() + "\">");
			Iterator<String> nit = sw.getNotifyListKeySet().iterator();

			while (nit.hasNext()) {
				String n = nit.next();
				sb.append("<notify map=\"" + n + "\">");
				ArrayList<NotifyEntry> nes = sw.getNotifyList(n);
				for (int i = 0; i < nes.size(); ++i) {
					NotifyEntry ne = nes.get(i);
					sb.append("<notifyEntry outMethod=\"" + ne.outMethod + "\" name=\"" + ne.name + "\" inMethod=\""
							+ ne.outMethod + "\" />");
				}
				sb.append("</notify>");
			}
			sb.append("</service>");
		}

		sb.append("</NotifyEntries>");

		return sb.toString();
	}

	/**
	 * 
	 * @param interfaceName
	 * @return
	 */
	public static Vector<String> getServicesFromInterface(String interfaceName) {
		Vector<String> ret = new Vector<String>();

		Iterator<String> it = registry.keySet().iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = registry.get(serviceName);
			Class<?> c = sw.service.getClass();

			Class<?>[] interfaces = c.getInterfaces();

			for (int i = 0; i < interfaces.length; ++i) {
				Class<?> m = interfaces[i];

				if (m.getCanonicalName().equals(interfaceName)) {
					ret.add(sw.service.getName());
				}

			}

		}

		return ret;
	}

	/*
	 * 
	 * Implementation - now back in Service
	 * 
	 * // new transfer state fns public static Service copyState (Service local,
	 * Service remote) { if (local == remote) return local;
	 * 
	 * return local; } // the assumption is remote has been serialized and the
	 * // top level fields need to be merged over public static Object deepCopy
	 * (Object local, Object remote) { if (local == remote) return local;
	 * 
	 * return local; }
	 */
	public static void requestRestart() {
		needsRestart = true;
	}

	/**
	 * 
	 * @return
	 */
	public static boolean needsRestart() {
		return needsRestart;
	}

	// ---------------- callback events begin -------------
	/**
	 * registration event
	 * 
	 * @param name
	 *            - the name of the Service which was successfully registered
	 * @return
	 */
	public ServiceWrapper registered(ServiceWrapper sw) {
		return sw;
	}

	/**
	 * release event
	 * 
	 * @param name
	 *            - the name of the Service which was successfully released
	 * @return
	 */
	public ServiceWrapper released(ServiceWrapper sw) {
		return sw;
	}

	/**
	 * collision event - when a registration is attempted but there is a name
	 * collision
	 * 
	 * @param name
	 *            - the name of the two Services with the same name
	 * @return
	 */
	public String collision(String name) {
		return name;
	}

	// ---------------- callback events end -------------

	// ---------------- Runtime begin --------------

	/**
	 * 
	 */
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

	/**
	 * 
	 * @return
	 */
	static String version() {
		String v = FileIO.getResourceFile("version.txt");
		System.out.println(v);
		return v;
	}

	static String helpString = "java -Djava.library.path=./libraries/native/x86.32.windows org.myrobotlab.service.Runtime -service gui GUIService -logLevel DEBUG -logToConsole";

	/**
	 * 
	 * @param cmdline
	 */
	public final static void invokeCMDLine(CMDLine cmdline) {

		if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
			help();
			return;
		}

		if (cmdline.containsKey("-v") || cmdline.containsKey("--version")) {
			version();
			return;
		}

		System.out.println("service count " + cmdline.getArgumentCount("-service") / 2);

		if (cmdline.getArgumentCount("-service") > 0 && cmdline.getArgumentCount("-service") % 2 == 0) {

			for (int i = 0; i < cmdline.getArgumentCount("-service"); i += 2) {

				log.info("attempting to invoke : org.myrobotlab.service."
						+ cmdline.getSafeArgument("-service", i + 1, "") + " named "
						+ cmdline.getSafeArgument("-service", i, ""));

				String name = cmdline.getSafeArgument("-service", i, "");
				String type = cmdline.getSafeArgument("-service", i + 1, "");
				ServiceInterface s = Runtime.create(name, type);

				if (s != null) {
					s.startService();
				} else {
					log.error("could not create service " + name + " " + type);
				}

				// if the service has a display
				// delay the display untill all Services have
				// been created
				if (s.hasDisplay()) {
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

	/**
	 * 
	 * @return
	 */
	static public String[] getServiceShortClassNames() {
		return getServiceShortClassNames(null);
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	static public String[] getServiceShortClassNames(String filter) {
		return ServiceInfo.getInstance().getShortClassNames(filter);
	}

	/**
	 * 
	 * initially I thought that is would be a good idea to dynamically load
	 * Services and append their definitions to the class path. This would
	 * "theoretically" be done with ivy to get/download the appropriate
	 * dependent jars from the repo. Then use a custom ClassLoader to load the
	 * new service.
	 * 
	 * Ivy works for downloading the appropriate jars & artifacts However, the
	 * ClassLoader became very problematic
	 * 
	 * There is much mis-information around ClassLoaders. The most knowledgeable
	 * article I have found has been this one :
	 * http://blogs.oracle.com/sundararajan
	 * /entry/understanding_java_class_loading
	 * 
	 * Overall it became a huge PITA with really very little reward. The
	 * consequence is all Services' dependencies and categories are defined here
	 * rather than the appropriate Service class.
	 * 
	 * @return
	 */

	/**
	 * @param name
	 *            - name of Service to be removed and whos resources will be
	 *            released
	 */
	static public void releaseService(String name) {
		Runtime.release(name);
	}

	/**
	 * this "should" be the gateway function to a MyRobotLab instance going
	 * through this main will allow the see{@link}MyRobotLabClassLoader to load
	 * the appropriate classes and give access to the addURL to allow dynamic
	 * additions of new modules without having to restart.
	 * 
	 * TODO : -cmd <method> invokes the appropriate static method e.g. -cmd
	 * setLogLevel DEBUG
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		URL url = null;
		try {
			url = new URL("http://0.0.0.0:0");
		} catch (MalformedURLException e2) {
			Service.logException(e2);
		}

		System.out.println(url.getHost());
		System.out.println(url.getPort());

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		try {
			if (cmdline.containsKey("-logToConsole")) {
				addAppender(LogAppender.Console);
				setLogLevel(LogLevel.Debug);
			} else if (cmdline.containsKey("-logToRemote")) {
				String host = cmdline.getSafeArgument("-logToRemote", 0, "localhost");
				String port = cmdline.getSafeArgument("-logToRemote", 1, "4445");
				addAppender(LogAppender.Remote, host, port);
				setLogLevel(LogLevel.Debug);
			} else {
				addAppender(LogAppender.File);
				setLogLevel(LogLevel.Warn);
			}

			if (cmdline.containsKey("-logLevel")) {
				setLogLevel(LogLevel.tryParse(cmdline.getSafeArgument("-logLevel", 0, "DEBUG")));
			}

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND
			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work

			if (cmdline.containsKey("-update")) {
				// force all updates
				updateAll();
				return;
			} else {
				invokeCMDLine(cmdline);
			}
		} catch (Exception e) {
			Service.logException(e);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	static public ServiceInterface createAndStart(String name, String type) {
		ServiceInterface s = create(name, type);
		if (s == null) {
			log.error("cannot start service " + name);
			return null;
		}
		s.startService();
		return s;
	}

	/**
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	static public synchronized ServiceInterface create(String name, String type) {
		return create(name, "org.myrobotlab.service.", type);
	}

	/**
	 * @param name
	 *            - name of Service
	 * @param pkgName
	 *            - package of Service in case Services are created in different
	 *            packages
	 * @param type
	 *            - type of Service
	 * @return
	 */
	static public synchronized ServiceInterface create(String name, String pkgName, String type) {
		try {
			log.debug("Runtime.create - Class.forName");
			// get String Class
			String typeName = pkgName + type;
			// Class<?> cl = Class.forName(typeName);
			// Class<?> cl = Class.forName(typeName, false,
			// ClassLoader.getSystemClassLoader());
			return createService(name, typeName);
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	/**
	 * FIXME - deprecate - require calling code to implement loop - support only
	 * the single update(fullTypeName) - that way calling code can handle
	 * detailed info such as reporting to gui/console which components are being
	 * updated and which have errors in the update process. Will need a list of
	 * all or filtered ArrayList<fullTypeName>
	 * 
	 * update - force system to check for all dependencies of all possible
	 * Services - Ivy will attempt to check & fufill dependencies by downloading
	 * jars from the repo
	 */
	static public void updateAll() {

		boolean getNewRepoData = true;

		// TODO - have it return list of data objects "errors" so
		// events can be generated
		serviceInfo.clearErrors();

		// FIXME - not needed - set defaults [update all = true]
		if (getNewRepoData)
			serviceInfo.getRepoServiceData("serviceData.xml");

		if (!serviceInfo.hasErrors()) {
			serviceInfo.update();
			if (!serviceInfo.hasErrors()) {
				return;// true !
			}
		}

		List<String> errors = serviceInfo.getErrors();
		for (int i = 0; i < errors.size(); ++i) {
			log.error(errors.get(i));
		}
	}

	/**
	 * publishing point of Ivy sub system - sends even failedDependency when the
	 * retrieve report for a Service fails
	 * 
	 * @param dep
	 * @return
	 */
	public String failedDependency(String dep) {
		return dep;
	}

	/**
	 * @param name
	 * @param cls
	 * @return
	 */
	static public synchronized ServiceInterface createService(String name, String fullTypeName) {
		log.debug("Runtime.createService");
		if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) // ||
																										// !cls.isInstance(Service.class))
																										// \
		{
			log.error(fullTypeName + " not a type or " + name + " not defined ");
			return null;
		}

		ServiceWrapper sw = Runtime.getService(name);
		if (sw != null) {
			log.debug("service " + name + " already exists");
			return sw.service;
		}

		try {

			// TODO - determine if there have been new classes added from ivy
			log.debug("ABOUT TO LOAD CLASS");

			log.info("loader for this class " + Runtime.class.getClassLoader().getClass().getCanonicalName());
			log.info("parent " + Runtime.class.getClassLoader().getParent().getClass().getCanonicalName());
			log.info("system class loader " + ClassLoader.getSystemClassLoader());
			log.info("parent should be null"
					+ ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
			log.info("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
			log.info("thread context parent "
					+ Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
			log.info("refreshing classloader");

			Class<?> cls = Class.forName(fullTypeName);
			Constructor<?> constructor = cls.getConstructor(new Class[] { String.class });

			// create an instance
			Object newService = constructor.newInstance(new Object[] { name });
			log.info("returning " + fullTypeName);
			return (Service) newService;
		} catch (Exception e) {
			Service.logException(e);
		}
		return null;
	}

	// ---------------- Runtime end --------------

	/**
	 * 
	 * @return
	 */
	public static String dump() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nhosts:\n");

		Iterator<URL> hkeys = hosts.keySet().iterator();
		while (hkeys.hasNext()) {
			URL url = hkeys.next();
			ServiceEnvironment se = hosts.get(url);
			sb.append("\t");
			sb.append(url);
			if ((se.accessURL != url) && (!url.equals(se.accessURL))) {
				sb.append(" key not equal to data ");
				sb.append(se.accessURL);
			}
			sb.append("\n");

			// Service Environment
			Iterator<String> it2 = se.serviceDirectory.keySet().iterator();
			while (it2.hasNext()) {
				String serviceName = it2.next();
				ServiceWrapper sw = se.serviceDirectory.get(serviceName);
				sb.append("\t\t");
				sb.append(serviceName);
				if ((sw.name != sw.name) && (!serviceName.equals(sw.name))) {
					sb.append(" key not equal to data ");
					sb.append(sw.name);
				}

				if ((sw.host.accessURL != url) && (!sw.host.accessURL.equals(url))) {
					sb.append(" service wrapper host accessURL " + sw.host.accessURL + " not equal to " + url);
				}
				sb.append("\n");
			}
		}

		sb.append("\nregistry:");

		Iterator<String> rkeys = registry.keySet().iterator();
		while (rkeys.hasNext()) {
			String serviceName = rkeys.next();
			ServiceWrapper sw = registry.get(serviceName);
			sb.append("\n");
			sb.append(serviceName);
			sb.append(" ");
			sb.append(sw.host.accessURL);
		}

		return sb.toString();
	}

	/**
	 * this method attempts to connect to the repo and populate information
	 * regarding the latest ServiceDescriptors and their latest dependencies
	 */
	public static void checkForUpdates() {

		// serviceInfo.getRepoServiceData();
		// get local data
		serviceInfo.getLocalServiceData();

		// get remote data
		serviceInfo.getRepoData();

		// notify ready for updates
		getInstance().invoke("proposedUpdates", serviceInfo);
	}

	/**
	 * this method is an event notifier that there were updates found
	 */
	public ServiceInfo proposedUpdates(ServiceInfo si) {
		return si;
	}

	/**
	 * 
	 */
	public static void installLatestAll() {
		// FIXME - implement & updateAll which updates all "installed"
		// serviceInfo.getRepoServiceData();

	}

	/**
	 * 
	 * @param fullTypeName
	 */
	public void update(String fullTypeName) {
		ServiceInfo.getInstance().getLocalServiceData();

		ServiceInfo.getInstance().resolve(fullTypeName);

	}

	/**
	 * published events
	 * 
	 * @param className
	 * @return
	 */
	public String resolveBegin(String className) {
		return className;
	}

	/**
	 * 
	 * @param errors
	 * @return
	 */
	public List<String> resolveError(List<String> errors) {
		return errors;
	}

	/**
	 * 
	 * @param className
	 * @return
	 */
	public String resolveSuccess(String className) {
		return className;
	}

	/**
	 * 
	 */
	public void resolveEnd() {
	}

}
