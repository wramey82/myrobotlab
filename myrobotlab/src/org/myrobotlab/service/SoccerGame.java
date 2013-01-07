package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

/*
 * TODO :
 *     AuthenticationProvider interface ????
 *     WebServiceHandler interface ????
 */
public class SoccerGame extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(SoccerGame.class.getCanonicalName());

	public HashMap<String, Object> session = new HashMap<String, Object>();

	int maxPlayers = 6;
	Date gameEndTime = null;
	Date gameStartTime = null;
	// clock thread
	String team0 = "team0";
	String team1 = "team1";

	ArrayList<Player> players = new ArrayList<Player>();

	public class Player {
		int number;
		int fouls;
		String name;
		String team;
		String status;
		Arduino arduino = null;
	}

	public SoccerGame(String n) {
		super(n, SoccerGame.class.getCanonicalName());
		for (int i = 0; i < maxPlayers; ++i) {
			Player p = new Player();
			p.arduino = new Arduino("p" + i);
			p.number = i;
			p.name = "p" + i;
			p.team = (i < 3) ? team0 : team1;
			p.status = "available";
			players.add(p);
		}
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	// TODO - public exec (Message ? ) handler
	public void logon(String name, String password) {
		log.info("logon " + name + " password " + password);
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		SoccerGame template = new SoccerGame("soccergame");
		template.startService();

		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();

	}

}
