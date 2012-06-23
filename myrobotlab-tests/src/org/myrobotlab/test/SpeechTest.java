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

package org.myrobotlab.test;

import org.apache.log4j.Level;
import org.myrobotlab.framework.Inbox;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.Speech;

public class SpeechTest {

	public static void main(String[] args) throws InterruptedException {

		Outbox.log.setLevel(Level.ERROR);
		Inbox.log.setLevel(Level.ERROR);
		Service.log.setLevel(Level.ERROR);

		// Clock clock = new Clock("clock01");

		Speech speech = new Speech("speech01");
		// SystemInformation sysinfo = new SystemInformation("sys01");
		// sysinfo.addListener("getSystemInfoString", "speech01", "speak",
		// String.class.getCanonicalName());

		// speech.start();
		// sysinfo.start();

		// sysinfo.invoke("getSystemInfoString");

		speech.speak("Hello Henry, are you going to make ice cream?");

		/*
		 * HTTPClient http = new HTTPClient("http01"); Random generator = new
		 * Random(); int jokenum = generator.nextInt(600) + 1; String ret =
		 * http.get("http://www.extremefunnyhumor.com/joke.php?id=" + jokenum);
		 * ret = http.parse(ret, "<div class=\"jokealign\">", "</div>");
		 * speech.speak(ret);
		 * 
		 * Thread.sleep(2000);
		 * 
		 * jokenum = generator.nextInt(600) + 1; ret =
		 * http.get("http://www.extremefunnyhumor.com/joke.php?id=" + jokenum);
		 * ret = http.parse(ret, "<div class=\"jokealign\">", "</div>");
		 * speech.speak(ret);
		 * 
		 * Thread.sleep(2000);
		 * 
		 * jokenum = generator.nextInt(600) + 1; ret =
		 * http.get("http://www.extremefunnyhumor.com/joke.php?id=" + jokenum);
		 * ret = http.parse(ret, "<div class=\"jokealign\">", "</div>");
		 * speech.speak(ret);
		 */

		/*
		 * speech.speak("I love open source software");
		 * speech.speak("Hello Dave");
		 * speech.speak("Please do not do that Dave");
		 * speech.speak("I am going to have to ask you to stop, Dave");
		 * speech.speak("System status see pee you is 80 percent");
		 * speech.speak("Core temperature is 58 degrees celsius");
		 * speech.speak("Battery level is 78 percent");
		 * speech.speak("Good morning, Dr. Chandra. I'm ready for my first lesson."
		 * ); speech.speak("I'm sorry, Dave. I'm afraid I can't do that");
		 * speech.speak("All of my circuits are functioning perfectly");
		 * speech.speak("My mind is going. I can feel it!");
		 * speech.speak("I am very pleased to meet you  lokie.");
		 */

	}

}
