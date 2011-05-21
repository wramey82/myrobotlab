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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * ConfigurationManager OUTERMAP INNERMAP ??
 * 
 * Purpose: Manage and maintain all configuration for the application. A Tree
 * like structure which is thread safe.
 * <p>
 * Similar to a registry - you must set the prefix/root node then all
 * manipulations set() get() replace() remove() are relative to the root node.
 * <p>
 * load() save() are modified - under config - filenames will denote root
 * location ie. hyperparasite_6666_com.myrobotLab.service.Webcam will be the
 * root prefix for all config in the file
 * <p>
 * setRoot is a special key which works like a registry entry - so a file name
 * without context can be used to load all configuration
 * <p>
 * 
 * <pre>
 *  example 1:
 *  somefile.txt:
 *  setRoot=[hyperparasite_6666_com.myrobotLab.service.Webcam]
 *  width=320
 *  height=240
 *  
 *  example 2:
 *  is equivalent to somefile.txt:  
 *  hyperparasite_6666_com.myrobotLab.service.Webcam_width=320
 *  hyperparasite_6666_com.myrobotLab.service.Webcam_height=240
 *  
 *  example 3:
 *  which is equivalent to file hyperparasite_6666_com.myrobotLab.service.Webcam.txt: 
 *  width=320
 *  height=240
 * </pre>
 * <p>
 * TODO:
 * <ul>
 * <li>set - needs to behave like the original Properties.set - and replace all
 * values - add can append
 * <li>add - can set if empty - and append if duplicated
 * <li>set (name, value, index) - if index exists replaces - if not returns
 * false
 * <li>bug, since load() uses java.util.Properties (cheesey) load() does not
 * support multiple keys
 * <li>multiple setRoot are not supported currently. In example 1 multiple roots
 * can not be specified.
 * <li>do your own parsing
 * <li>comments from load() not supported either
 * <li>give an example of use
 * <li>support comment
 * <li>TEST modification of added values - all elements SHOULD BE COPIES in add
 * replace functions
 * <li>conceptualize ArrayList and HashMap - and determine how format of storage
 * would be and - what indicators there would be to change the data into a
 * HashMap or ArrayList
 * </ul>
 * <p>
 * See Also:
 * <p>
 * http://kickjava.com/src/java/util/Properties.java.htm <br>
 * http://www.ibm.com/developerworks/java/library/j-jtp07233.html<br>
 * http://java.sun.com/docs/books/tutorial/essential/environment/properties.html
 * <br>
 * 
 * @author greg perry
 * @version %I%, %G%
 * 
 */

public class ConfigurationManager implements Serializable {
	static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(ConfigurationManager.class.toString());
	public final static String PATH_DELIMETER = "/";
	public final static String ELEMENT_DELIMETER = ",";

	public enum OutputFormat {
		PROPERTIES, XML
	}

	final String host;
	final String serviceName;
	final String serviceRoot;
	static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> data = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();
	static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> dataPrevious = null;
	// boolean throwIfEmpty = false;
	boolean throwable = false;

