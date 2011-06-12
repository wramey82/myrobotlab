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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import wiiusej.WiiUseApiManager;
import wiiusej.Wiimote;
import wiiusej.wiiusejevents.physicalevents.ExpansionEvent;
import wiiusej.wiiusejevents.physicalevents.IREvent;
import wiiusej.wiiusejevents.physicalevents.MotionSensingEvent;
import wiiusej.wiiusejevents.physicalevents.WiimoteButtonsEvent;
import wiiusej.wiiusejevents.utils.WiimoteListener;
import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.DisconnectionEvent;
import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.NunchukInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.NunchukRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.StatusEvent;

import org.myrobotlab.framework.Service;

public class SLAM extends Service implements WiimoteListener {

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(SLAM.class
			.getCanonicalName());

	Wiimote[] wiimotes = null;
	Wiimote wiimote = null;

	public SLAM(String n) {
		super(n, SLAM.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	public void onButtonsEvent(WiimoteButtonsEvent arg0) {
		System.out.println(arg0);
		if (arg0.isButtonAPressed()) {
			WiiUseApiManager.shutdown();
		}
	}

	public void onIrEvent(IREvent arg0) {
		System.out.println(arg0);
		invoke("publishIR", arg0);
	}

	public IREvent publishIR(IREvent ir) {
		return ir;
	}

	public void onMotionSensingEvent(MotionSensingEvent arg0) {
		System.out.println(arg0);
	}

	public void onExpansionEvent(ExpansionEvent arg0) {
		System.out.println(arg0);
	}

	public void onStatusEvent(StatusEvent arg0) {
		System.out.println(arg0);
	}

	public void onDisconnectionEvent(DisconnectionEvent arg0) {
		System.out.println(arg0);
	}

	public void onNunchukInsertedEvent(NunchukInsertedEvent arg0) {
		System.out.println(arg0);
	}

	public void onNunchukRemovedEvent(NunchukRemovedEvent arg0) {
		System.out.println(arg0);
	}

	public Wiimote[] getWiimotes() {
		wiimotes = WiiUseApiManager.getWiimotes(1, true);
		wiimote = wiimotes[0];
		return wiimotes;
	}

	public Wiimote[] getWiimotes(int n, boolean rumble) {
		return WiiUseApiManager.getWiimotes(n, rumble);
	}

	public void activateListening() {
		wiimote.addWiiMoteEventListeners(this);
	}

	public void activateIRTRacking() {
		wiimote.activateIRTRacking();
	}

	public void activateMotionSensing() {
		wiimote.activateMotionSensing();
	}

	public void setIrSensitivity(int level) {
		wiimote.setIrSensitivity(level);
	}

	public void setLeds(boolean l1, boolean l2, boolean l3, boolean l4) {
		wiimote.setLeds(l1, l2, l3, l4);
	}

	public static void main(String[] args) {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		SLAM wii = new SLAM("wii");
		GUIService gui = new GUIService("gui");
		wii.getWiimotes();
		wii.activateIRTRacking();
		wii.setIrSensitivity(5); // 1-5 (highest)
		wii.activateListening();

		wii.setLeds(true, false, false, false);
		wii.setLeds(true, true, false, false);
		wii.setLeds(true, true, true, false);
		wii.setLeds(false, false, false, false);

		wii.startService();
		gui.startService();

		gui.display();

		while (true) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public void onClassicControllerInsertedEvent(
			ClassicControllerInsertedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClassicControllerRemovedEvent(
			ClassicControllerRemovedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGuitarHeroInsertedEvent(GuitarHeroInsertedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGuitarHeroRemovedEvent(GuitarHeroRemovedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getToolTip() {
		return "<html>addendum of WiiDAR - SLAM (not implemented)</html>";
	}
	
}
