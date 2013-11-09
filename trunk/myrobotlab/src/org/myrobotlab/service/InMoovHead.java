package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class InMoovHead extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoovHead.class);

	transient public OpenCV opencv;
	transient public Tracking headTracking;
	transient public Tracking eyesTracking;
	transient public MouthControl mouthControl;
	transient public Servo jaw;
	transient public Servo eyeX, eyeY;
	transient public Servo x;
	transient public Servo y;
	transient public Arduino arduino;

	// static in Java are not overloaded but overwritten - there is no polymorphism for statics
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		
		peers.suggestAs("mouthControl.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headArduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");		
		peers.suggestAs("eyesTracking.opencv", "opencv", "OpenCV", "shared head OpenCV");	
		peers.suggestAs("headTracking.x", "x", "Servo", "shared servo");		
		peers.suggestAs("headTracking.y", "y", "Servo", "shared servo");		
		peers.suggestAs("eyesTracking.x", "eyeX", "Servo", "shared servo");		
		peers.suggestAs("eyesTracking.y", "eyeY", "Servo", "shared servo");		
		peers.suggestAs("opencv", "opencv", "OpenCV", "shared head OpenCV");	
		
		peers.put("mouthControl", "MouthControl", "MouthControl");	
		peers.put("opencv", "OpenCV", "shared OpenCV instance");
		peers.put("headTracking", "Tracking", "Head tracking system");
		peers.put("eyesTracking", "Tracking", "Tracking for the eyes");
		peers.put("jaw", "Servo", "Jaw servo");
		peers.put("eyeX", "Servo", "Eyes pan servo");
		peers.put("eyeY", "Servo", "Eyes tilt servo");
		peers.put("x", "Servo", "Head pan servo");
		peers.put("y", "Servo", "Head tilt servo");
		peers.put("headArduino", "Arduino", "Arduino controller for this arm");
		
		
		
		// TODO better be whole dam tree ! - have to recurse based on Type !!!!
		/*
		peers.suggestAs("mouthControl.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("headTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		peers.suggestAs("eyesTracking.arduino", "headArduino", "Arduino", "shared head Arduino");
		*/
				
		return peers;
	}
	
	public InMoovHead(String n) {
		super(n, InMoovHead.class.getCanonicalName());
		opencv = (OpenCV)createPeer("opencv");
		headTracking = (Tracking)createPeer("headTracking");
		eyesTracking = (Tracking)createPeer("eyesTracking");
		mouthControl = (MouthControl) createPeer("mouthControl");
		jaw = (Servo) createPeer("jaw");
		eyeX = (Servo) createPeer("eyeX");
		eyeY = (Servo) createPeer("eyeY");
		x = (Servo) createPeer("x");
		y = (Servo) createPeer("y");
		arduino = (Arduino) createPeer("headArduino");
		
		// FIXME DEFAULT I2C ????
		// eyeX.setPin(pin)
		// eyeY.setPin(pin)
		// FIXME PIN WRONG 

		eyeX.setPin(6);
		eyeY.setPin(7);

		jaw.setPin(14); // SHARED !!!
		y.setPin(12);
		x.setPin(13);
		
		x.setMinMax(30, 150);
		y.setMinMax(20, 160);
		
		jaw.setRest(0);
	}

	@Override
	public void startService() {
		super.startService();
		opencv.startService();
		headTracking.startService();
		eyesTracking.startService();
		mouthControl.startService();
		jaw.startService();
		eyeX.startService();
		eyeY.startService();
		x.startService();
		y.startService();
		arduino.startService();
	}

	// FIXME - make interface for Arduino / Servos !!!
	public boolean connect(String port) {
		startService(); // NEEDED? I DONT THINK SO....

		if (arduino == null) {
			error("arduino is invalid");
			return false;
		}

		arduino.connect(port);

		if (!arduino.isConnected()) {
			error("arduino %s not connected", arduino.getName());
			return false;
		}

		attach();
		rest();
		broadcastState();
		return true;
	}

	/**
	 * attach all the servos - this must be re-entrant and accomplish the
	 * re-attachment when servos are detached
	 * 
	 * @return
	 */
	public boolean attach() {
		arduino.servoAttach(eyeX);
		arduino.servoAttach(eyeY);
		arduino.servoAttach(jaw);
		arduino.servoAttach(x);
		arduino.servoAttach(y);
		return true;
	}

	public void moveTo(Integer rothead, Integer headY) {
		moveTo(rothead, headY, null, null, null);
	}

	public void moveTo(Integer rothead, Integer headY, Integer eyeX, Integer eyeY) {
		moveTo(rothead, headY, eyeX, eyeY, null);
	}

	public void moveTo(Integer rothead, Integer headY, Integer eyeX, Integer eyeY, Integer jaw) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s.moveTo %d %d %d %d %d", rothead, headY, eyeX, eyeY, jaw));
		}
		this.x.moveTo(rothead);
		this.y.moveTo(headY);
		if (eyeX != null)
			this.eyeX.moveTo(eyeX);
		if (eyeY != null)
			this.eyeY.moveTo(eyeY);
		if (jaw != null)
			this.jaw.moveTo(jaw);
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
		x.rest();
		y.rest();
		eyeX.rest();
		eyeY.rest();
		jaw.rest();
	}

	// FIXME - should be broadcastServoState
	public void broadcastState() {
		// notify the gui
		x.broadcastState();
		y.broadcastState();
		eyeX.broadcastState();
		eyeY.broadcastState();
		jaw.broadcastState();
	}

	public void detach() {	
		x.detach();
		y.detach();
		eyeX.detach();
		eyeY.detach();
		jaw.detach();
	}

	public void release() {
		detach();
		x.releaseService();
		y.releaseService();
		eyeX.releaseService();
		eyeY.releaseService();
		jaw.releaseService();
	}

	public void setSpeed(Float headXSpeed, Float headYSpeed, Float eyeXSpeed, Float eyeYSpeed, Float jawSpeed) {
		if (log.isDebugEnabled()){
			log.debug(String.format("%s setSpeed %d %d %d %d %d", headXSpeed, headYSpeed, eyeXSpeed, eyeYSpeed, jawSpeed));
		}
		
		x.setSpeed(headXSpeed);
		y.setSpeed(headYSpeed);
		eyeX.setSpeed(eyeXSpeed);
		eyeY.setSpeed(eyeYSpeed);
		jaw.setSpeed(jawSpeed);
			
	}

	public String getScript() {
		return String.format("%s.moveTo(\"%s\",%d,%d,%d,%d,%d)\n", getName(), x.getPosition(), y.getPosition(), eyeX.getPosition(), eyeY.getPosition(),
				jaw.getPosition());
	}

	public void setpins(int headXPin, int headYPin, int eyeXPin, int eyeYPin, int jawPin) {
		log.info(String.format("setPins %d %d %d %d %d %d", headXPin, headYPin, eyeXPin, eyeYPin, jawPin));
		x.setPin(headXPin);
		y.setPin(headYPin);
		eyeX.setPin(eyeXPin);
		eyeY.setPin(eyeYPin);
		jaw.setPin(jawPin);
	}

	public boolean isValid() {
		x.moveTo(x.getRest() + 2);
		y.moveTo(y.getRest() + 2);
		eyeX.moveTo(eyeX.getRest() + 2);
		eyeY.moveTo(eyeY.getRest() + 2);
		jaw.moveTo(jaw.getRest() + 2);
		return true;
	}

	public void setLimits(int headXMin, int headXMax, int headYMin, int headYMax, int eyeXMin, int eyeXMax, int eyeYMin, int eyeYMax, int jawMin, int jawMax) {
		x.setMinMax(headXMin, headXMax);
		y.setMinMax(headYMin, headYMax);
		eyeX.setMinMax(eyeXMin, eyeXMax);
		eyeY.setMinMax(eyeYMin, eyeYMax);
		jaw.setMinMax(jawMin, jawMax);
	}

	// ----- initialization end --------
	// ----- movements begin -----------

	@Override
	public String getDescription() {
		return "InMoov Head Service";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		// use case - before merge
		Service.reserveRootAs("head01.mouthControl.arduino", "dorkbutt");
		log.warn("{}", Runtime.buildDNA("head01", "InMoovHead"));

		InMoovHead head = (InMoovHead)Runtime.createAndStart("head01", "InMoovHead");
		
		head.connect("COM12");
		
		head.moveTo(40, 60);
		head.moveTo(90, 90);
		head.moveTo(110, 130);
		/*
		
		log.warn("actual dna \n" + Service.getDNA());
		
		log.warn("-----------------------------------------------------");
		Index<ServiceReservation> dna = Runtime.buildDNA("head01", "InMoovHead");
		log.warn("-----------------------------------------------------");
		log.warn("\n" + dna);
		
		Service.mergePeerDNA("head01", "InMoovHead");
		// use case - after merge
		Service.reserveRootAs("head01.mouthControl.arduino", "dorkbutt");
		log.warn("actual dna \n{}", Service.getDNA());

		//log.warn("\n" + Service.getAllPeers("head01","InMoovHead").show());
		
		//Service.mergePeerDNA("head01", "InMoovHead"); // TODO getPeers getAllPeers mergeAllPeersReservations
		//log.warn(Service.getDNA());
		InMoovHead head = new InMoovHead("head01");
		log.warn(Service.getDNA().getRootNode().toString());
		*/
		
		Runtime.createAndStart("gui", "GUIService");
	
		// Gson gson = new
		// GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().create();
		//head.createReserved("HeadTracking");
		//head.startReserved("HeadTracking");

		// head.startService();
		// log.warn(gson.toJson(Service.getReservations()));

		
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
