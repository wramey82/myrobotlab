package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 * 
 * SaberTooth service for the sabertooth motor controller
 * command reference : http://www.dimensionengineering.com/datasheets/Sabertooth2x25.pdf
 * 
 *  Packet PseudoCode
 * 	Putc(address);
	Putc(0);
	Putc(speed);
	Putc((address + 0 + speed) & 0b01111111);

 *
 */
public class SaberTooth extends Service implements MotorControl {

	private static final long serialVersionUID = 1L;

	public final static int PACKETIZED_SERIAL_MODE = 4;
	
	int mode = PACKETIZED_SERIAL_MODE;
	
	public final static Logger log = LoggerFactory.getLogger(SaberTooth.class);
		
	public SerialDeviceService serial;
	
	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);
		// put peer definitions in
		peers.put("serial", "Arduino", "Serial Port");

		return peers;
				
	}
	
	public SaberTooth(String n) {
		super(n);	
		serial = (SerialDeviceService) startPeer("serial");
	}
	
	public boolean connect(String port){
		// The valid baud rates are 2400, 9600, 19200 and 38400 baud
		return serial.connect(port, 38400, 8, 1, 0);
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public boolean setController(MotorController controller) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean detach() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAttached() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(Float power) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveTo(Integer newPos) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveFor(Float power, Float duration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveFor(Float power, Float duration, Boolean block) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getPowerLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void invertDirection(boolean invert) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDirectionInverted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stopAndLock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unlock() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaxPower(float max) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		SaberTooth sabertooth = new SaberTooth("sabertooth");
		sabertooth.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * 
		 */
	}



}
