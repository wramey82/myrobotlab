package org.myrobotlab.service;

import java.util.Date;

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
	
	Servo left;
	Servo right;
	
	Date gameEndTime;
	
	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(SteamPunkGame.class.getCanonicalName());

	public SteamPunkGame(String n) {
		super(n, SteamPunkGame.class.getCanonicalName());
	}
	
	
	public void initGame()
	{
		
		voice = new Speech("voice");
		voice.startService();
		
		//tenSecondCountdown();
		
		audio = new AudioFile("audio");
		audio.startService();

		countdown = new Clock("countdown");
		countdown.startService();
		
		eye = new IPCamera("eye");
		eye.startService();

		eyebot = new Arduino("eyebot");
		eyebot.startService();
				
		reactor = new Arduino("reactor");
		reactor.startService();
				
		remote = new RemoteAdapter("remote");
		remote.startService();
		
		left = new Servo("left");
		right = new Servo("right");		
	
		gui = new GUIService("gui");
		gui.startService();
		gui.display();
		
	}
	
	
	public void startGame()
	{
		fiveMinuteWarning();
		
		gameEndTime = Clock.getFutureDate(1, 0);
		
		countdown.addClockEvent(Clock.add(gameEndTime, -60), this.getName(), "oneMinuteWarning", (Object[])null);
		countdown.addClockEvent(Clock.add(gameEndTime, -30), this.getName(), "thirtySecondWarning", (Object[])null);
		countdown.addClockEvent(Clock.add(gameEndTime, -11), this.getName(), "tenSecondCountdown", (Object[])null);
		countdown.addClockEvent(Clock.add(gameEndTime, -1), this.getName(), "failure", (Object[])null);
		countdown.startCountDown(gameEndTime);
		
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public void fiveMinuteWarning()
	{
		voice.setLanguage("en");
		voice.speak("t minus 5 minutes until core meltdown. have a nice day");
		voice.setLanguage("ja");
		voice.speak("t minus 5 minutes until core meltdown. have a nice day");
	}
	
	public void oneMinuteWarning()
	{
		voice.setLanguage("en");
		voice.speak("one minute until core meltdown.  its getting very warm here");
		//voice.setLanguage("ja");
		//voice.speak("one minute until core meltdown.  its getting very warm here");
	}
	
	public void thirtySecondWarning()
	{
		voice.setLanguage("en");
		voice.speak("30 seconds until core meltdown. its time to panic");
		//voice.setLanguage("ja");
		//voice.speak("30 seconds until core meltdown. its time to panic");
	}
	
	public void tenSecondCountdown()
	{
		voice.speak("ten");
		voice.speak("nine");
		voice.speak("eight");
		voice.speak("seven");
		voice.speak("six");
		voice.speak("five");
		voice.speak("four");
		voice.speak("three");
		voice.speak("two");
		voice.speak("one");
		audio.playFile("klaxon.mp3");
	}
	
	public void failure()
	{
		voice.setLanguage("en");
		voice.speak("oh no! the core has melted. Im sorry. thanks for playing");
		audio.playFile("explosion.mp3");
		// reactor - changes
	}

	public void success()
	{
		countdown.stopClock();
		voice.setLanguage("en");
		voice.speak("core is stable. congratulations you have successfully diverted a steampunk disaster");
		audio.playFile("applause.mp3");
	}
	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		SteamPunkGame game = new SteamPunkGame("game");
		game.startService();
		game.initGame();
		//game.startGame();
	}


}
