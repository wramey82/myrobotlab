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

	
	public void initializeLeft(String LeftBoardType, String LeftComPort)
	{
		arduinoLeft.setBoard(LeftBoardType);
		arduinoLeft.setSerialDevice(LeftComPort, 57600, 8, 1, 0);

		// wait a sec for serial ports to come online
		sleep(1);
		
		// servo limits
		bicepsLeft.setPositionMax(90);
		omoplatLeft.setPositionMax(80);
		omoplatLeft.setPositionMin(10);
		rotateLeft.setPositionMin(40);
		
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
		
		// initial positions
		thumbLeft.moveTo(0);
		indexLeft.moveTo(0);
		majeureLeft.moveTo(0);
		ringFingerLeft.moveTo(0);
		pinkyLeft.moveTo(0);
		
		wristLeft.moveTo(90);
		bicepsLeft.moveTo(0);
		rotateLeft.moveTo(90);
		
		shoulderLeft.moveTo(30);
		omoplatLeft.moveTo(10);

		neck.moveTo(90);
		rothead.moveTo(90);
		

		leftHand = new Hand("left", thumbLeft, indexLeft, majeureLeft, ringFingerLeft, pinkyLeft, wristLeft, rotateLeft);


		// refresh the gui
		arduinoLeft.broadcastState();
		thumbLeft.broadcastState();
		indexLeft.broadcastState();
		majeureLeft.broadcastState();
		ringFingerLeft.broadcastState();
		pinkyLeft.broadcastState();
		wristLeft.broadcastState();
		bicepsLeft.broadcastState();
		rotateLeft.broadcastState();
		shoulderLeft.broadcastState();
		omoplatLeft.broadcastState();
		neck.broadcastState();
		rothead.broadcastState();
				
		
		arduinoLeft.pinMode(17, Arduino.OUTPUT);
		arduinoLeft.analogReadPollingStart(17);
		sleep(500);
		arduinoLeft.analogReadPollingStop(17);

	}
	
	public void initializeRight(String RightBoardType, String RightComPort)
	{
		arduinoRight.setBoard(RightBoardType);
		arduinoRight.setSerialDevice(RightComPort, 57600, 8, 1, 0);

		// wait a sec for serial ports to come online
		sleep(1);
		
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

		//arduinoRight.servoAttach(neck.getName(), 12);
		//arduinoRight.servoAttach(rothead.getName(), 13);
		
		// initial positions
		thumbRight.moveTo(0);
		indexRight.moveTo(0);
		majeureRight.moveTo(0);
		ringFingerRight.moveTo(0);
		pinkyRight.moveTo(0);
		
		wristRight.moveTo(90);
		bicepsRight.moveTo(0);
		rotateRight.moveTo(90);
		
		shoulderRight.moveTo(30);
		omoplatRight.moveTo(10);

		//neck.moveTo(90);
		//rothead.moveTo(90);

		rightHand = new Hand("right", thumbRight, indexRight, majeureRight, ringFingerRight, pinkyRight, wristRight, rotateRight);

		// refresh the gui
		arduinoRight.broadcastState();
		thumbRight.broadcastState();
		indexRight.broadcastState();
		majeureRight.broadcastState();
		ringFingerRight.broadcastState();
		pinkyRight.broadcastState();
		wristRight.broadcastState();
		bicepsRight.broadcastState();
		rotateRight.broadcastState();
		shoulderRight.broadcastState();
		omoplatRight.broadcastState();
				
		// servo limits
		bicepsRight.setPositionMax(90);
		omoplatRight.setPositionMax(80);
		omoplatRight.setPositionMin(10);
		rotateRight.setPositionMin(40);
		
		arduinoRight.pinMode(17, Arduino.OUTPUT);
		arduinoRight.analogReadPollingStart(17);
		sleep(500);
		arduinoRight.analogReadPollingStop(17);
		
	}
	
	public void initialize(String LeftBoardType, String LeftComPort, String RightBoardType, String RightComPort) {

		// when speaking don't listen
		ear.attach(mouth.getName());
		initializeLeft(LeftBoardType, LeftComPort);
		initializeRight(RightBoardType, RightComPort);

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
	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky)
	{
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, null, null);
	}
	
	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist)
	{
		setHandSpeed(which, thumb, index, majeure, ringFinger, pinky, wrist, null);
	}
	
	public void setHandSpeed(String which, Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist, Float rotate)
	{
		if (which.equals("left"))
		{
			leftHand.thumb.setSpeed(thumb);
			leftHand.index.setSpeed(index);
			leftHand.majeure.setSpeed(majeure);
			leftHand.ringFinger.setSpeed(ringFinger);
			leftHand.pinky.setSpeed(pinky);
			leftHand.wrist.setSpeed(wrist);
			leftHand.rotate.setSpeed(rotate);
		} else if (which.equals("right"))
		{
			rightHand.thumb.setSpeed(thumb);
			rightHand.index.setSpeed(index);
			rightHand.majeure.setSpeed(majeure);
			rightHand.ringFinger.setSpeed(ringFinger);
			rightHand.pinky.setSpeed(pinky);
			rightHand.wrist.setSpeed(wrist);
			rightHand.rotate.setSpeed(rotate);
		} else if (which.equals("both"))
		{
			rightHand.thumb.setSpeed(thumb);
			leftHand.thumb.setSpeed(thumb);
			rightHand.index.setSpeed(index);
			leftHand.index.setSpeed(index);
			rightHand.majeure.setSpeed(majeure);
			leftHand.majeure.setSpeed(majeure);
			rightHand.ringFinger.setSpeed(ringFinger);
			leftHand.ringFinger.setSpeed(ringFinger);
			rightHand.pinky.setSpeed(pinky);
			leftHand.pinky.setSpeed(pinky);
			rightHand.wrist.setSpeed(wrist);
			leftHand.wrist.setSpeed(wrist);
			rightHand.rotate.setSpeed(rotate);
			leftHand.rotate.setSpeed(rotate);
		} else {
			log.warn(String.format("dont have a %s hand", which));
		}
	}

	public void systemCheck() {
		// check arduinos

		mouth.speak("starting system check");
		
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

		mouth.speak("completed system check");
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
