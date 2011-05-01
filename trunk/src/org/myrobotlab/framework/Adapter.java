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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

public class Adapter {
	public final static Logger LOG = Logger.getLogger(Adapter.class.toString());

	@SuppressWarnings("unchecked")
	static public Object invoke(String classname, String fn, Object params[])
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, NoSuchMethodException,
			InvocationTargetException {

		Object retobj = null;
		Class c = Class.forName(classname); // Dynamically load the class
		Object o = c.newInstance(); // Dynamically instantiate it

		String dataClassName = "";
		Class[] paramTypes = new Class[params.length]; // this part is weak
		for (int i = 0; i < params.length; ++i) {
			// paramTypes[i] = String.class;
			paramTypes[i] = params[i].getClass();
			dataClassName = params[i].getClass().getCanonicalName();
		}

		// try to get method which has the correct parameter types
		LOG.info("****invoking " + classname + "." + fn + "(" + dataClassName
				+ ")****");
		Method meth = c.getDeclaredMethod(fn, paramTypes);

		// load the parameters
		// invoke the method - return the return object
		retobj = meth.invoke(o, params);
		return retobj;

	}

}
