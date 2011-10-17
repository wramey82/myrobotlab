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

package org.myrobotlab.service;

import java.awt.Toolkit;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class SystemWrapper extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(SystemWrapper.class.getCanonicalName());

	// fields
	public SystemWrapper(String n) {
		super(n, SystemWrapper.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {		
	}
	
	public int exec (String[] params)
	{
		Runtime r = Runtime.getRuntime();
		try {
			Process p = r.exec(params);
			return p.exitValue();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public void beep()
	{
		Toolkit.getDefaultToolkit().beep();
	}
	
	@Override
	public String getToolTip() {
		return "used to wrap the Java System and Runtime objects.";
	}


}
