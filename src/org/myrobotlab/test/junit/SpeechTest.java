package org.myrobotlab.test.junit;

import org.junit.Test;

import org.myrobotlab.service.Speech;

public class SpeechTest {

	@Test
	public void testSpeakFloat() {
		Speech speech = new Speech("speech");
		speech.startService();
		speech.speak((float) 3.141529);
	}

	@Test
	public void testSpeakString() {
		Speech speech = new Speech("speech");
		speech.startService();
		speech.speak("hello dave");
		speech.speak("all my circuits are functioning perfectly");
	}

}
