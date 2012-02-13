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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class Speech extends Service {

	/*
	 * Speech supports 2 different text to speech systems
	 * One is FreeTTS and the other is a remote/cloud access of ATTs online implementation.
	 * The FreeTTS is a complete voice system and can be loaded with different external/thirdParty
	 * voices.  
	 * 
	 * The ATT is probably on the edge of licensing. An online system at ATT is available to use.
	 * This service will send the text to that online service, download the file and play it.
	 * Once it is downloaded, it will use the same file each time the text phrase is requested.
	 * 
	 * There is a front-end set of functions and a back-end set of functions.
	 * The front-end concerns how the calling process and request will be handled.
	 * There are 4 types of speaking.
	 * 		speakNormal - 	when this function is utilized, it means any simultaneous requests for speech will
	 * 						be dropped.  This most closely approximates human speech.  You may have a bazillion
	 * 						thoughts going on in your head but you only have 1 mouth.
	 * 		speakQueued -	this function queues up all of the requests for speech and will speak each one until done.
	 * 						This can have the behavior of being very out of context, as speaking takes considerable time
	 * 						relative to many other processes.
	 * 		speakBlocking - This blocks the calling thread until the speak function is finished. I can see very little
	 * 						meaningful use for this.
	 * 		speakMulti 	  - This will create threads for each requests possibly allowing every speech thread to complete
	 * 						in the same time.  (very Cybil)
	 * The back-end are just types of speech engines (ATT, FREETTS)
	 */
	
	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(Speech.class.getCanonicalName());
	
	// TODO - seperate all of the var into appropriate parts - ie Global ATT Google FreeTTS
	
	transient private Voice myVoice = null;
	private boolean initialized = false;
	public AudioFile speechAudioFile = null;
	
	public static enum FrontendType {NORMAL, QUEUED, BLOCKING, MULTI};
	public static enum BackendType {ATT, FREETTS, GOOGLE};
	
	public String voiceName = "audrey"; // both voice systems have a list of available voice names
	public int volume = 100;

	public FrontendType frontendType = FrontendType.NORMAL;
	public BackendType backendType = BackendType.GOOGLE;

	boolean fileCacheInitialized = false;
	
	private boolean isSpeaking = false;
	
	final public static HashMap<String, String> googleLanguageMap = new HashMap<String, String>();
	
	public Speech(String n) {
		super(n, Speech.class.getCanonicalName());
		LOG.info("Using voice: " + voiceName);
		
		googleLanguageMap.put("english", "en");
		googleLanguageMap.put("danish", "da");
		googleLanguageMap.put("dutch", "nl");
		googleLanguageMap.put("german", "de");
		googleLanguageMap.put("french", "fr");
		googleLanguageMap.put("japanese", "ja");
		googleLanguageMap.put("portuguese", "pt");
	}

	public void loadDefaultConfiguration() {
	}
	
	// having this synchronization and frontend type
	// will probably negate the need for a isSpeaking event
	@SuppressWarnings("unused")
	public synchronized Boolean isSpeaking(Boolean b)
	{
		LOG.error("isSpeaking " + b);
		isSpeaking = b;
		return isSpeaking;
	}
	
	public void setFrontendType(String t)
	{
		if ("NORMAL".equals(t))
		{
			frontendType = FrontendType.NORMAL;
		} else if ("QUEUED".equals(t))
		{
			frontendType = FrontendType.QUEUED;
		} else if ("BLOCKING".equals(t))
		{
			frontendType = FrontendType.BLOCKING;
		} else if ("MULTI".equals(t))
		{
			frontendType = FrontendType.MULTI;
		} else {
			LOG.error("type " + t + " not supported");
		}
	}

	public final static String BACKEND_TYPE_ATT = "ATT";
	public final static String BACKEND_TYPE_FREETTS = "FREETTS";
	public final static String BACKEND_TYPE_GOOGLE = "GOOGLE";
	
	
	public void setBackendType(String t)
	{
		if (BACKEND_TYPE_ATT.equals(t))
		{
			backendType = BackendType.ATT;
		} else if (BACKEND_TYPE_FREETTS.equals(t))
		{
			backendType = BackendType.FREETTS;
		} else if (BACKEND_TYPE_GOOGLE.equals(t))
		{
			backendType = BackendType.GOOGLE;
		} else {
			LOG.error("type " + t + " not supported");
		}
	}
	
	// get list of voices from back-end
	public ArrayList<String> listAllVoices() {
		LOG.info("All voices available:");
		ArrayList<String> voiceList = new ArrayList<String>();
		if (backendType == BackendType.FREETTS)
		{
			VoiceManager voiceManager = VoiceManager.getInstance();
			Voice[] voices = voiceManager.getVoices();
			for (int i = 0; i < voices.length; i++) {
				LOG.info("    " + voices[i].getName() + " ("
						+ voices[i].getDomain() + " domain)");
				voiceList.add(voices[i].getName());
			}
		} else if (backendType == BackendType.ATT)
		{
			// TODO get list of att voices
			// could do it dynamically... not yet
			voiceList.add("crystal");
			voiceList.add("mike");
			voiceList.add("rich");
			voiceList.add("lauren");

			voiceList.add("audrey");
			
		} else {
			LOG.error("voice backendType " + backendType + " not supported");
		}
		
		return voiceList;
	}


	@Override
	public void stopService() {		
		if (myVoice != null && myVoice.isLoaded())
		{
			myVoice.deallocate();
		}
		if (speechAudioFile != null)
		{
			speechAudioFile.stopService();
			speechAudioFile = null;
		}
		super.stopService();
	}

	
	// front-end functions 	
	public void speak(String toSpeak) {
		if (frontendType == FrontendType.NORMAL)
		{
			speakNormal(toSpeak);
		} else if (frontendType == FrontendType.QUEUED)
		{
			//speakQueued
		}
	}
	
	public boolean speakNormal(String toSpeak)
	{
		// idealy in "normal" speech our ideas are queued
		// until we have time to actually say them
		
		if (backendType == BackendType.ATT) {
			// in(createMessage(name, "speakATT", toSpeak));
			LOG.error("no longer supported as per the deathstar's liscense agreement");
		} else if (backendType == BackendType.FREETTS) { // festival tts
			// speakFreeTTS(toSpeak);
			in(createMessage(getName(), "speakFreeTTS", toSpeak));
		} else if (backendType == BackendType.GOOGLE) { // festival tts
			in(createMessage(getName(), "speakGoogle", toSpeak));
		} else {
			LOG.error("back-end speech backendType " + backendType + " not supported ");
		}

		return true;
		
	}

	// BACK-END FUNCTIONS BEGIN ------------------------------------------------
	public void speakATT(String toSpeak)
	{
		if (speechAudioFile == null) {
			speechAudioFile = new AudioFile("speechAudioFile");
		}
		
		if (!fileCacheInitialized)
		{
			boolean success = (new File("audioFile/att/" + voiceName)).mkdirs();
		    if (!success) {
		      LOG.debug("could not create directory: audioFile/att/" + voiceName);
		    } else {
		    	fileCacheInitialized = true;
		    }
		}
		
		String audioFile = "audioFile/att/" + voiceName + "/" + toSpeak + ".wav";
		File f = new File(audioFile);
		LOG.info(f + (f.exists() ? " is found " : " is missing "));

		if (!f.exists()) {
			// if the wav file does not exist fetch it from att site
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("voice", voiceName);
			params.put("txt", toSpeak);
			params.put("speakButton", "SPEAK");
			/*
			As per http://www2.research.att.com/~ttsweb/tts/faq.php & Mark & ATT
			 I have removed the cgi link - will be moving to Google speech as it does
			 not have such restrictions
			*/
			// HTTPClient.HTTPData data = HTTPClient.post("http://192.20.225.36/tts/cgi-bin/nph-talk", params);
			HTTPClient.HTTPData data = null;
			String redirect = null;
			try {
				redirect = "http://"
						+ data.method.getURI().getHost()
						+ data.method.getResponseHeader("location")
								.getValue();
				HTTPClient.HTTPData data2 = HTTPClient.get(redirect);

				FileOutputStream fos = new FileOutputStream(audioFile);
				fos.write(data2.method.getResponseBody());

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				logException(e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logException(e);
			}

		}

		// audio file it - THIS BLOCKS - YAY !  ITS A GOOD THING ! -  JUST DONT CALL IT DIRECTY FROM ANOTHER SERVICE
		// @Blocks 
		speechAudioFile.playWAVFile(audioFile);		
	}
	
	public void speakFreeTTS(String toSpeak)
	{
		if (myVoice == null)
		{
			// The VoiceManager manages all the voices for FreeTTS.
			VoiceManager voiceManager = VoiceManager.getInstance();
			Voice[] possibleVoices = voiceManager.getVoices();
			
			LOG.error("possible voices");
			for (int i = 0; i < possibleVoices.length; ++i)
			{
				LOG.error(possibleVoices[i].getName());				
			}
			voiceName = "kevin16";
			myVoice = voiceManager.getVoice(voiceName);

			if (myVoice == null) {
				LOG.error("Cannot find a voice named " + voiceName
						+ ".  Please specify a different voice.");
			} else {
				initialized = true;
			}
		}
		
		try {
			// TODO - do pre-speak not here	if (!myVoice.isLoaded())
			myVoice.allocate();
		LOG.info("voice allocated");
		} catch (Exception e)
		{
			LOG.error(e);
		}

		if (initialized) {
			invoke("isSpeaking", true);
			myVoice.speak(toSpeak);
			invoke("isSpeaking", false);
		} else {
			LOG.error("can not speak - uninitialized");
		}
		
	}
	
	public void setLanguage(String l)
	{
		in(createMessage(getName(), "queueSetLanguage", l));
	}
	
	public void queueSetLanguage(String l)
	{
		fileCacheInitialized = false;
		language = l;
	}
	
	//String language = "eng-us";
	String language = "en";
	public void speakGoogle(String toSpeak)
	{
		if (speechAudioFile == null) {
			speechAudioFile = new AudioFile("speechAudioFile");
			speechAudioFile.startService();
		}
		
		if (!fileCacheInitialized)
		{
			boolean success = (new File("audioFile/google/" + language + "/" + voiceName)).mkdirs();
		    if (!success) {
		      LOG.debug("could not create directory: audioFile/google/" + language + "/" + voiceName);
		    } else {
		    	fileCacheInitialized = true;
		    }
		}
		
		String audioFile = "audioFile/google/" + language + "/" + voiceName + "/" + toSpeak + ".mp3";
		File f = new File(audioFile);
		LOG.info(f + (f.exists() ? " is found " : " is missing "));

		if (!f.exists()) {
			// if the wav file does not exist fetch it from att site
			/*
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("voice", voiceName);
			params.put("txt", toSpeak);
			params.put("speakButton", "SPEAK");
			*/
			// rel="noreferrer"
			// http://translate.google.com/translate_tts?tl=en&q=text
			// http://translate.google.com/translate_tts?tl=fr&q=Bonjour
			// http://translate.google.com/translate_tts?tl=en&q=hello%20there%20my%20good%20friend
			
			try {
				URI uri = new URI ("http", null, "translate.google.com", 80, "/translate_tts", "tl="+language+"&q=" + toSpeak, null);
				LOG.error(uri.toASCIIString());
				HTTPClient.HTTPData data = HTTPClient.get(uri.toASCIIString());
				
				FileOutputStream fos = new FileOutputStream(audioFile);
				fos.write(data.method.getResponseBody());

			} catch (Exception e) {
				Service.logException(e);
			}

		}

		// audio file it
		// boolean isBlocking = true; is Blocking YAY - dont call from different service
		
		invoke("isSpeaking", true);
		speechAudioFile.playFile(audioFile, true);	
		invoke("isSpeaking", false);		
	}
	
	// codes - http://code.google.com/apis/language/translate/v2/using_rest.html
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		
		Speech speech = new Speech("speech");
		speech.startService();
//		speech.setBackendType(BACKEND_TYPE_FREETTS);
		speech.setBackendType(BACKEND_TYPE_GOOGLE);
//		speech.setLanguage("fr");
		speech.speak("it is a pleasure to meet you I am speaking.  I do love to speak. What should we talk about.");
		speech.speak("hello! this is an attempt to generate inflection did it work?");
		speech.speak("hello there. this is a long and detailed message");
		speech.speak("1 2 3 4 5 6 7 8 9 10, i know how to count");
		speech.speak("the time is 12:30");
		speech.speak("oink oink att is good but not so good");
		speech.speak("num, num, num, num, num");
		speech.speak("charging");
		speech.speak("thank you");
		speech.speak("good bye");
		speech.speak("I believe I have bumped into something");
		speech.speak("Ah, I have found a way out of this situation");
		speech.speak("aaaaaaaaah, long vowels sound");
		
	}

	@Override
	public String getToolTip() {
		return "<html>text to speech module</html>";
	}
	
}
