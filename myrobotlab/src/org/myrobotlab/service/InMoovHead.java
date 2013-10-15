package org.myrobotlab.service;

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
	transient public MouthControl mouthcontrol;
	transient public Servo jaw;
	transient public Servo eyeX, eyeY;
	transient public Servo rothead;
	transient public Servo neck;
	transient public Arduino arduino;
	
	public int rotheadPin = 13;
	public int neckPin = 12;
	public int jawPin = 0;
	public int eyeXPin = 0;
	public int eyeYPin = 0;
	
	public int rotheadMin = 30;
	public int rotheadMax = 150;
	public int neckMin = 20;
	public int neckMax = 160;
	public int eyeXMin = 0;
	public int eyeXMax = 180;
	public int eyeYMin = 0;
	public int eyeYMax = 180;	
	public int jawMin = 0;
	public int jawMax = 180;
	
	
	public InMoovHead(String n) {
		super(n, InMoovHead.class.getCanonicalName());	
		
		// head servos FIXME needs to be another service!!
		reserveRoot("opencv", "OpenCV", "OpenCV service");
		reserveRoot("arduino", "Arduino", "Arduino service");
		reserve("HeadTracking", "Tracking", "Tracking service for InMoov head");
		reserve("EyesTracking", "Tracking", "Tracking service for InMoov eyes");
		reserve("MouthControl", "MouthControl", "Mouth control"); // this is a composite
		// merging all opencv references
		reserve("HeadTrackingOpenCV", "opencv", "OpenCV", "shared opencv service with HeadTracking");
		reserve("EyesTrackingOpenCV", "opencv", "OpenCV", "shared opencv service with EyesTracking");
		// merging all arduino references
		reserve("HeadTrackingArduino", "arduino", "Arduino", "shared arduino service with HeadTracking");
		reserve("EyesTrackingArduino", "arduino", "Arduino", "shared arduino service with EyesTracking");
		reserve("MouthControlArduino", "arduino", "Arduino", "shared arduino service with MouthControl");		
	}
	
	@Override
	public void startService() {
		super.startService();
		createPeers();
		startPeers();
	}
	
	// TODO - createPeers & startPeers only by iterating through reservation data structure !!
	// only possible if there are no local references !!!! - hmmm may be worth it
	// need better error handling around global data structures
	public void createPeers()
	{
		//log.warn(gson.toJson(Service.getReservations()));
		opencv = (OpenCV)createRootReserved("OpenCV");
		headTracking = (Tracking)createReserved("HeadTracking");
		eyesTracking = (Tracking)createReserved("EyesTracking");
		mouthcontrol = (MouthControl)createReserved("MouthControl");
	}
	
	public void startPeers()
	{
		opencv = (OpenCV)startReserved("OpenCV");
		headTracking = (Tracking)startReserved("HeadTracking");
		eyesTracking = (Tracking)startReserved("EyesTracking");
		mouthcontrol = (MouthControl)startReserved("MouthControl");
		/*
		jaw = (Servo)startReserved("Jaw");
		eyeX = (Servo)startReserved("EyeX");
		eyeY = (Servo)startReserved("EyeY");
		rothead = (Servo)startReserved("Rothead");
		neck = (Servo)startReserved("Neck");
		arduino = (Arduino)startReserved("Arduino");	
		*/
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
		
		jaw = (Servo)startReserved("Jaw");
		eyeX = (Servo)startReserved("EyeX");
		eyeY = (Servo)startReserved("EyeY");
		rothead = (Servo)startReserved("Rothead");
		arduino = (Arduino)startReserved("Arduino");	

		jaw.setPin(jawPin);
		eyeX.setPin(eyeXPin);
		eyeY.setPin(eyeYPin);
		rothead.setPin(rotheadPin);
		
		arduino.servoAttach(jaw);
		arduino.servoAttach(eyeX);
		arduino.servoAttach(eyeY);
		arduino.servoAttach(rothead);
		
		return true;
	}

	public void moveTo(Integer rothead, Integer neck) {
		moveTo(rothead, neck, null, null, null);
	}
	
	public void moveTo(Integer rothead, Integer neck, Integer eyeX, Integer eyeY) {
		moveTo(rothead, neck, eyeX, eyeY, null);
	}

	public void moveTo(Integer rothead, Integer neck, Integer eyeX, Integer eyeY, Integer jaw) {
		if (log.isDebugEnabled()){
			log.debug(String.format("%s.moveTo %d %d %d %d %d", rothead, neck, eyeX, eyeY, jaw));
		}
		this.rothead.moveTo(rothead);
		this.neck.moveTo(neck);
		if (eyeX != null)this.eyeX.moveTo(eyeX);
		if (eyeY != null)this.eyeY.moveTo(eyeY);
		if (jaw != null)this.jaw.moveTo(jaw);
	}

	public void rest() {
		// initial positions
		setSpeed(1.0f,1.0f,1.0f,1.0f,1.0f);
		rothead.moveTo(90);
		neck.moveTo(90);
		eyeX.moveTo(90);
		eyeY.moveTo(90);
		jaw.moveTo(0);
	}
	
	public void broadcastState() {
		// notify the gui
		rothead.broadcastState();
		neck.broadcastState();
		eyeX.broadcastState();
		eyeY.broadcastState();
		jaw.broadcastState();
	}
	


	public void detachServos() {
		if (rothead != null) {
			rothead.detach();
		}
		if (neck != null) {
			neck.detach();
		}
		if (eyeX != null) {
			eyeX.detach();
		}
		if (eyeY != null) {
			eyeY.detach();
		}
		if (jaw != null) {
			jaw.detach();
		}
	}

	public void release() {
		detachServos();

		if (rothead != null) {
			rothead.releaseService();
			rothead = null;
		}
		if (neck != null) {
			neck.releaseService();
			neck = null;
		}
		if (eyeX != null) {
			eyeX.releaseService();
			eyeX = null;
		}
		if (eyeY != null) {
			eyeY.releaseService();
			eyeY = null;
		}
		if (jaw != null) {
			jaw.releaseService();
			jaw = null;
		}
	}

	public void setSpeed(Float rothead, Float neck, Float eyeX, Float eyeY, Float jaw) {
		this.rothead.setSpeed(rothead);  
		this.neck.setSpeed(neck);
		this.eyeX.setSpeed(eyeX);
		this.eyeY.setSpeed(eyeY);
		this.jaw.setSpeed(jaw);
	}
	
	public String getScript() {
		return String.format("%s.moveHand(\"%s\",%d,%d,%d,%d,%d)\n", getName(), rothead.getPosition(), neck.getPosition(), eyeX.getPosition(),
				eyeY.getPosition(), jaw.getPosition());
	}
	
	public void setpins(int rothead, int neck, int eyeX, int eyeY, int jaw, int wrist){
		log.info(String.format("setPins %d %d %d %d %d %d", rothead, neck, eyeX, eyeY, jaw, wrist));
		rotheadPin = rothead;
		neckPin = neck;
		eyeXPin = eyeX;
		eyeYPin = eyeY;
		jawPin = jaw;
	}
	
	
	public boolean isValid()
	{
		rothead.moveTo(92);
		neck.moveTo(92);
		eyeX.moveTo(92);
		eyeY.moveTo(92);
		jaw.moveTo(2);
		return true;
	}

	public void setLimits(int rotheadMin, int rotheadMax, int neckMin, int neckMax, int eyeXMin, int eyeXMax, int eyeYMin, int eyeYMax, int jawMin, int jawMax)
	{
		this.rotheadMin = rotheadMin;
		this.rotheadMax = rotheadMax;
		this.neckMin = neckMin;
		this.neckMax = neckMax;
		this.eyeXMin = eyeXMin;
		this.eyeXMax = eyeXMax;
		this.eyeYMin = eyeYMin;
		this.eyeYMax = eyeYMax;	
		this.jawMin = jawMin;
		this.jawMax = jawMax;	
	}
	
	public void setLimits()
	{
		rothead.setMinMax(rotheadMin, rotheadMax);
		neck.setMinMax(neckMin, neckMax);
		eyeX.setMinMax(eyeXMin, eyeXMax);
		eyeY.setMinMax(eyeYMin, eyeYMax);
		jaw.setMinMax(jawMin, jawMax);
	}
	
	// ----- initialization end --------
	// ----- movements begin -----------

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		InMoovHead head = new InMoovHead("head01");
		
		//Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().create();
		head.createReserved("HeadTracking");
		head.startReserved("HeadTracking");
		 
		//head.startService();			
		//log.warn(gson.toJson(Service.getReservations()));
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
