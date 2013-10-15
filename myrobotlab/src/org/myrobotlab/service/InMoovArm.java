package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class InMoovArm extends Service {

	public final static Logger log = LoggerFactory.getLogger(InMoovArm.class);
	private static final long serialVersionUID = 1L;
		
	/**
	 * peer services
	 */
	public Servo bicep;
	public Servo rotate;
	public Servo shoulder;
	public Servo omoplate;
	
	public Arduino arduino;
	
	// user defined cached values begin
	
	// pins and defaults
	public int bicepPin = 8;
	public int rotatePin = 9;
	public int shoulderPin = 10;
	public int omoplatePin = 11;
	
	// servo limits
	public int bicepMin = 0;
	public int bicepMax = 90;
	public int rotateMin = 40;
	public int rotateMax = 180;
	public int shoulderMin = 0;
	public int shoulderMax = 180;
	public int omoplateMin = 10;
	public int omoplateMax = 80;
	
	// default rest position
	public int bicepRest = 0;
	public int rotateRest = 90;
	public int shoulderRest = 30;
	public int omoplateRest = 10;

	// user defined cached values end

	public InMoovArm(String n) {
		super(n, InMoovArm.class.getCanonicalName());	
	}
	
	@Override
	public void startService() {
		super.startService();
		startPeers();
		setLimits();
		attachServos();
		rest();
		broadcastState();
	}

	public void createPeers()
	{
		bicep = (Servo)createReserved("Bicep");
		rotate = (Servo)createReserved("Rotate");
		shoulder = (Servo)createReserved("Shoulder");
		omoplate = (Servo)createReserved("Omoplate");
		
		arduino = (Arduino)createReserved("Arduino");
	}
	
	public void startPeers()
	{
		bicep = (Servo)startReserved("Bicep");
		rotate = (Servo)startReserved("Rotate");
		shoulder = (Servo)startReserved("Shoulder");
		omoplate = (Servo)startReserved("Omoplate");
		
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
		// IMPORTANT at this point the attachment REQUIRES - actual services
		// and connectivity !!!
		
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

		// this is done here
		// so that this.setPins does not require actual creation of servos
		// good / bad ??
		bicep.setPin(bicepPin);
		rotate.setPin(rotatePin);
		shoulder.setPin(shoulderPin);
		omoplate.setPin(omoplatePin);
	
		arduino.servoAttach(bicep);
		arduino.servoAttach(rotate);
		arduino.servoAttach(shoulder);
		arduino.servoAttach(omoplate);
		
		return true;
	}

	@Override
	public String getDescription() {
		return "the InMoov Arm Service";
	}
	
	public void rest() {
		
		setSpeed(1.0f,1.0f,1.0f,1.0f);

		// initial position
		bicep.moveTo(bicepRest);
		rotate.moveTo(rotateRest);
		shoulder.moveTo(shoulderRest);
		omoplate.moveTo(omoplateRest);
	}
	// ------------- added set pins
	public void setpins(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		 bicepPin=bicep;
		 rotatePin=rotate;
		 shoulderPin=shoulder;
		 omoplatePin=omoplate;
	}
	
	public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax, int omoplateMin, int omoplateMax)
	{
		this.bicepMin = bicepMin;
		this.bicepMax = bicepMax;
		this.rotateMin = rotateMin;
		this.rotateMax = rotateMax;
		this.shoulderMin = shoulderMin;
		this.shoulderMax = shoulderMax;
		this.omoplateMin = omoplateMin;
		this.omoplateMax = omoplateMax;	
	}
	
	public void setLimits()
	{
		bicep.setMinMax(bicepMin, bicepMax);
		rotate.setMinMax(rotateMin, rotateMax);
		shoulder.setMinMax(shoulderMin, shoulderMax);
		omoplate.setMinMax(omoplateMin, omoplateMax);
	}
	
	public void setRestPos(int bicepRest, int rotateRest, int shoulderRest, int omoplateRest)
	{
		this.bicepRest = bicepRest;
		this.rotateRest = rotateRest;
		this.shoulderRest = shoulderRest;
		this.omoplateRest = omoplateRest;
	}
	
	

	public void broadcastState() {
		// notify the gui
		bicep.broadcastState();
		rotate.broadcastState();
		shoulder.broadcastState();
		omoplate.broadcastState();
	}

	public void detach() {
		if (bicep != null) {
			bicep.detach();
		}
		if (rotate != null) {
			rotate.detach();
		}
		if (shoulder != null) {
			shoulder.detach();
		}
		if (omoplate != null) {
			omoplate.detach();
		}
	}

	public void release() {
		detach();
		if (bicep != null) {
			bicep.releaseService();
			bicep = null;
		}
		if (rotate != null) {
			rotate.releaseService();
			rotate = null;
		}
		if (shoulder != null) {
			shoulder.releaseService();
			shoulder = null;
		}
		if (omoplate != null) {
			omoplate.releaseService();
			omoplate = null;
		}

	}

	public void setSpeed(Float bicep, Float rotate, Float shoulder, Float omoplate) {
		this.bicep.setSpeed(bicep);
		this.rotate.setSpeed(rotate);
		this.shoulder.setSpeed(shoulder);
		this.omoplate.setSpeed(omoplate);
	}

	public String getScript() {
		return String
				.format("%s.moveArm(%d,%d,%d,%d)\n", getName(), bicep.getPosition(), rotate.getPosition(), shoulder.getPosition(), omoplate.getPosition());
	}

	public void moveTo(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		this.bicep.moveTo(bicep);
		this.rotate.moveTo(rotate);
		this.shoulder.moveTo(shoulder);
		this.omoplate.moveTo(omoplate);

	}
	
	public boolean isValid()
	{
		bicep.moveTo(2);
		rotate.moveTo(92);
		shoulder.moveTo(32);
		omoplate.moveTo(12);	
		return true;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		InMoovArm template = new InMoovArm("template");
		template.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
