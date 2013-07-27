package org.myrobotlab.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.MethodEntry;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEnvironment;
import org.myrobotlab.framework.ServiceInfo;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.net.HTTPRequest;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

/**
 * 
 * Runtime is responsible for the creation and removal of all Services and the
 * associated static registries It maintains state information regarding
 * possible & running local Services It maintains state information regarding
 * foreign Runtimes It is a singleton and should be the only service of Runtime
 * running in a process The host and registry maps are used in routing
 * communication to the asppropriate service (be it local or remote) It will be
 * the first Service created It also wraps the real JVM Runtime object
 * 
 */
@Root
public class Runtime extends Service {
	// TODO this should be something a little more unique - tied to version?
	private static final long serialVersionUID = 1L;

	// ---- rte members begin ----------------------------
	static private HashMap<URI, ServiceEnvironment> hosts = new HashMap<URI, ServiceEnvironment>();;
	static private HashMap<String, ServiceWrapper> registry = new HashMap<String, ServiceWrapper>();

	// FIXME - this should be a GUI thing only ! or getPrettyMethods or static
	// filterMethods
	static private HashMap<String, String> hideMethods = new HashMap<String, String>();

	private static boolean needsRestart = false;

	private static String runtimeName;
	
	private Date startDate = new Date();

	private final static String helpString = "java -Djava.library.path=./libraries/native/x86.32.windows org.myrobotlab.service.Runtime -service gui GUIService -logLevel INFO -logToConsole";

	// ---- rte members end ------------------------------

	private static long uniqueID = new Random(System.currentTimeMillis()).nextLong();
	
	// ---- Runtime members begin -----------------
	public final ServiceInfo serviceInfo = new ServiceInfo();

	/*
	 * @Element public String proxyHost;
	 * 
	 * @Element public String proxyPort;
	 * 
	 * @Element public String proxyUserName;
	 * 
	 * @Element public String proxyPassword;
	 */

	static ServiceInterface gui = null;
	// ---- Runtime members end -----------------

	public final static Logger log = LoggerFactory.getLogger(Runtime.class);

	/**
	 * Object used to synchronize initializing this singleton.
	 */
	private static final Object instanceLockObject = new Object();
	/**
	 * The singleton of this class.
	 */
	private static Runtime localInstance = null;
	
	
	// auto update begin ------
	//@Element
	//private static boolean isAutoUpdateEnabled = false;
	 // default eveery 5 minutes 
	private static int autoUpdateCheckIntervalSeconds = 300;

	public static void startAutoUpdate(int seconds) {
		Runtime runtime = Runtime.getInstance();
		//isAutoUpdateEnabled = true;
		autoUpdateCheckIntervalSeconds = seconds;
		// only runtime can auto-update
		runtime.timer.schedule(new AutoUpdate(), autoUpdateCheckIntervalSeconds * 1000); 
	}
	
	public static String getVersion()
	{
		return FileIO.getResourceFile("version.txt");
	}
	
	public String getUptime()
	{
		Date now = new Date();
		long diff = now.getTime() - startDate.getTime();
		 
		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);

