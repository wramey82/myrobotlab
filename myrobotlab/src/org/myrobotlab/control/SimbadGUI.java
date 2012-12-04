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

package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.Simbad;
import org.myrobotlab.service.interfaces.GUI;

public class SimbadGUI extends ServiceGUI implements ActionListener{

	static final long serialVersionUID = 1L;

	Simbad mySimbad = null;
	
	public SimbadGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public void init() {
        mySimbad = (Simbad)Runtime.getServiceWrapper(boundServiceName).service;
	}
	
	// FIXME - is get/set state interact with Runtime registry ??? 
	// it probably should
	public void getState(Simbad c)
	{
	}


	// FIXME sendNotifyStateRequest("publishState", "getState", String type); <- Class.forName(type)
	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", Simbad.class); 
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", Simbad.class);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}
