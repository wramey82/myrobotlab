package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class SteamPunkGame extends Service {

	Clock countdown;
	Speech voice;
	IPCamera eye;
	Arduino eyebot;		
	Arduino reactor;
	AudioFile audio;
	RemoteAdapter remote;
	GUIService gui;
	
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(SteamPunkGame.class.getCanonicalName());

	public SteamPunkGame(String n) {
		super(n, SteamPunkGame.class.getCanonicalName());
	}
	
	public void startGame()
	{
		countdown = new Clock("countdown");
		countdown.startService();
		
		voice = new Speech("voice");
		voice.startService();

		eye = new IPCamera("eye");
		eye.startService();

		eyebot = new Arduino("eyebot");
		eyebot.startService();
				
		reactor = new Arduino("reactor");
		reactor.startService();
		
		audio = new AudioFile("audio");
		audio.startService();
		
		remote = new RemoteAdapter("remote");
		remote.startService();
	
		gui = new GUIService("gui");
		gui.startService();
		gui.display();
		
	}
	
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public void oneMinuteWarning()
	{
		voice.speak("one minute until core meltdown");
	}
	
	public void thirtySecondWarning()
	{
		voice.speak("30 seconds until core meltdown");
	}
	
	public void klaxons()
	{
		audio.playFile("klaxons.mp3");
	}
	
	public void explosion()
	{
		audio.playFile("explosion.mp3");
		// reactor - changes
	}

	public void success()
	{
		audio.playFile("success.mp3", true);
		audio.playFile("applause.mp3");
		// reactor - changes
	}
	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		SteamPunkGame game = new SteamPunkGame("game");
		game.startService();
		game.startGame();
	}


}
