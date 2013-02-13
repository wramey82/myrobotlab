package org.myrobotlab.service;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;

public class SteamPunkBeerBot extends Service {

	IPCamera beerEye;
	Roomba beerbot;

	GUIService beergui;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(SteamPunkBeerBot.class.getCanonicalName());

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
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		SteamPunkBeerBot game = new SteamPunkBeerBot("game");
		game.startService();
		game.startGame();
	}

}
