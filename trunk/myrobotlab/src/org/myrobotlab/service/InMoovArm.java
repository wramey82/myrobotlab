package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class InMoovArm extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoovArm.class);

	/**
	 * peer services
	 */
	transient public Servo bicep;
	transient public Servo rotate;
	transient public Servo shoulder;
	transient public Servo omoplate;
	transient public Arduino arduino;
	
	public Servo getBicep() {
		return bicep;
	}

	public void setBicep(Servo bicep) {
		this.bicep = bicep;
	}

	public Servo getRotate() {
		return rotate;
	}

	public void setRotate(Servo rotate) {
		this.rotate = rotate;
	}

	public Servo getShoulder() {
		return shoulder;
	}

	public void setShoulder(Servo shoulder) {
		this.shoulder = shoulder;
	}

	public Servo getOmoplate() {
		return omoplate;
	}

	public void setOmoplate(Servo omoplate) {
		this.omoplate = omoplate;
	}

	public Arduino getArduino() {
		return arduino;
	}

	public void setArduino(Arduino arduino) {
		this.arduino = arduino;
	}

	// static in Java are not overloaded but overwritten - there is no polymorphism for statics
	public static Peers getPeers(String name)
	{
		Peers peers = new Peers(name);
		peers.put("bicep", "Servo", "Bicep servo");
		peers.put("rotate", "Servo", "Rotate servo");
		peers.put("shoulder", "Servo", "Shoulder servo");
		peers.put("omoplate", "Servo", "Omoplate servo");
		peers.put("arduino", "Arduino", "Arduino controller for this arm");
		return peers;
	}
	
	public InMoovArm(String n) {
		super(n);
		//createReserves(n); // Ok this might work but IT CANNOT BE IN SERVICE FRAMEWORK !!!!!
		bicep = (Servo) createPeer("bicep");
		rotate = (Servo) createPeer("rotate");
		shoulder = (Servo) createPeer("shoulder");
		omoplate = (Servo) createPeer("omoplate");
		arduino = (Arduino) createPeer("arduino");
		
		// connection details
		bicep.setPin(8);
		rotate.setPin(9);
		shoulder.setPin(10);
		omoplate.setPin(11);
		
		bicep.setController(arduino);
		rotate.setController(arduino);
		shoulder.setController(arduino);
		omoplate.setController(arduino);
		
		bicep.setMinMax(0, 90);
		rotate.setMinMax(40, 180);
		shoulder.setMinMax(0, 180);
		omoplate.setMinMax(10, 80);
		
		bicep.setRest(0);
		rotate.setRest(90);
		shoulder.setRest(30);
		omoplate.setRest(10);
	}

	@Override
	public void startService() {
		super.startService();
		bicep.startService();
		rotate.startService();
		shoulder.startService();
		omoplate.startService();
		arduino.startService();
	}

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
		boolean result = true; 
		result &= bicep.attach();
		result &= rotate.attach();
		result &= shoulder.attach();
		result &= omoplate.attach();
		
		/*
		result &= arduino.servoAttach(bicep);
		sleep(InMoov.MSG_DELAY);
		result &= arduino.servoAttach(rotate);
		sleep(InMoov.MSG_DELAY);
		result &= arduino.servoAttach(shoulder);		
		sleep(InMoov.MSG_DELAY);
		result &= arduino.servoAttach(omoplate);
		sleep(InMoov.MSG_DELAY);
		*/
		return result;
	}

	@Override
	public String getDescription() {
		return "the InMoov Arm Service";
	}

	public void rest() {

		setSpeed(1.0f, 1.0f, 1.0f, 1.0f);

		// initial position
		bicep.rest();
		rotate.rest();
		shoulder.rest();
		omoplate.rest();
	}

	// ------------- added set pins
	public void setpins(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {

		log.info(String.format("setPins %d %d %d %d %d %d", bicep, rotate, shoulder, omoplate));
		//createPeers();
		this.bicep.setPin(bicep);
		this.rotate.setPin(rotate);
		this.shoulder.setPin(shoulder);
		this.omoplate.setPin(omoplate);
	}

	public void setLimits(int bicepMin, int bicepMax, int rotateMin, int rotateMax, int shoulderMin, int shoulderMax, int omoplateMin, int omoplateMax) {
		bicep.setMinMax(bicepMin, bicepMax);
		rotate.setMinMax(rotateMin, rotateMax);
		shoulder.setMinMax(shoulderMin, shoulderMax);
		omoplate.setMinMax(omoplateMin, omoplateMax);
	}

	public void broadcastState() {
		// notify the gui
		bicep.broadcastState();
		rotate.broadcastState();
		shoulder.broadcastState();
		omoplate.broadcastState();
	}

	public void detach() {
			bicep.detach();
			//sleep(InMoov.MSG_DELAY);
			rotate.detach();
			//sleep(InMoov.MSG_DELAY);
			shoulder.detach();
			//sleep(InMoov.MSG_DELAY);
			omoplate.detach();
			//sleep(InMoov.MSG_DELAY);
	}

	// FIXME - releasePeers()
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
		return String.format("%s.moveArm(%d,%d,%d,%d)\n", getName(), bicep.getPosition(), rotate.getPosition(), shoulder.getPosition(), omoplate.getPosition());
	}

	public void moveTo(Integer bicep, Integer rotate, Integer shoulder, Integer omoplate) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("%s moveTo %d %d %d %d %d", getName(), bicep, rotate, shoulder, omoplate));
		}
		this.bicep.moveTo(bicep);
		this.rotate.moveTo(rotate);
		this.shoulder.moveTo(shoulder);
		this.omoplate.moveTo(omoplate);
	}

	public boolean isValid() {
		bicep.moveTo(bicep.getRest() + 2);
		rotate.moveTo(rotate.getRest() + 2);
		shoulder.moveTo(shoulder.getRest() + 2);
		omoplate.moveTo(omoplate.getRest() + 2);
		return true;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);
		
		InMoovArm arm = (InMoovArm)Runtime.create("arm","InMoovArm");
		arm.connect("COM9");
		arm.startService();

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}

}
