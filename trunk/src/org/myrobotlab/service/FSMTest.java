package org.myrobotlab.service;

import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class FSMTest extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger LOG = Logger.getLogger(FSMTest.class.getCanonicalName());

	// TODO - these services could be accessed via RuntimeEnvironment.service("opencv");
	// TODO - service could be bound or created in an init - bound for remote
	
	OpenCV opencv = null;
	SpeechRecognition speechRecognition = null;
	Speech speech = null;
	
	public FSMTest(String n) {
		super(n, FSMTest.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public void init ()
	{
		speechRecognition = new SpeechRecognition ("sphinx");
		speechRecognition.startService();
		speech = new Speech("speech");
		speech.startService();
		//speechRecognition.notify("publish", speech.name, "speakATT", String.class);
		speechRecognition.notify("publish", name, "sayResults", String.class);

	}
	
	public void talkingNotListening()
	{
		speechRecognition.stopRecording();
	}
	
	
	Random generator = new Random(4);
	
	public void sayResults (String data)
	{
		switch (generator.nextInt())
		{
			case 1:
			speech.speakATT("I don't know");
			break;
			case 2:
			speech.speakATT("please tell me");
			break;
			case 3:
			speech.speakATT("tell me");
			break;
			case 4:
			speech.speakATT("what is it");
			break;
		}
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		FSMTest template = new FSMTest("fsm");
		template.startService();
		template.init();
		template.speechRecognition.stopRecording();
		template.speechRecognition.startRecording();
		
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
