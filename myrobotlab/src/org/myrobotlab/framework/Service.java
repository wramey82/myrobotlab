/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.SimpleTimeZone;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.net.CommunicationManager;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.data.NameValuePair;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * 
 * Service is the base of the MyRobotLab Service Oriented Architecture. All
 * meaningful Services derive from this class. There is a _TemplateService.java
 * in the org.myrobotlab.service package. This can be used as a very fast
 * template for creating new Services. Each Service begins with two threads One
 * is for the "Outbox" this delivers messages out of the Service. The other is
 * the "run" thread which processes all incoming messages.
 * 
 * @author GroG
 * 
 *         Dependencies: apache Log4J - logging simpleframework - basic
 *         configuration
 * 
 */
public abstract class Service implements Runnable, Serializable, ServiceInterface {

	// TODO - UNDERSTAND THAT host:port IS THE KEY !! - IT IS A COMBONATIONAL
	// KEY :0 WORKS IN PROCESS OUT OF
	// PROCESS ip:port WORKS !
	// host + ":" + servicePort + serviceClass + "/" +
	// this.getClass().getCanonicalName() + "/" + name;

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(Service.class.toString());
	protected String host = null; // TODO - should be final??? helpful in
									// testing??? TODO - put in
									// RuntimeEnvironment???
	@Element
	private final String name;
	public final String serviceClass; // TODO - remove
	private boolean isRunning = false;
	protected transient Thread thisThread = null;
	Outbox outbox = null;
	Inbox inbox = null;
	public URL url = null;

	public boolean performanceTiming = false;

	protected CommunicationInterface cm = null;
	/**
	 * @deprecated
	 */
	protected ConfigurationManager cfg = null;
	protected ConfigurationManager hostcfg = null;

	// performance timing
	public long startTimeMilliseconds = 0;

	// relay directives
	static public final String PROCESS = "PROCESS";
	static public final String RELAY = "RELAY";
	static public final String IGNORE = "IGNORE";
	static public final String BROADCAST = "BROADCAST";
	static public final String PROCESSANDBROADCAST = "PROCESSANDBROADCAST";

	// logging
	public final static String LOG_LEVEL_DEBUG = "DEBUG";
	public final static String LOG_LEVEL_INFO = "INFO";
	public final static String LOG_LEVEL_WARN = "WARN";
	public final static String LOG_LEVEL_ERROR = "ERROR";
	public final static String LOG_LEVEL_FATAL = "FATAL";
	public final static String LOGGING_APPENDER_NONE = "none";
	public final static String LOGGING_APPENDER_CONSOLE = "console";
	public final static String LOGGING_APPENDER_CONSOLE_GUI = "console gui";
	public final static String LOGGING_APPENDER_ROLLING_FILE = "file";
	public final static String LOGGING_APPENDER_SOCKET = "remote";

	public String anonymousMsgRequest = PROCESS;
	public String outboxMsgHandling = RELAY;
	protected static String cfgDir = System.getProperty("user.dir") + File.separator + ".myrobotlab";
	private static boolean hostInitialized = false;

	abstract public String getToolTip();

	/**
	 * framework interface for Services which can display themselves most will
	 * not implement this method. keeps the framework display type agnostic
	 */
	public void display() {
	}

	/**
	 * returns if the Service has a display - this would be any Service who had
	 * a display system GUIService (Swing) would be an example, most Services
	 * would return false keeps the framework display type agnostic
	 * 
	 * @return
	 */
	public boolean hasDisplay() {
		return false;
	}

	public Service(String instanceName, String serviceClass) {
		this(instanceName, serviceClass, null);
	}

	public String getName() {
		return name;
	}

	public Service(String instanceName, String serviceClass, String inHost) {

		// if I'm not a Runtime - then
		// start the Runtime
		if (!Runtime.isRuntime(this)) {
			Runtime.getInstance();
		}

		if (inHost != null) {
			try {
				url = new URL(inHost); // TODO - initialize once
										// RuntimeEnvironment
			} catch (MalformedURLException e) {
				log.error(inHost + " not a valid URL");
			}
		}
		// determine host name
		host = getHostName(inHost);// TODO - initialize once RuntimeEnvironment

		name = instanceName;
		this.serviceClass = serviceClass;
		this.inbox = new Inbox(name);
		this.outbox = new Outbox(this);

		hostcfg = new ConfigurationManager(host);
		cfg = new ConfigurationManager(host, name);

		// global defaults begin - multiple services will re-set defaults
		loadGlobalMachineDefaults(); // TODO - put in RuntimeEnvironments

		// service instance defaults
		loadServiceDefaultConfiguration();

		// TODO - if a gui is involved you may want to prompt

		// over-ride process level with host file
		if (!hostInitialized) {
			initialize();
		}

		// service instance level defaults
		loadDefaultConfiguration();

		// over-ride service level with service file
		cfg.load(host + "." + name + ".properties");

		// now that cfg is ready make a communication manager
		cm = new CommunicationManager(this);

		registerServices();// FIXME - deprecate - remove !
		registerLocalService(url);

	}

