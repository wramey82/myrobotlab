package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MouthControl extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MouthControl.class.getCanonicalName());

	public int MouthPin = 9;
	public int Mouthclose = 20;
	public int Mouthopen = 4;
	public int delaytime = 100;
	public int delaytimestop = 200;
	public int delaytimeletter = 60;
	public Servo mouthServo;

	public MouthControl(String n) {
		super(n, MouthControl.class.getCanonicalName());
	}

	// --- set servo pin
	public void setpin(Integer pin) {
		MouthPin = pin;
	}

	public void setdelays(Integer d1, Integer d2, Integer d3) {
		delaytime = d1;
		delaytimestop = d2;
		delaytimeletter = d3;
	}

	public void setmouth(Integer closed, Integer opened) {
		Mouthclose = closed;
		Mouthopen = opened;
	}

	public boolean attach(Arduino arduino, Speech mouth) {

		if (arduino != null && mouth != null) {
			mouthServo = (Servo) Runtime.createAndStart("mouthMove", "Servo");
			// attach to controller
			// ------------- changed to use set pins
			arduino.servoAttach(mouthServo.getName(), MouthPin);

			// broadcastState();
			subscribe("saying", mouth.getName(), "saying", String.class);
			log.info(String.format("attached Mouth Control service %s to speech service %s with default message routes", mouth.getName(), getName()));
			// mouthServo.moveTo(Mouthclose);
			sleep(5);
			return true;
		} else {
			log.info("did not get right arrg to attach mouth control.");
			return false;
		}

	}

	public synchronized void saying(String text) {
		log.info("move moving to :" + text);
		if (mouthServo != null) { // mouthServo.moveTo(Mouthopen);
			boolean ison = false;
			String testword;
			String[] a = text.split(" ");
			for (int w = 0; w < a.length; w++) {
				// String word = ;
				// log.info(String.valueOf(a[w].length()));

				if (a[w].endsWith("es")) {
					testword = a[w].substring(0, a[w].length() - 2);

				} else if (a[w].endsWith("e")) {
					testword = a[w].substring(0, a[w].length() - 1);
					// log.info("e gone");
				} else {
					testword = a[w];

				}

				char[] c = testword.toCharArray();

				for (int x = 0; x < c.length; x++) {
					char s = c[x];

					if ((s == 'a' || s == 'e' || s == 'i' || s == 'o' || s == 'u' || s == 'y') && !ison) {

						mouthServo.moveTo(Mouthopen); // # move the servo to the
														// open spot
						ison = true;
						sleep(delaytime);
						mouthServo.moveTo(Mouthclose);// #// close the servo
					} else if (s == '.') {
						ison = false;
						sleep(delaytimestop);
					} else {
						ison = false;
						sleep(delaytimeletter); // # sleep half a second
					}

				}

				sleep(80);
			}

		} else {
			log.info("need to attach first");
		}
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();

	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		// LoggingFactory.getInstance().setLevel(Level.INFO);
		MouthControl MouthControl = new MouthControl("MouthControl");
		MouthControl.startService();

		Runtime.createAndStart("gui", "GUIService");
		// Python python = new Python("python");
		// python.startService();

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
		// create an Arduino service named arduino
		// Arduino arduino = new Arduino("arduino");
		// arduino.startService();
		// Speech speech = new Speech("speech");
		// speech.startService();

		// # set the board type
		// arduino.setBoard("atmega328p"); // atmega168 | mega2560 | etc
		// # set serial device
		// arduino.setSerialDevice("COM16",57600,8,1,0);
		// sleep(1) # give it a second for the serial device to get ready

		// # update the gui with configuration changes
		// arduino.publishState();

		// # set the pinMode of pin 13 to output
		// arduino.pinMode(13, Arduino.OUTPUT);
		// #words =
		// # Speak with initial defaults - Google en

		// MouthControl.attach(arduino, speech);
		// sleep(5);
		// speech.speak("hello it is a pleasure to meet you.");
	}

}
