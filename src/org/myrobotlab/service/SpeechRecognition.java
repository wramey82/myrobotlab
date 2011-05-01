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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.speech.recognition.GrammarException;

import org.apache.log4j.Logger;

import org.myrobotlab.framework.Service;
import org.myrobotlab.speech.DialogManager;
import org.myrobotlab.speech.NewGrammarDialogNodeBehavior;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.TimerPool;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class SpeechRecognition extends Service {

	public final static Logger LOG = Logger.getLogger(SpeechRecognition.class
			.getCanonicalName());

	Microphone microphone = null;
	ConfigurationManager cm = null;
	Recognizer recognizer = null;
	Thread listener = null;
	DialogManager dialogManager = null;
	SpeechProcessor speechProcessor = null;

	// boolean isListening = true;

	public SpeechRecognition(String n) {
		super(n, SpeechRecognition.class.getCanonicalName());
	}

	// TODO - changeVoice (String newVoice)
	public void loadDefaultConfiguration() {
		cfg.set("volume", 75);
		// cfg.set("grammerConfigXML", "dialogManager.xml");
		cfg.set("grammarConfigXML", "simple.xml");
		// cfg.set("useDialogManager", false);
	}

	public String recognized(String speech) {
		return speech;
	}

	public void stopRecording() {
		microphone.stopRecording();

	}

	public void startRecording() {
		microphone.startRecording();
	}

	public boolean isRecording() {
		return microphone.isRecording();
	}

	public void startService() {
		super.startService();
		speechProcessor = new SpeechProcessor(this);
		speechProcessor.start();
	}

	public void stopService() {
		if (speechProcessor != null) {
			speechProcessor.isRunning = false;
		}
		speechProcessor = null;
		super.stopService();
	}

	// TODO - lame - quick hack to give the "run" function back to
	// SpeechRecognition
	// just surrounded with another inner class
	class SpeechProcessor extends Thread {
		SpeechRecognition myService = null;
		public boolean isRunning = false;

		public SpeechProcessor(SpeechRecognition myService) {
			this.myService = myService;
		}

		public void run() {

			/*
			 * if (args.length > 0) { cm = new ConfigurationManager(args[0]); }
			 * else { cm = new
			 * ConfigurationManager(HelloWorld.class.getResource(
			 * "helloworld.config.xml")); }
			 */
			isRunning = true;
			// cm = new
			// ConfigurationManager(HelloWorld.class.getResource("helloworld.config.xml"));
			URL url = this.getClass().getResource(cfg.get("grammarConfigXML"));
			cm = new ConfigurationManager(url);

			// PropertySheet ps = cm.getPropertySheet("jsgfGrammar");
			// String grammarLocation = ps.getString("grammarLocation");
			// cm = new ConfigurationManager("SpeechRecognition");

			if (cfg.get("grammarConfigXML").compareTo("dialogManager.xml") == 0) {
				dialogManager = (DialogManager) cm.lookup("dialogManager");

				dialogManager.generateGrammarFiles(myService);

				// TODO - generate below
				// MyBehavior()
				dialogManager.addNode("service", new MyBehavior());
				dialogManager.addNode("tilt", new MyBehavior());
				dialogManager.addNode("pan", new MyBehavior());

				dialogManager.setInitialNode("service");

				System.out.println("Loading dialogs ...");

				try {
					dialogManager.allocate();

					// test.getGrammar().getRuleGrammar();
					// LOG.info(email.getGrammar().getRuleGrammar());

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.out.println("Running  ...");

				dialogManager.go();

				System.out.println("Cleaning up  ...");

				dialogManager.deallocate();

			} else {

				// start the word recognizer
				recognizer = (Recognizer) cm.lookup("recognizer");
				recognizer.allocate();

				// start the microphone or exit if the programm if this is not
				// possible
				microphone = (Microphone) cm.lookup("microphone");
				if (!microphone.startRecording()) {
					LOG.error("Cannot start microphone.");
					recognizer.deallocate();
					System.exit(1);
				}

				// System.out.println("Say: (Good morning | Hello) ( Bhiksha | Evandro | Paul | Philip | Rita | Will )");

				// loop the recognition until the programm exits.
				while (isRunning) {

					LOG.info("listening");

					Result result = recognizer.recognize();

					//LOG.error(result.getBestPronunciationResult()); - TODO - try it
					
					if (result != null) {
						String resultText = result.getBestFinalResultNoFiller();
						if (resultText.length() > 0) {
							recognized(resultText);
							if (resultText.length() > 0) {
								invoke("publish", resultText);
							} else {
								invoke("publish", "what");
							}

						}
						LOG.error("You said: " + resultText + '\n');
					} else {
						try {
							Thread.sleep(300);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						recognized("what did you say");
						LOG.info("I can't hear what you said.\n");
					}
				}
			}

		}

	}

	public String publish(String word) {
		return word;
	}

	/**
	 * Defines the standard behavior for a node. The standard behavior is:
	 * <ul>
	 * <li>On entry the set of sentences that can be spoken is displayed.
	 * <li>On recognition if a tag returned contains the prefix 'dialog_' it
	 * indicates that control should transfer to another dialog node.
	 * </ul>
	 * 
	 * 
	 */
	class MyBehavior extends NewGrammarDialogNodeBehavior {

		private Collection<String> sampleUtterances;

		/** Executed when we are ready to recognize */
		public void onReady() {
			super.onReady();
			help();
		}

		/**
		 * Displays the help message for this node. Currently we display the
		 * name of the node and the list of sentences that can be spoken.
		 */
		protected void help() {
			System.out.println(" ======== " + getGrammarName() + " =======");
			dumpSampleUtterances();
			System.out.println(" =================================");
		}

		/**
		 * Executed when the recognizer generates a result. Returns the name of
		 * the next dialog node to become active, or null if we should stay in
		 * this node
		 * 
		 * @param result
		 *            the recongition result
		 * @return the name of the next dialog node or null if control should
		 *         remain in the current node.
		 */
		public String onRecognize(Result result) throws GrammarException {
			String tag = super.onRecognize(result);

			if (tag != null) {
				System.out.println("\n " + result.getBestFinalResultNoFiller()
						+ '\n');
				if (tag.equals("exit")) {
					System.out.println("Goodbye! Thanks for visiting!\n");
					System.exit(0);
				}
				if (tag.equals("help")) {
					help();
				} else if (tag.equals("stats")) {
					TimerPool.dumpAll();
				} else if (tag.startsWith("goto_")) {
					return tag.replaceFirst("goto_", "");
				} else if (tag.startsWith("browse")) {
					execute(tag);
				}
			} else {
				System.out.println("\n Oops! didn't hear you.\n");
			}
			return null;
		}

		/**
		 * execute the given command
		 * 
		 * @param cmd
		 *            the command to execute
		 */
		private void execute(String cmd) {
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				// if we can't run the command, just fall back to
				// a non-working demo.
			}
		}

		/**
		 * Collects the set of possible utterances.
		 * <p/>
		 * TODO: Note the current implementation just generates a large set of
		 * random utterances and tosses away any duplicates. There's no
		 * guarantee that this will generate all of the possible utterances.
		 * (yep, this is a hack)
		 * 
		 * @return the set of sample utterances
		 */
		private Collection<String> collectSampleUtterances() {
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < 100; i++) {
				String s = getGrammar().getRandomSentence();
				if (!set.contains(s)) {
					set.add(s);
				}
			}

			List<String> sampleList = new ArrayList<String>(set);
			Collections.sort(sampleList);
			return sampleList;
		}

		/** Dumps out the set of sample utterances for this node */
		private void dumpSampleUtterances() {
			if (sampleUtterances == null) {
				sampleUtterances = collectSampleUtterances();
			}

			for (String sampleUtterance : sampleUtterances) {
				System.out.println("  " + sampleUtterance);
			}
		}

		/**
		 * Indicated that the grammar has changed and the collection of sample
		 * utterances should be regenerated.
		 */
		protected void grammarChanged() {
			sampleUtterances = null;
		}
	}

	@Override
	public String getToolTip() {
		return "<html>speech recoginition service wrapping Sphinx 4</html>";
	}
	
}
