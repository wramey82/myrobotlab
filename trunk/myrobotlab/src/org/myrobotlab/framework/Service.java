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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.Timer;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.CommunicationManager;
import org.myrobotlab.net.Heartbeat;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.data.NameValuePair;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

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
/*
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((MESSAGE_RECORDING_FORMAT_BINARY == null) ? 0 : MESSAGE_RECORDING_FORMAT_BINARY.hashCode());
		result = prime * result + ((MESSAGE_RECORDING_FORMAT_XML == null) ? 0 : MESSAGE_RECORDING_FORMAT_XML.hashCode());
		result = prime * result + ((TSFormatter == null) ? 0 : TSFormatter.hashCode());
		result = prime * result + (allowExport ? 1231 : 1237);
		result = prime * result + ((anonymousMsgRequest == null) ? 0 : anonymousMsgRequest.hashCode());
		result = prime * result + ((cal == null) ? 0 : cal.hashCode());
		result = prime * result + ((cfg == null) ? 0 : cfg.hashCode());
		result = prime * result + ((cm == null) ? 0 : cm.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((hostcfg == null) ? 0 : hostcfg.hashCode());
		result = prime * result + ((inbox == null) ? 0 : inbox.hashCode());
		result = prime * result + (isRunning ? 1231 : 1237);
		result = prime * result + ((lastRecordingFilename == null) ? 0 : lastRecordingFilename.hashCode());
		result = prime * result + ((methodSet == null) ? 0 : methodSet.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((outbox == null) ? 0 : outbox.hashCode());
		result = prime * result + ((outboxMsgHandling == null) ? 0 : outboxMsgHandling.hashCode());
		result = prime * result + ((serviceClass == null) ? 0 : serviceClass.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Service other = (Service) obj;
		if (MESSAGE_RECORDING_FORMAT_BINARY == null) {
			if (other.MESSAGE_RECORDING_FORMAT_BINARY != null)
				return false;
		} else if (!MESSAGE_RECORDING_FORMAT_BINARY.equals(other.MESSAGE_RECORDING_FORMAT_BINARY))
			return false;
		if (MESSAGE_RECORDING_FORMAT_XML == null) {
			if (other.MESSAGE_RECORDING_FORMAT_XML != null)
				return false;
		} else if (!MESSAGE_RECORDING_FORMAT_XML.equals(other.MESSAGE_RECORDING_FORMAT_XML))
			return false;
		if (TSFormatter == null) {
			if (other.TSFormatter != null)
				return false;
		} else if (!TSFormatter.equals(other.TSFormatter))
			return false;
		if (allowExport != other.allowExport)
			return false;
		if (anonymousMsgRequest == null) {
			if (other.anonymousMsgRequest != null)
				return false;
		} else if (!anonymousMsgRequest.equals(other.anonymousMsgRequest))
			return false;
		if (cal == null) {
			if (other.cal != null)
				return false;
		} else if (!cal.equals(other.cal))
			return false;
		if (cfg == null) {
			if (other.cfg != null)
				return false;
		} else if (!cfg.equals(other.cfg))
			return false;
		if (cm == null) {
			if (other.cm != null)
				return false;
		} else if (!cm.equals(other.cm))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (hostcfg == null) {
			if (other.hostcfg != null)
				return false;
		} else if (!hostcfg.equals(other.hostcfg))
			return false;
		if (inbox == null) {
			if (other.inbox != null)
				return false;
		} else if (!inbox.equals(other.inbox))
			return false;
		if (isRunning != other.isRunning)
			return false;
		if (lastRecordingFilename == null) {
			if (other.lastRecordingFilename != null)
				return false;
		} else if (!lastRecordingFilename.equals(other.lastRecordingFilename))
			return false;
		if (methodSet == null) {
			if (other.methodSet != null)
				return false;
		} else if (!methodSet.equals(other.methodSet))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (outbox == null) {
			if (other.outbox != null)
				return false;
		} else if (!outbox.equals(other.outbox))
			return false;
		if (outboxMsgHandling == null) {
			if (other.outboxMsgHandling != null)
				return false;
		} else if (!outboxMsgHandling.equals(other.outboxMsgHandling))
			return false;
		if (serviceClass == null) {
			if (other.serviceClass != null)
				return false;
		} else if (!serviceClass.equals(other.serviceClass))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
*/
	
	private static final long serialVersionUID = 1L;
	transient public final static Logger log = LoggerFactory.getLogger(Service.class);
	protected String host = null; // TODO - should be final??? helpful in
									// testing??? TODO - put in
									// RuntimeEnvironment???
	@Element
	private final String name;
	private String lastRecordingFilename;
	public final String serviceClass; // TODO - remove
	private boolean isRunning = false;
	protected transient Thread thisThread = null;
	
	transient Outbox outbox = null;
	transient Inbox inbox = null;
	
	@Element
	protected boolean allowExport = true; 
	
	public URI url = null;
	
	public transient final Timer timer;

	transient protected CommunicationInterface cm = null;
	/**
	 * @deprecated
	 */
	transient protected ConfigurationManager cfg = null;
	transient protected ConfigurationManager hostcfg = null;

	// relay directives
	static public final String PROCESS = "PROCESS";
	static public final String RELAY = "RELAY";
	static public final String IGNORE = "IGNORE";
	static public final String BROADCAST = "BROADCAST";
	static public final String PROCESSANDBROADCAST = "PROCESSANDBROADCAST";

	public String anonymousMsgRequest = PROCESS;
	public String outboxMsgHandling = RELAY;
	protected final static String cfgDir = String.format("%1$s%2$s.myrobotlab", System.getProperty("user.dir"), File.separator);
	private static boolean hostInitialized = false;
	
	// using a HashMap means no duplicates
	protected Set<String> methodSet;


	transient SimpleDateFormat TSFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	transient Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));

	// recordings
	static private boolean isRecording = false;
	public final String MESSAGE_RECORDING_FORMAT_XML = "MESSAGE_RECORDING_FORMAT_XML";
	public final String MESSAGE_RECORDING_FORMAT_BINARY = "MESSAGE_RECORDING_FORMAT_BINARY";

	private transient ObjectOutputStream recording;
	private transient ObjectInputStream playback;
	private transient OutputStream recordingXML;
	private transient OutputStream recordingPython;

	/**
	 * TODO - deprecate - use annotations
	 */
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

	/**
	 * local service constructor - initializes with null host env
	 * @param instanceName
	 * @param serviceClass
	 */
	public Service(String instanceName, String serviceClass) {
		this(instanceName, serviceClass, null);
	}

	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param instanceName
	 * @param serviceClass
	 * @param inHost
	 */
	public Service(String instanceName, String serviceClass, String inHost) {

		// load all string signatures of our methods
		// FIXME - use Method - currently does not support parameters
		if (methodSet == null)
		{
			methodSet = getMessageSet();
		}
		
		
		// if I'm not a Runtime and not explicitly requesting a Runtime - then start a Runtime
		if (!Runtime.isRuntime(this) && !serviceClass.equals("org.myrobotlab.service.Runtime")) {
			Runtime.getInstance();
		}

		if (inHost != null) {
			try {
				url = new URI(inHost); 
			} catch (Exception e) {
				log.error(String.format("%1$s not a valid URI", inHost));
			}
		}
		
		host = getHostName(inHost);
		name = instanceName;
		this.timer = new Timer(String.format("%s_timer", name));
		this.serviceClass = serviceClass;
		this.inbox = new Inbox(name);
		this.outbox = new Outbox(this);

		hostcfg = new ConfigurationManager(host);
		cfg = new ConfigurationManager(host, name);

		// global defaults begin - multiple services will re-set defaults
		loadGlobalMachineDefaults(); // TODO - put in RuntimeEnvironments

		// service instance defaults
		loadServiceDefaultConfiguration();

		// over-ride process level with host file
		if (!hostInitialized) {
			initialize();
		}

		// over-ride service level with service file
		cfg.load(String.format("%1$s.%2$s.properties", host, name));

		// now that cfg is ready make a communication manager
		cm = new CommunicationManager(this);

		TSFormatter.setCalendar(cal);

		// FIXME - deprecate - remove !
		//registerServices();
	
		Runtime.register(this, url);
	}

	
	public static synchronized void initialize() {
		String libararyPath = System.getProperty("java.library.path");
		String userDir = System.getProperty("user.dir");

		String vmName = System.getProperty("java.vm.name");
		// TODO this should be a single log statement
		// http://developer.android.com/reference/java/lang/System.html
		log.info("---------------normalized-------------------");
		log.info(String.format("ivy [runtime,%1$s.%2$d.%3$s]", Platform.getArch(), Platform.getBitness(), Platform.getOS()));
		log.info(String.format("os.name [%1$s] getOS [%2$s]", System.getProperty("os.name"), Platform.getOS()));
		log.info(String.format("os.arch [%1$s] getArch [%2$s]", System.getProperty("os.arch"), Platform.getArch()));
		log.info(String.format("getBitness [%1$d]", Platform.getBitness()));
		log.info(String.format("java.vm.name [%1$s] getVMName [%2$s]", vmName, Platform.getVMName()));
		log.info(String.format("version [%s]", FileIO.getResourceFile("version.txt")));
		log.info(String.format("/resource [%s]", FileIO.getResouceLocation()));
		log.info(String.format("jar path [%s]", FileIO.getResourceJarPath()));
		log.info(String.format("sun.arch.data.model [%s]", System.getProperty("sun.arch.data.model")));

		log.info("---------------non-normalized---------------");
		log.info(String.format("java.vm.name [%s]", vmName));
		log.info(String.format("java.vm.version [%s]", System.getProperty("java.vm.version")));
		log.info(String.format("java.vm.vendor [%s]", System.getProperty("java.vm.vendor")));
		log.info(String.format("java.vm.version [%s]", System.getProperty("java.vm.version")));
		
		log.info(String.format("java.vm.vendor [%s]", System.getProperty("java.runtime.version")));

		
		log.info(String.format("java.home [%s]", System.getProperty("java.home")));
		log.info(String.format("os.version [%s]", System.getProperty("os.version")));
		log.info(String.format("java.class.path [%s]", System.getProperty("java.class.path")));
		log.info(String.format("java.library.path [%s]", libararyPath));
		log.info(String.format("user.dir [%s]", userDir));
		log.info(String.format("total mem [%d] Mb", Runtime.getTotalMemory() / 1048576)); 
		log.info(String.format("total free [%d] Mb", Runtime.getFreeMemory() / 1048576));

		// load root level configuration
		// ConfigurationManager rootcfg = new ConfigurationManager(); // FIXME -
		// deprecate
		// rootcfg.load(host + ".properties");
		hostInitialized = true;

		// create local configuration directory
		new File(cfgDir).mkdir();
	}

	/**
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isReady() {
		return true;
	}


	/**
	 * method of serializing default will be simple xml to name file
	 */
	public boolean save() {
		Serializer serializer = new Persister();

		try {
			File cfg = new File(String.format("%s%s%s.xml", cfgDir, File.separator, this.getName()));
			serializer.write(this, cfg);
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param o
	 * @param cfgFileName
	 * @return
	 */
	public boolean save(Object o, String cfgFileName) {
		Serializer serializer = new Persister();

		try {
			File cfg = new File(String.format("%1$s%2$s%3$s", cfgDir, File.separator, cfgFileName));
			serializer.write(o, cfg);
		} catch (Exception e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param cfgFileName
	 * @param data
	 * @return
	 */
	public boolean save(String cfgFileName, String data) {
		// saves user data in the .myrobotlab directory
		// with the file naming convention of name.<cfgFileName>
		try {
			FileIO.stringToFile(String.format("%1$s%2$s%3$s.%4$s", cfgDir, File.separator, this.getName(), cfgFileName), data);
		} catch (Exception e) {
			Logging.logException(e);
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

	/**
	 * 
	 * @param o
	 * @param inCfgFileName
	 * @return
	 */
	public boolean load(Object o, String inCfgFileName) {
		String filename = null;
		if (inCfgFileName == null) {
			filename = String.format("%s%s%s.xml", cfgDir, File.separator, this.getName(), ".xml");
		} else {
			filename = String.format("%s%s%s", cfgDir, File.separator, inCfgFileName);
		}
		if (o == null) {
			o = this;
		}
		Serializer serializer = new Persister();
		try {
			File cfg = new File(filename);
			if (cfg.exists()) {
				serializer.read(o, cfg);
				return true;
			}
			log.info(String.format("cfg file %1$s does not exist", filename));
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
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
		// hostCFG.set("Communicator",
		// "org.myrobotlab.net.CommObjectStreamOverTCPUDP");
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


	/**
	 * sleep without the throw
	 * 
	 * @param millis
	 */
	public static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logException(e);
		}
	}

	public void stopService() {
		if (timer != null)
		{
			timer.cancel();
			timer.purge();
		}

		isRunning = false;
		// stopping the phone
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
		createAndStartSubServices(); // FIXME - deprecate !
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
	
	/**
	 * location for dependent service creation
	 * overridden by services for there specific dependency
	 * fulfillment 
	 */
	public void createAndStartSubServices()
	{
		
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
				// TODO should this declaration be outside the while loop? if
				// so, make sure to release prior to continue
				Message m = getMsg();

				if (!preRoutingHook(m)) {
					continue;
				}

				// route if necessary
				if (!m.getName().equals(this.getName())) // && RELAY
				{
					outbox.add(m); // RELAYING
					continue; // sweet - that was a long time coming fix !
				}

				if (!preProcessHook(m)) {
					continue;
				}
				// TODO should this declaration be outside the while loop?
				Object ret = invoke(m);
				if (Message.BLOCKING.equals(m.status)) {
					// TODO should this declaration be outside the while loop?
					// create new message reverse sender and name set to same
					// msg id
					Message msg = createMessage(m.sender, m.method, ret);
					msg.sender = this.getName();
					msg.msgID = m.msgID;
					// msg.status = Message.BLOCKING;
					msg.status = Message.RETURN;

					outbox.add(msg);
				}
			}
		} catch (InterruptedException e) {
			isRunning = false;
			if (thisThread != null) {
				log.warn(thisThread.getName());
			}
			log.warn("service INTERRUPTED ");
		}
	}

	/**
	 * Creating a message function call - without specifying the recipients -
	 * static routes will be applied this is good for Motor drivers - you can
	 * swap motor drivers by creating a different static route The motor is not
	 * "Aware" of the driver - only that it wants to method="write" data to the
	 * driver
	 * 
	 * @param method
	 * @param o
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

	/**
	 * 
	 * @param msg
	 */
	public void out(Message msg) {
		outbox.add(msg);
	}

	/**
	 * 
	 * @param msg
	 */
	public void in(Message msg) {
		inbox.add(msg);
	}

	/**
	 * 
	 * @return
	 */
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

	/**
	 * 
	 * @param classname
	 * @return
	 */
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

	/**
	 * Used by CommunicationManager to get new instances of Communicators
	 * 
	 * @param classname
	 * @param service
	 * @return
	 */
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

	/**
	 * 
	 * @param classname
	 * @param param
	 * @return
	 */
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
	/**
	 * 
	 * @param classname
	 * @param param
	 * @return
	 * @throws ClassNotFoundException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IllegalArgumentException 
	 */
	static public Object getNewInstance(String classname, Object... param) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> c;
		//try {
			c = Class.forName(classname);
			Class<?>[] paramTypes = new Class[param.length];
			for (int i = 0; i < param.length; ++i) {
				paramTypes[i] = param[i].getClass();
			}
			Constructor<?> mc = c.getConstructor(paramTypes);
			return mc.newInstance(param); // Dynamically instantiate it

		//} catch (Exception e) {
		//	logException(e);
		//}

		//return null;
	}

	// parameterType is not used for any critical look-up - but can be used at
	// runtime check to
	// check parameter mating

	public void subscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType) {
		MRLListener listener = null;
		if (parameterType != null) {
			listener = new MRLListener(outMethod, getName(), inMethod, parameterType);
		} else {
			listener = new MRLListener(outMethod, getName(), inMethod, null);
		}

		send(publisherName, "addListener", listener);
	}

	public void unsubscribe(String outMethod, String publisherName, String inMethod, Class<?>... parameterType) {

		MRLListener listener = null;
		if (parameterType != null) {
			listener = new MRLListener(outMethod, getName(), inMethod, parameterType);
		} else {
			listener = new MRLListener(outMethod, getName(), inMethod, null);
		}

		send(publisherName, "removeListener", listener);
	}

	/**
	 * this is called from video widget subscribe
	 * 
	 * @param listener
	 */
	public void addListener(MRLListener listener) {
		if (outbox.notifyList.containsKey(listener.outMethod.toString())) {
			// iterate through all looking for duplicate
			boolean found = false;
			ArrayList<MRLListener> nes = outbox.notifyList.get(listener.outMethod.toString());
			for (int i = 0; i < nes.size(); ++i) {
				MRLListener entry = nes.get(i);
				if (entry.equals(listener)) {
					log.warn(String.format("attempting to add duplicate MRLListener %1$s", listener));
					found = true;
					break;
				}
			}
			if (!found) {
				log.info(String.format("adding addListener from %s.%s to %s.%s", this.getName(), listener.outMethod, listener.name, listener.inMethod));
				nes.add(listener);
			}
		} else {
			ArrayList<MRLListener> nel = new ArrayList<MRLListener>();
			nel.add(listener);
			log.info(String.format("adding addListener from %s.%s to %s.%s", this.getName(), listener.outMethod, listener.name, listener.inMethod));
			outbox.notifyList.put(listener.outMethod.toString(), nel);
		}

	}

	/**
	 * 
	 * @param outMethod
	 * @param namedInstance
	 * @param inMethod
	 * @param paramTypes
	 */
	public void addListener(String outMethod, String namedInstance, String inMethod, Class<?>... paramTypes) {
		MRLListener listener = new MRLListener(outMethod, namedInstance, inMethod, paramTypes);
		addListener(listener);
	}

	/**
	 * 
	 * @param name
	 * @param outAndInMethod
	 */
	public void addListener(String name, String outAndInMethod, Class<?>... paramTypes) {
		addListener(outAndInMethod, name, outAndInMethod, paramTypes);
	}

	/**
	 * 
	 * @param outMethod
	 * @param serviceName
	 * @param inMethod
	 * @param paramTypes
	 */
	public void removeListener(String outMethod, String serviceName, String inMethod, Class<?>... paramTypes) {
		if (outbox.notifyList.containsKey(outMethod)) {
			ArrayList<MRLListener> nel = outbox.notifyList.get(outMethod);
			for (int i = 0; i < nel.size(); ++i) {
				MRLListener target = nel.get(i);
				if (target.name.compareTo(serviceName) == 0) {
					nel.remove(i);
				}
			}
		} else {
			log.error(String.format("removeListener requested %1$s.%2$s to be removed - but does not exist", serviceName, outMethod));
		}
	}

	public void removeListener(String serviceName, String inOutMethod, Class<?>... paramTypes) {
		removeListener(inOutMethod, serviceName, inOutMethod, paramTypes);
	}

	public void removeListener(String serviceName, String inOutMethod) {
		removeListener(inOutMethod, serviceName, inOutMethod, (Class<?>[]) null);
	}

	/**
	 * 
	 */
	public void removeAllListeners() {
		outbox.notifyList.clear();
	}

	/**
	 * 
	 * @param listener
	 */
	public void removeListener(MRLListener listener) {
		if (!outbox.notifyList.containsKey(listener.outMethod.toString())) {
			log.error(String.format("removeListener requested %1$s to be removed - but does not exist", listener));
			return;
		}
		ArrayList<MRLListener> nel = outbox.notifyList.get(listener.outMethod.toString());
		for (int i = 0; i < nel.size(); ++i) {
			MRLListener target = nel.get(i);
			if (target.name.compareTo(listener.name) == 0) {
				nel.remove(i);
			}
		}
	}

	// TODO - refactor / remove
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public Object invoke(Message msg) {
		Object retobj = null;

		log.debug(String.format("--invoking %1$s.%3$s(%4$s) %2$s--", name, msg.msgID, msg.method, msg.getParameterSignature()));

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

	/**
	 * 
	 * @param method
	 * @param param
	 * @return
	 */
	public Object invoke(String method, Object param) {
		if (param != null) {
			Object[] params = new Object[1];
			params[0] = param;
			return invoke(this, method, params);
		} else {
			return invoke(this, method, null);
		}
	}

	/**
	 * convenience reflection methods 2 parameters
	 * 
	 * @param method
	 * @param param1
	 * @param param2
	 * @return
	 */
	public Object invoke(String method, Object param1, Object param2) {
		Object[] params = new Object[2];
		params[0] = param1;
		params[1] = param2;

		return invoke(method, params);
	}

	/**
	 * convenience reflection methods 3 parameters
	 * 
	 * @param method
	 * @param param1
	 * @param param2
	 * @param param3
	 * @return
	 */
	public Object invoke(String method, Object param1, Object param2, Object param3) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;

		return invoke(method, params);
	}

	/**
	 * convenience reflection methods 4 parameters
	 * 
	 * @param method
	 * @param param1
	 * @param param2
	 * @param param3
	 * @param param4
	 * @return
	 */
	public Object invoke(String method, Object param1, Object param2, Object param3, Object param4) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;
		params[3] = param4;

		return invoke(method, params);
	}

	/**
	 * invoke in the context of a Service
	 * 
	 * @param method
	 * @param params
	 * @return
	 */
	public Object invoke(String method, Object[] params) {
		// log invoking call
		
		if (log.isDebugEnabled()) {
			StringBuilder paramTypeString = new StringBuilder();
			if (params != null) {
				for (int i = 0; i < params.length; ++i) {
					if (params[i] == null)
					{
						paramTypeString.append("null");
					} else {
						paramTypeString.append(params[i].getClass().getCanonicalName());
					}
					if (params.length != i + 1) {
						paramTypeString.append(",");
					}
				}
			} else {
				paramTypeString.append("null");
			}
		}
		Object retobj = invoke(this, method, params);
		return retobj;
	}

	/**
	 * general static base invoke
	 * 
	 * @param object
	 * @param method
	 * @param params
	 * @return
	 */
	final public Object invoke(Object object, String method, Object params[]) {
		Object retobj = null;
		Class<?> c;
		c = object.getClass();

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
		Method meth = null;
		try {
			// TODO - method cache map
			// can not auto-box or downcast with this method - getMethod will return a "specific & exact" match based
			// on parameter types - the thing is we may have a typed signature which will allow execution - but
			// if so we need to search
			meth = c.getMethod(method, paramTypes); // getDeclaredMethod zod !!!
			retobj = meth.invoke(object, params);

			// put return object onEvent
			out(method, retobj);
		} catch (NoSuchMethodException e) {
			log.warn(String.format("%s.%s NoSuchMethodException - attempting upcasting", 
					c.getSimpleName(),
					MethodEntry.getPrettySignature(method, paramTypes, null)));

	
			// TODO - optimize with a paramter TypeConverter & Map
			// c.getMethod - returns on EXACT match - not "Working" match
			Method[] allMethods = c.getMethods(); // ouch
			log.warn(String.format("ouch! need to search through %d methods", allMethods.length));

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
				try {
					log.debug("found appropriate method");
					retobj = m.invoke(object, params);

					// put return object onEvent
					out(method, retobj);
					return retobj;
				} catch (Exception e1) {
					log.error(String.format("boom goes method %1$s", m.getName()));
					Logging.logException(e1);
				}
			}

			log.error(String.format("did not find method - %s(%s)", method, Message.getParameterSignature(params)));
		} catch (Exception e) {
			Logging.logException(e);
		}

		return retobj;
	}

	/**
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public Message getMsg() throws InterruptedException {
		return inbox.getMsg();
	}

	/*
	 * send takes a name of a target system - the method - and a list of
	 * parameters and invokes that method at its destination.
	 */

	/**
	 * this send forces remote connect - for registering services
	 * 
	 * @param url
	 * @param method
	 * @param param1
	 */
	public void send(URI url, String method, Object param1) {
		Object[] params = new Object[1];
		params[0] = param1;
		Message msg = createMessage(name, method, params);
		outbox.getCommunicationManager().send(url, msg);
	}

	// BOXING - BEGIN --------------------------------------

	/**
	 * 0?
	 * 
	 * @param name
	 * @param method
	 */
	public void send(String name, String method) {
		send(name, method, (Object[]) null);
	}

	public void startRecording() {
		invoke("startRecording", new Object[] { null });
	}

	public String startRecording(String filename) {
		String filenameXML = String.format("%s/%s_%s.xml", cfgDir, getName(), TSFormatter.format(new Date()));
		String filenamePython = String.format("%s/%s_%s.py", cfgDir, getName(), TSFormatter.format(new Date()));
		if (filename == null) {
			filename = String.format("%s/%s_%s.msg", cfgDir, getName(), TSFormatter.format(new Date()));
			lastRecordingFilename = filename;
		}

		log.info(String.format("started recording %s to file %s", getName(), filename));

		try {
			recording = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
			recordingXML = new BufferedOutputStream(new FileOutputStream(filenameXML), 8 * 1024);
			recordingXML.write("<Messages>\n".getBytes());

			recordingPython = new BufferedOutputStream(new FileOutputStream(filenamePython), 8 * 1024);

			isRecording = true;
		} catch (Exception e) {
			logException(e);
		}
		return filenamePython;
	}

	public void stopRecording() {
		log.info("stopped recording");
		isRecording = false;
		if (recording == null) {
			return;
		}
		try {

			recordingPython.flush();
			recordingPython.close();
			recordingPython = null;

			recordingXML.write("\n</Messages>".getBytes());
			recordingXML.flush();
			recordingXML.close();
			recordingXML = null;

			recording.flush();
			recording.close();
			recording = null;
		} catch (IOException e) {
			logException(e);
		}

	}

	public void loadRecording(String filename) {
		isRecording = false;

		if (filename == null) {
			filename = lastRecordingFilename;
		}

		try {
			playback = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
			while (true) {
				Message msg = (Message) playback.readObject();
				if (msg.name.startsWith("BORG")) {
					msg.name = Runtime.getInstance().getName();
				}
				outbox.add(msg);
			}
		} catch (Exception e) {
			logException(e);
		}
	}
	
	/**
	 * uses the Runtime to send a message on behalf of "name"'d service
	 * @param senderName
	 * @param name
	 * @param method
	 * @param data
	 */
	public static void proxySend(String senderName, String name, String method, Object... data) {
		Message msg = Runtime.getInstance().createMessage(name, method, data);
		msg.sender = senderName;
		msg.sendingMethod = "send";
		Runtime.getInstance().getOutbox().add(msg);
	}	

	/**
	 * boxing - the right way - thank you Java 5
	 * 
	 * @param name
	 * @param method
	 * @param data
	 */
	public void send(String name, String method, Object... data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.getName();
		// All methods which are invoked will
		// get the correct sendingMethod
		// here its hardcoded
		msg.sendingMethod = "send";

		if (isRecording) {
			try {
				
				// python
				String msgName = (msg.name.equals(Runtime.getInstance().getName())) ? "runtime" : msg.name;
				recordingPython.write(String.format("%s.%s(", msgName, msg.method).getBytes());
				if (data != null) {
					for (int i = 0; i < data.length; ++i) {
						Object d = data[i];
						if (d.getClass() == Integer.class || d.getClass() == Float.class || d.getClass() == Boolean.class || d.getClass() == Double.class
								|| d.getClass() == Short.class || d.getClass() == Short.class) {
							recordingPython.write(d.toString().getBytes());

							// FIXME Character probably blows up
						} else if (d.getClass() == String.class || d.getClass() == Character.class) { 
							recordingPython.write(String.format("\"%s\"", d).getBytes());
						} else {
							recordingPython.write("object".getBytes());
						}
						if (i < data.length - 1) {
							recordingPython.write(",".getBytes());
						}
					}
				}
				recordingPython.write(")\n".getBytes());
				recordingPython.flush();

			} catch (IOException e) {
				logException(e);
			}
		}
		outbox.add(msg);
	}

	// BOXING - End --------------------------------------
	public Object sendBlocking(String name, String method) {
		return sendBlocking(name, method, (Object[])null);
	}

	public Object sendBlocking(String name, String method, Object... data) {
		return sendBlocking(name, 1000, method, data); // default 1 sec timeout - TODO - make configurable
	}
	/**
	 * 
	 * @param name
	 * @param method
	 * @param data
	 * @return
	 */
	public Object sendBlocking(String name, Integer timeout, String method, Object... data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.getName();
		msg.status = Message.BLOCKING;
		msg.msgID = Runtime.getUniqueID();

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
				returnContainer.wait(timeout); // NEW !!! TIMEOUT !!!!
			}
		} catch (InterruptedException e) {
			logException(e);
		}

		return returnContainer[0];
	}

	// TODO - remove or reconcile - RemoteAdapter and Service are the only ones
	// using this
	/**
	 * 
	 * @param name
	 * @param method
	 * @param data
	 * @return
	 */
	public Message createMessage(String name, String method, Object data) {
		if (data == null) {
			return createMessage(name, method, null);
		}
		Object[] d = new Object[1];
		d[0] = data;
		return createMessage(name, method, d);
	}

	// master TODO - Probably simplyfy to take array of object
	/**
	 * 
	 * @param name
	 * @param method
	 * @param data
	 * @return
	 */
	public Message createMessage(String name, String method, Object[] data) {
		Message msg = new Message(); // TODO- optimization - have a contructor
										// for all parameters

		Date d = new Date();

		if (name != null) {
			msg.name = name; // destination instance name
		}
		msg.sender = this.getName();
		msg.data = data;
		msg.method = method;

		return msg;
	}

	/**
	 * 
	 * @return
	 */
	public Inbox getInbox() {
		return inbox;
	}

	/**
	 * 
	 * @return
	 */
	public Outbox getOutbox() {
		return outbox;
	}

	/**
	 * 
	 * @return
	 */
	public Thread getThisThread() {
		return thisThread;
	}

	/**
	 * 
	 * @param thisThread
	 */
	public void setThisThread(Thread thisThread) {
		this.thisThread = thisThread;
	}

	/**
	 * 
	 * @return
	 */
	public String getHost() {
		return host;
	}

	/**
	 * 
	 * @param host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 
	 * @param inHost
	 * @return
	 */
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
	/**
	 * 
	 * @param conn
	 * @return
	 */
	public IPAndPort noConnection(IPAndPort conn) {
		log.error(String.format("could not connect to %1$s:%2$d", conn.IPAddress, conn.port));
		return conn;
	}

	/**
	 * 
	 * @param conn
	 * @return
	 */
	public IPAndPort connectionBroken(IPAndPort conn) {
		log.error(String.format("the connection %1$s:%2$d has been broken", conn.IPAddress, conn.port));
		return conn;
	}

	// connection publish points - end ---------------

	/**
	 * 
	 * @param className
	 * @param methodName
	 * @param params
	 * @return
	 */
	public static String getMethodToolTip(String className, String methodName, Class<?>[] params) {
		Class<?> c;
		Method m;
		ToolTip tip = null;
		try {
			c = Class.forName(className);

			m = c.getMethod(methodName, params);

			tip = m.getAnnotation(ToolTip.class);
		} catch (SecurityException e) {
			logException(e);
		} catch (NoSuchMethodException e) {
			logException(e);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		if (tip == null) {
			return null;
		}
		return tip.value();
	}

	/**
	 * 
	 * @return
	 */
	public CommunicationInterface getComm() {
		return cm;
	}

	/**
	 * 
	 * @param e
	 * @return
	 */
	public final static String stackToString(final Throwable e) {
		StringWriter sw;
		try {
			sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
		} catch (Exception e2) {
			return "bad stackToString";
		}
		return "------\r\n" + sw.toString() + "------\r\n";
	}

	/**
	 * 
	 * @param e
	 */
	public final static void logException(final Throwable e) {
		log.error(stackToString(e));
	}

	/**
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

				if (!(Modifier.isPublic(f.getModifiers()) && !(f.getName().equals("log")) && !Modifier.isTransient(f.getModifiers()))) {
					log.debug(String.format("skipping %1$s", f.getName()));
					continue;
				}
				Type t = f.getType();

				log.info(String.format("setting %1$s", f.getName()));
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
					log.info(String.format("setting reference to remote object %1$s", f.getName()));
					targetClass.getDeclaredField(f.getName()).set(target, f.get(source));
				}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		return target;
	}




	// TODO - DEPRICATE !!!!
	/**
	 * 
	 */
	/*
	public synchronized void registerServices() {
		log.debug(String.format("%1$s registerServices", name));

		Class<?> c;
		try {
			c = Class.forName(serviceClass);

			HashMap<String, Object> hideMethods = cfg.getMap("hideMethods");

			// try to get method which has the correct parameter types
			// http://java.sun.com/developer/technicalArticles/ALT/Reflection/
			Method[] methods = c.getDeclaredMethods();
			// register this service
			hostcfg.setServiceEntry(host, name, serviceClass, 0, new Date(), this, getToolTip());

			Method m;
			Class<?>[] paramTypes;
			Class<?> returnType;
			for (int i = 0; i < methods.length; ++i) {
				m = methods[i];
				paramTypes = m.getParameterTypes();
				returnType = m.getReturnType();

				if (!hideMethods.containsKey(m.getName())) {
					// service level
					hostcfg.setMethod(host, name, m.getName(), returnType, paramTypes);
				}
			}

			Class<?>[] interfaces = c.getInterfaces();
			Class<?> interfc;
			for (int i = 0; i < interfaces.length; ++i) {
				interfc = interfaces[i];

				log.info(String.format("adding interface %1$s", interfc.getCanonicalName()));

				hostcfg.setInterface(host, name, interfc.getClass().getCanonicalName());
			}

			Type[] intfs = c.getGenericInterfaces();
			Type t;
			for (int j = 0; j < intfs.length; ++j) {
				t = intfs[j];
				hostcfg.setInterface(host, name, t.toString().substring(t.toString().indexOf(" ") + 1));
			}
		} catch (ClassNotFoundException e) {
			logException(e);
		} catch (SecurityException e) {
			logException(e);
		} catch (IllegalArgumentException e) {
			logException(e);
		}
	}
*/
	/**
	 * Outbound connect - initial request to connect and
	 * register services with a remote system 
	 */
	public void connect(String login, String password, String remoteHost, int port) {
		try {
			log.info("{} connect ", getName());
			// FIXME - change to URI - use default protocol tcp:// mrl:// udp://
			StringBuffer urlstr = new StringBuffer().append("tcp://");

			if (login != null) {
				urlstr.append(login).append(":");
			}

			if (password != null) {
				urlstr.append(password).append("@");
			}

			InetAddress inetAddress = InetAddress.getByName(remoteHost);

			urlstr.append(inetAddress.getHostAddress()).append(":").append(port);

			URI remoteURL = null;
			remoteURL = new URI(urlstr.toString());

			ServiceEnvironment mrlInstance = Runtime.getLocalServicesForExport();

			// FIXME - make a configurable gateway !!!
			Runtime.getInstance().send(remoteURL, "registerServices", mrlInstance);
		} catch (Exception e) {
			logException(e);
		}
	}

	// new state functions begin --------------------------
	/**
	 * 
	 */
	public void broadcastState() {
		invoke("publishState");
	}

	/**
	 * 
	 * @return
	 */
	public Service publishState() {
		return this;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public Service setState(Service s) {
		return (Service) copyShallowFrom(this, s);
	}


	@Override
	public String getSimpleName() {
		/*
		String serviceClassName = this.getClass().getCanonicalName();
		return serviceClassName.substring(serviceClassName.lastIndexOf(".") + 1);
		*/
		return this.getClass().getSimpleName(); 
	}

	public String getTypeName() {
		return this.getClass().getCanonicalName();
	}

	// ---------------- logging end ---------------------------

	/**
	 * 
	 * @return
	 */
	public static String getCFGDir() {
		return cfgDir;
	}

	/**
	 * 
	 */
	public ArrayList<String> getNotifyListKeySet() {
		ArrayList<String> ret = new ArrayList<String>();
		if (getOutbox() == null)
		{
			// this is remote system - it has a null outbox, because its
			// been serialized with a transient outbox
			// and your in a skeleton
			// use the runtime to send a message
			@SuppressWarnings("unchecked")
			ArrayList<String> remote = (ArrayList<String>)Runtime.getInstance().sendBlocking(getName(), "getNotifyListKeySet");
			return remote;
		} else {
			ret.addAll(getOutbox().notifyList.keySet());
		}
		return ret;
	}

	/**
	 * 
	 */
	public ArrayList<MRLListener> getNotifyList(String key) {
		if (getOutbox() == null)
		{
			// this is remote system - it has a null outbox, because its
			// been serialized with a transient outbox
			// and your in a skeleton
			// use the runtime to send a message
			@SuppressWarnings("unchecked") // FIXME - parameters !
			ArrayList<MRLListener> remote = (ArrayList<MRLListener>)Runtime.getInstance().sendBlocking(getName(), "getNotifyList", new Object[]{key});
			return remote;
			
		} else {
			return getOutbox().notifyList.get(key);
		}
	}

	/**
	 * a default way to attach Services to other Services An example would be
	 * attaching a Motor to a MotorControl or a Speaking service (TTS) to a
	 * Listening service (STT) such that when the system is speaking it does not
	 * try to listen & act on its own speech (feedback loop)
	 * 
	 * FIXME - the GUIService currently has attachGUI() and detachGUI() - these
	 * are to bind Services with their swing views/tab panels. It should be
	 * generalized to this attach method
	 * 
	 * @param serviceName
	 * @return if successful
	 * 
	 */
	

	public String getServiceResourceFile(String subpath) {
		return FileIO.getResourceFile(String.format("%s/%s", this.getSimpleName(), subpath));
	}

	/**
	 * called typically from a remote system When 2 MRL instances are connected
	 * they contain serialized non running Service in a registry, which is
	 * maintained by the Runtime. The data can be stale.
	 * 
	 * Messages are sometimes sent (often in the gui) which prompt the remote
	 * service to "broadcastState" a new serialized snapshot is broadcast to all
	 * subscribed methods, but there is no guarantee that the registry is
	 * updated
	 * 
	 * This method will update the registry, additionally it will block until
	 * the refresh response comes back
	 * 
	 * @return
	 */
	/*
	 * public Service updateState(String serviceName) {
	 * sendBlocking(serviceName, "broadcastState", null); ServiceWrapper sw =
	 * Runtime.getService(serviceName); if (sw == null) {
	 * log.error(String.format("service wrapper came back null for %s",
	 * serviceName)); return null; }
	 * 
	 * return (Service)sw.get(); }
	 */
	public Heartbeat echoHeartbeat(Heartbeat pulse) {
		return pulse;
	}

	public void startHeartbeat() {
		// getComm().
	}

	public void stopHeartbeat() {
	}
	
	public boolean allowExport()
	{
		return allowExport;
	}
	
	public void allowExport(Boolean b)
	{
		allowExport = b;
	}

	
	/**
	 * pure string interface for control facets which only support strings - like javascript, web, etc...
	 * @param name
	 * @return
	 */
	public boolean attach(String name) {
		return attach(name, (Object[]) null);
	}

	
	/**
	 * this framework attach supports string interface
	 * it will invoke an attach on the actual service with a
	 * "real" type
	 * 
	 * @param name
	 * @param data
	 * @return
	 */
	public boolean attach(String name, Object... data) {
		ServiceInterface si = Runtime.getService(name);
		return (boolean) invoke("attach", si);
	}
	
	/**
	 * set status broadcasts an information string to any subscribers
	 * @param msg
	 */
	
	private long lastInfo = 0;
	private long lastWarn = 0;
	private long lastError = 0;

	public String lastErrorMsg;
	
	public void info(String msg)
	{
		log.info(msg);
		// can only read "so" fast
		if (System.currentTimeMillis() - lastInfo > 300)
		{
			invoke("publishStatus", "info",  msg);
			lastInfo = System.currentTimeMillis();
		}
	}
	
	public void info(String format, Object...args)
	{
		info(String.format(format,args));
	}
	
	public void warn(String format, Object...args)
	{
		warn(String.format(format,args));
	}
	
	public void error(String format, Object...args)
	{
		error(String.format(format,args));
	}

	public void error(String msg)
	{
		lastErrorMsg = msg;
		log.error(msg);
		if (System.currentTimeMillis() - lastWarn > 300)
		{
			invoke("publishStatus", "error", msg);
			lastWarn = System.currentTimeMillis();
		}
	}
	
	public String getLastError()
	{
		return lastErrorMsg;
	}
	
	public void warn(String msg)
	{
		lastErrorMsg = msg;
		log.error(msg);
		if (System.currentTimeMillis() - lastError > 300)
		{
			invoke("publishStatus", "warn", msg);
			lastError = System.currentTimeMillis();
		}
	}

	public String publishStatus(String status, String msg)
	{
		return String.format("%s %s %s", status, getName(), msg);
	}
	
	public HashSet<String> getMessageSet()
	{
		HashSet<String> ret = new HashSet<String>();
		Method[] methods = getMethods();
		log.info("%s loading %d non-routable methods", getName(), methods.length);
		for (int i = 0; i < methods.length; ++i) {
			ret.add(methods[i].getName());
			log.debug("method {}.{} is not routable", getName(), methods[i].getName());
		}
		
		return ret;
	}
	
	public Method[] getMethods()
	{
		return this.getClass().getMethods();
	}
	

}
