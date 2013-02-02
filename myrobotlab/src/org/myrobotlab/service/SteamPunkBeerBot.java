package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class SteamPunkBeerBot extends Service {

	IPCamera beerEye;
	Roomba beerbot;

	GUIService beergui;

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(SteamPunkBeerBot.class.getCanonicalName());

	public SteamPunkBeerBot(String n) {
		super(n, SteamPunkBeerBot.class.getCanonicalName());
	}

	public void startGame() {

		beerbot = new Roomba("beerbot");
		beerbot.startService();

		beerEye = new IPCamera("beerEye");
		beerEye.startService();

		beergui = new GUIService("beergui");
		beergui.startService();
		beergui.display();

		beerEye.invoke("setEnableControls", false); // invoke to send
													// notifications

	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		SteamPunkBeerBot game = new SteamPunkBeerBot("game");
		game.startService();
		game.startGame();
	}

}