	public class CFGError extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public CFGError(String s) {
			super(s);
		}
	}

	public ConfigurationManager() {
		this(null, null);
	}

	public ConfigurationManager(String host) {
		this(host, null);
	}

	public ConfigurationManager(String host, String serviceName) {
		if (host == null || host == "") {
			host = "";
			// LOG.warn("setting host to \"\" is not recommended");
		}

		if (serviceName == null || serviceName == "") {
			serviceName = "";
			// LOG.warn("setting serviceName to \"\" is not recommended");
		}

		this.host = host;
		this.serviceName = serviceName;
		if (host == "" || serviceName == "") {
			serviceRoot = host + serviceName;
		} else {
			serviceRoot = host + PATH_DELIMETER + serviceName;
		}
	}

	public void clear() {
		dataPrevious = data;
		data = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();
	}

	public String getRoot() {
		return host;
	}

	// TODO - refactor and remove - there is only 1 root
	public String getServiceRoot() {
		return serviceRoot;
	}

	public String getServiceName() {
		return serviceName;
	}

	/***
	 * All functions at this point are "Service" related - ie they operate at a
	 * named instance level To access host/process level config - a new set of
	 * accessors need to be created. To accessors for Service and Methods have
	 * already been defined
	 */

	public boolean containsKey(String name) {
		// String key = (host == null)? name : host + PATH_DELIMETER + name;
		if (name == null)
			return false;

		if (!data.containsKey(serviceRoot)) {
			return false;
		}
		ConcurrentHashMap<String, Object> p = data.get(serviceRoot);
		return p.containsKey(name);
	}

	/*
	 * public void climb (String branch) { if (host.length() > 0) { host +=
	 * PATH_DELIMETER + branch; } else { host = branch; } }
	 * 
	 * public boolean drop () { if (host.indexOf(PATH_DELIMETER) != -1) {
	 * this.host = host.substring(0, host.lastIndexOf(PATH_DELIMETER)); return
	 * true; } else { return false; } }
	 * 
	 * public boolean drop (String s) { int pos =
	 * host.lastIndexOf(PATH_DELIMETER + s + PATH_DELIMETER); if (pos != -1) {
	 * this.host = host.substring(0, pos + s.length() +
	 * PATH_DELIMETER.length()); return true; } else { return false; } }
	 * 
	 * public boolean dropToRoot () { if (host.indexOf(PATH_DELIMETER) != -1) {
	 * this.host = host.substring(0, host.indexOf(PATH_DELIMETER)); return true;
	 * } else { return false; }
	 * 
	 * }
	 */

	// branch size
	public int size() {
		return data.size();
	}

	public int size(String name) {
		String key = serviceRoot + PATH_DELIMETER + name;
		if (!data.containsKey(key)) {
			return 0;
		}

		return data.get(key).size();
	}

	public String[] split(String name) {
		return split(name, null);
	}

	public String[] split(String name, String defaultValue) {
		String s = get(name, defaultValue);

		return s.split(ELEMENT_DELIMETER);

	}

	public HashMap<String, Object> getMap(final String name) {
		// concatenate the current host and path past in
		String fullKey = serviceRoot + PATH_DELIMETER + name;

		// remove a leading / a pos 0 if it exists
		if (fullKey.charAt(0) == '/') {
			fullKey = fullKey.substring(1);
		}

		if (data.containsKey(fullKey)) {
			HashMap<String, Object> ret = new HashMap<String, Object>();
			ret.putAll(data.get(fullKey));
			return ret;
		}

		return null;

	}

	/***
	 * Master get - all gets process through this one
	 */

	public Object get(final String name, final Object defaultValue) {
		// concatenate the current host and path past in
		String fullKey = serviceRoot + PATH_DELIMETER + name;

		// remove a leading / a pos 0 if it exists
		if (fullKey.charAt(0) == '/') {
			fullKey = fullKey.substring(1);
		}

		String outerKey;
		String innerKey;

		// break the fullkey into a outerKey + innerKey
		int outerKeyPos = fullKey.lastIndexOf(PATH_DELIMETER);
		if (outerKeyPos != -1) {
			outerKey = fullKey.substring(0, outerKeyPos);
			innerKey = fullKey.substring(outerKeyPos + 1);
		} else {
			outerKey = fullKey;
			innerKey = "";
		}

		if (data.containsKey(outerKey)) {
			ConcurrentHashMap<String, Object> p = data.get(outerKey);
			if (p.containsKey(innerKey)) {
				return p.get(innerKey);
			}

		}

		return defaultValue;

	}

	/***
	 * get - String interface. 2nd Master get calls the object get but all other
	 * getTypes are processed through this function
	 * 
	 * @param path
	 * @param defaultValue
	 *            - default value supplied
	 * @return String
	 */
	public String get(final String path, final String defaultValue) {
		return (String) get(path, (Object) defaultValue);
		// split host off of name
	}

	/***
	 * Function get(name) - based on no default - better be set previously
	 * getInt getFloat getBoolean get(String) getObject uses the 2nd master
	 * string interface
	 * 
	 * Data store will be String unless forced to Object null will be supported
	 * 
	 */

	// TODO - trial get with no default - will throw if not set !!
	/*
	 * this "should be" getString for symmetry - but as the default parameter
	 * goes to String vs Object I decided to leave it short and call it "get"
	 * 
	 * the expectation along with all getPrimitiveType("key") is if its not set
	 * then it will throw with a CFGError
	 * 
	 * if you want to default it use a get with a default - these will not throw
	 * - unless they are set with a bad value - (nulls/not set) will become
	 * defaults
	 */
	public String get(final String path) {
		String v = (String) get(path, (Object) null);
		if (v == null) {
			throw new CFGError("String " + path + " not set");
		}
		return v;
	}

	public int getInt(final String path) {
		String v = get(path, null);
		int ret;
		if (v == null || v == "") {
			throw new CFGError("getInt " + path + " not set");
		}
		try {
			ret = Integer.parseInt(v);
		} catch (NumberFormatException ex) {
			throw new CFGError("getInt " + path + "'s value " + v
					+ " is not valid");
		}

		return ret;
	}

	public boolean getBoolean(final String path) {
		String v = get(path, null);
		boolean ret;
		if (v == null || v == "") {
			throw new CFGError("getBoolean" + path + " not set");
		}

		ret = Boolean.parseBoolean(v);

		return ret;
	}

	public float getFloat(final String path) {
		String v = get(path, null);
		float ret;
		if (v == null || v == "") {
			throw new CFGError("getFloat " + path + " not set");
		}
		try {
			ret = Float.parseFloat(v);
		} catch (NumberFormatException ex) {
			throw new CFGError("getFloat " + path + "'s value " + v
					+ " is not valid");
		}

		return ret;
	}

	// get - based on default supplied - does not set value in config to default
	// only gets
	// if not found uses default
	// get with int float boolean String and Object as a possible default
	public float get(final String name, final float defaultValue) {
		String value = null;
		try {

			Float i = new Float(defaultValue);
			value = get(name, i.toString());
			float v = Float.parseFloat(value);
			return v;
		} catch (NumberFormatException e) {
			throw new CFGError("float " + name + "'s value " + value
					+ " is not a valid it");
		}
	}

	public int get(final String name, final int defaultValue) {
		try {

			Integer i = new Integer(defaultValue);
			String value = get(name, i.toString());
			int v = Integer.parseInt(value);
			return v;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public boolean get(final String name, final boolean defaultValue) {
		Boolean i = new Boolean(defaultValue);
		String value = get(name, i.toString());
		boolean v = Boolean.parseBoolean(value);
		return v;
	}

	// get end -------------------------------

	// master set
	// lower level set - you must specify the host completely
	public Object set(String host, String name, Object value) {
		if (value == null) {
			throw new CFGError("set " + name + " can not set null as value");
		}

		// concatenate the current host and path past in
		String fullKey = serviceRoot + PATH_DELIMETER + name;

		// remove a leading / a pos 0 if it exists
		if (fullKey.charAt(0) == '/') {
			fullKey = fullKey.substring(1);
		}

		String outerKey;
		String innerKey;

		// break the fullkey into a outerKey + innerKey
		int outerKeyPos = fullKey.lastIndexOf(PATH_DELIMETER);
		if (outerKeyPos != -1) {
			outerKey = fullKey.substring(0, outerKeyPos);
			innerKey = fullKey.substring(outerKeyPos + 1);
		} else {
			outerKey = fullKey;
			innerKey = "";
		}

		if (data.containsKey(outerKey)) {
			ConcurrentHashMap<String, Object> p = data.get(outerKey);
			return p.put(innerKey, value);
		} else {
			ConcurrentHashMap<String, Object> p = new ConcurrentHashMap<String, Object>();
			Object ret = p.put(innerKey, value);
			data.put(outerKey, p);
			return ret;
		}

	}

	// master set - model after Properties - TODO find a way to support comments
	// overloaded - expected
	public Object set(String name, Object value) {
		return set(serviceRoot, name, value);
		/*
		 * if (data.containsKey(serviceRoot)) { ConcurrentHashMap<String,Object>
		 * p = data.get(serviceRoot); p.put(name, value); } else {
		 * ConcurrentHashMap<String,Object> p = new
		 * ConcurrentHashMap<String,Object>(); if (value == null) { throw new
		 * CFGError("set " + name + " can not set null as value"); } p.put(name,
		 * value); data.put(serviceRoot, p); }
		 */

	}

	public Object set(String name, boolean value) {
		if (value) {
			return set(name, "true");
		} else {
			return set(name, "false");
		}
	}

	public Object set(String name, int value) {
		Integer i = new Integer(value);
		return set(name, i.toString());
	}

	/*
	 * set (name, NearPrimitive value) Necessary to downcast boxing near
	 * primitive objects since primitives are stored as Strings and the
	 * getInt(name) uses a String cast which will explode casting Integer to
	 * String
	 */
	public Object set(String name, Boolean value) {
		if (value == null)
			throw (new CFGError("near primitive object " + name
					+ " can not be set to null"));
		return set(name, value.booleanValue());
	}

	public Object set(String name, Integer value) {
		if (value == null)
			throw (new CFGError("near primitive object " + name
					+ " can not be set to null"));
		return set(name, value.intValue());
	}

	public Object set(String name, Float value) {
		if (value == null)
			throw (new CFGError("near primitive object " + name
					+ " can not be set to null"));
		return set(name, value.floatValue());
	}

	public Object set(String name, Date value) {
		DateFormat df = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");

		if (value == null) {
			CFGError e = new CFGError("set (" + name
					+ ", (Date) null) not valid ");
			throw e;
		}
		if (value != null) {
			String v = df.format(value);
			return set(name, v);
		}

		return null;
	}

	public Date getDate(String name) {
		return getDate(name, null);
	}

	public Date getDate(String name, String defaultValue) {
		String s = get(name, defaultValue);
		if (s == null || s == "") {
			if (throwable) {
				throw (new CFGError("getDate " + name + " is an invalid date"));
			} else
				return null;
		}
		DateFormat df = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");
		Date d = null;
		try {
			d = df.parse(s);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d;
	}

	public Object set(String name, float value) {
		Float i = new Float(value);
		return set(name, i.toString());
	}

	/*
	 * public Properties getProperties (final String name) { if
	 * (data.containsKey(host + PATH_DELIMETER + name)) { return data.get(host +
	 * PATH_DELIMETER + name); }
	 * 
	 * return null; }
	 * 
	 * public Properties getProperties () { if (data.containsKey(host)) { return
	 * data.get(host); }
	 * 
	 * return null; }
	 */
	// get end -------------------------------

	// TODO - merge option
	public void load(String filename) {
		try {
			// TODO - cheezy loading from java properties because I'm too lazy
			// to parse/tokenize
			// String data = FileIO.fileToString(filename);
			// parsing / tokenizing is a pain in the arse
			// going to let java properties do it for me - don't support
			// serializing comments
			// or serializing multiple values
			Properties p = new Properties();
			FileReader fr = new FileReader(filename);
			BufferedReader is = new BufferedReader(fr);
			p.load(is);
			for (Enumeration e = p.propertyNames(); e.hasMoreElements();) {
				// String key = getLeafKey((String)e.nextElement());
				String name = (String) e.nextElement();
				String value = p.getProperty(name);
				// String key = (host == null)? name : host + PATH_DELIMETER +
				// name;

				set(name, value); // add ?? YES TODO
			}

			/*
			 * Properties sysprops = System .getProperties(); for ( Enumeration
			 * e = sysprops.propertyNames(); e.hasMoreElements(); ) { String key
			 * = (String)e.nextElement(); String value = sysprops.getProperty(
			 * key ); System.out.println( key + "=" + value ); } // end for
			 */

		} catch (FileNotFoundException e) {
			LOG.warn("file " + filename + " not found");
		} catch (IOException e) {
			LOG.warn("IOException on " + filename);
		}

	}

	// TODO save will save out the configuration associated with the current root
	public void save(String filename) {
		try {
			FileWriter outfile = new FileWriter(filename);
			PrintWriter out = new PrintWriter(outfile);

			out.write(toString(OutputFormat.PROPERTIES));
			// TODO - make keys
			/*
			 * while(it.hasNext()) { data.get(it.next()).store(outfile,
			 * "comments"); }
			 */
			// data.store(outfile,"this is a comment");
			// out.write(toString());
			out.close();
			outfile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * remove Map
	 */

	public Object removeMap(String path) {
		// if (data.containsKey(serviceRoot))
		if (data.containsKey(path)) {
			return data.remove(path);
		}
		return null;
	}

	/*
	 * remove value from current root map
	 */

	public Object remove(String key) {
		return remove(getRoot(), key);
	}

	/*
	 * remove key in Map
	 */
	public Object remove(String path, String key) {
		// if (data.containsKey(serviceRoot))
		if (data.containsKey(path)) {
			ConcurrentHashMap<String, Object> p = data.get(path);
			return p.remove(key);
		}
		return null;
	}

	/********************************************************
	 * keySet
	 * 
	 * TODO: 1. Large optimization would be to create a new HashMap index every
	 * time a new host was set This would use slightly more memory but the list
	 * return order would be constant (K) versus Order N. 2. get the loop out so
	 * getList can use the same loop & key functions
	 */
	public Set<String> keySet(String name) {
		// compute host path & name (must be identical to master get(String
		// name)

		// concatenate the current host and path past in
		String fullKey = serviceRoot + PATH_DELIMETER + name;

		// remove a leading / a pos 0 if it exists
		if (fullKey.charAt(0) == '/') {
			fullKey = fullKey.substring(1);
		}

		/*
		 * String outerKey; String innerKey;
		 * 
		 * // break the fullkey into a outerKey + innerKey int outerKeyPos =
		 * fullKey.lastIndexOf(PATH_DELIMETER); if (outerKeyPos != -1) {
		 * outerKey = fullKey.substring(0, outerKeyPos); innerKey =
		 * fullKey.substring(outerKeyPos+1); } else { //name = path; }
		 */
		if (data.containsKey(fullKey)) {
			// Set<Object> d = data.get(host + PATH_DELIMETER + path).keySet();
			return data.get(fullKey).keySet();
		}

		return null;
	}

	public Set<String> keySet() {
		if (data.containsKey(serviceRoot)) {
			return data.get(serviceRoot).keySet();
		}
		return null;
	}

	public String toString(OutputFormat format) {
		if (format == null) {
			return data.toString();
		} else // if(format == OutputFormat.PROPERTIES)
		{
			StringBuffer sb = new StringBuffer();
			// Set<String> names = keySet(); // The set of names in the map.

			
			Iterator it = null;

/*			
			TODO !!!! - change Outer/Inner map to REAL TREE map of maps!!!!!!
			String rootPrefix = "";
			if (getRoot() == null || getRoot().length() == 0)
			{	// root level
				it = data.keySet().iterator();
			} else {
				// host level
				it = data.get(getRoot()).keySet().iterator();
				rootPrefix = getRoot() + PATH_DELIMETER;
			}
*/			
			it = data.keySet().iterator();
							
			// TODO - make keys
			while (it.hasNext()) {
//				String rootKey = rootPrefix + (String) it.next();  TODO TREE is map of map
				String rootKey = (String) it.next();
				Iterator inner = data.get(rootKey).keySet().iterator();
				ConcurrentHashMap<String, Object> innerProperties = data.get(rootKey);

				while (inner.hasNext()) {
					// data.get(it.next()).store(outfile, "comments");
					String elementKey = (String) inner.next();
					String fullKey = rootKey + PATH_DELIMETER + elementKey;
					// sb.append(it.next() + PATH_DELIMETER + inner.next() + "="
					// + data.get(it.next()).get(inner.next()) );
					sb.append(fullKey + "=" + innerProperties.get(elementKey)
							+ "\n");
				}
			}

			return sb.toString();
		}
	}

	public String toString() {
		return toString(null);
	}

	public void loadFromXML(InputStream in) throws IOException,
			InvalidPropertiesFormatException {
		// data.loadFromXML(in);
	}

	void storeToXML(OutputStream os, String comment) throws IOException {
		// data.storeToXML(os, comment);
	}

	void storeToXML(OutputStream os, String comment, String encoding)
			throws IOException {
		// data.storeToXML(os, comment, encoding);
	}

	public void setThrowable(final boolean value) {
		throwable = value;
	}

	public Object getLocalServiceHandle(final String name) {
		return (Object) get("service/" + name + "/localServiceHandle",
				(Object) null);
	}

	public ServiceEntry getServiceEntry(final String name) {
		ServiceEntry se = new ServiceEntry();
		String seName = get("service/" + name, null);
		if (seName != null) {
			se.name = name;
			se.host = get("service/" + name + "/host");
			se.serviceClass = get("service/" + name + "/serviceClass");
			se.servicePort = getInt("service/" + name + "/servicePort");
			se.lastModified = getDate("service/" + name + "/lastModified");
			se.localServiceHandle = get("service/" + name
					+ "/localServiceHandle", (Object) null);
			se.toolTip = get("service/" + name + "/toolTip");

			return se;
		}

		return null;
	}

	// TODO - not the nicest interface to have a bunch of nulls fields
	public ServiceEntry getFullServiceEntry(final String name) {
		ServiceEntry se = getServiceEntry(name);
		// se.methods = getMethodMap(name); TODO - if this is populated it wil
		// drag parameterTypes into Applet Displays (and all their dependencies
		// !)
		se.interfaces = getInterfaceMap(name);

		return se;

	}

	/*
	public MethodEntry getMethodEntry(final String serviceName, final String method) 
	{
		MethodEntry me = new MethodEntry();
		String meName = get("service/" + serviceName + "/method/" + method, null);
				
		if (meName != null) {
			//me.name = signature;
			me.name = (String) get("service/" + serviceName + "/method/" + method + "/name", (Object) null);
			me.returnType = (Class) get("service/" + serviceName + "/method/" + method + "/returnType", (Object) null);
			me.parameterTypes = (Class[]) get("service/" + serviceName + "/method/" + method + "/parameterTypes", (Object) null);
			return me;
		}

		return null;
	}
	*/

	public InterfaceEntry getInterfaceEntry(final String serviceName,
			final String name) {
		InterfaceEntry me = new InterfaceEntry();
		String meName = get("service/" + serviceName + "/interface/" + name,
				null);
		if (meName != null) {
			me.name = name;
			/* silly but reserved for future use */
			// me.name = (Class)get("service/" + serviceName + "/interface/" +
			// name + "/name", (Object)null);
			return me;
		}

		return null;
	}

	public Object removeServiceEntry(final String name) {
		// remove the services map for this service
		removeMap(getRoot() + "/service/" + name);

		// remove the service entry
		return remove(getRoot() + "/service", name);
	}

	public void removeServiceEntries(final String host, final int port) {
		Iterator<String> it = keySet("service").iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceEntry se = getServiceEntry(serviceName);
			if (se.host.compareTo(host) == 0 && se.servicePort == port) {
				removeServiceEntry(serviceName);
			}
		}

	}

	public HashMap<String, ServiceEntry> getServiceMap() {
		HashMap<String, ServiceEntry> sem = new HashMap<String, ServiceEntry>();
		Iterator<String> it = keySet("service").iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			sem.put(serviceName, getServiceEntry(serviceName));
		}

		return sem;

	}

	public HashMap<String, MethodEntry> getMethodMap(String serviceName) 
	{
		HashMap<String, MethodEntry> mem = new HashMap<String, MethodEntry>();

		Iterator<String> it = keySet("service/" + serviceName + "/methods").iterator();
//		Iterator<String> it = keySet("service/" + serviceName + "/method").iterator();
		while (it.hasNext()) {
			String signature = it.next();
			MethodEntry me = MethodEntry.parseSignature(signature);
			mem.put(signature, me);
		}
	
		/*
		it = keySet("service/" + serviceName + "/method").iterator();
		while (it.hasNext()) {
			String methodName = it.next();
			mem.put(methodName, getMethodEntry(serviceName, methodName));
		}
		*/

		return mem;
	}

	public HashMap<String, InterfaceEntry> getInterfaceMap(String serviceName) {
		HashMap<String, InterfaceEntry> mem = new HashMap<String, InterfaceEntry>();
		Set<String> s = keySet("service/" + serviceName + "/interface");

		if (s != null) {
			Iterator<String> it = s.iterator();
			while (it.hasNext()) {
				String interfaceName = it.next();
				mem.put(interfaceName, getInterfaceEntry(serviceName,
						interfaceName));
			}
		}

		return mem;

	}

	public Vector<String> getServiceVector() {
		Iterator<String> it = keySet("service").iterator();
		Vector<String> v = new Vector<String>();
		while (it.hasNext()) {
			v.add(it.next());
		}

		return v;
	}

	public ArrayList<ServiceEntry> getLocalServiceEntries() {
		ArrayList<ServiceEntry> al = new ArrayList<ServiceEntry>();
		Iterator<String> it = keySet("service").iterator();
		while (it.hasNext()) {
			String serviceName = it.next();
			if (getLocalServiceHandle(serviceName) != null) {
				al.add(getServiceEntry(serviceName));
			}

		}
		return al;
	}

	public void setServiceEntry(ServiceEntry se) {
		setServiceEntry(
				// se.host, host is not specified - because it "should" be a
				// host level configuration that put it in
				se.host, se.name, se.serviceClass, se.servicePort,
				se.lastModified, se.localServiceHandle, se.toolTip);

		if (se.methods != null) {
			Iterator<String> it = se.methods.keySet().iterator();
			while (it.hasNext()) {
				MethodEntry me = se.methods.get(it.next());
				setMethod(host, se.name, me.name, me.returnType,
						me.parameterTypes); // FYI ! - if you have dependencies
											// on types they will get dragged
											// into Applet displays
			}
		}

		if (se.interfaces != null) {
			Iterator<String> it = se.interfaces.keySet().iterator();
			while (it.hasNext()) {
				String interfaceName = it.next();
				InterfaceEntry me = se.interfaces.get(interfaceName);
				setInterface(host, se.name, me.name);
			}

		}

	}

	// setServiceEntry should be used with a host level cfg manager - e.g.
	// ConfigurationManger cfg = new ConfigurationManager("localhost");
	public void setServiceEntry(final String host, final String name,
			final String serviceClass, final int servicePort,
			final Date lastModified, final Object localServiceHandle, final String toolTip) {
		// ConfigurationManager cfg = new ConfigurationManager();
		set("service/" + name, "");
		set("service/" + name + "/host", host);
		set("service/" + name + "/serviceClass", serviceClass);
		set("service/" + name + "/servicePort", servicePort);
		set("service/" + name + "/lastModified", lastModified);
		if (toolTip != null)
		{
			set("service/" + name + "/toolTip", toolTip);
		}
		if (localServiceHandle != null) {
			set("service/" + name + "/localServiceHandle",
					(Object) localServiceHandle);
		}
	}

	// FYI ! - if you have dependencies on types they will get dragged into
	// Applet displays
	// must be set with a host level config
	// public void setMethod(final String host, final String serviceName, final
	// String methodName, final String direction , final String dataClass )
	public void setMethod(final String host, final String serviceName,
			final String methodName, final Class returnType,
			final Class[] parameterTypes) {
		
		String signature = MethodEntry.getSignature(methodName, parameterTypes, returnType);
		//String signature = MethodEntry.getPrettySignature(methodName, parameterTypes, returnType);
		//String signature = methodName;

		//set("service/" + serviceName + "/methods/" + signature, "");
		
		set("service", "");
		set("service/" + serviceName + "/methods/" + signature, "");
		set("service/" + serviceName + "/methods/" + signature + "/name", methodName);
		set("service/" + serviceName + "/methods/" + signature + "/returnType",returnType);
		set("service/" + serviceName + "/methods/" + signature + "/parameterTypes", parameterTypes);
		//set("service/" + serviceName + "/method/" + methodName + "/signature", signature);
	}

	public void setInterface(final String host, final String serviceName,
			final String interfaceName) {
		set("service", "");
		set("service/" + serviceName + "/interface/" + interfaceName, "");
		set("service/" + serviceName + "/interface/" + interfaceName + "/name",interfaceName);
		// set("service/" + serviceName +"/interface/" + interfaceName,
		// interfaceName);
		// set("service/" + serviceName +"/method/" + methodName + "/direction",
		// direction);
		// set( "service/" + serviceName +"/method/" + methodName +
		// "/dataClass", dataClass);
	}

	public Vector<String> getInterfaces(String serviceName) {
		Set<String> set = keySet("service/" + serviceName + "/interface");
		if (set == null) {
			return null;
		}
		Iterator<String> it = set.iterator();
		Vector<String> v = new Vector<String>();
		while (it.hasNext()) {
			v.add(it.next());
		}

		return v;

	}

	public Vector<String> getServicesFromInterface(String interfaceName) {
		Vector<String> services = getServiceVector();
		Vector<String> servicesWithInterface = new Vector<String>();
		// Iterator<String> it = set.iterator();
		// Vector<String> v = new Vector<String>();

		for (int i = 0; i < services.size(); ++i) {
			String serviceName = services.get(i);
			Vector<String> interfaces = getInterfaces(serviceName);
			boolean found = false;

			if (interfaces != null) {
				for (int j = 0; j < interfaces.size(); ++j) {
					if (interfaces.get(j).compareTo(interfaceName) == 0)
						found = true;
				}
			}

			if (found) {
				servicesWithInterface.add(serviceName);
			}
		}

		return servicesWithInterface;
	}

}