		StringBuffer sb = new StringBuffer();
		sb.append(diffDays).append(" days ").append(diffHours).append(" hours ").append(diffMinutes).append(" minutes ").append(diffSeconds).append(" seconds ");
		return sb.toString();
	}
	
	public static void stopAutoUpdate()
	{
		Runtime runtime = Runtime.getInstance();
		runtime.timer.cancel();
		runtime.timer.purge();
	}

	static class AutoUpdate extends TimerTask {

		@Override
		public void run() {
			Runtime runtime = Runtime.getInstance();
			runtime.info("starting auto-update check");
			
			String newVersion = Runtime.getBleedingEdgeVersionString();
			String currentVersion = FileIO.getResourceFile("version.txt");
			log.info(String.format("comparing new version %s with current version %s", newVersion, currentVersion));
			if (newVersion == null)
			{
				runtime.info("newVersion == null - nothing available");
			} else if (currentVersion.compareTo(newVersion) >= 0) {
				log.info("no updates");
				runtime.info("no updates available");
			} else {
				runtime.info(String.format("updating with %s", newVersion));
				// Custom button text
				runtime.timer.schedule(new AutoUpdate(), autoUpdateCheckIntervalSeconds * 1000); 
				Runtime.getBleedingEdgeMyRobotLabJar();
				Runtime.restart("moveUpdate");

			}
			
			log.info("re-setting timer begin");
			runtime.timer.schedule(new AutoUpdate(), autoUpdateCheckIntervalSeconds * 1000); 
			log.info("re-setting timer end");
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param n
	 */
	public Runtime(String n) {
		super(n, Runtime.class.getCanonicalName());

		synchronized (instanceLockObject) {
			localInstance = this;
		}

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

	Platform getLocalPlatform() {
		return getPlatform(null);
	}

	Platform getPlatform(URI uri) {
		ServiceEnvironment local = hosts.get(uri);
		if (local != null) {
			return local.platform;
		}

		error("can't get local platform in service environment");

		return null;
	}

	String getLocalVersion() {
		return getVersion(null);
	}

	String getVersion(URI uri) {
		ServiceEnvironment local = hosts.get(uri);
		if (local != null) {
			return local.version;
		}

		error("can't get local version in service environment");

		return null;
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
	 * 
	 * @return
	 */
	public static Runtime getInstance() {
		if (localInstance == null) {
			synchronized (instanceLockObject) {
				if (localInstance == null) {
					// TODO should this be configurable?
					if (runtimeName == null) {
						// runtimeName = String.format("MRL%1$d", new
						// Random().nextInt(99999));
						runtimeName = "runtime";
					}
					localInstance = new Runtime(runtimeName);
				}
			}
		}
		return localInstance;
	}

	/**
	 * Stops all service-related running items. This releases the singleton
	 * referenced by this class, but it does not guarantee that the old service
	 * will be GC'd. FYI - if stopServices does not remove INSTANCE - it is not
	 * re-entrant in junit tests
	 */
	@Override
	public void stopService() {
		if (timer != null)
		{
			timer.cancel(); // stop all scheduled jobs
			timer.purge();
		}
		super.stopService();
		localInstance = null;
	}

	/**
	 * Get the tool tip for this class.
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

	/*
	 * BLOCKS ON BAD READ or Process termination static public void
	 * createProcess(String[] cmdline) throws IOException { Process process =
	 * new ProcessBuilder(cmdline).start(); InputStream is =
	 * process.getInputStream(); InputStreamReader isr = new
	 * InputStreamReader(is); BufferedReader br = new BufferedReader(isr);
	 * String line;
	 * 
	 * System.out.printf("Output of running %s is:", Arrays.toString(cmdline));
	 * 
	 * while ((line = br.readLine()) != null) { System.out.println(line); } }
	 */

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

	// Reference - cpu utilization http://www.javaworld.com/javaworld/javaqa/2002-11/01-qa-1108-cpu.html
	
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

	// public static synchronized boolean register(URI url, Service s)
	// TODO more aptly named registerLocal(Service s) ?
	// FIXME - getState publish setState need to reconcile with
	// these definitions
	/**
	 * ONLY CALLED BY registerServices2 ... would be a bug if called from
	 * foreign service - (no platform! - unless ServiceEnvironment already
	 * exists)
	 * 
	 * 
	 * @param url
	 * @param s
	 * @return
	 */
	public final static synchronized Service register(Service s, URI url) {
		ServiceEnvironment se = null;
		if (!hosts.containsKey(url)) {
			se = new ServiceEnvironment(url);
			hosts.put(url, se);
		} else {
			se = hosts.get(url);
		}

		if (se.serviceDirectory.containsKey(s.getName())) {
			log.error(String.format("attempting to register %1$s which is already registered in %2$s", s.getName(), url));
			if (localInstance != null) {
				localInstance.invoke("collision", s.getName());
				Runtime.getInstance().error(String.format(" name collision with %s", s.getName()));
			}
			return s;
		}
		ServiceWrapper sw = new ServiceWrapper(s, se.accessURL);
		se.serviceDirectory.put(s.getName(), sw);
		registry.put(s.getName(), sw);
		if (localInstance != null) {
			localInstance.invoke("registered", sw);
		}

		return s;
	}

	/**
	 * the bare bones needed to register - just a name - if that's the case a
	 * MessagingListingService is created. This would be created by a Java
	 * application which was not really a service
	 * 
	 * @param name
	 */
	/*
	 * deprecated - use Proxy service public static synchronized void
	 * register(String name) { MessageListenerService mls = new
	 * MessageListenerService(name); ServiceWrapper sw = new
	 * ServiceWrapper(mls);
	 * 
	 * // FIXME // can't this be static // seems a kudge here...
	 * Runtime.getInstance().register(sw); }
	 */

	/**
	 * called by remote/foreign systems to register a new service through a
	 * subscription
	 * 
	 * @param sw
	 */
	public synchronized void register(ServiceWrapper sw) {
		log.debug(String.format("register(ServiceWrapper %s)", sw.name));

		if (registry.containsKey(sw.name)) {
			log.warn("{} already defined - will not re-register", sw.name);
			return;
		}

		ServiceEnvironment se = hosts.get(sw.getAccessURL());
		if (se == null) {
			error(String.format("no service environment for %s", sw.getAccessURL()));
			return;
		}

		if (se.serviceDirectory.containsKey(sw.name)) {
			log.info("{} already registered", sw.name);
			return;
		}
		// FIXME - does the refrence of this service wrapper need to point back
		// to the
		// service environment its referencing?
		// sw.host = se; - can't do this because its final

		se.serviceDirectory.put(sw.name, sw);
		registry.put(sw.name, sw);
		if (localInstance != null) {
			localInstance.invoke("registered", sw);
		}

	}

	/**
	 * registers an initial ServiceEnvironment which is a complete set of
	 * Services from a foreign instance of MRL. It returns whether changes have
	 * been made. This is necessary to determine if the register should be
	 * echoed back.
	 * 
	 * @param uri
	 * @param s
	 * @return false if it already matches
	 */
	public static synchronized boolean register(ServiceEnvironment s) {
		URI uri = s.accessURL;
		if (!hosts.containsKey(uri)) {
			log.info(String.format("adding new ServiceEnvironment {}", uri));
		} else {
			// FIXME ? - replace regardless ???
			if (areEqual(s)) {
				log.info(String.format("ServiceEnvironment {} already exists - with same count and names", uri));
				return false;
			}
			log.info(String.format("replacing ServiceEnvironment {}", uri.toString()));
		}

		hosts.put(uri, s);

		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			log.info(String.format("adding %s to registry", serviceName));
			registry.put(serviceName, s.serviceDirectory.get(serviceName));
			localInstance.invoke("registered", s.serviceDirectory.get(serviceName));

			ServiceWrapper sw = getServiceWrapper(serviceName);
			// recently added to support new registry data model
			// and break cyclic reference
			if (uri != null)
			{
				sw.host = uri;
			}
			if ("org.myrobotlab.service.Runtime".equals(sw.getServiceType())) {
				log.info(String.format("found runtime %s", serviceName));
				localInstance.subscribe("registered", serviceName, "register", ServiceWrapper.class);
			}

		}

		return true;
	}

	/**
	 * Checks if s has the same service directory content as the environment at
	 * url.
	 * 
	 * @param s
	 * @param url
	 * @return
	 */
	private static boolean areEqual(ServiceEnvironment s) {
		URI url = s.accessURL;
		ServiceEnvironment se = hosts.get(url);
		if (se.serviceDirectory.size() != s.serviceDirectory.size()) {
			return false;
		}

		s.serviceDirectory.keySet().iterator();
		Iterator<String> it = s.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			if (!se.serviceDirectory.containsKey(serviceName)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * FIXME - possibly needed when the intent is to remove the registration of
	 * a foreign Service
	 */
	public static void unregister(URI url, String name) {
		if (!registry.containsKey(name)) {
			log.error(String.format("unregister %1$s does not exist in registry", name));
		} else {
			registry.remove(name);
		}

		if (!hosts.containsKey(url)) {
			log.error(String.format("unregister environment does note exist for %1$s.%2$s", url, name));
			return;
		}

		ServiceEnvironment se = hosts.get(url);

		if (!se.serviceDirectory.containsKey(name)) {
			log.error(String.format("unregister %2$s does note exist for %1$s.%2$s", url, name));
			return;
		}
		localInstance.invoke("released", se.serviceDirectory.get(name));
		se.serviceDirectory.remove(name);
	}

	/**
	 * unregister a service environment
	 * 
	 * @param url
	 */
	public static void unregisterAll(URI url) {
		if (!hosts.containsKey(url)) {
			log.error(String.format("unregisterAll %1$s does not exist", url));
			return;
		}

		ServiceEnvironment se = hosts.get(url);

		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			unregister(url, serviceName);
		}
	}

	/**
	 * unregister everything
	 */
	public static void unregisterAll() {
		Iterator<URI> it = hosts.keySet().iterator();
		while (it.hasNext()) {
			unregisterAll(it.next());
		}
	}

	/**
	 * 
	 * @return
	 */
	public int getServiceCount() {
		int cnt = 0;
		Iterator<URI> it = hosts.keySet().iterator();
		ServiceEnvironment se;
		Iterator<String> it2;
		while (it.hasNext()) {
			se = hosts.get(it.next());
			it2 = se.serviceDirectory.keySet().iterator();
			while (it2.hasNext()) {
				++cnt;
				it2.next();
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

		// URI is null but the "acceptor" will fill in the correct URI/ID
		ServiceEnvironment export = new ServiceEnvironment(null);

		Iterator<String> it = local.serviceDirectory.keySet().iterator();
		String name;
		ServiceWrapper sw;
		ServiceWrapper sw2;
		ServiceInterface si;
		while (it.hasNext()) {
			si = null;
			name = it.next();
			sw = local.serviceDirectory.get(name);
			if (sw.get().allowExport()) {
				log.info(String.format("exporting service: %s of type %s", name, sw.getServiceType()));
				// create new structure - otherwise it won't be correctly
				// filtered
				si = sw.get();
			} else {
				log.info(String.format("%s will not be exported", name));
				continue;
			}
			sw2 = new ServiceWrapper(name, si, export.accessURL);
			export.serviceDirectory.put(name, sw2);
			// TODO - add exclusive list & name vs type exclusions
		}

		return export;
	}

	public static boolean isLocal(String serviceName) {
		ServiceWrapper sw = getServiceWrapper(serviceName);
		if (sw == null) {
			log.error(String.format("%s not defined - can't determine if its local"));
			return false;
		}

		return sw.isLocal();
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static ServiceWrapper getServiceWrapper(String name) {

		if (!registry.containsKey(name)) {
			log.debug(String.format("service %1$s does not exist", name));
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
		log.warn("releasing service {}", name);
		ServiceWrapper sw = getServiceWrapper(name);
		if (sw == null || !sw.isValid()) {
			log.error("no service wrapper for " + name);
			return false;
		}
		URI url = sw.getAccessURL();
		if (url == null) {
			sw.get().stopService();// if its a local Service shut it
									// down
		}
		ServiceEnvironment se = hosts.get(url);
		localInstance.invoke("released", se.serviceDirectory.get(name));
		registry.remove(name);
		se.serviceDirectory.remove(name);
		if (se.serviceDirectory.size() == 0) {
			log.info("service directory empty - removing host");
			hosts.remove(se); // TODO - invoke message
		}
		log.warn("released{}", name);
		return true;
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	public static boolean release(URI url) /* release process environment */
	{
		boolean ret = true;
		ServiceEnvironment se = hosts.get(url);
		if (se == null) {
			log.warn("attempt to release {} not successful - it does not exist", url);
			return false;
		}
		log.info(String.format("releasing url %1$s", url));
		String[] services = (String[]) se.serviceDirectory.keySet().toArray(new String[se.serviceDirectory.keySet().size()]);
		String runtimeName = null;
		ServiceInterface service;
		for (int i = 0; i < services.length; ++i) {
			service = registry.get(services[i]).get();
			if (service != null && "Runtime".equals(service.getSimpleName())) {
				runtimeName = service.getName();
				log.info(String.format("delaying release of Runtime %1$s", runtimeName));
				continue;
			}
			ret &= release(services[i]);
		}

		if (runtimeName != null) {
			ret &= release(runtimeName);
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
	 * 
	 * FIXME - send SHUTDOWN event to all running services with a timeout period
	 * - end with System.exit()
	 */
	public static void releaseAll() /* local only? YES !!! LOCAL ONLY !! */
	{
		log.debug("releaseAll");

		// FIXME - release all by calling sub methods & normalize code

		// FIXME - this is a bit of a lie
		// broadcasting the info all services are released before releasing them
		// but you can't send the info if everything has been released :P

		ServiceEnvironment se = hosts.get(null); // local services only
		if (se == null) {
			log.info("releaseAll called when everything is released, all done here");
			return;
		}
		Iterator<String> seit = se.serviceDirectory.keySet().iterator();
		String serviceName;
		ServiceWrapper sw;
		while (seit.hasNext()) {
			serviceName = seit.next();
			sw = se.serviceDirectory.get(serviceName);
			localInstance.invoke("released", se.serviceDirectory.get(serviceName));
		}

		seit = se.serviceDirectory.keySet().iterator();
		while (seit.hasNext()) {
			serviceName = seit.next();
			sw = se.serviceDirectory.get(serviceName);
			log.info(String.format("stopping service %1$s/%2$s", se.accessURL, serviceName));

			if (sw.service == null) {
				log.warn("unknown type and/or remote service");
				continue;
			}
			sw.service.stopService();
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
	public static ServiceEnvironment getServiceEnvironment(URI url) {
		if (hosts.containsKey(url)) {
			return hosts.get(url); // FIXME should return copy
		}
		return null;
	}

	/**
	 * 
	 * @return
	 */
	public static HashMap<URI, ServiceEnvironment> getServiceEnvironments() {
		return new HashMap<URI, ServiceEnvironment>(hosts);
	}

	/**
	 * 
	 * @param serviceName
	 * @return
	 */
	public static HashMap<String, MethodEntry> getMethodMap(String serviceName) {
		if (!registry.containsKey(serviceName)) {
			log.error(String.format("%1$s not in registry - can not return method map", serviceName));
			return null;
		}

		HashMap<String, MethodEntry> ret = new HashMap<String, MethodEntry>();
		ServiceWrapper sw = registry.get(serviceName);

		Class<?> c = sw.service.getClass();
		Method[] methods = c.getDeclaredMethods();

		Method m;
		MethodEntry me;
		String s;
		for (int i = 0; i < methods.length; ++i) {
			m = methods[i];

			if (hideMethods.containsKey(m.getName())) {
				continue;
			}
			me = new MethodEntry();
			me.name = m.getName();
			me.parameterTypes = m.getParameterTypes();
			me.returnType = m.getReturnType();
			s = MethodEntry.getSignature(me.name, me.parameterTypes, me.returnType);
			ret.put(s, me);
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

			out.writeObject(hosts);

			out.writeObject(registry);
			out.writeObject(hideMethods);
			out.flush();
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		} finally {
			if (out != null) {
				try {
					// todo - should we flush first?
					out.close();
				} catch (Exception e) {
				}
			}
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
		ObjectInputStream in = null;
		try {
			FileInputStream fis;
			fis = new FileInputStream(filename);
			in = new ObjectInputStream(fis);
			// instance = (RuntimeEnvironment)in.readObject();
			hosts = (HashMap<URI, ServiceEnvironment>) in.readObject();
			registry = (HashMap<String, ServiceWrapper>) in.readObject();
			hideMethods = (HashMap<String, String>) in.readObject();
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		} finally {
			if (in != null) {
				try {
					// TODO do we need to flush first?
					in.close();
				} catch (Exception e) {
				}
			}
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
		String serviceName;
		ServiceWrapper sw;
		while (it.hasNext()) {
			serviceName = it.next();
			sw = se.serviceDirectory.get(serviceName);
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
		Iterator<String> it = registry.keySet().iterator();
		String serviceName;
		ServiceWrapper sw;
		String n;
		ArrayList<MRLListener> nes;
		MRLListener listener;

		StringBuffer sb = new StringBuffer().append("<NotifyEntries>");
		while (it.hasNext()) {
			serviceName = it.next();
			sw = registry.get(serviceName);
			sb.append("<service name=\"").append(sw.getName()).append("\" serviceEnironment=\"").append(sw.getAccessURL()).append("\">");
			ArrayList<String> nlks = sw.getNotifyListKeySet();
			if (nlks != null) {
				Iterator<String> nit = nlks.iterator();

				while (nit.hasNext()) {
					n = nit.next();
					sb.append("<addListener map=\"").append(n).append("\">");
					nes = sw.getNotifyList(n);
					for (int i = 0; i < nes.size(); ++i) {
						listener = nes.get(i);
						sb.append("<MRLListener outMethod=\"").append(listener.outMethod).append("\" name=\"").append(listener.name).append("\" inMethod=\"")
								.append(listener.outMethod).append("\" />");
					}
					sb.append("</addListener>");
				}
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
		String serviceName;
		ServiceWrapper sw;
		Class<?> c;
		Class<?>[] interfaces;
		Class<?> m;
		while (it.hasNext()) {
			serviceName = it.next();
			sw = registry.get(serviceName);
			c = sw.service.getClass();
			interfaces = c.getInterfaces();
			for (int i = 0; i < interfaces.length; ++i) {
				m = interfaces[i];

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
	 * @param filename
	 *            - the name of the Service which was successfully registered
	 * @return
	 */
	public ServiceWrapper registered(ServiceWrapper sw) {
		return sw;
	}

	/**
	 * release event
	 * 
	 * @param filename
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
		System.out.println(String.format("Runtime %s", FileIO.getResourceFile("version.txt")));
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
	 * @param cmdline
	 */
	public final static void createServices(CMDLine cmdline) {

		if (cmdline.containsKey("-h") || cmdline.containsKey("--help")) {
			help();
			return;
		}

		if (cmdline.containsKey("-v") || cmdline.containsKey("--version")) {
			System.out.print(FileIO.getResourceFile("version.txt"));
			return;
		}

		System.out.println(String.format("createServices service count %1$d", cmdline.getArgumentCount("-service") / 2));

		if (cmdline.getArgumentCount("-service") > 0 && cmdline.getArgumentCount("-service") % 2 == 0) {

			for (int i = 0; i < cmdline.getArgumentCount("-service"); i += 2) {

				log.info(String.format("attempting to invoke : org.myrobotlab.service.%1$s named %2$s", cmdline.getSafeArgument("-service", i + 1, ""),
						cmdline.getSafeArgument("-service", i, "")));

				String name = cmdline.getSafeArgument("-service", i, "");
				String type = cmdline.getSafeArgument("-service", i + 1, "");
				ServiceInterface s = Runtime.create(name, type);

				if (s != null) {
					s.startService();
				} else {
					log.error(String.format("could not create service %1$s %2$s", name, type));
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
			return;
		} /*
		 * LIST ???
		 * 
		 * else if (cmdline.hasSwitch("-list")) { Runtime runtime =
		 * Runtime.getInstance(); if (runtime == null) {
		 * 
		 * } else { System.out.println(getServiceShortClassNames()); } return; }
		 */
		help();
	}

	/**
	 * 
	 * @return
	 */
	public String[] getServiceShortClassNames() {
		return getServiceShortClassNames("all");
	}

	/**
	 * 
	 * @param filter
	 * @return
	 */
	public String[] getServiceShortClassNames(String filter) {
		return serviceInfo.getShortClassNames(filter);
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

		CMDLine cmdline = new CMDLine();
		cmdline.splitLine(args);

		
		Logging logging = LoggingFactory.getInstance();

		try {

			if (cmdline.containsKey("-runtimeName")) {
				runtimeName = cmdline.getSafeArgument("-runtimeName", 0, "MRL");
			}

			if (cmdline.containsKey("-logToConsole")) {
				logging.addAppender(Appender.CONSOLE);
			} else if (cmdline.containsKey("-logToRemote")) {
				String host = cmdline.getSafeArgument("-logToRemote", 0, "localhost");
				String port = cmdline.getSafeArgument("-logToRemote", 1, "4445");
				logging.addAppender(Appender.REMOTE, host, port);
			} else {
				if (cmdline.containsKey("-multiLog"))
				{
					logging.addAppender(Appender.FILE, "multiLog", null);
				} else {
					logging.addAppender(Appender.FILE);
				}
			}
			
			if (cmdline.containsKey("-autoUpdate")) {
				startAutoUpdate(autoUpdateCheckIntervalSeconds);
			}

			logging.setLevel(cmdline.getSafeArgument("-logLevel", 0, "INFO"));

			log.info(cmdline.toString());
			

			// LINUX LD_LIBRARY_PATH MUST BE EXPORTED - NO OTHER SOLUTION FOUND
			// hack to reconcile the different ways os handle and expect
			// "PATH & LD_LIBRARY_PATH" to be handled
			// found here -
			// http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
			// but does not work

			if (cmdline.containsKey("-update")) {
				// force all updates
				ArrayList<String> services = cmdline.getArgumentList("-update");
				
				if (services.size() == 0){
					Runtime runtime = Runtime.getInstance();
					if (runtime != null) {
						runtime.updateAll();
					} else {
						log.error("runtime is null");
					}
					return;
				} else {
					// TODO - do specific service install
					log.error("not implemented yet");
				}
			} else {
				createServices(cmdline);
			}

			invokeCommands(cmdline);

			// outbound - auto-connect
			if (cmdline.containsKey("-connect")) {
				String host = cmdline.getSafeArgument("-connect", 0, "localhost");
				String portStr = cmdline.getSafeArgument("-connect", 1, "6767");
				int port = Integer.parseInt(portStr);
				Runtime.getInstance().connect(null, null, host, port);
			}

		} catch (Exception e) {
			Logging.logException(e);
			System.out.print(Logging.stackToString(e));
			Service.sleep(2000);
		}
	}

	static public void invokeCommands(CMDLine cmdline) {
		int argCount = cmdline.getArgumentCount("-invoke");
		if (argCount > 1) {

			StringBuffer params = new StringBuffer();

			ArrayList<String> invokeList = cmdline.getArgumentList("-invoke");
			Object[] data = new Object[argCount - 2];
			for (int i = 2; i < argCount; ++i) {
				data[i - 2] = invokeList.get(i);
				params.append(String.format("%s ", invokeList.get(i)));
			}

			String name = cmdline.getArgument("-invoke", 0);
			String method = cmdline.getArgument("-invoke", 1);

			log.info(String.format("attempting to invoke : %s.%s(%s)\n", name, method, params.toString()));
			getInstance().send(name, method, data);

		}

	}

	static public ServiceInterface createAndStart(String name, String type) {
		return createAndStart(name, type, null);
	}

	/**
	 * creates and starts and returns reference to service
	 * if a "real" service has been passed in, it will start it and return 
	 * its reference, a reference to a "real" service wins over a name
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
	static public ServiceInterface createAndStart(String name, String type, ServiceInterface in) {
		ServiceInterface s = null;
		if (in != null) {
			s = in;
		} else {
			s = create(name, type);
		}

		if (s == null) {
			log.error("cannot start service " + name);
			return null;
		}
		s.startService();
		// new assumption - if you have a display - you probably want to display
		// it
		// also allows complete dynamic loading of GUIService without any
		// GUIService
		// references - so the android project does not whine...
		s.display();
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
			Logging.logException(e);
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
	public void updateAll() {

		boolean getNewRepoData = true;

		// TODO - have it return list of data objects "errors" so
		// events can be generated
		serviceInfo.clearErrors();

		// FIXME - not needed - set defaults [update all = true]
		if (getNewRepoData) {
			serviceInfo.getRepoFile("serviceData.xml");
		}
		if (!serviceInfo.hasErrors()) {
			serviceInfo.update();
			Runtime.getBleedingEdgeMyRobotLabJar();
			Runtime.restart("moveUpdate");
		}

		List<String> errors = serviceInfo.getErrors();
		for (int i = 0; i < errors.size(); ++i) {
			error(errors.get(i));
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
		if (name == null || name.length() == 0 || fullTypeName == null || fullTypeName.length() == 0) {
			log.error(String.format("%1$s not a type or %2$s not defined ", fullTypeName, name));
			return null;
		}

		ServiceWrapper sw = Runtime.getServiceWrapper(name);
		if (sw != null) {
			log.debug(String.format("service %1$s already exists", name));
			return sw.service;
		}

		try {

			// TODO - determine if there have been new classes added from ivy
			log.debug("ABOUT TO LOAD CLASS");
			// TODO reduce the amount of log calls and put them in one log
			// statement
			log.info("loader for this class " + Runtime.class.getClassLoader().getClass().getCanonicalName());
			log.info("parent " + Runtime.class.getClassLoader().getParent().getClass().getCanonicalName());
			log.info("system class loader " + ClassLoader.getSystemClassLoader());
			log.info("parent should be null" + ClassLoader.getSystemClassLoader().getParent().getClass().getCanonicalName());
			log.info("thread context " + Thread.currentThread().getContextClassLoader().getClass().getCanonicalName());
			log.info("thread context parent " + Thread.currentThread().getContextClassLoader().getParent().getClass().getCanonicalName());
			log.info("refreshing classloader");
			
			Runtime runtime = Runtime.getInstance();
			if (!runtime.isInstalled(fullTypeName))
			{
				runtime.error("%s is not installed - please install it", fullTypeName);
				//return null;
			}

			Class<?> cls = Class.forName(fullTypeName);
			Constructor<?> constructor = cls.getConstructor(new Class[] { String.class });

			// create an instance
			Object newService = constructor.newInstance(new Object[] { name });
			log.info("returning " + fullTypeName);
			return (Service) newService;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	// ---------------- Runtime end --------------

	/**
	 * 
	 * @return
	 */
	public static String dump() {
		StringBuffer sb = new StringBuffer().append("\nhosts:\n");

		Iterator<URI> hkeys = hosts.keySet().iterator();
		URI url;
		ServiceEnvironment se;
		Iterator<String> it2;
		String serviceName;
		ServiceWrapper sw;
		while (hkeys.hasNext()) {
			url = hkeys.next();
			se = hosts.get(url);
			sb.append("\t").append(url);
			if ((se.accessURL != url) && (!url.equals(se.accessURL))) {
				sb.append(" key not equal to data ").append(se.accessURL);
			}
			sb.append("\n");

			// Service Environment
			it2 = se.serviceDirectory.keySet().iterator();
			while (it2.hasNext()) {
				serviceName = it2.next();
				sw = se.serviceDirectory.get(serviceName);
				sb.append("\t\t").append(serviceName);
				if ((sw.name != sw.name) && (!serviceName.equals(sw.name))) {
					sb.append(" key not equal to data ").append(sw.name);
				}

				if ((sw.host != url) && (!sw.host.equals(url))) {
					sb.append(" service wrapper host accessURL ").append(sw.host).append(" not equal to ").append(url);
				}
				sb.append("\n");
			}
		}

		sb.append("\nregistry:");

		Iterator<String> rkeys = registry.keySet().iterator();
		while (rkeys.hasNext()) {
			serviceName = rkeys.next();
			sw = registry.get(serviceName);
			sb.append("\n").append(serviceName).append(" ").append(sw.host);
		}

		return sb.toString();
	}

	/**
	 * this method attempts to connect to the repo and populate information
	 * regarding the latest ServiceDescriptors and their latest dependencies
	 */
	public void checkForUpdates() {

		// assume your dealing with the local system
		Runtime runtime = getInstance();

		// serviceInfo.getRepoServiceData();
		// get local data
		runtime.serviceInfo.getLocalServiceData();

		// get remote data
		runtime.serviceInfo.getRepoData();

		// addListener ready for updates
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
	 * @param fullTypeName
	 */
	public void update(String fullTypeName) {
		serviceInfo.getLocalServiceData();
		serviceInfo.resolve(fullTypeName);
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
	 * event fired when a new artifact is download
	 * 
	 * @param module
	 * @return
	 */
	public String newArtifactsDownloaded(String module) {
		return module;
	}

	/**
	 * 
	 */
	public void resolveEnd() {
	}

	// FIXME - you don't need that many "typed" messages - resolve,
	// resolveError, ... etc
	// just use & parse "message"

	public static String message(String msg) {
		getInstance().invoke("publishMessage", msg);
		log.info(msg);
		return msg;
	}

	public String publishMessage(String msg) {
		return msg;
	}

	public static String getBleedingEdgeVersionString() {
		try {
			//
			// String listURL =
			// "http://code.google.com/p/myrobotlab/downloads/list?can=2&q=&sort=uploaded&colspec=Filename%20Summary%20Uploaded%20ReleaseDate%20Size%20DownloadCount";
			String listURL = "http://code.google.com/p/myrobotlab/downloads/list?can=1&q=&colspec=Filename+Summary+Uploaded+ReleaseDate+Size+DownloadCount";
			log.info(String.format("getting list of dist %s", listURL));
			HTTPRequest http;
			http = new HTTPRequest(listURL);
			String s = http.getString();
			log.info(String.format("recieved [%s]", s));
			log.info("parsing");
			String myrobotlabBleedingEdge = "myrobotlab.bleeding.edge.";
			int p0 = s.indexOf(myrobotlabBleedingEdge);
			if (p0 > 0) {
				p0 += myrobotlabBleedingEdge.length();
				int p1 = s.indexOf(".jar", p0);
				String intermediate = s.substring(p0, p1);
				log.info(intermediate);
				return intermediate.trim();
			} else {
				log.error(String.format("could not parse results for %s in getBleedingEdgeVersionString", listURL));
			}
		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	public static void getBleedingEdgeMyRobotLabJar() {

		try {
			log.info("getBleedingEdgeMyRobotLabJar");
			String version = getBleedingEdgeVersionString();
			String latestMRLJar = "http://myrobotlab.googlecode.com/files/myrobotlab.bleeding.edge." + version + ".jar";
			log.info(String.format("getting latest build from %s", latestMRLJar));
			HTTPRequest zip = new HTTPRequest(latestMRLJar);
			byte[] jarfile = zip.getBinary();

			File updateDir = new File("update");
			updateDir.mkdir();
			File backupDir = new File("backup");
			backupDir.mkdir();

			FileOutputStream out = new FileOutputStream("update/myrobotlab.jar");
			try {
				out.write(jarfile);
				log.info("getBleedingEdgeMyRobotLabJar - done - since there is an update you will probably want to run scripts/update.(sh)(bat) to replace the jar");
			} catch (Exception e) {
				Logging.logException(e);
			} finally {
				out.close();
			}

			//

		} catch (IOException e) {
			Logging.logException(e);
		}
	}

	// References : 
	// http://java.dzone.com/articles/programmatically-restart-java
	static public void restart(String restartScript) {
		log.info("new components - restart?");

		Runtime.releaseAll();
		try {
			if (restartScript == null) {
				if (Platform.isWindows()) {
					java.lang.Runtime.getRuntime().exec("cmd /c start myrobotlab.bat");
				} else {
					java.lang.Runtime.getRuntime().exec("./myrobotlab.sh");
				}
			} else {
				if (Platform.isWindows()) {
					java.lang.Runtime.getRuntime().exec(String.format("cmd /c start scripts\\%s.cmd", restartScript));
				} else {
					String command = String.format("./scripts/%s.sh", restartScript);
					File exe = new File(command); // FIXME - NORMALIZE !!!!!
					if (!exe.setExecutable(true)) {
						log.error(String.format("could not set %s to executable permissions", command));
					}
					java.lang.Runtime.getRuntime().exec(command);
				}
			}
		} catch (Exception ex) {
			Logging.logException(ex);
		}
		System.exit(0);

	}

	static public ServiceInterface getService(String name) {
		ServiceWrapper sw = getServiceWrapper(name);
		if (sw == null) {
			log.error(String.format("getService %s not found", name));
			return null;
		}
		return sw.get();
	}

	/**
	 * save all configuration from all local services
	 * 
	 * @return
	 */
	static public boolean saveAll() {
		boolean ret = true;
		ServiceEnvironment se = getLocalServices();
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			ret &= sw.service.save();
		}

		return ret;
	}

	/**
	 * load all configuration from all local services
	 * 
	 * @return
	 */
	static public boolean loadAll() {
		boolean ret = true;
		ServiceEnvironment se = getLocalServices();
		Iterator<String> it = se.serviceDirectory.keySet().iterator();
		String serviceName;
		while (it.hasNext()) {
			serviceName = it.next();
			ServiceWrapper sw = se.serviceDirectory.get(serviceName);
			ret &= sw.service.load();
		}

		return ret;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	// public static String newRMLInstanceCommandLineTemplate =
	// " -classpath \"./libraries/jar/*%1$s./libraries/jar/%2$s/*%1$s%3$s\" -Djava.library.path=\"./libraries/native/%2$s%1$s\" org.myrobotlab.service.Runtime -service %4$s -logToConsole";

	public static ArrayList<String> buildMLRCommandLine(HashMap<String, String> services, String runtimeName) {

		ArrayList<String> ret = new ArrayList<String>();

		// library path
		String platform = String.format("%s.%s.%s", Platform.getArch(), Platform.getBitness(), Platform.getOS());
		String libraryPath = String.format("-Djava.library.path=\"./libraries/native/%s\"", platform);
		ret.add(libraryPath);

		// class path
		String systemClassPath = System.getProperty("java.class.path");
		ret.add("-classpath");
		String classPath = String.format("./libraries/jar/*%1$s./libraries/jar/%2$s/*%1$s%3$s", Platform.getClassPathSeperator(), platform, systemClassPath);
		ret.add(classPath);

		ret.add("org.myrobotlab.service.Runtime");

		// ----- application level params --------------

		// services
		if (services.size() > 0) {
			ret.add("-service");
			for (Map.Entry<String, String> o : services.entrySet()) {
				ret.add(o.getKey());
				ret.add(o.getValue());
			}
		}

		// runtime name
		if (runtimeName != null) {
			ret.add("-runtimeName");
			ret.add(runtimeName);
		}

		// logLevel

		// logToConsole
		// ret.add("-logToConsole");

		return ret;
	}

	// TODO - load it up in gui & send it out
	public static void spawnRemoteMRL(String runtimeName) {
		HashMap<String, String> services = new HashMap<String, String>();
		services.put("remote", "RemoteAdapter");
		newMRLInstance(buildMLRCommandLine(services, runtimeName));
	}

	public static void newMRLInstance(ArrayList<String> args) {
		try {

			String separator = System.getProperty("file.separator");
			// String classpath = System.getProperty("java.class.path");
			String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
			// ProcessBuilder processBuilder = new ProcessBuilder(path, "-cp",
			// classpath, clazz.getCanonicalName());
			args.add(0, path);

			log.info(Arrays.toString(args.toArray()));
			ProcessBuilder processBuilder = new ProcessBuilder(args);
			processBuilder.directory(new File(System.getProperty("user.dir")));
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			// System.out.print(process.getOutputStream());

			// TODO - Read out dir output
			/*
			 * InputStream is = process.getInputStream(); InputStreamReader isr
			 * = new InputStreamReader(is); BufferedReader br = new
			 * BufferedReader(isr); String line;
			 * //System.out.printf("Output of running %s is:\n",
			 * Arrays.toString(buildMLRCommandLine())); while ((line =
			 * br.readLine()) != null) { System.out.println(line); }
			 */

			// Wait to get exit value
			/*
			 * try { int exitValue = process.waitFor();
			 * System.out.println("\n\nExit Value is " + exitValue); } catch
			 * (InterruptedException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 * 
			 * process.waitFor(); System.out.println("Fin");
			 */
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public static void startSecondJVM(Class<? extends Object> clazz, boolean redirectStream) throws Exception {
		System.out.println(clazz.getCanonicalName());
		String separator = System.getProperty("file.separator");
		String classpath = System.getProperty("java.class.path");
		String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
		ProcessBuilder processBuilder = new ProcessBuilder(path, "-cp", classpath, clazz.getCanonicalName());
		processBuilder.redirectErrorStream(redirectStream);
		Process process = processBuilder.start();
		process.waitFor();
		System.out.println("Fin");
	}

	/**
	 * Inbound registerServices is the method initially called to contact a
	 * remote process. Often is will be a GUI sending this request over the wire
	 * to be received in another process by a RemoteAdapter. if the registration
	 * of foreign Services is successful, the request is echoed back to the
	 * sender, such that all Services ready for export are shared.
	 * 
	 * it is also invoked by RemoteAdapters as they recieve the initial request
	 * the remote URI is filled in from the other end of the socket info Since
	 * the foreign Services do not necessarily know how they are identified the
	 * ServiceEnvironment & Service accessURL are filled in here
	 * 
	 * @param hostAddress
	 * @param port
	 * @param msg
	 * 
	 *            FIXME - put into Runtime to normalize and simplify
	 */
	public void registerServices(Message msg) {
		if (msg.data.length == 0 || !(msg.data[0] instanceof ServiceEnvironment)) {
			log.error("inbound registerServices not valid");
			return;
		}
		try {
			ServiceEnvironment mrlInstance = (ServiceEnvironment) msg.data[0];

			if (!Runtime.register(mrlInstance)) {
				return;
			}
			// if successfully registered with "new" Services - echo a
			// registration back
			ServiceEnvironment echoLocal = Runtime.getLocalServicesForExport();
			// send echo of local services back to sender
			send(msg.sender, "registerServices", echoLocal);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logException(e);
		}
	}

	
	/**
	 * unique id's are need for sendBlocking - to uniquely identify the message 
	 * this is a method to support that - it is unique within a process, but not
	 * accross processes
	 * @return a unique id
	 */
	public static synchronized long getUniqueID()
	{
		 ++uniqueID;
		 return uniqueID;
	}
	
	public boolean isInstalled(String fullTypeName)
	{
		return !getServiceInfo().hasUnfulfilledDependencies(fullTypeName);
	}
	
}
