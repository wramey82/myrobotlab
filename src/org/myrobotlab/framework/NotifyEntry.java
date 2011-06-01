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

import java.io.Serializable;
import java.util.Arrays;

import org.apache.log4j.Logger;

public final class NotifyEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(NotifyEntry.class);

	public String outMethod; // the keyed out method
	public String name; // globally unique name of Service a Message will be
						// sent to
	public String inMethod; // the method which will be invoked from the
								// Message
	public Class<?>[] paramTypes = null; // the parameter type of the inMethod - named
								// parameterType vs dataType, because this will
								// always specify parameters not return types

	private int _hashCode = 0;
	
	public NotifyEntry(String outMethod,String name,String inMethod, Class<?>[] paramTypes) {
		this.outMethod = outMethod;
		this.inMethod = inMethod;
		this.name = name;
		this.paramTypes = paramTypes;
	}
	
	
	final public boolean equals(final NotifyEntry other)
	{
		//if (paramTypes.toString().equals(other.outMethod))
		if (Arrays.equals(paramTypes, other.paramTypes)
				&& name.equals(other.name)
				&& inMethod.equals(other.inMethod)
				&& outMethod.equals(other.outMethod)
				)
		{
			return true;
		}
		return false;
	}
	
	final public int hashCode()
	{
		if (_hashCode == 0)
		{
			_hashCode = 37 + outMethod.hashCode() + name.hashCode() + inMethod.hashCode();
			for (int i = 0; i < paramTypes.length; ++i)
			{
				_hashCode += paramTypes[i].hashCode();
			}
		}
		
		return _hashCode;
	}

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("{");
		ret.append("\"outMethod\":" + "\"" + outMethod + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"inMethod\":" + "\"" + inMethod + "\"");
		ret.append("\"paramType\":" + "\"" + paramTypes + "\"");
		ret.append("}");
		return ret.toString();
	}

}