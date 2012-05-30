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

package org.myrobotlab.attic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceDirectoryUpdate;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.AudioFile;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.Servo;
import org.myrobotlab.service.Speech;
import org.myrobotlab.service.data.IPAndPort;

public class SoccerGame extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(SoccerGame.class
			.getCanonicalName());

	RemoteAdapter remote = new RemoteAdapter("remote");
	ArrayList<Player> players = new ArrayList<Player>();
	HashMap<String, Player> currentPlayers = new HashMap<String, Player>();
	int redTeam = 0;
	int blueTeam = 0;
	Arduino arduino = new Arduino("arduino");
	Servo pan = new Servo("pan");
	Servo tilt = new Servo("tilt");
	// OpenCV camera = new OpenCV("camera");
	Speech announcer = new Speech("announcer");
	GUIService gui = new GUIService("gamegui");
	AudioFile audio = new AudioFile("audio");

	Properties security = null;
	Timer timer = null;
	TimerTask timerTask = null;

	int gameSeconds = 530;
	int numberOfPlayers = 5;

	public SoccerGame(String n) {
		this(n, null);
	}

	public SoccerGame(String n, String serviceDomain) {
		super(n, SoccerGame.class.getCanonicalName(), serviceDomain);
	}

	@Override
	public void loadDefaultConfiguration() {
	}

	/*
	 * class Pinger implements Runnable {
	 * 
	 * @Override public void run() { //
	 * 
	 * }
	 * 
	 * }
	 */

	public Object[] makeHTMLErrorParam(String text) {
		Object[] param = new Object[1];
		param[0] = "<html><body><font color=\"red\">" + text
				+ "</font></body></html>";
		log.error(text);
		return param;
	}

	public Object[] makeHTMLOKParam(String text) {
		Object[] param = new Object[1];
		param[0] = "<html><body><font color=\"green\">" + text
				+ "</font></body></html>";
		log.error(text);
		return param;
	}

	public void startGame() {
		// create the players begin -------------
		for (int i = 0; i < numberOfPlayers; ++i) {
			Player player;

			// if (i == numberOfPlayers - 1)
			if (i == 0) {
				player = new Player("lazer_turret_mk0" + i);
				player.type = "turret";
			} else if (i == 1) {
				player = new Player("lazer_wheels_mk0" + i);
				player.type = "wheel";

			} else {
				player = new Player("player0" + i);
			}

			//player.game = this;
			player.startService();
			players.add(player);
		}
		// create the players end -------------

		// security begin -----------------------
		security = new Properties();
		try {
			security.load(new FileInputStream("security.txt"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Enumeration<?> e = security.propertyNames();

		for (; e.hasMoreElements();) {
			log.info(e.nextElement());
		}
		// security end -----------------------

		// set up aux devices begin -----------
		pan.attach(arduino.getName(), 6);
		tilt.attach(arduino.getName(), 5);
		// set up aux devices end -----------

		// set up global message routes begin--
		// when the remote adapter finds a client disconnecting - send a msg to
		// the SoccerGame to disconnect them
		remote.notify("connectionBroken", this.getName(), "connectionBrokenEvent", IPAndPort.class); // TODO - TAKE CLASS
		// set up global message routes end--

		// start services begin----------------
		audio.startService();
		remote.startService();
		announcer.startService();
		arduino.startService();
		tilt.startService();
		pan.startService();
		// camera.start();
		gui.startService();
		gui.display();
		// start services end ----------------

	}

	// TODO - clean up NOTIFY ENTRIES !!!!!

	// new player association request // registerServices
	public synchronized void registerServices(ServiceDirectoryUpdate sdu) {
		log.info(getName() + " new soccer player request ! " + sdu);

		Player newPlayer = null;

		// look for available slot
		int i = 0;
		for (i = 0; i < numberOfPlayers; ++i) {
			if (!players.get(i).isConnected) {
				newPlayer = players.get(i);
				//newPlayer.IPAddress = sdu.remoteHostname;
				//newPlayer.port = sdu.remoteServicePort;
				newPlayer.isConnected = true;
				//log.info(sdu.remoteHostname + " has been assigned player0" + i);
				break;
				// time for player reset
			}
		}

		ServiceEntry client = null;
		//ServiceEntry client = sdu.serviceEntryList_.get(0); // should have 1 and
															// only 1

		// put on time queue ? - if not available

		// make an association !!
		// allow the registration of the gui
		// temporarily register requesting client cause - we need to communicate
		// back to them
		// hostcfg.save("pre.sdu.txt");
		//super.registerServices(sdu); - depricated
		// hostcfg.save("post.sdu.txt");

		String login = "";
		/*
		String login = sdu.login;
		log.info("login request from " + login + " @ " + sdu.remoteHostname
				+ ":" + sdu.remoteServicePort);
				*/

		if (newPlayer == null) {
			send(
					client.name,
					"setRemoteConnectionStatus",
					makeHTMLErrorParam("too many players - no new players now ... sorry"));
			return;
		}

		if (currentPlayers.containsKey(login)) {
			send(client.name, "setRemoteConnectionStatus",
					makeHTMLErrorParam(login
							+ " already has control of player "
							+ currentPlayers.get(login).getName()));
			return;
		}

		/*
		 * if (!security.containsKey(sdu.login) || ((String)
		 * security.get(sdu.login)).compareTo(sdu.password) != 0) {
		 * send(client.getName(), "setRemoteConnectionStatus", makeHTMLErrorParam
		 * ("login " + sdu.login + "failed")); log.error("login " + sdu.login +
		 * "/" + sdu.password + "failed"); return; }
		 */

		send(client.name, "setRemoteConnectionStatus",
				makeHTMLOKParam("connected to " + newPlayer.getName()
						+ " welcome to the game !"));

		// TODO - prepare to dump service description if user no good
		// assign number to player
		// assign gui name
		// put in gui view service

		ServiceEntry se;
		//sdu.serviceEntryList_.clear();

		se = hostcfg.getServiceEntry(newPlayer.getName());
		se.localServiceHandle = null;
		//sdu.serviceEntryList_.add(se);

		if (newPlayer.type.compareTo("cameraman") == 0) {
			/*
			 * se = hostcfg.getServiceEntry("pan"); se.localServiceHandle =
			 * null; sdu.serviceEntryList_.add(se);
			 * 
			 * se = hostcfg.getServiceEntry("tilt"); se.localServiceHandle =
			 * null; sdu.serviceEntryList_.add(se);
			 * 
			 * se = hostcfg.getServiceEntry("camera"); se.localServiceHandle =
			 * null; sdu.serviceEntryList_.add(se);
			 * 
			 * 
			 * se = hostcfg.getServiceEntry("arduino"); se.localServiceHandle =
			 * null; sdu.serviceEntryList_.add(se);
			 */

			broadCastToPlayers("addLogEntry", newPlayer.login
					+ " is a cameraman");
			broadCastToPlayers("addLogEntry", "starting new game");
			broadCastToPlayers("addLogEntry", " game over in " + gameSeconds
					+ " seconds");

			timer = new Timer();
			timer.schedule(new EndGame(), gameSeconds * 1000);
		}

		// associate login & player #
		newPlayer.setGUIName(client.name);
		
		/*
		newPlayer.login = sdu.login;
		newPlayer.IPAddress = sdu.remoteHostname;
		newPlayer.port = sdu.remoteServicePort;
		*/

		currentPlayers.put(login, newPlayer);
		// send back service info

		// sendServiceDirectoryUpd ate(sdu.remoteHostname,
		// sdu.remoteServicePort);
		// hostcfg.save("soccergame.txt");

		// SET UP MESSAGING
		// SEND NOTIFICATION ENTRY - ON UPDATEGUI
		Object[] params = new Object[4];
		params[0] = "guiUpdated";
		params[1] = newPlayer.getName();
		params[2] = "guiUpdated";
		// params[3] = new Object[0];
		params[3] = Integer.class.toString();
		// params[3] = Integer.class.getCanonicalName();
		send(client.name, "notify", params);

		// / TODO - MAP OUT MESSAGES

		// send configuration -
		send(client.name, "setCFG", "playerType", "cameraman");

		// GUI INFO
		// ESTABLH CONNECTIVITY TO THE REMOTE GUI !!
		//sendServiceDirectoryUpdate("", "", client.getName(), sdu.hostname,sdu.remoteServicePort, sdu);
		// GUI BEING CREATED

		// set message routes between player & gui

		// add the remote info
		// update player status client & server
		// assign to a team

		// newPlayer.setLogin(login);
		newPlayer.login = login; // MUST DO IT THIS WAY - MUST WAIT FOR REMOTE
									// GUI TO INITIALIZE
		log.info("blue team size " + blueTeam + "red team size " + redTeam);
		if (blueTeam < redTeam) {
			++blueTeam;
			newPlayer.team = "blue";
		} else {
			++redTeam;
			newPlayer.team = "red";
		}

		announce(newPlayer.login + " is here on the " + newPlayer.team
				+ " team!");
		broadCastMsgToPlayers("addLogEntry", "<font color=\"" + newPlayer.team
				+ "\">" + newPlayer.login + "</font> is here on the "
				+ newPlayer.team + " team!");

	}

	public void announce(String msg) {
		log.error(msg);
		announcer.speak(msg);
		broadCastMsgToPlayers("addLogEntry", msg);
	}

	public void removePlayerFromPlay(Player p) {
		log.error("removing " + p.login + " from the game");
		if (p.team.compareTo("red") == 0) {
			--redTeam;
		} else if (p.team.compareTo("blue") == 0) {
			--blueTeam;
		}

		broadCastMsgToPlayers("addLogEntry", " removing <font =\"" + p.team
				+ "\">" + p.login + "</font> from the game");

		currentPlayers.remove(p.login);
		p.IPAddress = "";
		p.isConnected = false;
		p.guiIsInitialized = false;
		p.login = "";
		p.port = 0;

	}

	public void connectionBrokenEvent(IPAndPort conn) {
		// remove gui from game
		// remove player from current players
		// reduce team
		// update others

		log.error("connection broke - " + conn.IPAddress + ":" + conn.port);
		log.error("connection broke - removing " + conn.IPAddress + ":"
				+ conn.port);

		hostcfg.removeServiceEntries(conn.IPAddress, conn.port);

		for (int i = 0; i < players.size(); ++i) {
			Player p = players.get(i);
			log.info(p.login + " @ " + p.IPAddress + ":" + p.port);
			// if (p != null && p.IPAddress == null || p != null &&
			// p.IPAddress.compareTo(conn.IPAddress) == 0 && p.port ==
			// conn.port)
			if (p.IPAddress.compareTo(conn.IPAddress) == 0
					&& p.port == conn.port) {
				removePlayerFromPlay(p);
			}

		}

		updateRoster();
	}

	public void broadCastMsgToPlayers(String fn, String param) {
		broadCastToPlayers(fn, "<html><body>" + param + "</body></html>");
	}

	public void broadCastToPlayers(String fn, Object param) {
		for (int i = 0; i < players.size(); ++i) {
			Player p = players.get(i);
			if (p.GUIName.length() != 0) {
				p.send(p.GUIName, fn, param); // update ui
			}
		}
	}

	public void updateRoster() {
		ArrayList<String> roster = new ArrayList<String>();

		for (int i = 0; i < players.size(); ++i) {
			Player p = players.get(i);
			String info = "<html>" + p.getName() + " ";
			if (p.team.compareTo("red") == 0) {
				info += "<font color=\"red\">" + p.login + "</html>";
			} else {
				info += "<font color=\"blue\">" + p.login + "</html>";
			}

			roster.add(info);
		}
		broadCastToPlayers("rosterUpdate", roster);
	}

	class EndGame extends TimerTask {
		public void run() {
			broadCastToPlayers("addLogEntry", "Ending Game 5 seconds");
			try {
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "5");
				announce("resistance if futile, you will be assimilated in 5, 4, 3, 2, 1");
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "4");
				// announce("4");
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "3");
				// announce("3");
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "2");
				// announce("2");
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "1");
				// announce("1");
				Thread.sleep(1000);
				broadCastToPlayers("addLogEntry", "GAME OVER !");
				announce("GAME OVER");
				Thread.sleep(1000);
				announce("All Your Base Are Belong To Us");

				for (int i = 0; i < players.size(); ++i) {
					Player p = players.get(i);
					if (p.login.length() > 0) {
						removePlayerFromPlay(p);
						updateRoster();
					}
				}
				// remote.disconnectAll();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		SoccerGame game = new SoccerGame("match01");
		game.startService();
		game.startGame();
	}
	
	@Override
	public String getToolTip() {
		return "<html>beginning of LMR soccars</html>";
	}
	
}
