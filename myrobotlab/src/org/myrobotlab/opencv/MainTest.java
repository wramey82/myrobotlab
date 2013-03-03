package org.myrobotlab.opencv;

import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Log;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.data.Pin;

public class MainTest {

	public MainTest() {
		System.out.println("Started");
		Log log = (Log) Runtime.createAndStart("log", "Log");
		GUIService gui = (GUIService) Runtime.createAndStart("gui", "GUIService");
		Arduino arduino = (Arduino) Runtime.createAndStart("ard", "Arduino");

		gui.display();

		arduino.setSerialDevice("/dev/ttyACM0", 57600, 8, 1, 0);
		arduino.connect();

		log.subscribe("publishPin", arduino.getName(), "log", Pin.class);
		

		while (true) {
			try {

				// ################# Doesn't seem to work here.
				// ###################
				for (Pin p : arduino.getPinList()) {
					System.out.print(p.value + " ");
				}
				System.out.println();
				arduino.digitalWrite(8, 1); // turn on LED
				arduino.digitalWrite(9, 0); // turn on LED
				Thread.sleep(500);
				arduino.digitalWrite(8, 0); // turn on LED
				arduino.digitalWrite(9, 1); // turn on LED
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new MainTest();
	}

}