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
import java.util.SimpleTimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.comm.CommunicationManager;
import org.myrobotlab.service.OpenCV.FilterWrapper;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.data.NameValuePair;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.GUI;
import org.simpleframework.xml.Element;

public abstract class Service implements Runnable, Serializable {

	// TODO - UNDERSTAND THAT host:port IS THE KEY !! - IT IS A COMBONATIONAL
	// KEY :0 WORKS IN PROCESS OUT OF
	// PROCESS ip:port WORKS !
	// host + ":" + servicePort + serviceClass + "/" +
	// this.getClass().getCanonicalName() + "/" + name;
	
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(Service.class.toString());
	protected String host = null; // TODO - should be final???
	@Element
	public final String name;
	public final String serviceClass; // TODO - remove
	protected boolean isRunning = false;
	protected transient Thread thisThread = null;
	Outbox outbox = null;
	Inbox inbox = null;
	public URL url = null;
	
	boolean performanceTiming = false;
	
	protected CommunicationInterface cm = null;
	protected ConfigurationManager cfg = null;
	protected ConfigurationManager hostcfg = null;

	// performance timing
	public long startTimeMilliseconds = 0;

	// TODO - use enumeration
	static public final String PROCESS = "PROCESS";
	static public final String RELAY = "RELAY";
	static public final String IGNORE = "IGNORE";
	static public final String BROADCAST = "BROADCAST";
	static public final String PROCESSANDBROADCAST = "PROCESSANDBROADCAST";

	public String anonymousMsgRequest = PROCESS;
	public String outboxMsgHandling = RELAY;
	
	private static boolean hostInitialized = false;
	
	abstract public String getToolTip();

	public Service(String instanceName, String serviceClass) {
		this(instanceName, serviceClass, null);
	}

