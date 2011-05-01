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
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.comm.CommunicationManager;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.IPAndPort;
import org.myrobotlab.service.data.NameValuePair;

public abstract class Service implements Runnable {

	// TODO - UNDERSTAND THAT host:port IS THE KEY !! - IT IS A COMBONATIONAL
	// KEY :0 WORKS IN PROCESS OUT OF
	// PROCESS ip:port WORKS !
	// host + ":" + servicePort + serviceClass + "/" +
	// this.getClass().getCanonicalName() + "/" + name;
	
	public final static Logger LOG = Logger.getLogger(Service.class.toString());
	protected String host = null; // TODO - should be final???
	public final String name;
	public final String serviceClass;
	protected boolean isRunning = false;
	protected Thread thisThread = null;
	Outbox outbox = null;
	Inbox inbox = null;
	protected CommunicationManager cm = null;
	protected ConfigurationManager cfg = null;
	protected ConfigurationManager hostcfg = null;

	// performance timing
	public long startTimeMilliseconds = 0;

	// public enum MsgHandling {PROCESS, RELAY, IGNORE, BROADCAST,
	// PROCESSANDBROADCAST};

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
		thisThread = new Thread(this, instanceName);

		// determine host name
		host = getHostName(inHost);

		this.name = instanceName;
		this.serviceClass = serviceClass;
		this.inbox = new Inbox(name);
		this.outbox = new Outbox(this);

		// config begin
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
			// load root level configuration
			ConfigurationManager rootcfg = new ConfigurationManager();
			rootcfg.load(host + ".properties");
			rootcfg.save("root.txt");
			hostInitialized = true;
		}
		
//		hostcfg.save(host + ".out.txt");
//		cfg.save(host + ".2.out.txt");		
		
		// service instance level defaults
		loadDefaultConfiguration();		
		
		// over-ride service level with service file
		cfg.load(host + "." + name + ".properties");
