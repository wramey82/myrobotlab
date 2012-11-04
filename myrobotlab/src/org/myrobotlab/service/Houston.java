package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class Houston extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Houston.class.getCanonicalName());

	// create service for Houston
	Arduino arduino = (Arduino)Runtime.createAndStart("arduino","Arduino");

	Servo lshoulder = (Servo)Runtime.createAndStart("lshoulder","Servo");
	Servo lbicep = (Servo)Runtime.createAndStart("lbicep","Servo");
	Servo lelbow = (Servo)Runtime.createAndStart("lelbow","Servo");

	Servo rshoulder = (Servo)Runtime.createAndStart("rshoulder","Servo");
	Servo rbicep = (Servo)Runtime.createAndStart("rbicep","Servo");
	Servo relbow = (Servo)Runtime.createAndStart("relbow","Servo");
	
	// 4 motors 
	Motor lfmotor = (Motor)Runtime.createAndStart("lfmotor","Motor");// left front
	Motor rfmotor = (Motor)Runtime.createAndStart("rfmotor","Motor");// right front
	Motor lbmotor = (Motor)Runtime.createAndStart("lbmotor","Motor");// left back
	Motor rbmotor = (Motor)Runtime.createAndStart("rbmotor","Motor");// right back

	
	public Houston(String n) {
		super(n, Houston.class.getCanonicalName());
	}
	
	public void initialize(String boardType, String comPort)
	{
				
		// set config for the services
		arduino.setBoard(boardType); // atmega168 | mega2560 | etc;
		arduino.setSerialDevice(comPort,57600,8,1,0);
		sleep(1); // give it a second for the serial device to get ready;

		// attach Servos & Motors to arduino;
		arduino.servoAttach(lshoulder.getName(), 46);
		arduino.servoAttach(lbicep.getName(), 47);
		arduino.servoAttach(lelbow.getName(), 48);
		arduino.servoAttach(rshoulder.getName(), 50);
		arduino.servoAttach(rbicep.getName(), 51);
		arduino.servoAttach(relbow.getName(), 52);

		arduino.motorAttach(lfmotor.getName(), 4, 30);
		arduino.motorAttach(rfmotor.getName(), 5, 31);
		arduino.motorAttach(lbmotor.getName(), 6, 32);
		arduino.motorAttach(rbmotor.getName(), 7, 33);

		// update the gui with configuration changes;
		arduino.publishState();

		lshoulder.publishState();
		lbicep.publishState();
		lelbow.publishState();
		rshoulder.publishState();
		rbicep.publishState();
		relbow.publishState();

		lfmotor.publishState();
		rfmotor.publishState();
		lbmotor.publishState();
		rbmotor.publishState();

	}
	
	public void systemTest()
	{
	    int lfaencoder = 38;
	    int analogSensorPin = 67;

		// system check - need to do checks to see all systems are go !
		// start the analog pin sample to display
		// in the oscope
		arduino.analogReadPollingStart(analogSensorPin);

		// change the pinMode of digital pin 13
		arduino.pinMode(lfaencoder, Arduino.OUTPUT);

		// begin tracing the digital pin 13 
		arduino.digitalReadPollStart(lfaencoder);

		// turn off the trace
		// arduino.digitalReadPollStop(lfaencoder)
		// turn off the analog sampling
		// arduino.analogReadPollingStop(analogSensorPin)
		
	}
	
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		
		Houston houston = new Houston("houston");
		houston.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