	public Service(String instanceName, String serviceClass, String inHost) {
		
		if (inHost != null)
		{
			try {
				url = new URL(inHost);
			} catch (MalformedURLException e) {
				LOG.error(inHost + " not a valid URL");
			}
		}
		// determine host name
		host = getHostName(inHost);
		
		this.name = instanceName;
		this.serviceClass = serviceClass;
		this.inbox = new Inbox(name);
		this.outbox = new Outbox(this);

		// config begin - for chumby
		hostcfg = new ConfigurationManager(host);
		cfg = new ConfigurationManager(host, name); 

		// global defaults begin - multiple services will re-set defaults
		loadGlobalMachineDefaults();

		// service instance defaults
		loadServiceDefaultConfiguration();

		// TODO - if a gui is involved you may want to prompt

		// over-ride process level with host file
		if (!hostInitialized)
		{
			
			String libararyPath = System.getProperty("java.library.path");
			String userDir = System.getProperty("user.dir");

			LOG.info("os.name [" + System.getProperty("os.name") + "]");
			LOG.info("os.version [" + System.getProperty("os.version") + "]");
			LOG.info("os.arch [" + System.getProperty("os.arch") + "]");
			LOG.info("java.class.path [" + System.getProperty("java.class.path") + "]");
			LOG.info("java.library.path [" + libararyPath + "]");
			LOG.info("user.dir [" + userDir + "]");
			
			// load root level configuration
			ConfigurationManager rootcfg = new ConfigurationManager();
			rootcfg.load(host + ".properties");
			hostInitialized = true;
		}
		
		// service instance level defaults
		loadDefaultConfiguration();		
		
		// over-ride service level with service file
		cfg.load(host + "." + name + ".properties");

		// now that cfg is ready make a communication manager
		cm = new CommunicationManager(this);

		registerServices();
		registerServices2(url);

	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean isReady() {
		return true;
	}

	public void logTime(String tag) // TODO - this should be a library function
									// service.util.PerformanceTimer()
	{
		if (startTimeMilliseconds == 0) {
			startTimeMilliseconds = System.currentTimeMillis();
		}
		if (performanceTiming) {
			LOG.info("performance clock :"
					+ (System.currentTimeMillis() - startTimeMilliseconds
							+ " ms " + tag));
		}
	}

	/*
	 * setCFG a Service level accessor for remote messages to change
	 * configuration of foreign services.
	 */
	public ConfigurationManager getCFG() {
		return cfg;
	}

	public ConfigurationManager getHostCFG() {
		return hostcfg;
	}

	public String getCFG(String name) {
		return cfg.get(name);
	}

	public void setCFG(NameValuePair mvp) {
		cfg.set(mvp.name.toString(), mvp.value.toString());
	}

	// TODO - should be Object value? - would that mean all clients need to cast?
	public void setCFG(String name, String value) {
		cfg.set(name, value);
	}

	public void setCFG(String name, Integer value) {
		cfg.set(name, value);
	}
	
	public void loadGlobalMachineDefaults() {
		// create root configuration
		ConfigurationManager hostCFG = new ConfigurationManager(host);
		// add global config
		hostCFG.set("servicePort", 3389);
		hostCFG.set("Communicator",
				"org.myrobotlab.comm.CommObjectStreamOverTCPUDP");
		hostCFG.set("Serializer", "org.myrobotlab.comm.SerializerObject");
	}

	public void loadServiceDefaultConfiguration() {
		cfg.set("outboxMsgHandling", RELAY);
		cfg.set("anonymousMsgRequest", PROCESS);

		cfg.set("hideMethods/main", "");
		cfg.set("hideMethods/loadDefaultConfiguration", "");
		cfg.set("hideMethods/getToolTip", "");
		cfg.set("hideMethods/run", "");
		cfg.set("hideMethods/access$0", ""); // TODO - Lame inner class slop - this should be fixed at the source

		outboxMsgHandling = cfg.get("outboxMsgHandling");
		anonymousMsgRequest = cfg.get("anonymousMsgRequest");
	}

	abstract public void loadDefaultConfiguration();

	public void pause(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void stopService() {
		isRunning = false;
		outbox.stop();
		if (thisThread != null) {
			thisThread.interrupt();
		}
		thisThread = null;
	}
	
	public void releaseService()
	{
		// note - if stopService is overwritten with extra 
		// threads - releaseService will need to be overwritten too
		stopService(); 
		RuntimeEnvironment.unregister(url, name);
	}

	public void startService() // TODO - startService pauseService stopService - also
						// HIDE THIS ! - make runnable
	{
		outbox.start();
		if (thisThread == null)
		{
			thisThread = new Thread(this, name); 
		}
		thisThread.start();
		isRunning = true;
	}

	// override for extended functionality
	public boolean preRoutingHook(Message m)
	{
		return true;
	}

	// override for extended functionality
	public boolean preProcessHook(Message m)
	{
		return true;
	}
	
	
	@Override
	final public void run() {
		isRunning = true;

		try {
			while (isRunning) {
				
				Message m = getMsg();

				if (!preRoutingHook(m)) {continue;}
				
				// route if necessary
				if (!m.name.equals(this.name)) // && RELAY
				{
					outbox.add(m); // RELAYING
				}

				if (!preProcessHook(m)) {continue;}
				
				Object ret = invoke(m);
				if (m.status.compareTo(Message.BLOCKING) == 0) {
					// create new message reverse sender and name
					// set to same msg id
					Message msg = createMessage(m.sender, m.method, ret);
					msg.sender = this.name;
					msg.msgID = m.msgID;
					// msg.status = Message.BLOCKING;
					msg.status = Message.RETURN;
					
					outbox.add(msg);
				}
				
				
			}
		} catch (InterruptedException e) {
			if (thisThread != null) {
				LOG.warn(thisThread.getName());
			}
			LOG.warn("service INTERRUPTED ");
			isRunning = false;
		}
	}

	/*
	 * process a message - typically it will either invoke a function directly
	 * or invoke a function and create a return msg with return data
	 */
/*	Depricated with Hooks
	public void process(Message m) {
		Object ret = invoke(m);
		if (m.status.compareTo(Message.BLOCKING) == 0) {
			// create new message reverse sender and name
			// set to same msg id
			Message msg = createMessage(m.sender, m.method, ret);
			msg.sender = this.name;
			msg.msgID = m.msgID;
			// msg.status = Message.BLOCKING;
			msg.status = Message.RETURN;

			// Thread.sleep(arg0)
			outbox.add(msg);

		}
	}
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
			m.sender = this.name;
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
			LOG.error(stackToString(e));
		} catch (InstantiationException e) {
			LOG.error(stackToString(e));
		} catch (IllegalAccessException e) {
			LOG.error(stackToString(e));
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
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static public Object getNewInstance(String classname,
			String boundServiceName, GUI service) {
		try {
			Object[] params = new Object[2];
			params[0] = boundServiceName;
			params[1] = service;
			Class c;
			c = Class.forName(classname);
			Constructor mc = c.getConstructor(new Class[] { String.class,
					GUI.class });
			return mc.newInstance(params);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static public Object getNewInstance(String classname, String param) {
		Object params[] = null;
		if (param != null) {
			params = new Object[1];
			params[0] = param;
		}

		Class c;
		try {
			c = Class.forName(classname);
			Constructor mc = c.getConstructor(new Class[] { param.getClass() });
			return mc.newInstance(params); // Dynamically instantiate it

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	// TODO - make this the custom call
	@SuppressWarnings("unchecked")
	static public Object getNewInstance(String classname, Object[] param) {
		Class c;
		try {
			c = Class.forName(classname);
			Class[] paramTypes = new Class[param.length];
			for (int i = 0; i < param.length; ++i) {
				paramTypes[i] = param[i].getClass();
			}
			Constructor mc = c.getConstructor(paramTypes);
			return mc.newInstance(param); // Dynamically instantiate it

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
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
	public void notify(NotifyEntry ne) {
		// TODO - this would have to change if signature was used
		if (outbox.notifyList.containsKey(ne.outMethod_.toString())) {
			outbox.notifyList.get(ne.outMethod_.toString()).add(ne);
		} else {
			ArrayList<NotifyEntry> nel = new ArrayList<NotifyEntry>();
			nel.add(ne);
			outbox.notifyList.put(ne.outMethod_.toString(), nel);
		}
	}
*/
	// parameterType is not used for any critical look-up - but can be used at
	// runtime check to
	// check parameter mating

	// this is called from video widget sendNotifyRequest
	public void notify(NotifyEntry ne)	 
	{
		if (outbox.notifyList.containsKey(ne.outMethod.toString())) {
			// iterate through all looking for duplicate
			boolean found = false;
			ArrayList<NotifyEntry> nes = outbox.notifyList.get(ne.outMethod.toString());
			for (int i = 0; i < nes.size(); ++i)
			{
				NotifyEntry entry = nes.get(i);
				if (entry.equals(ne))
				{
					LOG.warn("attempting to add duplicate NotifyEntry " + ne);
					found = true;
					break;
				}
			}
			if (!found)
			{
				LOG.info("adding notify from " + this.name + "." + ne.outMethod + " to " + ne.name + "." + ne.inMethod);
				nes.add(ne);
			}
		} else {
			ArrayList<NotifyEntry> nel = new ArrayList<NotifyEntry>();
			nel.add(ne);
			LOG.info("adding notify from " + this.name + "." + ne.outMethod + " to " + ne.name + "." + ne.inMethod);
			outbox.notifyList.put(ne.outMethod.toString(), nel);
		}
		
	}
	
	public void notify(String outMethod, String namedInstance, String inMethod,
			Class<?>[] paramTypes)	 
	{
		NotifyEntry ne = new NotifyEntry(outMethod, namedInstance, inMethod, paramTypes);
		notify(ne);
	}

	public void notify(String name, String outAndInMethod) {
		notify(outAndInMethod, name, outAndInMethod, (Class<?>[])null);
	}

	public void notify(String name, String outAndInMethod, Class<?> parameterType) {
		notify(outAndInMethod, name, outAndInMethod, new Class[]{parameterType});
	}
	
	public void notify(String name, String outAndInMethod, Class<?>[] parameterTypes) {
		notify(outAndInMethod, name, outAndInMethod, parameterTypes);
	}

	// convenience boxing functions
	public void notify(String outMethod, String namedInstance, String inMethod) 
	{
		
		notify(outMethod, namedInstance, inMethod, (Class[])null);
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class<?> parameterType1) {
		
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class<?> parameterType1, Class<?> parameterType2) {
		
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class<?> parameterType1, Class<?> parameterType2, Class<?> parameterType3) {
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2, parameterType3} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class<?> parameterType1, Class<?> parameterType2,
			Class<?> parameterType3, Class<?> parameterType4) {
			notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2, parameterType3, parameterType4} );
	}

	// TODO - change to addListener and removeListener
	public void removeNotify(String serviceName,
			String inOutMethod) 
	{
		removeNotify(inOutMethod, serviceName, inOutMethod, (Class[])null);
	}
	public void removeNotify(String outMethod, String serviceName,
			String inMethod, Class<?>[] paramTypes) 
	{
		
		if (outbox.notifyList.containsKey(outMethod)) {
			ArrayList<NotifyEntry> nel = outbox.notifyList.get(outMethod);
			for (int i = 0; i < nel.size(); ++i) {
				NotifyEntry target = nel.get(i);
				if (target.name.compareTo(serviceName) == 0) {
					nel.remove(i);
				}
			}
		} else {
			LOG.error("removeNotify requested " + serviceName + "." + outMethod
					+ " to be removed - but does not exist");
		}
		
	}
	
	public void removeNotify(String outMethod, String serviceName,
			String inMethod, Class<?> paramTypes)
	{
		
	}

	public void removeNotify() {
		outbox.notifyList.clear();
	}

	public void removeNotify(NotifyEntry ne) {
		if (outbox.notifyList.containsKey(ne.outMethod.toString())) {
			ArrayList<NotifyEntry> nel = outbox.notifyList.get(ne.outMethod
					.toString());
			for (int i = 0; i < nel.size(); ++i) {
				NotifyEntry target = nel.get(i);
				if (target.name.compareTo(ne.name) == 0) {
					nel.remove(i);
				}
			}
		} else {
			LOG.error("removeNotify requested " + ne
					+ " to be removed - but does not exist");
		}

	}

	// TODO - refactor / remove
	public Object invoke(Message msg) {

		Object retobj = null;

		LOG.info("***" + name + " msgid " + msg.msgID + " invoking "
				+ msg.method + " (" + msg.getParameterSignature() + ")***");

		// TODO - good stuff here ! - e.g
		// if msg not for me what do i do ? RELAY <- this could make a proxy
		// serve
		// if recipient is anonymous what do i do ?
		// if (anonymousMsgRequest == MsgHandling.RELAY &&
		// msg.name.compareTo(name) != 0) {
		// cfg.setIfEmpty(name + ".anonymousMsgRequest", "PROCESS");
		// TODO - strange place to put it ... it should be in Process inbox=getMsg(); if !me && RELAY send to outbox
		
		/* SHOULD BE CONTROLLED WITH HOOKS - DEPRICATING
		if (anonymousMsgRequest == RELAY && msg.name.compareTo("") != 0) {
			LOG.warn("RELAY " + name + " - sending to " + msg.name);
			out(msg);
			return null;
		}
		*/

		retobj = invoke(msg.method, msg.data);

		// TODO - look for sendBlocking - then send call back obj to sender

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
	public Object invoke(String method, Object param1, Object param2,
			Object param3) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;

		return invoke(method, params);
	}

	// convenience reflection methods 4 parameters
	public Object invoke(String method, Object param1, Object param2,
			Object param3, Object param4) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;
		params[3] = param4;

		return invoke(method, params);
	}

	public Object invoke(String method, Object[] params) // TODO remove -
															// useless - but
															// somehow being
															// used
	{
		return invoke(this, method, params);
	}

	public Object invoke(Object object, String method, Object params[]) 
	{

		Object retobj = null;
		Class<?> c;

		try {
			// c = Class.forName(classname); // TODO - test if cached references
			// are faster than lookup
			c = object.getClass();

			Class<?>[] paramTypes = null;
			if (params != null) {
				paramTypes = new Class[params.length]; // this part is weak
				for (int i = 0; i < params.length; ++i) {
					if (params[i] != null) {
						paramTypes[i] = params[i].getClass();
					} else {
						paramTypes[i] = null;
					}
				}
			}

			// log invoking call
			if (Logger.getRootLogger().getLevel() == Level.DEBUG) {
				String paramTypeString = "";
				if (params != null) {
					for (int i = 0; i < params.length; ++i) {
						paramTypeString += params[i].getClass()
								.getCanonicalName();
						if (params.length != i + 1) {
							paramTypeString += ",";
						}
					}
				} else {
					paramTypeString = "null";
				}
				LOG.debug("****invoking " + host + "/" + c.getCanonicalName()
						+ "." + method + "(" + paramTypeString + ")****");
			}

			Method meth = c.getMethod(method, paramTypes);

			retobj = meth.invoke(object, params);

			out(method, retobj);

		} catch (SecurityException e) {
			LOG.error("SecurityException");
			LOG.error(stackToString(e));
		} catch (NoSuchMethodException e) {
			LOG.error("NoSuchMethodException");
			LOG.error(stackToString(e));
		} catch (IllegalArgumentException e) {
			LOG.error("IllegalArgumentException");
			LOG.error(stackToString(e));
		} catch (IllegalAccessException e) {
			LOG.error("IllegalArgumentException");
			LOG.error(stackToString(e));
		} catch (InvocationTargetException e) {
			LOG.error("InvocationTargetException");
			LOG.error(stackToString(e));
		}

		return retobj;

	}

	public Message getMsg() throws InterruptedException {
		return inbox.getMsg();
	}

	public void listServices(String callBackName) {
		LOG.info(name + " sendServiceDirectoryUpdate ");
		ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();

		// setting my data for it
		//sdu.hostname = host;
		// sdu.servicePort.set(servicePort);
		// sdu.serviceEntryList_ = svcDir.getLocal();
		// TODO - implement

		Message msg = createMessage(callBackName, "registerServices", sdu);
		out(msg);

	}

	/*
	 * send takes a name of a target system - the method - and a list of
	 * parameters and invokes that method at its destination.
	 */

	// BOXING - BEGIN --------------------------------------
	
	// this send forces remote connect - for registering services 
	public void send(URL url, String method, Object param1)
	{
		Object[] params = new Object[1];
		params[0] = param1;
		Message msg = createMessage(name, method, params);
		outbox.getCommunicationManager().send(url, msg);
	}
	
	
	public void send(String name, String method, Object param1, Object param2,
			Object param3, Object param4) {
		Object[] params = new Object[4];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;
		params[3] = param4;
		Message msg = createMessage(name, method, params);
		msg.sender = this.name;
		outbox.add(msg);
	}
	
	public void send(String name, String method, Object param1, Object param2,
			Object param3) {
		Object[] params = new Object[3];
		params[0] = param1;
		params[1] = param2;
		params[2] = param3;
		Message msg = createMessage(name, method, params);
		msg.sender = this.name;
		outbox.add(msg);
	}

	public void send(String name, String method) {
		Message msg = createMessage(name, method, null);
		msg.sender = this.name;
		outbox.add(msg);
	}

	public void send(String name, String method, Object param1) {
		Message msg = createMessage(name, method, param1);
		msg.sender = this.name;
		outbox.add(msg);
	}

	public void send(String name, String method, Object param1, Object param2) {
		Object[] params = new Object[2];
		params[0] = param1;
		params[1] = param2;
		Message msg = createMessage(name, method, params);
		msg.sender = this.name;
		outbox.add(msg);
	}

	public void send(String name, String method, Object[] data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.name;
		outbox.add(msg);
	}
	
	// BOXING - End --------------------------------------
	

	public Object sendBlocking(String name, String method, Object[] data) {
		Message msg = createMessage(name, method, data);
		msg.sender = this.name;
		msg.status = Message.BLOCKING;

		Object[] returnContainer = new Object[1];
		/*
		 * if (inbox.blockingList.contains(msg.msgID)) { LOG.error("DUPLICATE");
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
			e.printStackTrace();
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

		msg.sender = this.name;
		if (msg.msgID.length() == 0) {
			msg.msgID = formatter.format(d);
		}
		msg.timeStamp = formatter.format(d);
		if (name != null) {
			msg.name = name; // destination instance name
		}
		msg.data = data;
		msg.method = method;
		msg.encoding = "NONE";// TODO - should be Option value

		if (msg.name.length() == 0) {
			LOG.debug("create message " + host + "/*/" + msg.method + "#"
					+ msg.getParameterSignature());
		} else {
			LOG.debug("create message " + host + "/" + msg.name + "/"
					+ msg.method + "#" + msg.getParameterSignature());
		}
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
			LOG.error("could not find host, host is null or empty !");
		}

		return "localhost"; // no network - still can't be null // chumby
	}

	// connection publish points - begin ---------------
	public IPAndPort noConnection(IPAndPort conn) {
		LOG.error("could not connect to " + conn.IPAddress + ":" + conn.port);
		return conn;
	}

	public IPAndPort connectionBroken(IPAndPort conn) {
		LOG.error("the connection " + conn.IPAddress + ":" + conn.port
				+ " has been broken");
		return conn;
	}
	// connection publish points - end ---------------

	@SuppressWarnings("unchecked")
	public static String getMethodToolTip (String className, String methodName, Class[] params)
	{
		
		try {
		
			Class c = Class.forName(className);
	
			Method m;
			m = c.getMethod(methodName, params);
	
			ToolTip tip = m.getAnnotation(ToolTip.class);
		
		if (tip != null)
		{
			return tip.value();
		}
		
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
		
	}
	
	public CommunicationInterface getComm()
	{
		return cm;
	}
	
	 public final static String stackToString(final Exception e) {
		  try {
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    return "------\r\n" + sw.toString() + "------\r\n";
		  }
		  catch(Exception e2) {
		    return "bad stackToString";
		  }
	}
	 
	public final static void logException(final Exception e) {
		LOG.error(stackToString(e));
	}

	public static void copyDataFrom(Object target, Object source) {
		if (target == source) { // data is myself - operating on local copy
			return;
		}

		Class<?> sourceClass = source.getClass();
		Class<?> targetClass = target.getClass();
		Field fields[] = sourceClass.getDeclaredFields();
		for (int j = 0, m = fields.length; j < m; j++) {
			try {
				Field f = fields[j];

				if (Modifier.isPublic(f.getModifiers())
						&& !(f.getName().equals("LOG"))
						&& !Modifier.isTransient(f.getModifiers())) {

					Type t = f.getType();

					// LOG.info(Modifier.toString(f.getModifiers()));
					// f.isAccessible()

					LOG.info("setting " + f.getName());
					if (t.equals(java.lang.Boolean.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setBoolean(
								target, f.getBoolean(source));
					} else if (t.equals(java.lang.Character.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setChar(
								target, f.getChar(source));
					} else if (t.equals(java.lang.Byte.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setByte(
								target, f.getByte(source));
					} else if (t.equals(java.lang.Short.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setShort(
								target, f.getShort(source));
					} else if (t.equals(java.lang.Integer.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setInt(
								target, f.getInt(source));
					} else if (t.equals(java.lang.Long.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setLong(
								target, f.getLong(source));
					} else if (t.equals(java.lang.Float.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setFloat(
								target, f.getFloat(source));
					} else if (t.equals(java.lang.Double.TYPE)) {
						targetClass.getDeclaredField(f.getName()).setDouble(
								target, f.getDouble(source));
					} else {
						LOG.info("cloning object " + f.getName());
						targetClass.getDeclaredField(f.getName()).set(target,
								f.get(source));
					}

				}
			} catch (Exception e) {
				Service.logException(e);
			}
			// System.out.println(names[i] + ", " + fields[j].getName() + ", "
			// + fields[j].getType().getName() + ", " +
			// Modifier.toString(fields[j].getModifiers()));
		}

	}
	
	public Service publishServiceData(String name)
	{
		/*
		if (filters.containsKey(name)) {
			return new FilterWrapper(name, filters.get(name));
		} else {
			LOG.error("setFilterData " + name + " does not exist");
		}
		*/
		
		return null;
	}
	
	public void setServiceData (FilterWrapper filterData)
	{
		/*
		if (filters.containsKey(filterData.name)) {
			Service.copyDataFrom(filters.get(filterData.name), filterData.filter);
		} else {
			LOG.error("setFilterData " + filterData.name + " does not exist");
		}
		*/
		
	}
	
	public  void registerServices(String hostAddress, int port, Message msg) 
	{
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
			
			LOG.info(name + " recieved service directory update from " + sdu.remoteURL);
	
			if (RuntimeEnvironment.register(sdu.remoteURL, sdu.serviceEnvironment))
			{
				ServiceDirectoryUpdate echoLocal = new ServiceDirectoryUpdate();
				echoLocal.remoteURL = sdu.url;
				echoLocal.serviceEnvironment = RuntimeEnvironment.getLocalServices();
				
				send (msg.sender, "registerServices", echoLocal); // broadcast to all
			}
		
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public synchronized void registerServices2(URL host) {
		RuntimeEnvironment.register(host, this); // problem with this in it does not broadcast
	}
	
	public synchronized void registerServices() {
		LOG.debug(name + " registerServices");

		try {

			Class<?> c;
			c = Class.forName(serviceClass);

			HashMap<String, Object> hideMethods = cfg.getMap("hideMethods");

			
			// try to get method which has the correct parameter types
			// http://java.sun.com/developer/technicalArticles/ALT/Reflection/
			Method[] methods = c.getDeclaredMethods();
			// register this service
			hostcfg.setServiceEntry(host, name, serviceClass, 0, new Date(),
					this, getToolTip());

			for (int i = 0; i < methods.length; ++i) {
				Method m = methods[i];
				Class<?>[] paramTypes = m.getParameterTypes();
				Class<?> returnType = m.getReturnType();

				
				if (!hideMethods.containsKey(m.getName()))
				{
					// service level
					hostcfg.setMethod(host, name, m.getName(), returnType, paramTypes);
				}
			}
			
			Class<?>[] interfaces = c.getInterfaces();

			for (int i = 0; i < interfaces.length; ++i) {
				Class<?> interfc = interfaces[i];

				LOG.info("adding interface "
						+ interfc.getCanonicalName());

				hostcfg.setInterface(host, name, interfc.getClass()
						.getCanonicalName());

			}

			Type[] intfs = c.getGenericInterfaces();
			for (int j = 0; j < intfs.length; ++j) {
				Type t = intfs[j];
				String nameOnly = t.toString().substring(
						t.toString().indexOf(" ") + 1);
				hostcfg.setInterface(host, name, nameOnly);
			}

			// hostcfg.save("cfg.txt");

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// send(name, "registerServicesNotify");

	}

	// this event comes after services have been changed
	//public void registerServicesNotify() {
	//	LOG.info("registerServicesNotify");
	//}

	// TODO - overload ?!?!?
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
		LOG.error(name + " sendServiceDirectoryUpdate ");

		// TODO - GET ENV FROM service name - get ip & port
		/*
		for (int i = 0; i < sdu.serviceEntryList_.size(); ++i) {
			ServiceEntry se = sdu.serviceEntryList_.get(i);
			se.host = sdu.remoteHostname; 
			se.servicePort = sdu.remoteServicePort;
			LOG.error("registering services from foriegn source " + se.host
					+ ":" + se.servicePort + "/" + se.name);
			hostcfg.setServiceEntry(se);
		}
		*/

		send(name, "registerServicesNotify");
	}

	// TODO - stub out
	//public synchronized void removeServices(ServiceDirectoryUpdate sdu) {
	//}

	/*
	 *  sending a registerServices to an unknown process id
	 */

	public void sendServiceDirectoryUpdate(String login, String password, String name, String remoteHost, 
			int port, ServiceDirectoryUpdate sdu) {
		LOG.info(name + " sendServiceDirectoryUpdate ");

		StringBuffer urlstr = new StringBuffer();
		
		urlstr.append("http://"); // TODO - extend URL into something which can handle socket:// protocol
		
		if (login != null && login.length() > 0)
		{
			urlstr.append(login);
			urlstr.append(":");
			urlstr.append(password);
			urlstr.append("@");
		}
		
		InetAddress inetAddress = null;
		
		try {
			//InetAddress inetAddress = InetAddress.getByName("208.29.194.106");
			inetAddress = InetAddress.getByName(remoteHost);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		urlstr.append(inetAddress.getHostAddress());
		urlstr.append(":");
		urlstr.append(port);
		
		URL remoteURL = null;
		try {
			remoteURL = new URL(urlstr.toString());
		} catch (MalformedURLException e) {
			LOG.error(Service.stackToString(e));
			return;
		}
		
		if (sdu == null) {
			sdu = new ServiceDirectoryUpdate();
			

			// DEFAULT SERVICE IS TO SEND THE WHOLE LOCAL LIST - this can be
			// overloaded if you dont want to send everything
			sdu.serviceEnvironment = RuntimeEnvironment.getLocalServices();
		}

		sdu.remoteURL = remoteURL;
		sdu.url = url;
		
		send(remoteURL, "registerServices", sdu);

	}


}
