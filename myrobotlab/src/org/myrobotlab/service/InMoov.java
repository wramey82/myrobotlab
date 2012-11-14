package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

public class InMoov extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(InMoov.class.getCanonicalName());
	
	String bodyPartContext = null;

	public class Hand {
		String name;
		
		Servo thumb;
		Servo index;
		Servo majeure;
		Servo ringFinger;
		Servo pinky;
		Servo wrist;
		Servo rotate; // ?

		public Hand(String name, Servo thumb, Servo index, Servo majeure, Servo ringFinger, Servo pinky, Servo wrist, Servo rotate) {
			this.name = name;
			this.thumb = thumb;
			this.index = index;
			this.majeure = majeure;
			this.ringFinger = ringFinger;
			this.pinky = pinky;
			this.wrist = wrist;
			this.rotate = rotate;
		}
	}
	
	Hand leftHand;
	Hand rightHand;

	Servo thumbLeft = (Servo)Runtime.createAndStart("thumbLeft", "Servo");
	Servo indexLeft = (Servo)Runtime.createAndStart("indexLeft", "Servo");
	Servo majeureLeft = (Servo)Runtime.createAndStart("majeureLeft", "Servo");
	Servo ringFingerLeft = (Servo)Runtime.createAndStart("ringFingerLeft", "Servo");
	Servo pinkyLeft = (Servo)Runtime.createAndStart("pinkyLeft", "Servo");
	Servo wristLeft = (Servo)Runtime.createAndStart("wristLeft", "Servo");
	Servo bicepsLeft = (Servo)Runtime.createAndStart("bicepsLeft", "Servo");
	Servo rotateLeft = (Servo)Runtime.createAndStart("rotateLeft", "Servo");
	Servo shoulderLeft = (Servo)Runtime.createAndStart("shoulderLeft", "Servo");
	Servo omoplatLeft = (Servo)Runtime.createAndStart("omoplatLeft", "Servo");

	Servo neck = (Servo)Runtime.createAndStart("neck", "Servo");
	Servo rothead = (Servo)Runtime.createAndStart("rothead", "Servo");

	Servo thumbRight = (Servo)Runtime.createAndStart("thumbRight", "Servo");
	Servo indexRight = (Servo)Runtime.createAndStart("indexRight", "Servo");
	Servo majeureRight = (Servo)Runtime.createAndStart("majeureRight", "Servo");
	Servo ringFingerRight = (Servo)Runtime.createAndStart("ringFingerRight", "Servo");
	Servo pinkyRight = (Servo)Runtime.createAndStart("pinkyRight", "Servo");
	Servo wristRight = (Servo)Runtime.createAndStart("wristRight", "Servo");
	Servo bicepsRight = (Servo)Runtime.createAndStart("bicepsRight", "Servo");
	Servo rotateRight = (Servo)Runtime.createAndStart("rotateRight", "Servo");
	Servo shoulderRight = (Servo)Runtime.createAndStart("shoulderRight", "Servo");
	Servo omoplatRight = (Servo)Runtime.createAndStart("omoplatRight", "Servo");

	Arduino arduinoLeft = (Arduino)Runtime.createAndStart("arduinoLeft", "Arduino"); 
	Arduino arduinoRight = (Arduino)Runtime.createAndStart("arduinoRight", "Arduino"); 
	
	Sphinx ear = (Sphinx)Runtime.createAndStart("ear", "Sphinx"); 
	Speech mouth = (Speech)Runtime.createAndStart("mouth", "Speech"); 
	OpenCV opencv = (OpenCV)Runtime.createAndStart("opencv", "OpenCV");

	public InMoov(String n) {
		super(n, InMoov.class.getCanonicalName());
	}

	public void initialize(String LeftBoardType, String LeftComPort, String RightBoardType, String RightComPort) {
		arduinoLeft.setBoard(LeftBoardType);
		arduinoRight.setBoard(RightBoardType);
		arduinoLeft.setSerialDevice(LeftComPort, 57600, 8, 1, 0);
		arduinoRight.setSerialDevice(RightComPort, 57600, 8, 1, 0);

		// wait a sec for serial ports to come online
		sleep(1);
		
		arduinoLeft.servoAttach(thumbLeft.getName(), 2);
		arduinoLeft.servoAttach(indexLeft.getName(), 3);
		arduinoLeft.servoAttach(majeureLeft.getName(), 4);
		arduinoLeft.servoAttach(ringFingerLeft.getName(), 5);
		arduinoLeft.servoAttach(pinkyLeft.getName(), 6);
		arduinoLeft.servoAttach(wristLeft.getName(), 7);
		arduinoLeft.servoAttach(bicepsLeft.getName(), 8);
		arduinoLeft.servoAttach(rotateLeft.getName(), 9);
		arduinoLeft.servoAttach(shoulderLeft.getName(), 10);
		arduinoLeft.servoAttach(omoplatLeft.getName(), 11);

		arduinoLeft.servoAttach(neck.getName(), 12);
		arduinoLeft.servoAttach(rothead.getName(), 13);

		arduinoRight.servoAttach(thumbRight.getName(), 2);
		arduinoRight.servoAttach(indexRight.getName(), 3);
		arduinoRight.servoAttach(majeureRight.getName(), 4);
		arduinoRight.servoAttach(ringFingerRight.getName(), 5);
		arduinoRight.servoAttach(pinkyRight.getName(), 6);
		arduinoRight.servoAttach(wristRight.getName(), 7);
		arduinoRight.servoAttach(bicepsRight.getName(), 8);
		arduinoRight.servoAttach(rotateRight.getName(), 9);
		arduinoRight.servoAttach(shoulderRight.getName(), 10);
		arduinoRight.servoAttach(omoplatRight.getName(), 11);

		leftHand = new Hand("left", thumbLeft, indexLeft, majeureLeft, ringFingerLeft, pinkyLeft, wristLeft, rotateLeft);
		rightHand = new Hand("right", thumbRight, indexRight, majeureRight, ringFingerRight, pinkyRight, wristRight, rotateRight);


		// refresh the gui
		arduinoLeft.publishState();
		thumbLeft.publishState();
		indexLeft.publishState();
		majeureLeft.publishState();
		ringFingerLeft.publishState();
		pinkyLeft.publishState();
		wristLeft.publishState();
		bicepsLeft.publishState();
		rotateLeft.publishState();
		shoulderLeft.publishState();
		omoplatLeft.publishState();
		neck.publishState();
		rothead.publishState();
		arduinoRight.publishState();
		thumbRight.publishState();
		indexRight.publishState();
		majeureRight.publishState();
		ringFingerRight.publishState();
		pinkyRight.publishState();
		wristRight.publishState();
		bicepsRight.publishState();
		rotateRight.publishState();
		shoulderRight.publishState();
		omoplatRight.publishState();
		
		// when speaking don't listen
		ear.attach(mouth.getName());
		
		// servo limits
		bicepsLeft.setPositionMax(90);
		bicepsRight.setPositionMax(90);
		omoplatLeft.setPositionMax(80);
		omoplatRight.setPositionMax(80);
		omoplatLeft.setPositionMin(10);
		omoplatRight.setPositionMin(10);
		rotateLeft.setPositionMin(40);
		rotateRight.setPositionMin(40);

	}
	
	// lower higher concepts - 10 degree
	// much higher 
	// remember move - save 
	
	// low level moveHand
	
	// 
	
	public void handOpen(String which)
	{
		moveHand(which, 0, 0, 0, 0, 0);
	}
	
	public void handClose(String which)
	{
		moveHand(which, 130, 180, 180, 180, 180);
	}
	
	public void handRest(String which)
	{
		moveHand(which, 60, 40, 30, 40, 40);		
	}
	
	public void openPinch(String which)
	{
		moveHand(which, 0, 0, 180, 180, 180);		
	}

	public void closePinch(String which)
	{
		moveHand(which, 130, 140, 180, 180, 180);		
	}

	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky)
	{
		moveHand(which, thumb, index, majeure, ringFinger, pinky, null, null);
	}
	
	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist)
	{
		moveHand(which, thumb, index, majeure, ringFinger, pinky, wrist, null);
	}
	
	public void moveHand(String which, Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist, Integer rotate)
	{
		if (which.equals("left"))
		{
			leftHand.thumb.moveTo(thumb);
			leftHand.index.moveTo(index);
			leftHand.majeure.moveTo(majeure);
			leftHand.ringFinger.moveTo(ringFinger);
			leftHand.pinky.moveTo(pinky);
			leftHand.wrist.moveTo(wrist);
			leftHand.rotate.moveTo(rotate);
		} else if (which.equals("right"))
		{
			rightHand.thumb.moveTo(thumb);
			rightHand.index.moveTo(index);
			rightHand.majeure.moveTo(majeure);
			rightHand.ringFinger.moveTo(ringFinger);
			rightHand.pinky.moveTo(pinky);
			rightHand.wrist.moveTo(wrist);
			rightHand.rotate.moveTo(rotate);
		} else if (which.equals("both"))
		{
			rightHand.thumb.moveTo(thumb);
			leftHand.thumb.moveTo(thumb);
			rightHand.index.moveTo(index);
			leftHand.index.moveTo(index);
			rightHand.majeure.moveTo(majeure);
			leftHand.majeure.moveTo(majeure);
			rightHand.ringFinger.moveTo(ringFinger);
			leftHand.ringFinger.moveTo(ringFinger);
			rightHand.pinky.moveTo(pinky);
			leftHand.pinky.moveTo(pinky);
			rightHand.wrist.moveTo(wrist);
			leftHand.wrist.moveTo(wrist);
			rightHand.rotate.moveTo(rotate);
			leftHand.rotate.moveTo(rotate);
		} else {
			log.warn(String.format("dont have a %s hand", which));
		}
	}

	public void systemCheck() {
		// check arduinos

		arduinoLeft.pinMode(17, 0);
		arduinoLeft.analogReadPollingStart(17);
		sleep(1);
		arduinoLeft.pinMode(17, 0);
		arduinoLeft.analogReadPollingStop(17);

		arduinoRight.pinMode(17, 0);
		arduinoRight.analogReadPollingStart(17);
		sleep(1);
		arduinoRight.pinMode(17, 0);
		arduinoRight.analogReadPollingStop(17);

		// check servos

		// check ear

		// check mount - all my circuits are functioning perfectly

	}

	public void allOpen(String hand) {

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

		InMoov inMoov = new InMoov("inMoov");
		inMoov.startService();
		
		Runtime.createAndStart("jython", "Jython");
		Runtime.createAndStart("gui", "GUIService");
		
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
