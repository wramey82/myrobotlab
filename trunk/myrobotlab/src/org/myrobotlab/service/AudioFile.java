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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.player.advanced.AdvancedPlayer;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class AudioFile extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(AudioFile.class.getCanonicalName());
	transient AePlayWave wavPlayer = new AePlayWave();

	public AudioFile(String n) {
		super(n, AudioFile.class.getCanonicalName());
	}

	public void play(String name) {
		playFile("audioFile/" + name + ".mp3", false);
	}

	public void playResource(String filename) {
		playResource(filename, false);
	}

	public void playFileBlocking(String filename) {
		playFile(filename, true);
	}

	public void playFile(String filename) {
		playFile(filename, false);
	}

	public void playFile(String filename, Boolean isBlocking) {
		playFile(filename, isBlocking, false);
	}

	public void playResource(String filename, Boolean isBlocking) {
		playFile(filename, isBlocking, true);
	}

	public void playFile(String filename, Boolean isBlocking, Boolean isResource) {
		final AdvancedPlayer player;

		// TODO - cache file - for quick playing again - delete cache after set
		// time
		try {
			
			InputStream is;
			if (isResource) {
				is = AudioFile.class.getResourceAsStream(filename);
			} else {
				is = new FileInputStream(filename);
			}
			
			BufferedInputStream bis = new BufferedInputStream(is);
			player = new AdvancedPlayer(bis);
			
			if (!isBlocking) {
				new Thread() {
					public void run() {
						try {
							invoke("started");
							player.play();
							invoke("stopped");
						} catch (Exception e) {
							System.out.println(e);
						}
					}
				}.start();
			} else {
				invoke("started");
				player.play();
				invoke("stopped");
			}

		} catch (Exception e) {
			Logging.logException(e);
			log.error("Problem playing file " + filename);
			return;
		}

	}

	public void started() {
		log.info("started");
	}

	public void stopped() {
		log.info("stopped");
	}

	/* BEGIN - TODO - reconcile - find how javazoom plays wave */

	public void playWAV(String name) {
		// new AePlayWave("audioFile/" + name + ".wav").start();
		wavPlayer.playAeWavFile("audioFile/" + name + ".wav");
	}

	public void playWAVFile(String name) {
		// new AePlayWave(name).start();
		wavPlayer.playAeWavFile(name);
	}

	public void playBlockingWavFile(String filename) {
		wavPlayer.playAeWavFile(filename);
	}

	enum Position {
		LEFT, RIGHT, NORMAL
	};

	public class AePlayWave extends Thread {

		private String filename;

		private Position curPosition;

		private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

		public AePlayWave() {
		}

		public AePlayWave(String wavfile) {
			filename = wavfile;
			curPosition = Position.NORMAL;
		}

		public AePlayWave(String wavfile, Position p) {
			filename = wavfile;
			curPosition = p;
		}

		public void playAeWavFile(String filename) {
			playAeWavFile(filename, Position.LEFT);
		}

		public void playAeWavFile(String filename, Position p) {

			this.filename = filename;
			this.curPosition = p;

			File soundFile = new File(filename);
			if (!soundFile.exists()) {
				System.err.println("Wave file not found: " + filename);
				return;
			}

			AudioInputStream audioInputStream = null;
			try {
				audioInputStream = AudioSystem.getAudioInputStream(soundFile);
			} catch (UnsupportedAudioFileException e1) {
				e1.printStackTrace();
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}

			AudioFormat format = audioInputStream.getFormat();
			SourceDataLine auline = null;
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			try {
				auline = (SourceDataLine) AudioSystem.getLine(info);
				auline.open(format);
			} catch (LineUnavailableException e) {
				logException(e);
				return;
			} catch (Exception e) {
				logException(e);
				return;
			}

			if (auline.isControlSupported(FloatControl.Type.PAN)) {
				FloatControl pan = (FloatControl) auline.getControl(FloatControl.Type.PAN);
				if (curPosition == Position.RIGHT)
					pan.setValue(1.0f);
				else if (curPosition == Position.LEFT)
					pan.setValue(-1.0f);
			}

			auline.start();
			int nBytesRead = 0;
			byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

			try {
				while (nBytesRead != -1) {
					nBytesRead = audioInputStream.read(abData, 0, abData.length);
					if (nBytesRead >= 0)
						auline.write(abData, 0, nBytesRead);
				}
			} catch (IOException e) {
				logException(e);
				return;
			} finally {
				auline.drain();
				auline.close();
			}

		}

		public Boolean playingFile(Boolean b) {
			return b;
		}

		// for non-blocking use
		public void run() {
			playAeWavFile(filename, curPosition);
		}
	}

	@Override
	public String getToolTip() {
		return "Plays back audio file. Can block or multi-thread play";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);

		AudioFile player = new AudioFile("player");
		player.startService();
		//player.playResource("Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		player.playResource("/resource/Clock/tick.mp3");
		//player.playBlockingWavFile("I am ready.wav");
		// player.play("hello my name is audery");
		//player.playWAV("hello my name is momo");
	}

}
