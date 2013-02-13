package org.myrobotlab.service;

import java.util.Date;

import org.myrobotlab.logging.Level;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.service.data.Pin;

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
	Servo hopper;

	Date gameEndTime;

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(SteamPunkGame.class.getCanonicalName());

	public SteamPunkGame(String n) {
		super(n, SteamPunkGame.class.getCanonicalName());
	}

	public void initGame() {
		hopper = new Servo("hopper");
		hopper.startService();

		reactor = new Arduino("reactor");
		reactor.startService();
		reactor.setSerialDevice("/dev/ttyUSB0", 57600, 8, 1, 0);
		reactor.broadcastState();
		sleep(1000);

		gui = new GUIService("gui");
		gui.startService();
		gui.display();

		reactor.digitalReadPollingStart(4);
		this.subscribe("publishPin", reactor.getName(), "publishPin", Pin.class);

		reactor.attach(hopper.getName(), 8);
		hopper.moveTo(90);
		// hopper.moveTo(0);

		voice = new Speech("voice");
		voice.startService();

		// tenSecondCountdown();

		audio = new AudioFile("audio");
		audio.startService();

		countdown = new Clock("countdown");
		countdown.startService();

		eye = new IPCamera("eye");
		eye.startService();

		eyebot = new Arduino("eyebot");
		eyebot.startService();
		eyebot.setSerialDevice("/dev/rfcomm0", 57600, 8, 1, 0);
		eyebot.broadcastState();
		sleep(1000);

		remote = new RemoteAdapter("remote");
		remote.startService();

		left = new Servo("left");
		left.startService();
		right = new Servo("right");
		right.startService();

		eyebot.attach(left.getName(), 8);
		eyebot.attach(right.getName(), 9);

		left.broadcastState();
		right.broadcastState();

	}

	boolean gameStarted = false;

	public void startGame() {
		fiveMinuteWarning();

		gameEndTime = Clock.getFutureDate(1, 0);

		countdown.addClockEvent(Clock.add(gameEndTime, -60), this.getName(), "oneMinuteWarning", (Object[]) null);
		countdown.addClockEvent(Clock.add(gameEndTime, -30), this.getName(), "thirtySecondWarning", (Object[]) null);
		countdown.addClockEvent(Clock.add(gameEndTime, -11), this.getName(), "tenSecondCountdown", (Object[]) null);
		countdown.addClockEvent(Clock.add(gameEndTime, -1), this.getName(), "failure", (Object[]) null);
		countdown.startCountDown(gameEndTime);

		gameStarted = true;

	}

	public void publishPin(Pin p) {
		if (gameStarted & p.value == 0) {
			success();
		}
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	public void fiveMinuteWarning() {
		voice.setLanguage("en");
		voice.speak("t minus 5 minutes until core meltdown. have a nice day");
		voice.setLanguage("ja");
		voice.speak("t minus 5 minutes until core meltdown. have a nice day");
	}

	public void oneMinuteWarning() {
		voice.setLanguage("en");
		voice.speak("one minute until core meltdown.  its getting very warm here");
		// voice.setLanguage("ja");
		// voice.speak("one minute until core meltdown.  its getting very warm here");
	}

	public void thirtySecondWarning() {
		voice.setLanguage("en");
		voice.speak("30 seconds until core meltdown. its time to panic");
		// voice.setLanguage("ja");
		// voice.speak("30 seconds until core meltdown. its time to panic");
	}

	public void tenSecondCountdown() {
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

	public void failure() {
		reactor.pinMode(12, Arduino.OUTPUT);
		reactor.digitalWrite(12, 0);
		reactor.digitalWrite(12, 1);
		reactor.digitalWrite(12, 0);
		reactor.digitalWrite(12, 1);
		reactor.digitalWrite(12, 0);
		reactor.digitalWrite(12, 1);

		voice.setLanguage("en");
		voice.speak("oh no! the core has melted. Im sorry. thanks for playing");
		audio.playFile("explosion.mp3");

		hopper.moveTo(0);
		// reactor - changes
	}

	public void success() {
		countdown.stopClock();
		voice.setLanguage("en");
		voice.speak("core is stable. congratulations you have successfully diverted a steampunk disaster");
		audio.playFile("applause.mp3");
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		SteamPunkGame game = new SteamPunkGame("game");
		game.startService();
		game.initGame();
		// game.startGame();
	}

}
