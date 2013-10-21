package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class InMoovHand extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovHand.class);
	
	transient public Servo thumb;
	transient public Servo index;
	transient public Servo majeure;
	transient public Servo ringFinger;
	transient public Servo pinky;
	transient public Servo wrist;

	transient public Arduino arduino;
	
	/**
	 * hand pins - need the ability to cache defaults and allow
	 * user to change all these values before starting
	 */
	int thumbPin = 2;
	int indexPin = 3;
	int majeurePin = 4;
	int ringFingerPin = 5;
	int pinkyPin = 6;
	int wristPin = 7;
	
	public InMoovHand(String n) {
		super(n, InMoovHand.class.getCanonicalName());	
		
		// FIXME FIXME FIXME - force naming conventions - a "reserve" happens in the context of a service
		// so this.getName()Peer !!! - must be non-static then
		reserve("Thumb", "Servo", "thumb servo");
		reserve("Index", "Servo", "index servo");
		reserve("Majeure", "Servo", "majeure servo");
		reserve("RingFinger", "Servo", "ringFinger servo");
		reserve("Pinky", "Servo", "pinky servo");
		reserve("Wrist", "Servo", "wrist servo");

		// most likely shared with some other composite !!!
		reserve("Arduino", "Arduino", "Arduino controller for this hand");
	}
	
	@Override
	public void startService() {
		super.startService();
		// .isValidToStart() !!! < check all user data !!!
		
		//createPeers(); // is this needed??
		startPeers();
		attachServos();
		rest();
		broadcastState();
	}

	public void createPeers()
	{
		thumb = (Servo)createReserved("Thumb");
		index = (Servo)createReserved("Index");
		majeure = (Servo)createReserved("Majeure");
		ringFinger = (Servo)createReserved("RingFinger");
		pinky = (Servo)createReserved("Pinky");
		wrist = (Servo)createReserved("Wrist");
		
		arduino = (Arduino)createReserved("Arduino");
	}
	
	public void startPeers()
	{
		thumb = (Servo)startReserved("Thumb");
		index = (Servo)startReserved("Index");
		majeure = (Servo)startReserved("Majeure");
		ringFinger = (Servo)startReserved("RingFinger");
		pinky = (Servo)startReserved("Pinky");
		wrist = (Servo)startReserved("Wrist");
		
		arduino = (Arduino)startReserved("Arduino");
	}
	
	
	public boolean connect(String port)
	{
		arduino = (Arduino)startReserved("Arduino");
		
		if (arduino == null)
		{
			error("arduino is invalid");
		}
		return arduino.connect(port);
	}
	
	/**
	 * attach all the servos - this must be re-entrant
	 * and accomplish the re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attachServos() 
	{		
		if (arduino == null)
		{
			error("invalid arduino");
			return false;
		}
		
		if (!arduino.isConnected())
		{
			error("arduino %s not connected", arduino.getName());
			return false;
		}

		thumb.setPin(thumbPin);
		index.setPin(indexPin);
		majeure.setPin(majeurePin);
		ringFinger.setPin(ringFingerPin);
		pinky.setPin(pinkyPin);
		wrist.setPin(wristPin);	
		
		arduino.servoAttach(thumb);
		arduino.servoAttach(index);
		arduino.servoAttach(majeure);
		arduino.servoAttach(ringFinger);
		arduino.servoAttach(pinky);
		arduino.servoAttach(wrist);
		
		return true;
	}

	
	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky) {
		moveTo(thumb, index, majeure, ringFinger, pinky, null);
	}

	public void moveTo(Integer thumb, Integer index, Integer majeure, Integer ringFinger, Integer pinky, Integer wrist) {
		if (log.isDebugEnabled()){
			log.debug(String.format("%s.moveTo %d %d %d %d %d %d", getName(), thumb, index, majeure, ringFinger, pinky, wrist));
		}
		this.thumb.moveTo(thumb);
		this.index.moveTo(index);
		this.majeure.moveTo(majeure);
		this.ringFinger.moveTo(ringFinger);
		this.pinky.moveTo(pinky);
		if (wrist != null)this.wrist.moveTo(wrist);
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f,1.0f,1.0f,1.0f,1.0f,1.0f);
		
		thumb.moveTo(0);
		index.moveTo(0);
		majeure.moveTo(0);
		ringFinger.moveTo(0);
		pinky.moveTo(0);
		wrist.moveTo(90);
	}
	
	public void broadcastState() {
		// notify the gui
		thumb.broadcastState();
		index.broadcastState();
		majeure.broadcastState();
		ringFinger.broadcastState();
		pinky.broadcastState();
		wrist.broadcastState();
	}
	


	public void detachServos() {
		if (thumb != null) {
			thumb.detach();
		}
		if (index != null) {
			index.detach();
		}
		if (majeure != null) {
			majeure.detach();
		}
		if (ringFinger != null) {
			ringFinger.detach();
		}
		if (pinky != null) {
			pinky.detach();
		}
		if (wrist != null) {
			wrist.detach();
		}
	}

	public void release() {
		detachServos();

		if (thumb != null) {
			thumb.releaseService();
			thumb = null;
		}
		if (index != null) {
			index.releaseService();
			index = null;
		}
		if (majeure != null) {
			majeure.releaseService();
			majeure = null;
		}
		if (ringFinger != null) {
			ringFinger.releaseService();
			ringFinger = null;
		}
		if (pinky != null) {
			pinky.releaseService();
			pinky = null;
		}
		if (wrist != null) {
			wrist.releaseService();
			wrist = null;
		}
	}

	public void setSpeed(Float thumb, Float index, Float majeure, Float ringFinger, Float pinky, Float wrist) {
		this.thumb.setSpeed(thumb);
		this.index.setSpeed(index);
		this.majeure.setSpeed(majeure);
		this.ringFinger.setSpeed(ringFinger);
		this.pinky.setSpeed(pinky);
		this.wrist.setSpeed(wrist);
	}
	
	public boolean isValid()
	{
		thumb.moveTo(2);
		index.moveTo(2);
		majeure.moveTo(2);
		ringFinger.moveTo(2);
		pinky.moveTo(2);
		wrist.moveTo(92);	
		return true;
	}

	public String getScript() {
		return String.format("%s.moveHand(%d,%d,%d,%d,%d,%d)\n", getName(), thumb.getPosition(), index.getPosition(), majeure.getPosition(),
				ringFinger.getPosition(), pinky.getPosition(), wrist.getPosition());
	}
	
	public void setpins(int thumb, int index, int majeure, int ringFinger, int pinky, int wrist){
		log.info(String.format("setPins %d %d %d %d %d %d", thumb, index, majeure, ringFinger, pinky, wrist));
		thumbPin = thumb;
		indexPin = index;
		majeurePin = majeure;
		ringFingerPin = ringFinger;
		pinkyPin = pinky;
		wristPin = wrist;
	}
	
	// ----- initialization end --------
	// ----- movements begin -----------

	public void close() {
		moveTo(130, 180, 180, 180, 180);
	}
	public void open() {
		moveTo(0, 0, 0, 0, 0);
	}
	
	public void openPinch() {
		moveTo(0, 0, 180, 180, 180);
	}

	public void closePinch() {
		moveTo(130, 140, 180, 180, 180);
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		InMoovHand rightHand = new InMoovHand("r01");
		rightHand.connect("COM4");
		rightHand.startService();	
		Runtime.createAndStart("webgui", "WebGUI");
		//rightHand.connect("COM12"); TEST RECOVERY !!!
		
		rightHand.close();
		rightHand.open();
		rightHand.openPinch();
		rightHand.closePinch();
		rightHand.rest();
		
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

	
}