	public static void initialize() {
		// ;.\libraries\x86\32\windows
		System.setProperty("java.library.path",
				System.getProperty("java.library.path") + File.pathSeparator + "libraries" + File.separator + "native"
						+ File.separator + Platform.getArch() + "." + Platform.getBitness() + "." + Platform.getOS());

		String libararyPath = System.getProperty("java.library.path");
		String userDir = System.getProperty("user.dir");

		// cfgDir = userDir + File.separator + ".myrobotlab";

		// http://developer.android.com/reference/java/lang/System.html
		log.info("---------------normalize-------------------");
		log.info("os.name [" + System.getProperty("os.name") + "] getOS [" + Platform.getOS() + "]");
		log.info("os.arch [" + System.getProperty("os.arch") + "] getArch [" + Platform.getArch() + "]");
		log.info("getBitness [" + Platform.getBitness() + "]");
		log.info("java.vm.name [" + System.getProperty("java.vm.name") + "] getArch [" + Platform.getVMName() + "]");

		log.info("---------------non-normalize---------------");
		log.info("java.vm.name [" + System.getProperty("java.vm.name") + "]");
		log.info("java.vm.vendor [" + System.getProperty("java.vm.vendor") + "]");
		log.info("java.home [" + System.getProperty("java.home") + "]");
		log.info("os.version [" + System.getProperty("os.version") + "]");
		log.info("java.class.path [" + System.getProperty("java.class.path") + "]");
		log.info("java.library.path [" + libararyPath + "]");
		log.info("user.dir [" + userDir + "]");

		// load root level configuration
		// ConfigurationManager rootcfg = new ConfigurationManager(); // FIXME -
		// deprecate
		// rootcfg.load(host + ".properties");
		hostInitialized = true;

		// create local configuration directory
		new File(cfgDir).mkdir();

	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isReady() {
		return true;
	}

	public void logTime(String tag) {
		if (startTimeMilliseconds == 0) {
			startTimeMilliseconds = System.currentTimeMillis();
		}
		if (performanceTiming) {
			log.error("performance clock :" + (System.currentTimeMillis() - startTimeMilliseconds + " ms " + tag));
		}
	}

	public static HashMap<String, Long> timerMap = null;

	static public void logTime(String timerName, String tag) {
		if (timerMap == null) {
			timerMap = new HashMap<String, Long>();
		}

		if (!timerMap.containsKey(timerName) || "start".equals(tag)) {
			timerMap.put(timerName, System.currentTimeMillis());
		}

		StringBuffer sb = new StringBuffer(40);
		sb.append("timer ");
		sb.append(timerName);
		sb.append(" ");
		sb.append(System.currentTimeMillis() - timerMap.get(timerName));
		sb.append(" ms ");
		sb.append(tag);

		log.error(sb);
	}

	/**
	 * method of serializing default will be simple xml to name file
	 */
	public boolean save() {
		Serializer serializer = new Persister();

		try {
			File cfg = new File(cfgDir + File.separator + this.getName() + ".xml");
			serializer.write(this, cfg);
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}

	public boolean save(Object o, String cfgFileName) {
		Serializer serializer = new Persister();

		try {
			File cfg = new File(cfgDir + File.separator + cfgFileName);
			serializer.write(o, cfg);
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}

	public boolean save(String cfgFileName, String data) {
		// saves user data in the .myrobotlab directory
		// with the file naming convention of name.<cfgFileName>
		try {
			FileIO.stringToFile(cfgDir + File.separator + this.getName() + "." + cfgFileName, data);
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}

	/**
	 * method of de-serializing default will to load simple xml from name file
	 */
	public boolean load() {
		return load(null, null);
	}

	public boolean load(Object o, String inCfgFileName) {
		String filename = null;
		if (inCfgFileName == null) {
			filename = cfgDir + File.separator + this.getName() + ".xml";
		} else {
			filename = cfgDir + File.separator + inCfgFileName;
		}
		if (o == null) {
			o = this;
		}
		Serializer serializer = new Persister();
		try {
			File cfg = new File(filename);
			if (cfg.exists()) {
				serializer.read(o, cfg);
			} else {
				log.info("cfg file " + filename + " does not exist");
			}
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}

	/*
	 * setCFG a Service level accessor for remote messages to change
	 * configuration of foreign services.
	 */
	/**
	 * @deprecated
	 */
	public ConfigurationManager getCFG() {
		return cfg;
	}

	/**
	 * @deprecated
	 */
	public ConfigurationManager getHostCFG() {
		return hostcfg;
	}

	/**
	 * @deprecated
	 */
	public String getCFG(String name) {
		return cfg.get(name);
	}

	/**
	 * @deprecated
	 */
	public void setCFG(NameValuePair mvp) {
		cfg.set(mvp.name.toString(), mvp.value.toString());
	}

	/**
	 * @deprecated
	 */
	public void setCFG(String name, String value) {
		cfg.set(name, value);
	}

	/**
	 * @deprecated
	 */
	public void setCFG(String name, Integer value) {
		cfg.set(name, value);
	}

	public void loadGlobalMachineDefaults() {
		// create root configuration
		ConfigurationManager hostCFG = new ConfigurationManager(host);
		// add global config
		hostCFG.set("servicePort", 3389);
		hostCFG.set("Communicator", "org.myrobotlab.net.CommObjectStreamOverTCPUDP");
		hostCFG.set("Serializer", "org.myrobotlab.net.SerializerObject");
	}

	public void loadServiceDefaultConfiguration() {
		cfg.set("outboxMsgHandling", RELAY);
		cfg.set("anonymousMsgRequest", PROCESS);

		cfg.set("hideMethods/main", "");
		cfg.set("hideMethods/loadDefaultConfiguration", "");
		cfg.set("hideMethods/getToolTip", "");
		cfg.set("hideMethods/run", "");
		cfg.set("hideMethods/access$0", ""); // TODO - Lame inner class slop -
												// this should be fixed at the
												// source

		outboxMsgHandling = cfg.get("outboxMsgHandling");
		anonymousMsgRequest = cfg.get("anonymousMsgRequest");
	}

	abstract public void loadDefaultConfiguration();

	// FIXME - remove - this is silly
	public void pause(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logException(e);
		}
	}

	public void stopService() {
		isRunning = false;
		getComm().getComm().stopService();
		outbox.stop();
		if (thisThread != null) {
			thisThread.interrupt();
		}
		thisThread = null;
	}

	public void releaseService() {
		// note - if stopService is overwritten with extra
		// threads - releaseService will need to be overwritten too
		stopService();
		Runtime.unregister(url, name);
	}

	public void startService() {
		if (!isRunning()) {
			outbox.start();
			if (thisThread == null) {
				thisThread = new Thread(this, name);
			}
			thisThread.start();
			isRunning = true;
		} else {
			log.warn("startService request: service " + name + " is already running");
		}
	}

	// override for extended functionality
	public boolean preRoutingHook(Message m) {
		return true;
	}

	// override for extended functionality
	public boolean preProcessHook(Message m) {
		return true;
	}

	@Override
	final public void run() {
		isRunning = true;

		try {
			while (isRunning) {

				Message m = getMsg();

				if (!preRoutingHook(m)) {
					continue;
				}

				// route if necessary
				if (!m.getName().equals(this.getName())) // && RELAY
				{
					outbox.add(m); // RELAYING
				}

				if (!preProcessHook(m)) {
					continue;
				}

				Object ret = invoke(m);
				if (Message.BLOCKING.equals(m.status)) {
					// create new message reverse sender and name
					// set to same msg id
					Message msg = createMessage(m.sender, m.method, ret);
					msg.sender = this.getName();
					msg.msgID = m.msgID;
					// msg.status = Message.BLOCKING;
					msg.status = Message.RETURN;

					outbox.add(msg);
				}

			}
		} catch (InterruptedException e) {
			if (thisThread != null) {
				log.warn(thisThread.getName());
			}
			log.warn("service INTERRUPTED ");
			isRunning = false;
		}
	}

	/*
	 * process a message - typically it will either invoke a function directly
	 * or invoke a function and create a return msg with return data
	 */
	/*
	 * Depricated with Hooks public void process(Message m) { Object ret =
	 * invoke(m); if (m.status.compareTo(Message.BLOCKING) == 0) { // create new
	 * message reverse sender and name // set to same msg id Message msg =
	 * createMessage(m.sender, m.method, ret); msg.sender = this.getName();
	 * msg.msgID = m.msgID; // msg.status = Message.BLOCKING; msg.status =
	 * Message.RETURN;
	 * 
	 * // Thread.sleep(arg0) outbox.add(msg);
	 * 
	 * } }
	 */

	/*
	 * Service.out - all data sent - get put into messages here
	 */

	/*
	 * public void out(Object o) { Message m = Operator.createMessage(o);
	 * switchboard.out(m); }
	 */

	/*
	 * out(String method, Object o) creating a message function call - without
	 * specificing the recipients - static routes will be applied this is good
	 * for Motor drivers - you can swap motor drivers by creating a different
	 * static route The motor is not "Aware" of the driver - only that it wants
	 * to method="write" data to the driver
	 */

	public void out(String method, Object o) {
		Message m = createMessage("", method, o); // create a un-named message
													// as output

		if (m.sender.length() == 0) {
			m.sender = this.getName();
		}
		if (m.sendingMethod.length() == 0) {
			m.sendingMethod = method;
		}
		outbox.add(m);
	}

	public void out(Message msg) {
		outbox.add(msg);
	}

	public void in(Message msg) {
		inbox.add(msg);
	}

	public String getIntanceName() {
		return name;
	}

	/*
	 * TODO - support multiple parameters Constructor c =
	 * A.class.getConstructor(new Class[]{Integer.TYPE, Float.TYPE}); A a =
	 * (A)c.newInstance(new Object[]{new Integer(1), new Float(1.0f)});
	 */
	// TODO - so now we support string constructors - it really should be any
	// params
	// TODO - without class specific parameters it will get "the real class"
	// regardless of casting

	static public Object getNewInstance(String classname) {
		Class<?> c;
		try {
			c = Class.forName(classname);
			return c.newInstance(); // Dynamically instantiate it
		} catch (ClassNotFoundException e) {
			log.error(stackToString(e));
		} catch (InstantiationException e) {
			log.error(stackToString(e));
		} catch (IllegalAccessException e) {
			log.error(stackToString(e));
		}
		return null;
	}

	// Used by CommunicationManager to get new instances of Communicators
	static public Object getNewInstance(String classname, Service service) {
		Class<?> c;
		try {
			c = Class.forName(classname);
			Constructor<?> mc = c.getConstructor(new Class[] { Service.class });
			return mc.newInstance(new Object[] { service });
		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (SecurityException e) {
			logException(e);
		} catch (NoSuchMethodException e) {
			logException(e);
		} catch (IllegalArgumentException e) {
			logException(e);
		} catch (InstantiationException e) {
			logException(e);
		} catch (IllegalAccessException e) {
			logException(e);
		} catch (InvocationTargetException e) {
			logException(e);
		}
		return null;
	}

	static public Object getNewInstance(String classname, String param) {
		Object params[] = null;
		if (param != null) {
			params = new Object[1];
			params[0] = param;
		}

		Class<?> c;
		try {
			c = Class.forName(classname);
			Constructor<?> mc = c.getConstructor(new Class[] { param.getClass() });
			return mc.newInstance(params); // Dynamically instantiate it

		} catch (Exception e) {
			logException(e);
		}
		return null;
	}

	// TODO - make this the custom call
	static public Object getNewInstance(String classname, Object[] param) {
		Class<?> c;
		try {
			c = Class.forName(classname);
			Class<?>[] paramTypes = new Class[param.length];
			for (int i = 0; i < param.length; ++i) {
				paramTypes[i] = param[i].getClass();
			}
			Constructor<?> mc = c.getConstructor(paramTypes);
			return mc.newInstance(param); // Dynamically instantiate it

		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (SecurityException e) {
			logException(e);
		} catch (NoSuchMethodException e) {
			logException(e);
		} catch (RuntimeException e) {
			logException(e);
		} catch (InstantiationException e) {
			logException(e);
		} catch (IllegalAccessException e) {
			logException(e);
		} catch (InvocationTargetException e) {
			logException(e);
		}

		return null;
	}

	/*
	 * notify notify list is list of outbound The purpose of this function is to
	 * join two named endpoints an out and an in out : outname-rettype=paramtype
	 * -> namedInstance, method, paramtype in : TODO - define Broadcast messages
	 * - and Type Joined messages
	 */
	/*
	 * public void notify(NotifyEntry ne) { // TODO - this would have to change
	 * if signature was used if
	 * (outbox.notifyList.containsKey(ne.outMethod_.toString())) {
	 * outbox.notifyList.get(ne.outMethod_.toString()).add(ne); } else {
	 * ArrayList<NotifyEntry> nel = new ArrayList<NotifyEntry>(); nel.add(ne);
	 * outbox.notifyList.put(ne.outMethod_.toString(), nel); } }
	 */
	// parameterType is not used for any critical look-up - but can be used at
	// runtime check to
	// check parameter mating

	// this is called from video widget sendNotifyRequest
	public void notify(NotifyEntry ne) {
		if (outbox.notifyList.containsKey(ne.outMethod.toString())) {
			// iterate through all looking for duplicate
			boolean found = false;
			ArrayList<NotifyEntry> nes = outbox.notifyList.get(ne.outMethod.toString());
			for (int i = 0; i < nes.size(); ++i) {
				NotifyEntry entry = nes.get(i);
				if (entry.equals(ne)) {
					log.warn("attempting to add duplicate NotifyEntry " + ne);
					found = true;
					break;
				}
			}
			if (!found) {
				log.info("adding notify from " + this.getName() + "." + ne.outMethod + " to " + ne.name + "."
						+ ne.inMethod);
				nes.add(ne);
			}
		} else {
			ArrayList<NotifyEntry> nel = new ArrayList<NotifyEntry>();
			nel.add(ne);
			log.info("adding notify from " + this.getName() + "." + ne.outMethod + " to " + ne.name + "." + ne.inMethod);
			outbox.notifyList.put(ne.outMethod.toString(), nel);
		}

	}

	public void notify(String outMethod, String namedInstance, String inMethod, Class<?>[] paramTypes) {
		NotifyEntry ne = new NotifyEntry(outMethod, namedInstance, inMethod, paramTypes);
		notify(ne);
	}

	// FIXME - wrong way to do boxing use ...

	public void notify(String name, String outAndInMethod) {
		notify(outAndInMethod, name, outAndInMethod, (Class<?>[]) null);
	}

	public void notify(String name, String outAndInMethod, Class<?> parameterType) {
		notify(outAndInMethod, name, outAndInMethod, new Class[] { parameterType });
	}

	public void notify(String name, String outAndInMethod, Class<?>[] parameterTypes) {
		notify(outAndInMethod, name, outAndInMethod, parameterTypes);
	}

	// convenience boxing functions
	public void notify(String outMethod, String namedInstance, String inMethod) {

		notify(outMethod, namedInstance, inMethod, (Class[]) null);
	}

	public void notify(String outMethod, String namedInstance, String inMethod, Class<?> parameterType1) {

		notify(outMethod, namedInstance, inMethod, new Class[] { parameterType1 });
	}

	public void notify(String outMethod, String namedInstance, String inMethod, Class<?> parameterType1,
			Class<?> parameterType2) {

		notify(outMethod, namedInstance, inMethod, new Class[] { parameterType1, parameterType2 });
	}

	public void notify(String outMethod, String namedInstance, String inMethod, Class<?> parameterType1,
			Class<?> parameterType2, Class<?> parameterType3) {
		notify(outMethod, namedInstance, inMethod, new Class[] { parameterType1, parameterType2, parameterType3 });
	}

	public void notify(String outMethod, String namedInstance, String inMethod, Class<?> parameterType1,
			Class<?> parameterType2, Class<?> parameterType3, Class<?> parameterType4) {
		notify(outMethod, namedInstance, inMethod, new Class[] { parameterType1, parameterType2, parameterType3,
				parameterType4 });
	}

	// TODO - change to addListener and removeListener
	public void removeNotify(String serviceName, String inOutMethod) {
		removeNotify(inOutMethod, serviceName, inOutMethod, (Class[]) null);
	}

	public void removeNotify(String outMethod, String serviceName, String inMethod, Class<?>[] paramTypes) {

		if (outbox.notifyList.containsKey(outMethod)) {
			ArrayList<NotifyEntry> nel = outbox.notifyList.get(outMethod);
			for (int i = 0; i < nel.size(); ++i) {
				NotifyEntry target = nel.get(i);
				if (target.name.compareTo(serviceName) == 0) {
					nel.remove(i);
				}
			}
		} else {
			log.error("removeNotify requested " + serviceName + "." + outMethod + " to be removed - but does not exist");
		}

	}

	public void removeNotify(String outMethod, String serviceName, String inMethod, Class<?> paramTypes) {

	}

	public void removeNotify() {
		outbox.notifyList.clear();
	}

	public void removeNotify(NotifyEntry ne) {
		if (outbox.notifyList.containsKey(ne.outMethod.toString())) {
			ArrayList<NotifyEntry> nel = outbox.notifyList.get(ne.outMethod.toString());
			for (int i = 0; i < nel.size(); ++i) {
				NotifyEntry target = nel.get(i);
				if (target.name.compareTo(ne.name) == 0) {
					nel.remove(i);
				}
			}
		} else {
			log.error("removeNotify requested " + ne + " to be removed - but does not exist");
		}

	}

	public Object invoke(Message msg) {

		Object retobj = null;

		log.info("invoking " + name + "." + msg.method + " (" + msg.getParameterSignature() + ") " + msg.msgID);

		retobj = invoke(msg.method, msg.data);

		return retobj;

	}

	/*
	 * invoke function to be used inside the service to "invoke" other functions
	 * which are expected to send an outbound message
	 * 
	 * the overload is to facilitate calls with a single parameter object this
	 * is important for maintaining the simplicity of connecting inputs and
	 * outputs - unless you support blocking until data has arrived on all
	 * parameters
	 */
	public Object invoke(String method) {
		return invoke(method, null);
	}

	public Object invoke(String method, Object param) {
		if (param != null) {
			Object[] params = new Object[1];
			params[0] = param;
			return invoke(this, method, params);
		} else {
			return invoke(this, method, null);
		}
	}

	// convenience reflection methods 2 parameters
	public Object invoke(String method, Object param1, Object param2) {
		Object[] params = new Object[2];
		params[0] = param1;
		params[1] = param2;

		return invoke(method, params);
	}

	// convenience reflection methods 3 parameters
	public Object invoke(String method, Object param1, Object param2, Object param3) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;

		return invoke(method, params);
	}

	// convenience reflection methods 4 parameters
	public Object invoke(String method, Object param1, Object param2, Object param3, Object param4) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;
		params[3] = param4;

		return invoke(method, params);
	}

	/* invoke in the context of a Service */
	public Object invoke(String method, Object[] params) {
		// log invoking call
		if (Logger.getRootLogger().getLevel() == Level.DEBUG) {
			String paramTypeString = "";
			if (params != null) {
				for (int i = 0; i < params.length; ++i) {
					paramTypeString += params[i].getClass().getCanonicalName();
					if (params.length != i + 1) {
						paramTypeString += ",";
					}
				}
			} else {
				paramTypeString = "null";
			}
			//log.debug("invoking " + "/" + name + "." + method + "("+ paramTypeString + ")");
		}

		Object retobj = invoke(this, method, params);

		return retobj;
	}

	/* general static base invoke */
	final public Object invoke(Object object, String method, Object params[]) {

		Object retobj = null;
		Class<?> c;
		c = object.getClass();

		try {
			// c = Class.forName(classname); // TODO - test if cached references
			// are faster than lookup

			Class<?>[] paramTypes = null;
			if (params != null) {
				paramTypes = new Class[params.length];
				for (int i = 0; i < params.length; ++i) {
					if (params[i] != null) {
						paramTypes[i] = params[i].getClass();
					} else {
						paramTypes[i] = null;
					}
				}
			}
			/*
			 * else { // null treated as empty array paramTypes = new
			 * Class<?>[0]; // no params }
			 */

			// TODO - method cache map

			Method meth = c.getMethod(method, paramTypes); // getDeclaredMethod
															// zod !!!

			retobj = meth.invoke(object, params);

			// put return object onEvent
			out(method, retobj);

		} catch (NoSuchMethodException e) {
			log.warn(c.getCanonicalName() + "#" + method + " NoSuchMethodException - attempting upcasting");

			// search for possible upcasting methods
			// scary ! resolution rules are undefined - first "found" first
			// invoked ?!?
			// TODO - optimize with method cache - heavy on memory, good on
			// speed
			// Performance Analysis
			// http://stackoverflow.com/questions/414801/any-way-to-further-optimize-java-reflective-method-invocation
			// TODO - optimize "setState" function since it is a framework
			// method - do not go through the search !
			Method[] allMethods = c.getMethods(); // ouch
			log.warn("ouch! need to search through " + allMethods.length + " methods");

			for (Method m : allMethods) {
				String mname = m.getName();
				if (!mname.equals(method)) {
					continue;
				}

				Type[] pType = m.getGenericParameterTypes();
				// checking parameter lengths
				if (params == null && pType.length != 0 || pType.length != params.length) {
					continue;
				}

				/*
				 * skip this attempt of type checking - can't get it to work -
				 * does it matter? // checking if we can match or upcast on each
				 * parameter for (int i=0; i < pType.length; ++i) { //if
				 * (params[i].getClass().isAssignableFrom(pType[i].getClass()))
				 * //if
				 * (pType[i].getClass().isAssignableFrom(params[i].getClass()))
				 * log.info(pType[i].getClass().getClass().getCanonicalName());
				 * log.info(pType[i].getClass().getCanonicalName());
				 * log.info(params[i].getClass().getCanonicalName());
				 * 
				 * if
				 * (pType[i].getClass().isAssignableFrom(pType[i].getClass())) {
				 * //m.setAccessible(true); - leave private alone //invoke it -
				 * we are done, whew! try { retobj = m.invoke(object, params);
				 * return retobj; } catch (Exception e1) {
				 * Service.logException(e1); }
				 * 
				 * }
				 * 
				 * }
				 */
				try {
					log.debug("found appropriate method");
					retobj = m.invoke(object, params);

					// put return object onEvent
					out(method, retobj);
					return retobj;
				} catch (Exception e1) {
					log.error("boom goes method " + m.getName());
					Service.logException(e1);
				}
			}

			log.error("did not find method [" + method + "] with " + ((params == null) ? "()" : params.length)
					+ " params");

		} catch (Exception e) {
			Service.logException(e);
		}

		return retobj;

	}

	public Message getMsg() throws InterruptedException {
		return inbox.getMsg();
	}

	/*
	 * send takes a name of a target system - the method - and a list of
	 * parameters and invokes that method at its destination.
	 */

	// this send forces remote connect - for registering services
	public void send(URL url, String method, Object param1) {
		Object[] params = new Object[1];
		params[0] = param1;
		Message msg = createMessage(name, method, params);
		outbox.getCommunicationManager().send(url, msg);
	}

	// BOXING - BEGIN --------------------------------------

	// 0?
	public void send(String name, String method) {
		Message msg = createMessage(name, method, null);
		msg.sender = this.getName();
		outbox.add(msg);
	}

	// boxing - the right way - thank you Java 5
	public void send(String name, String method, Object... data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.getName();
		// All methods which are invoked will 
		// get the correct sendingMethod
		// here its hardcoded
		msg.sendingMethod="send"; 
		outbox.add(msg);
	}

	// BOXING - End --------------------------------------

	public Object sendBlocking(String name, String method, Object[] data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.getName();
		msg.status = Message.BLOCKING;

		Object[] returnContainer = new Object[1];
		/*
		 * if (inbox.blockingList.contains(msg.msgID)) { log.error("DUPLICATE");
		 * }
		 */
		inbox.blockingList.put(msg.msgID, returnContainer);

		try {
			// block until message comes back
			synchronized (returnContainer) {
				outbox.add(msg);
				returnContainer.wait();
			}
		} catch (InterruptedException e) {
			logException(e);
		}

		return returnContainer[0];
	}

	// TODO - remove or reconcile - RemoteAdapter and Service are the only ones
	// using this
	public Message createMessage(String name, String method, Object data) {
		if (data != null) {
			Object[] d = new Object[1];
			d[0] = data;
			return createMessage(name, method, d);

		} else {
			return createMessage(name, method, null);
		}

	}

	// master TODO - Probably simplyfy to take array of object
	public Message createMessage(String name, String method, Object[] data) {
		Message msg = new Message(); // TODO- optimization - have a contructor
										// for all parameters

		Date d = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
		formatter.setCalendar(cal);

		msg.sender = this.getName();
		if (msg.msgID == null) {
			msg.msgID = formatter.format(d);
		}
		msg.timeStamp = formatter.format(d);
		if (name != null) {
			msg.name = name; // destination instance name
		}
		msg.data = data;
		msg.method = method;

/*		
		if (msg.getName().length() == 0) {
			log.debug("create message " + url + "/" + msg.method + "#" + msg.getParameterSignature());
		} else {
			log.debug("create message " + url + "/" + msg.getName() + "/" + msg.method + "#"
					+ msg.getParameterSignature());
		}
*/		
		return msg;
	}

	public Inbox getInbox() {
		return inbox;
	}

	public Outbox getOutbox() {
		return outbox;
	}

	public Thread getThisThread() {
		return thisThread;
	}

	public void setThisThread(Thread thisThread) {
		this.thisThread = thisThread;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public static String getHostName(final String inHost) {
		if (inHost != null)
			return inHost;

		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			log.error("could not find host, host is null or empty !");
		}

		return "localhost"; // no network - still can't be null // chumby
	}

	// connection publish points - begin ---------------
	public IPAndPort noConnection(IPAndPort conn) {
		log.error("could not connect to " + conn.IPAddress + ":" + conn.port);
		return conn;
	}

	public IPAndPort connectionBroken(IPAndPort conn) {
		log.error("the connection " + conn.IPAddress + ":" + conn.port + " has been broken");
		return conn;
	}

	// connection publish points - end ---------------

	public static String getMethodToolTip(String className, String methodName, Class<?>[] params) {

		try {

			Class<?> c = Class.forName(className);

			Method m;
			m = c.getMethod(methodName, params);

			ToolTip tip = m.getAnnotation(ToolTip.class);

			if (tip != null) {
				return tip.value();
			}

		} catch (SecurityException e) {
			logException(e);
		} catch (NoSuchMethodException e) {
			logException(e);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		return null;

	}

	public CommunicationInterface getComm() {
		return cm;
	}

	public final static String stackToString(final Throwable e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return "------\r\n" + sw.toString() + "------\r\n";
		} catch (Exception e2) {
			return "bad stackToString";
		}
	}

	public final static void logException(final Throwable e) {
		log.error(stackToString(e));
	}

	/*
	 * copyShallowFrom is used to help maintain state information with
	 */

	public static Object copyShallowFrom(Object target, Object source) {
		if (target == source) { // data is myself - operating on local copy
			return target;
		}

		Class<?> sourceClass = source.getClass();
		Class<?> targetClass = target.getClass();
		Field fields[] = sourceClass.getDeclaredFields();
		for (int j = 0, m = fields.length; j < m; j++) {
			try {
				Field f = fields[j];

				if (Modifier.isPublic(f.getModifiers()) && !(f.getName().equals("log"))
						&& !Modifier.isTransient(f.getModifiers())) {

					Type t = f.getType();

					// log.info(Modifier.toString(f.getModifiers()));
					// f.isAccessible()

					log.info("setting " + f.getName());
					if (t.equals(java.lang.Boolean.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setBoolean(target, f.getBoolean(source));
					} else if (t.equals(java.lang.Character.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setChar(target, f.getChar(source));
					} else if (t.equals(java.lang.Byte.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setByte(target, f.getByte(source));
					} else if (t.equals(java.lang.Short.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setShort(target, f.getShort(source));
					} else if (t.equals(java.lang.Integer.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setInt(target, f.getInt(source));
					} else if (t.equals(java.lang.Long.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setLong(target, f.getLong(source));
					} else if (t.equals(java.lang.Float.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setFloat(target, f.getFloat(source));
					} else if (t.equals(java.lang.Double.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setDouble(target, f.getDouble(source));
					} else {
						log.info("setting reference to remote object " + f.getName());
						targetClass.getDeclaredField(f.getName()).set(target, f.get(source));
					}

				} else {
					log.debug("skipping " + f.getName());
				}

			} catch (Exception e) {
				Service.logException(e);
			}
			// System.out.println(names[i] + ", " + fields[j].getName() + ", "
			// + fields[j].getType().getName() + ", " +
			// Modifier.toString(fields[j].getModifiers()));
		}

		return target;

	}

	/**
	 * Inbound registerServices is the method initially called to contact a
	 * remote process. Often is will be a GUI sending this request over the wire
	 * to be received in another process by a RemoteAdapter. if the registration
	 * of foreign Services is successful, the request is echoed back to the
	 * sender, such that all Services ready for export are shared.
	 * 
	 * it is also invoked by RemoteAdapters as they recieve the initial request
	 * the remote URL is filled in from the other end of the socket info Since
	 * the foreign Services do not necessarily know how they are identified the
	 * ServiceEnvironment & Service accessURL are filled in here
	 * 
	 * @param hostAddress
	 * @param port
	 * @param msg
	 */
	// FIXME - signature needs to be registerServices(URI, Message)
	public void registerServices(String hostAddress, int port, Message msg) {
		try {
			ServiceDirectoryUpdate sdu = (ServiceDirectoryUpdate) msg.data[0];

			StringBuffer sb = new StringBuffer();
			sb.append("http://");
			sb.append(hostAddress);
			sb.append(":");
			sb.append(port);

			sdu.remoteURL = new URL(sb.toString());

			sdu.url = url;

			sdu.serviceEnvironment.accessURL = sdu.remoteURL;
			log.info(name + " recieved service directory update from " + sdu.remoteURL);

			// if successfully registered with "new" Services - echo a
			// registration back
			if (Runtime.register(sdu.remoteURL, sdu.serviceEnvironment)) {
				ServiceDirectoryUpdate echoLocal = new ServiceDirectoryUpdate();
				echoLocal.remoteURL = sdu.url;
				echoLocal.serviceEnvironment = Runtime.getLocalServicesForExport();
				// send echo of local services back to sender
				send(msg.sender, "registerServices", echoLocal);
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

	}

	/**
	 * FIXME - registerLocalService() - calls RuntimeEnvironment(null, this)
	 * 
	 * @param host
	 *            always null for local service
	 */
	public synchronized void registerLocalService(URL url) {
		Runtime.register(this, url); // problem with this in it does not
										// broadcast
	}

	// TODO - DEPRICATE !!!!
	public synchronized void registerServices() {
		log.debug(name + " registerServices");

		try {

			Class<?> c;
			c = Class.forName(serviceClass);

			HashMap<String, Object> hideMethods = cfg.getMap("hideMethods");

			// try to get method which has the correct parameter types
			// http://java.sun.com/developer/technicalArticles/ALT/Reflection/
			Method[] methods = c.getDeclaredMethods();
			// register this service
			hostcfg.setServiceEntry(host, name, serviceClass, 0, new Date(), this, getToolTip());

			for (int i = 0; i < methods.length; ++i) {
				Method m = methods[i];
				Class<?>[] paramTypes = m.getParameterTypes();
				Class<?> returnType = m.getReturnType();

				if (!hideMethods.containsKey(m.getName())) {
					// service level
					hostcfg.setMethod(host, name, m.getName(), returnType, paramTypes);
				}
			}

			Class<?>[] interfaces = c.getInterfaces();

			for (int i = 0; i < interfaces.length; ++i) {
				Class<?> interfc = interfaces[i];

				log.info("adding interface " + interfc.getCanonicalName());

				hostcfg.setInterface(host, name, interfc.getClass().getCanonicalName());

			}

			Type[] intfs = c.getGenericInterfaces();
			for (int j = 0; j < intfs.length; ++j) {
				Type t = intfs[j];
				String nameOnly = t.toString().substring(t.toString().indexOf(" ") + 1);
				hostcfg.setInterface(host, name, nameOnly);
			}

			// hostcfg.save("cfg.txt");

		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (SecurityException e) {
			logException(e);
		} catch (IllegalArgumentException e) {
			logException(e);
		}

		// send(name, "registerServicesNotify");

	}

	/**
	 * Outbound sendServiceDirectoryUpdate - initial request to connect and
	 * register services with a remote system FIXME - change to register - with
	 * full signature
	 */
	public void sendServiceDirectoryUpdate(String login, String password, String name, String remoteHost, int port,
			ServiceDirectoryUpdate sdu) {

		try {

			log.info(name + " sendServiceDirectoryUpdate ");
			StringBuffer urlstr = new StringBuffer();
			urlstr.append("http://"); // FIXME - change to URI - use default
										// protocol tcp:// mrl:// udp://

			if (login != null) {
				urlstr.append(login);
				urlstr.append(":");
			}

			if (password != null) {
				urlstr.append(password);
				urlstr.append("@");
			}

			InetAddress inetAddress = null;
			// InetAddress.getByName("208.29.194.106");
			inetAddress = InetAddress.getByName(remoteHost);

			urlstr.append(inetAddress.getHostAddress());
			urlstr.append(":");
			urlstr.append(port);

			URL remoteURL = null;
			remoteURL = new URL(urlstr.toString());

			if (sdu == null) {
				sdu = new ServiceDirectoryUpdate();
				sdu.serviceEnvironment = Runtime.getLocalServicesForExport();
			}

			sdu.remoteURL = remoteURL; // nice but not right nor trustworthy
			sdu.url = url;

			send(remoteURL, "registerServices", sdu);

		} catch (Exception e) {
			logException(e);
			return;
		}

	}

	// new state functions begin --------------------------
	public void broadcastState() {
		invoke("publishState");
	}

	public Service publishState() {
		return this;
	}

	public Service setState(Service s) {
		return (Service) copyShallowFrom(this, s);
	}

	// ---------------- logging begin ---------------------------
	/**
	 * dynamically set the logging level
	 * 
	 * @param level
	 *            DEBUG | INFO | WARN | ERROR | FATAL
	 */
	public final static void setLogLevel(String level) {
		setLogLevel(null, level);
	}

	/**
	 * @param level
	 *            string input controls logging level
	 */
	public final static void setLogLevel(String object, String level) {
		log.debug("setLogLevel " + object + " " + level);
		Logger logger;
		if (object == null || object.length() == 0) {
			logger = Logger.getRootLogger();
		} else {
			logger = Logger.getLogger(object);
		}

		if ((LOG_LEVEL_INFO).equalsIgnoreCase(level)) {
			logger.setLevel(Level.INFO);
		} else if ((LOG_LEVEL_WARN).equalsIgnoreCase(level)) {
			logger.setLevel(Level.WARN);
		} else if ((LOG_LEVEL_ERROR).equalsIgnoreCase(level)) {
			logger.setLevel(Level.ERROR);
		} else if ((LOG_LEVEL_FATAL).equalsIgnoreCase(level)) {
			logger.setLevel(Level.FATAL);
		} else {
			logger.setLevel(Level.DEBUG);
		}

	}

	public static String getLogLevel(String object) {
		log.debug("getLogLevel " + object);
		Logger logger;
		if (object == null || object.length() == 0) {
			logger = Logger.getRootLogger();
		} else {
			logger = Logger.getLogger(object);
		}

		if (logger.getLevel() == Level.INFO) {
			return LOG_LEVEL_INFO;
		} else if (logger.getLevel() == Level.WARN) {
			return LOG_LEVEL_WARN;
		} else if (logger.getLevel() == Level.ERROR) {
			return LOG_LEVEL_ERROR;
		} else if (logger.getLevel() == Level.FATAL) {
			return LOG_LEVEL_FATAL;
		} else if (logger.getLevel() == Level.DEBUG) {
			return LOG_LEVEL_DEBUG;
		}

		return null;
	}

	/**
	 * dynamically set the level of the current Service logger in case you want
	 * to change the logging level of say an OpenCV Service to a different level
	 * than a Clock Service
	 * 
	 * @param level
	 *            DEBUG | INFO | WARN | ERROR | FATAL
	 */
	public void setMyLogLevel(String level) {
		Logger logger = Logger.getLogger(this.getClass().toString());
		if (("INFO").equalsIgnoreCase(level)) {
			logger.setLevel(Level.INFO);
		}
		if (("WARN").equalsIgnoreCase(level)) {
			logger.setLevel(Level.WARN);
		}
		if (("ERROR").equalsIgnoreCase(level)) {
			logger.setLevel(Level.ERROR);
		}
		if (("FATAL").equalsIgnoreCase(level)) {
			logger.setLevel(Level.FATAL);
		} else {
			logger.setLevel(Level.DEBUG);
		}
	}

	public static void addAppender(String type) {
		addAppender(type, null, null);
	}

	public static void addAppender(String type, String host, String port) {
		// same format as BasicConfigurator.configure()
		PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x - %m%n");
		Appender appender = null;

		try {

			if (LOGGING_APPENDER_CONSOLE.equals(type)) {
				appender = new ConsoleAppender(layout);
				appender.setName(LOGGING_APPENDER_CONSOLE);
			} else if (LOGGING_APPENDER_CONSOLE_GUI.equals(type)) {
				// FIXME must be done by a GUI
				// appender = new SocketAppender(host, Integer.parseInt(port));
				// appender.setName(LOGGING_APPENDER_SOCKET);
			} else if (LOGGING_APPENDER_SOCKET.equals(type)) {
				appender = new SocketAppender(host, Integer.parseInt(port));
				appender.setName(LOGGING_APPENDER_SOCKET);
			} else if (LOGGING_APPENDER_ROLLING_FILE.equals(type)) {
				String userDir = System.getProperty("user.dir");
				appender = new RollingFileAppender(layout, userDir + File.separator + "myrobotlab.log", false);
				appender.setName(LOGGING_APPENDER_ROLLING_FILE);
			} else {
				log.error("attempting to add unkown type of Appender " + type);
				return;
			}

		} catch (Exception e) {
			System.out.println(Service.stackToString(e));
		}

		if (appender != null) {
			Logger.getRootLogger().addAppender(appender);
		}

		if (LOGGING_APPENDER_NONE.equals(type)) {
			Logger.getRootLogger().removeAllAppenders();
		}

	}

	public static void remoteAppender(String name) {
		Logger.getRootLogger().removeAppender(name);
	}

	public static void removeAllAppenders() {
		Logger.getRootLogger().removeAllAppenders();
	}

	public String getShortTypeName() {
		String serviceClassName = this.getClass().getCanonicalName();
		return serviceClassName.substring(serviceClassName.lastIndexOf(".") + 1);
	}

	// ---------------- logging end ---------------------------

	/*
	 * Check RuntimeEnvironment for implementation // TODO - cache fields in a
	 * map if "searching" requires too much public Service setState(Service
	 * other) { // Service is local - not remote update // no need to
	 * reflectivly update if (this == other) { return this; }
	 * 
	 * return this; }
	 */

	public static String getCFGDir() {
		return cfgDir;
	}

	public Set<String> getNotifyListKeySet() {
		return getOutbox().notifyList.keySet();
	}

	public ArrayList<NotifyEntry> getNotifyList(String key) {
		// TODO refactor Outbox inteface
		return getOutbox().notifyList.get(key);
	}

}
