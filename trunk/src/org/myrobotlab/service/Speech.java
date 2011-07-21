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
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.URIException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
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
	
	transient private Voice myVoice = null;
	private boolean initialized = false;
	public AudioFile speechAudioFile = null;
	
	public static enum FrontendType {NORMAL, QUEUED, BLOCKING, MULTI};
	public static enum BackendType {ATT, FREETTS};
	
	public String voiceName = "audrey"; // both voice systems have a list of available voice names
	public int volume = 100;

	public FrontendType frontendType = FrontendType.NORMAL;
	public BackendType backendType = BackendType.ATT;

	boolean fileCacheInitialized = false;
	
	private boolean isSpeaking = false;
	
	public Speech(String n) {
		super(n, Speech.class.getCanonicalName());
		LOG.info("Using voice: " + voiceName);
	}

	public void loadDefaultConfiguration() {
	}
	
	// having this synchronization and frontend type
	// will probably negate the need for a isSpeaking event
	private synchronized Boolean isSpeaking(Boolean b)
	{
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

	public void setBackendType(String t)
	{
		if ("ATT".equals(t))
		{
			backendType = BackendType.ATT;
		} else if ("FREETTS".equals(t))
		{
			backendType = BackendType.FREETTS;
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
		// if we are already saying something
		// and a new request to say something comes in
		// we dump it we can only do so much, you know?
		// must not block
		if (!isSpeaking)
		{
			isSpeaking(true);
			if (backendType == BackendType.ATT) {
				//speakATT(toSpeak);//
				//invoke ("speakATT", toSpeak);
				in(createMessage(name, "speakATT", toSpeak));

			} else if (backendType == BackendType.FREETTS) { // festival tts
				speakFreeTTS(toSpeak);
			} else {
				LOG.error("back-end speech backendType " + backendType + " not supported ");
			}
			isSpeaking(false);
			return true;
		}
		
		return false;
		
	}

	// back-end functions
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
	}
	
	public void speakFreeTTS(String toSpeak)
	{
		if (myVoice == null)
		{
			// The VoiceManager manages all the voices for FreeTTS.
			VoiceManager voiceManager = VoiceManager.getInstance();
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
			myVoice.speak(toSpeak);
		} else {
			LOG.error("can not speak - uninitialized");
		}
		
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		Speech speech = new Speech("speech");
		speech.startService();
		speech.speak("num, num, num, num, num");
		speech.speak("charging");
		speech.speak("thank you");
		speech.speak("good bye");
		speech.speak("I believe I have bumped into something");
		speech.speak("Ah, I have found a way out of this situation");
	
	}

	@Override
	public String getToolTip() {
		return "<html>text to speech module</html>";
	}
	
}
