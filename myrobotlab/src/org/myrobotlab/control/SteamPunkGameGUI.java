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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.myrobotlab.service.SteamPunkGame;
import org.myrobotlab.service.interfaces.GUI;

public class SteamPunkGameGUI extends ServiceGUI implements ActionListener, KeyListener {

	static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(SteamPunkGameGUI.class.getCanonicalName());
	
	JButton keyboardControl = new JButton("keyboard control");
	JButton startGame = new JButton("start game");

	public SteamPunkGameGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {        
        keyboardControl.addKeyListener(this);
        JPanel buttonPanel = new JPanel();
		buttonPanel.add(keyboardControl);
		buttonPanel.add(startGame);
		startGame.addActionListener(this);
		display.add(buttonPanel);
	}

	public void getState(SteamPunkGame game) {
	}

	@Override
	public void attachGUI() {
		subscribe("publishState", "getState", SteamPunkGame.class);
		myService.send(boundServiceName, "publishState");
	}

	@Override
	public void detachGUI() {
		unsubscribe("publishState", "getState", SteamPunkGame.class);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
		if (o == startGame)
		{
			myService.send(boundServiceName, "startGame");
			startGame.setEnabled(false);
		}

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if(      keyCode == KeyEvent.VK_SPACE ) {
            myService.send(boundServiceName, "stop");
        }
        else if( keyCode == KeyEvent.VK_UP ) {
            myService.send(boundServiceName, "forward");
        }
        else if( keyCode == KeyEvent.VK_DOWN ) {
            myService.send(boundServiceName, "backward");
        }
        else if( keyCode == KeyEvent.VK_LEFT ) {
            myService.send(boundServiceName, "spinleft");
        }
        else if( keyCode == KeyEvent.VK_RIGHT ) {
            myService.send(boundServiceName, "spinright");
        }
        else if( keyCode == KeyEvent.VK_COMMA ) {
            myService.send(boundServiceName, "speedDown");
        }
        else if( keyCode == KeyEvent.VK_PERIOD ) {
            myService.send(boundServiceName, "speedUp");
        }
        else if( keyCode == KeyEvent.VK_R ) {
            myService.send(boundServiceName, "reset");
        }
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

}
