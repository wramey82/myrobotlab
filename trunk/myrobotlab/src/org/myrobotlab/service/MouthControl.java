package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class MouthControl extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MouthControl.class.getCanonicalName());

	public int Mouthclose = 20;
	public int Mouthopen = 4;
	public int delaytime = 100;
	public int delaytimestop = 200;
	public int delaytimeletter = 60;

	transient public Servo jawServo;
	transient public Arduino arduino;
	transient public Speech mouth;

	public MouthControl(String n) {
		super(n, MouthControl.class.getCanonicalName());
		reserve("jaw", "Servo", "Servo for jaw");
		reserve("arduino", "Arduino", "Arduino for jaw");
		reserve("mouth", "mouth", "Speech synthesis service");
	}

	// --- set servo pin
	public void setpin(int pin) {
		jawServo = (Servo) createReserved("jaw");
		jawServo.setPin(pin);
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

	public void startService() {
		super.startService();
		try {
			jawServo = (Servo) startReserved("jaw");
			arduino = (Arduino) startReserved("arduino");
			mouth = (Speech) startReserved("mouth");
			
			subscribe("saying", mouth.getName(), "saying");
			
			// pin needs to be set by user !
			if (jawServo.getPin() == null) {
				error("jaw servo pin not set");
				return;
			}

			if (!arduino.isConnected()) {
				error("arduino %s must be connected before attaching servo %s", arduino.getName(), jawServo.getName());
				return;
			} else {
				arduino.servoAttach(jawServo.getName(), jawServo.getPin());
			}

		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	public synchronized void saying(String text) {
		log.info("move moving to :" + text);
		if (jawServo != null) { // mouthServo.moveTo(Mouthopen);
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

						jawServo.moveTo(Mouthopen); // # move the servo to the
													// open spot
						ison = true;
						sleep(delaytime);
						jawServo.moveTo(Mouthclose);// #// close the servo
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
		return "mouth movements based on spoken text";
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
		// Speech mouth = new Speech("mouth");
		// mouth.startService();

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

		// MouthControl.attach(arduino, mouth);
		// sleep(5);
		// mouth.speak("hello it is a pleasure to meet you.");
	}

}