//		cfg.save(host + "." + name + ".out.txt");


		// now that cfg is ready make a communication manager
		cm = new CommunicationManager(this);

		registerServices();

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
		if (cfg.getBoolean("performanceTiming")) {
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

	public void setCFG(String name, String value) {
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
		// TODO - how to handle enums ?!?!
		// enum MsgHandling {PROCESS, RELAY, IGNORE, BROADCAST,
		// PROCESSANDBROADCAST};
		// also - 3 different level contexts - allow overrides
		// Global->Service->XMLAdapter
		// TODO - config precedence host:port/serviceName is redundant -
		// serviceName must be unique - a remote service will need host:port/
		// everything is context sensitive - preferably NOT - if NOT then full
		// path is needed however host:port depends on context
		// with local config root = .
		// serviceName = .name
		// without an adapter there is no listening port - what difference would
		// it be if you set host = hostname
		// currently hostkey = ":0" for local - SHOULD IT BE 127.0.0.1:6666 ? or
		// hyperparasite/127.0.0.1:666 ????
		// config would be
		// ":0/xml01.anonymousMsgRequest=RELAY"
		// "127.0.0.1:6666/xml01.anonymousMsgRequest=RELAY"
		// "xml01.anonymousMsgRequest=RELAY" is probably th best
		// cfg.setProperty("outboxMsgHandling", MsgHandling.RELAY);
		// cfg.setProperty("anonymousMsgRequest", MsgHandling.PROCESS);
		cfg.set("outboxMsgHandling", RELAY);
		cfg.set("anonymousMsgRequest", PROCESS);

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
		outbox.interrupt();
		if (thisThread != null) {
			thisThread.interrupt();
		}
		thisThread = null;
	}

	public void startService() // TODO - startService pauseService stopService - also
						// HIDE THIS ! - make runnable
	{
		outbox.start();
		thisThread.start();
		isRunning = true;
	}

	@Override
	public void run() {
		thisThread = Thread.currentThread();
		isRunning = true;

		try {
			while (isRunning) {
				process(getMsg());
			}
		} catch (InterruptedException e) {
			if (thisThread != null) {
				LOG.info(thisThread.getName());
			}
			LOG.info("service INTERRUPTED ");
			isRunning = false;
		}
	}

	/*
	 * process a message - typically it will either invoke a function directly
	 * or invoke a function and create a return msg with return data
	 */
	// public Object invoke(Object object, String method, Object params[]) //
	// TODO - Message carry an array of Params?

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

	@SuppressWarnings("unchecked")
	static public Object getNewInstance(String classname) {
		Class c;
		try {
			c = Class.forName(classname);
			return c.newInstance(); // Dynamically instantiate it
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // Dynamically load the class
		catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	// Used by CommunicationManager to get new instances of Communicators
	@SuppressWarnings("unchecked")
	static public Object getNewInstance(String classname, Service service) {
		Class c;
		try {
			c = Class.forName(classname);
			Constructor mc = c.getConstructor(new Class[] { Service.class });
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
			String boundServiceName, GUIService service) {
		try {
			Object[] params = new Object[2];
			params[0] = boundServiceName;
			params[1] = service;
			Class c;
			c = Class.forName(classname);
			Constructor mc = c.getConstructor(new Class[] { String.class,
					GUIService.class });
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
		if (outbox.notifyList.containsKey(ne.outMethod_.toString())) {
			outbox.notifyList.get(ne.outMethod_.toString()).add(ne);
		} else {
			ArrayList<NotifyEntry> nel = new ArrayList<NotifyEntry>();
			nel.add(ne);
			outbox.notifyList.put(ne.outMethod_.toString(), nel);
		}
		
	}
	
	public void notify(String outMethod, String namedInstance, String inMethod,
			Class[] paramTypes)	 
	{
		NotifyEntry ne = new NotifyEntry();
		ne.outMethod_ = outMethod;
		ne.inMethod_ = inMethod;
		ne.name = namedInstance;
		ne.paramTypes = paramTypes;
		
		notify(ne);
	}

	public void notify(String name, String outAndInMethod) {
		notify(outAndInMethod, name, outAndInMethod, (Class[])null);
	}

	public void notify(String name, String outAndInMethod, Class parameterType) {
		notify(outAndInMethod, name, outAndInMethod, new Class[]{parameterType});
	}
	
	public void notify(String name, String outAndInMethod, Class[] parameterTypes) {
		notify(outAndInMethod, name, outAndInMethod, parameterTypes);
	}

	// convenience boxing functions
	public void notify(String outMethod, String namedInstance, String inMethod) 
	{
		
		notify(outMethod, namedInstance, inMethod, (Class[])null);
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class parameterType1) {
		
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class parameterType1, Class parameterType2) {
		
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class parameterType1, Class parameterType2, Class parameterType3) {
		notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2, parameterType3} );
	}

	public void notify(String outMethod, String namedInstance, String inMethod,
			Class parameterType1, Class parameterType2,
			Class parameterType3, Class parameterType4) {
			notify(outMethod, namedInstance, inMethod, new Class[]{parameterType1, parameterType2, parameterType3, parameterType4} );
	}

	// TODO - change to addListener and removeListener
	public void removeNotify(String serviceName,
			String inOutMethod) 
	{
		removeNotify(inOutMethod, serviceName, inOutMethod, (Class[])null);
	}
	public void removeNotify(String outMethod, String serviceName,
			String inMethod, Class[] paramTypes) 
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
			String inMethod, Class paramTypes)
	{
		
	}

	public void removeNotify() {
		outbox.notifyList.clear();
	}

	public void removeNotify(NotifyEntry ne) {
		if (outbox.notifyList.containsKey(ne.outMethod_.toString())) {
			ArrayList<NotifyEntry> nel = outbox.notifyList.get(ne.outMethod_
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
		if (anonymousMsgRequest == RELAY && msg.name.compareTo("") != 0) {
			LOG.warn("RELAY " + name + " - sending to " + msg.name);
			out(msg);
			return null;
		}

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

	@SuppressWarnings("unchecked")
	public Object invoke(Object object, String method, Object params[]) // TODO
																		// -
																		// Message
																		// carry
																		// an
																		// array
																		// of
																		// Params?
	{

		Object retobj = null;
		Class c;

		try {
			// c = Class.forName(classname); // TODO - test if cached references
			// are faster than lookup
			// Object o = c.newInstance(); // Dynamically instantiate it TODO -
			// if necessary make another static invoke
			c = object.getClass();

			Class[] paramTypes = null;
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
			LOG.error(stack2String(e));
		} catch (NoSuchMethodException e) {
			LOG.error("NoSuchMethodException");
			LOG.error(stack2String(e));
		} catch (IllegalArgumentException e) {
			LOG.error("IllegalArgumentException");
			LOG.error(stack2String(e));
		} catch (IllegalAccessException e) {
			LOG.error("IllegalArgumentException");
			LOG.error(stack2String(e));
		} catch (InvocationTargetException e) {
			LOG.error("InvocationTargetException");
			LOG.error(stack2String(e));
		}

		return retobj;

	}

	public Message getMsg() throws InterruptedException {
		return inbox.getMsg();
	}

	/*
	 * TODO - start javadoc'ing registerService - for every named instance the
	 * service will need to register its capabilities and generate a event for
	 * an Operator - if it exists - so the capabilities are distributed to all
	 * the systems
	 */

	@SuppressWarnings("unchecked")
	public synchronized void registerServices() {
		LOG.debug(name + " registerServices");

		try {

			Class c;
			c = Class.forName(serviceClass);

			// try to get method which has the correct parameter types
			// http://java.sun.com/developer/technicalArticles/ALT/Reflection/
			Method[] methods = c.getDeclaredMethods();
			// register this service
			hostcfg.setServiceEntry(host, name, serviceClass, 0, new Date(),
					this, getToolTip());

			for (int i = 0; i < methods.length; ++i) {
				Method m = methods[i];
				Class[] paramTypes = m.getParameterTypes();
				Class returnType = m.getReturnType();

				/*
				LOG
						.info(returnType + " " + m.getName() + "("
								+ paramTypes.toString()
								+ ") adding as new MethodEntry");
								*/

				// TODO
				// http://host/class/namedInstance(no need)/method/class/data
				// host.namedInstance(Port).class(Type).function.parameter/returnvalue
				// http(s)://host:port/(class?) -
				// namedInstance/method/(class)parameter -- too verbose -
				// namedInstance will specify a class
				// http(s)://host:port/namedInstance/method/(container-
				// metaclass utf-8 encoded string + meta-container)parameter

				// register this services functions
				hostcfg.setMethod(host, name, m.getName(), returnType, paramTypes);
			}
			
			Class[] interfaces = c.getInterfaces();

			for (int i = 0; i < interfaces.length; ++i) {
				Class interfc = interfaces[i];

				LOG.info("adding interface "
						+ interfc.getClass().getCanonicalName());

				hostcfg.setInterface(host, name, interfc.getClass()
						.getCanonicalName());

			}

			// expose Service functions we want accessible
			// hostcfg.setMethod(host, name, "notify", "sink",
			// NotifyEntry.class.getCanonicalName());
			// hostcfg.setMethod(host, name, "removeNotify", "sink",
			// NotifyEntry.class.getCanonicalName());
			// hostcfg.setMethod(host, name, "sethostcfg", "sink",
			// NameValuePair.class.getCanonicalName());
			// hostcfg.setMethod(host, name, "registerServices", "sink",
			// ServiceDirectoryUpdate.class.getCanonicalName());

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
	public void registerServicesNotify() {
		LOG.info("registerServicesNotify");
	}

	// TODO - overload ?!?!?
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
		LOG.error(name + " sendServiceDirectoryUpdate ");
		// LOG.info(sdu);
		for (int i = 0; i < sdu.serviceEntryList_.size(); ++i) {
			ServiceEntry se = sdu.serviceEntryList_.get(i);
			se.host = sdu.remoteHostname; // ***THIS IS WERE WE PUSH THE CORRECT
											// IP & PORT IN - DONT KNOW IF ITS
											// THE RIGHT SPOT***
			se.servicePort = sdu.remoteServicePort;
			LOG.error("registering services from foriegn source " + se.host
					+ ":" + se.servicePort + "/" + se.name);
			hostcfg.setServiceEntry(se);
		}
		// svcDir.mergeForiegnSource(sdu.serviceEntryList_);
		// TODo - implement
		// update local service directory
		// send update of service back - what to filter?????????
		send(name, "registerServicesNotify");
	}

	// TODO - stub out
	public synchronized void removeServices(ServiceDirectoryUpdate sdu) {
	}

	// TODO - THIS IS CRAP !!! REFACTOR !!!!!
	public void sendServiceDirectoryUpdate(String name, String remoteHost,
			int port) {
		sendServiceDirectoryUpdate("", "", name, remoteHost, port, null);
	}

	public void sendServiceDirectoryUpdate(String remoteHost, int port) {
		sendServiceDirectoryUpdate("", "", "", remoteHost, port, null);
	}

	public void sendServiceDirectoryUpdate(String login, String password,
			String name, String remoteHost, int port) {
		sendServiceDirectoryUpdate(login, password, name, remoteHost, port,
				null);
	}

	public void sendServiceDirectoryUpdate(String login, String password,
			String name, String remoteHost, int port, ServiceDirectoryUpdate sdu) {
		LOG.info(name + " sendServiceDirectoryUpdate ");

		if (sdu == null) {
			sdu = new ServiceDirectoryUpdate();

			// DEFAULT SERVICE IS TO SEND THE WHOLE LOCAL LIST - this can be
			// overloaded if you dont want to send everything
			sdu.serviceEntryList_ = hostcfg.getLocalServiceEntries();
			for (int j = 0; j < sdu.serviceEntryList_.size(); ++j) {
				sdu.serviceEntryList_.get(j).localServiceHandle = null; // NULLING
																		// OUT
																		// LOCAL
																		// SERVICE
																		// HANDLE
																		// FOR
																		// TRANSPORT
																		// !
																		// (SHOULD
																		// DO IN
																		// CFG?)
			}
		}

		String dst;
		if (name == null || name.length() == 0) {
			dst = remoteHost;
		} else {
			dst = name;
		}

		sdu.login = login;
		sdu.password = password;

		sdu.hostname = host;
		Message msg = createMessage(dst, "registerServices", sdu);
		msg.msgType = "S"; // Service / System / Process level message - a
							// message which can be processed by any service
							// regardless of name
		// msg.hostname.set(remoteHost);
		// msg.servicePort.set(port);
		out(msg);
	}

	public void listServices(String callBackName) {
		LOG.info(name + " sendServiceDirectoryUpdate ");
		ServiceDirectoryUpdate sdu = new ServiceDirectoryUpdate();

		// setting my data for it
		sdu.hostname = host;
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
			LOG.info("create message " + host + "/*/" + msg.method + "#"
					+ msg.getParameterSignature());
		} else {
			LOG.info("create message " + host + "/" + msg.name + "/"
					+ msg.method + "#" + msg.getParameterSignature());
		}
		return msg;
	}

	/*
	 * TODO - QUERY INTO SERVICES || LINQ ?
	 * 
	 * public void setOperator (String FNName, Operator operator) {
	 * ArrayList<>ServiceEntry ServiceDirectory.getEndPointIndex() setOperator
	 * (this.name, FNName, operator.name); }
	 */

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

		return null;
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

	// TODO - move to string or error util
	 public static String stack2String(Exception e) {
		  try {
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    return "------\r\n" + sw.toString() + "------\r\n";
		  }
		  catch(Exception e2) {
		    return "bad stack2string";
		  }
		 }	
}
