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

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.IOData;

public class Player extends Service {

	public final static Logger LOG = Logger.getLogger(Player.class
			.getCanonicalName());
	public String IPAddress = ""; // GUI's ip
	public int port = 0; // GUI's port
	public boolean isConnected = false;
	public String team = "";
	public String login = "";
	public String GUIName = "";
	public boolean guiIsInitialized = false;
	public String type = "player";
	SoccerGame game = null;
	IOData io = new IOData();

	// lazer tower related
	int wheelPower = 100;

	int lightValue = 0;

	public Player(String n) {
		super(n, Player.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub
		// cfg.set("playerType", "player"); TODO - mess with this later
	}

	public String setGUIName(String guiname) {
		GUIName = guiname;
		return GUIName;
	}

	public String setTeam(String team) {
		this.team = team;
		send(GUIName, "setTeam", team); // update ui
		return team;
	}

	public String setLogin(String login) {
		this.login = login;
		send(GUIName, "setLogin", login); // update ui
		return login;
	}

	public String translate(int keycode) {
		String ret = "";

		return ret;
	}

	// http://rmhh.co.uk/ascii.html
	public Integer playerCommand(Integer cmd) {
		wheelPower = 100;
		int wheelTime = 1000;
		String cmdString = "";

		if (login.length() > 0) {
			switch (cmd) {
			case 38: // arrow up - move up
			{
				cmdString = "forward";

				if (type.compareTo("turret") == 0) {
					send("tilt", "move", -11);
					cmdString += " turret";
				} else if (type.compareTo("wheel") == 0) {

					send("arduino", "digitalWrite", new IOData(7, 1));
					send("arduino", "digitalWrite", new IOData(8, 0));
					send("arduino", "analogWrite", new IOData(9, wheelPower));
					send("arduino", "analogWrite", new IOData(10, wheelPower));

					Timer timer = new Timer();
					timer.schedule(new PlayerTask("move"), wheelTime);
				}
				// send("tilt","move", -1); lazer tower mk 1

				break;
			}
			case 40: // arrow down - move down
			{
				cmdString = "back";

				if (type.compareTo("turret") == 0) {
					send("tilt", "move", 1);
					cmdString += " turret";
				} else if (type.compareTo("wheel") == 0) {
					send("arduino", "digitalWrite", new IOData(7, 0));
					send("arduino", "digitalWrite", new IOData(8, 1));
					send("arduino", "analogWrite", new IOData(9, wheelPower));
					send("arduino", "analogWrite", new IOData(10, wheelPower));

					Timer timer = new Timer();
					timer.schedule(new PlayerTask("move"), wheelTime);
				}
				break;
			}
			case 37: // arrow left - move left
			{
				cmdString = "left";

				if (type.compareTo("turret") == 0) {
					send("pan", "move", 1);
					cmdString += " turret";
				} else if (type.compareTo("wheel") == 0) {

					send("arduino", "digitalWrite", new IOData(7, 1));
					send("arduino", "analogWrite", new IOData(9, wheelPower));
					send("arduino", "digitalWrite", new IOData(8, 1));
					send("arduino", "analogWrite", new IOData(10, wheelPower));

					Timer timer = new Timer();
					timer.schedule(new PlayerTask("move"), wheelTime);

					cmdString += " wheels";
				}

				// send("pan","move", -1);
				break;
			}
			case 39: // arrow right - move right
			{
				cmdString = "right";
				if (type.compareTo("turret") == 0) {
					send("pan", "move", -1);
					cmdString += " turret";
				} else if (type.compareTo("wheel") == 0) {
					send("arduino", "digitalWrite", new IOData(7, 0));
					send("arduino", "analogWrite", new IOData(9, wheelPower));
					send("arduino", "digitalWrite", new IOData(8, 0));
					send("arduino", "analogWrite", new IOData(10, wheelPower));

					Timer timer = new Timer();
					timer.schedule(new PlayerTask("move"), wheelTime);

					cmdString += " wheels";
				}

				// send("pan","move", 1);
				break;
			}
			case 32: // spacebar - laser 1 sec shot
			{
				cmdString = "\"lazer\"";
				io.address = 2;
				io.value = 1;
				send("arduino", "digitalWrite", io);
				game.audio.playFile("lazer.mp3");
				Timer timer = new Timer();
				timer.schedule(new PlayerTask("LZR"), 1000);
				break;
			}
			case 76: // l - light on
			{
				cmdString = "light on";
				io.address = 3;
				io.value = 0;
				send("arduino", "digitalWrite", io);
				break;
			}
			case 75: // k - klaxon
			{
				cmdString = "red alert";
				game.audio.playFile("klaxon.mp3");
				break;
			}
			case 59: // ; - light off
			{
				cmdString = "light off";
				io.address = 3;
				io.value = 130;
				send("arduino", "analogWrite", io);
				break;
			}
			case 65: // a - pulse of light off
			{
				cmdString = "light pulse";
				io.address = 3;
				io.value = 50;
				send("arduino", "analogWrite", io);
				Timer timer = new Timer();
				timer.schedule(new PlayerTask("pulse light"), 1000);
				break;
			}
			case 47: {
				send("tilt", "move", -1);
				break;
			}

			default: {
				LOG.error("playerCommand " + cmd);
				break;
			}
			}
			LOG.info(name + " " + cmd);
			game.broadCastMsgToPlayers("addLogEntry", "<font color=\"" + team
					+ "\">" + login + "</font> " + cmdString);
		}
		return cmd;
	}

	public String playerCommand(String cmd) {
		LOG.info(name + " " + cmd);
		game.broadCastMsgToPlayers("addLogEntry", "<font color=\"" + team
				+ "\">" + login + "</font> " + cmd);
		return cmd;
	}

	class PlayerTask extends TimerTask {
		String cmd = "";

		PlayerTask(String cmd) {
			this.cmd = cmd;
		}

		public void run() {
			if (this.cmd.compareTo("LZR") == 0) {
				io.address = 2;
				io.value = 0;
				send("arduino", "digitalWrite", io);// lazer off
			} else if (this.cmd.compareTo("pulse light") == 0) {
				io.address = 3;
				io.value = 255;
				send("arduino", "analogWrite", io);
			} else if (this.cmd.compareTo("move") == 0) {
				send("arduino", "analogWrite", new IOData(9, 0));
				send("arduino", "analogWrite", new IOData(10, 0));
			}
		}
	}

	public void announce(String msg) {
		game.announce(login + " says " + msg);
	}

	public void guiUpdated() {
		LOG.info("guiUpdated");

		// notification that GUI Update has occurred - gui is now functioning
		// remotely
		guiIsInitialized = true;
		send(GUIName, "setLogin", login); // update ui
		send(GUIName, "setTeam", team); // update ui
		send(GUIName, "setType", type); // update ui
		game.updateRoster();
	}

	@Override
	public String getToolTip() {
		return "<html>player bot for LMR soccars</html>";
	}
	
}
