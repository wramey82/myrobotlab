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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Speech extends Service {

	public final static Logger LOG = Logger.getLogger(Speech.class.getCanonicalName());
	private Voice myVoice = null;
	private boolean initialized = false;
	public AudioFile speechAudioFile = null;

	public Speech(String n) {
		super(n, Speech.class.getCanonicalName());
		listAllVoices();
		LOG.info("Using voice: " + cfg.get("voiceName"));

		// The VoiceManager manages all the voices for FreeTTS.
		VoiceManager voiceManager = VoiceManager.getInstance();
		myVoice = voiceManager.getVoice(cfg.get("voiceName"));

		if (myVoice == null) {
			LOG.error("Cannot find a voice named " + cfg.get("voiceName")
					+ ".  Please specify a different voice.");
		} else {
			initialized = true;
		}

	}

	// TODO - changeVoice (String newVoice)
	public void loadDefaultConfiguration() {
		// private String voiceName = "kevin16";
		// private String voiceName = "alan";
		// System.setProperty("mbrola.base",
		// "/home/gperry/workspace/myrobotlab/thirdParty/jars/mbrola");
		cfg.set("voiceName", "kevin16");
		// cfg.set("voiceName", "us1");
		cfg.set("volume", 100);
		// cfg.set("voiceName", "alan", "great scotch voice only numbers");
	}

	public void lostTrack() {
		speak("where did it go?");
		speak("where did it go?");
	}

	/**
	 * Example of how to list all the known voices.
	 */
	public static void listAllVoices() {
		LOG.info("All voices available:");
		VoiceManager voiceManager = VoiceManager.getInstance();
		Voice[] voices = voiceManager.getVoices();
		for (int i = 0; i < voices.length; i++) {
			LOG.info("    " + voices[i].getName() + " ("
					+ voices[i].getDomain() + " domain)");
		}
	}

	@Override
	public void startService() {
		/*
		 * Allocates the resources for the voice.
		 */
		myVoice.allocate();
	}

	@Override
	public void stopService() {
		/* Clean up and leave. */
		myVoice.deallocate();
	}

	public void speak(Float toSpeak) {
		if (initialized) {
			myVoice.speak(toSpeak.toString());
		} else {
			LOG.error("can not speak - uninitialized");
		}
	}

	public void speak(String toSpeak) {
		if (cfg.get("isATT", false)) {
			if (speechAudioFile == null) {
				speechAudioFile = new AudioFile("speechAudioFile");
			}

			String voiceName = cfg.get("ATTVoiceName", "audrey");
			String audioFile = "audioFile/att/" + voiceName + "/" + toSpeak
					+ ".wav";
			File f = new File(audioFile);
			LOG.info(audioFile
					+ (f + (f.exists() ? " is found " : " is missing ")));

			if (!f.exists()) {
				// if the wav file does not exist fetch it from att site
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("voice", voiceName);
				params.put("txt", toSpeak);
				params.put("speakButton", "SPEAK");
				HTTPClient.HTTPData data = HTTPClient.post(
						"http://192.20.225.36/tts/cgi-bin/nph-talk", params);
				String redirect = null;
				try {
					redirect = "http://"
							+ data.method.getURI().getHost()
							+ data.method.getResponseHeader("location")
									.getValue();
					HTTPClient.HTTPData data2 = HTTPClient.get(redirect);

					FileOutputStream fos = new FileOutputStream(audioFile);
					fos.write(data2.method.getResponseBody());

				} catch (URIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			// audio file it
			speechAudioFile.playWAVFile(audioFile);

		} else { // festival tts

			if (initialized) {
				myVoice.speak(toSpeak);
			} else {
				LOG.error("can not speak - uninitialized");
			}
		}
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		Speech speech = new Speech("speech");
		speech.startService();
		speech.speak("hello");
		speech.startService();
		speech.cfg.set("isATT", true);
		speech.speak("hello");
	
	}

	@Override
	public String getToolTip() {
		return "<html>text to speech module</html>";
	}
	
}
