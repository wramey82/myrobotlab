package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

public class InMoov extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(InMoov.class.getCanonicalName());

	// OBJECTIVE - try only have complex composite interaction here - everything
	// else should be done directly to targeted services !!!
	// OBJECTIVE - always return a service !!!

	// port map NOT SURE ????
	// will no right & left and com ports
	// 3 definitions at the top left right and head
	// port index, local references
	// HashMap <String, Arduino> arduinos = new HashMap <String, Arduino>();

	// services which do not require a body part
	// or can influence multiple body parts
	transient public Sphinx ear;
	transient public Speech mouth;
	transient public Python python;
	transient public WebGUI webgui;
	transient public Keyboard keyboard;
	transient public XMPP xmpp;

	// hands and arms
	transient public InMoovHead head;
	transient public InMoovHand rightHand;
	transient public InMoovHand leftHand;
	transient public InMoovArm rightArm;
	transient public InMoovArm leftArm;

	// easy global references ???
	transient public Tracking eyesTracking;
	transient public Tracking headTracking;
	transient public OpenCV opencv;

	// static in Java are not overloaded but overwritten - there is no
	// polymorphism for statics
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
				
		peers.put("ear", "Sphinx", "InMoov spech recognition service");
		peers.put("mouth", "Speech", "InMoov speech service");
		peers.put("python", "Python", "Python service");
		peers.put("webgui", "WebGUI", "WebGUI service");
		peers.put("keyboard", "Keyboard", "Keyboard service");
		peers.put("xmpp", "XMPP", "XMPP service");
		peers.put("head", "InMoovHead", "the head");
		peers.put("rightHand", "InMoovHand", "right hand");
		peers.put("leftHand", "InMoovHand", "left hand");
		peers.put("rightArm", "InMoovArm", "right arm");
		peers.put("leftArm", "InMoovArm", "left arm");
		return peers;
	}

	public InMoov(String n) {
		super(n);
	}

	public void addRoutes() {
		// register with runtime for any new services
		// their errors are routed to mouth
		subscribe(this.getName(), "publishError", "handleError");

		Runtime r = Runtime.getInstance();
		r.addListener(getName(), "registered");
	}

	/**
	 * Service registration event. On newly registered service the InMoov
	 * service will set up various routing.
	 * 
	 * Routing of errors back to the InMoov service. This will allow the mouth
	 * to announce errors
	 * 
	 * @param sw
	 */
	public void registered(ServiceInterface sw) {
		// FIXME FIXME FIXME !!! - this right idea - but expanded methods have
		// incorrect parameter placement !!
		// addListener & suscribe the same !!!!
		subscribe(sw.getName(), "publishError", "handleError");
	}

	public void handleError(String msg) {
		if (mouth != null) {
			mouth.speakBlocking(msg);
		}
	}

	public boolean startKeyboard() {
		info("starting keyboard");
		keyboard = (Keyboard) startPeer("keyboard");
		return true;
	}

	// NOTE - these start routines are relatively worthless
	// but only put in for consistency - since they
	// could be brought into the system at any time
	// and they expose interfaces which are dynamically generated
	public boolean startWebGUI() {
		info("starting webgui");
		webgui = (WebGUI) startPeer("webgui");
		return true;
	}

	public boolean startXMPP() {
		info("starting xmpp");
		xmpp = (XMPP) startPeer("xmpp");
		return true;
	}

	public boolean startPython() {
		info("starting python engine");
		python = (Python) startPeer("python");
		return true;
	}

	public void detachAll() {
		if (rightHand != null) {
			rightHand.detach();
		}
	}

	public Speech startMouth() {
		mouth = (Speech) startPeer("mouth");
		mouth.speak("starting mouth");

		if (ear != null) {
			ear.attach(mouth);
		}
		return mouth;
	}

	// TODO TODO TODO - context & status report - "current context is right hand"
	// FIXME - voice control for all levels (ie just a hand or head !!!!)
	public Sphinx startEar() {
		info("starting ear");

		ear = (Sphinx) startPeer("ear");
		if (mouth != null) {
			ear.attach(mouth);
		}

		ear.addCommand("rest", getName(), "rest");

		// depricated ????
		// double context - current contest "right hand"
		ear.addCommand("open hand", getName(), "handOpen", "right");
		ear.addCommand("close hand", getName(), "handClose", "right");
		ear.addCommand("capture gesture", getName(), "captureGesture");
		ear.addCommand("manual", ear.getName(), "lockOutAllGrammarExcept", "voice control");
		ear.addCommand("voice control", ear.getName(), "clearLock");

		ear.addComfirmations("yes", "correct", "yeah", "ya");
		ear.addNegations("no", "wrong", "nope", "nah");

		ear.startListening();

		return ear;
	}

	public final String left = "left";
	public final String right = "right";

	// ------ initialization begin ---------

	public InMoovHand startRightHand(String port) {
		rightHand = startHand(right, port);
		return rightHand;
	}

	public InMoovHand startLeftHand(String port) {
		leftHand = startHand(left, port);
		return leftHand;
	}

	public InMoovHand startHand(String side, String port) {
		InMoovHand hand = (InMoovHand) createPeer(String.format("%sHand", side));
		// connect will start if not already started
		if (!hand.arduino.isConnected()) {
			hand.connect(port);
		}

		if (!hand.isValid()) {
			error("%s hand is not valid", side);
		}

		return hand;
	}

	public InMoovArm startRightArm(String port) {
		rightArm = startArm(right, port);
		return rightArm;
	}

	public InMoovArm startleftArm(String port) {
		leftArm = startArm(left, port);
		return leftArm;
	}

	public InMoovArm startArm(String side, String port) {
		InMoovArm Arm = (InMoovArm) createPeer(String.format("%Arm", side));
		// connect will start if not already started
		if (!Arm.arduino.isConnected()) {
			Arm.connect(port);
		}

		if (!Arm.isValid()) {
			error("%s Arm is not valid", side);
		}

		return Arm;
	}

	public InMoovHead startHead(String port) {
		head = (InMoovHead) createPeer("head");
		head.connect(port);
		return head;
	}

	// ------ initialization end ---------
	// ------ composites begin -----------
	public void rest() {
		if (head != null) {
			head.rest();
		}
		if (rightHand != null) {
			rightHand.rest();
		}
		if (leftHand != null) {
			leftHand.rest();
		}
		if (rightArm != null) {
			rightArm.rest();
		}
		if (leftArm != null) {
			leftArm.rest();
		}
	}

	public void detach() {
		if (head != null) {
			head.detach();
		}
		if (rightHand != null) {
			rightHand.detach();
		}
		if (leftHand != null) {
			leftHand.detach();
		}
		if (rightArm != null) {
			rightArm.detach();
		}
		if (leftArm != null) {
			leftArm.detach();
		}
	}

	public void attach() {
		if (head != null) {
			head.attach();
		}
		if (rightHand != null) {
			rightHand.attach();
		}
		if (leftHand != null) {
			leftHand.attach();
		}
		if (rightArm != null) {
			rightArm.attach();
		}
		if (leftArm != null) {
			leftArm.attach();
		}
	}

	public void systemCheck() {
		// FIXME
		// systemCheck
		// or isValid unify it

		if (mouth != null) {
			mouth.speakBlocking("starting system check");
			mouth.speakBlocking("testing");
		}

		rest();
		sleep(500);

		// FIXME !!!! set to mout = speak errors & warnings !!!!

		if (rightHand != null) {
			rightHand.isValid();
		}

		sleep(500);
		rest();

		// check servos

		// check ear

		// check mount - all my circuits are functioning perfectly

		broadcastState();
		if (mouth != null) {
			mouth.speakBlocking("system check completed");
		}
	}

	// ------ composites end -----------
	// ------ deep gets begin -----------
	public Tracking getHeadTracking() {
		if (head == null) {
			head = (InMoovHead) createPeer("head");
		}
		return head.headTracking;
	}

	public Tracking getEyesTracking() {
		if (head == null) {
			head = (InMoovHead) createPeer("head");
		}
		return head.eyesTracking;
	}

	@Override
	public String getDescription() {
		return "The InMoov service";
	}

	// gestures end --------------
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		
		log.warn(Runtime.buildDNA("i01", "InMoov").toString());

		 // log.info(Runtime.buildDNA("i01.head", "InMoovHead").toString());
		 
		InMoov i01 = (InMoov) Runtime.createAndStart("i01", "InMoov");
		InMoovHead head = i01.startHead("COM4");
		Tracking eyes = i01.getEyesTracking();
		Tracking neck = i01.getHeadTracking();
		
		eyes.faceDetect();
		
		GUIService gui = (GUIService) Runtime.createAndStart("gui", "GUIService");
		
		
		i01.startRightHand("COM4");
		
		Runtime.createAndStart("gui", "GUIService");
		
		i01.addRoutes();
		i01.startMouth();
		
		head.x.moveTo(96);
		head.x.moveTo(150);
		head.x.moveTo(88);
		
		Tracking t = i01.getHeadTracking();
		t.x.moveTo(30);
		t.x.moveTo(90);
		t.x.moveTo(150);
		t.x.moveTo(90);
		t.x.moveTo(30);
		t.x.setSpeed(0.2f);
		t.x.moveTo(30);
		t.x.moveTo(90);
		t.x.moveTo(150);
		t.x.moveTo(90);
		t.x.moveTo(30);

		// i01.getHeadTracking().faceDetect();

		// get("eyesTracking");

		

		InMoovHand hand = i01.startRightHand("COM12");
		hand.close();
		hand.moveTo(30, 30, 30, 30, 30);
		hand.moveTo(40, 40, 40, 40, 40);
		hand.moveTo(60, 60, 60, 60, 60, 60);
		hand.detach();
		hand.moveTo(30, 30, 30, 30, 30);

		hand.attach();
		hand.moveTo(30, 30, 30, 30, 30);
		hand.open();
		log.info("here");
		Runtime.createAndStart("gui", "GUIService");
	}

	// I2C - needs bus and address (if on BBB or RasPi) .. or Arduino - needs
	// port / bus & address
	// startRightHand(String port)
	// startRightHand(String bus, String address)
	// startRightHand(String port, String bus, String address)

}
