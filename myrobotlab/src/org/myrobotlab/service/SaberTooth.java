package org.myrobotlab.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.serial.SerialDeviceService;
import org.myrobotlab.service.Arduino.MotorData;
import org.myrobotlab.service.data.Pin;
import org.myrobotlab.service.interfaces.MotorControl;
import org.myrobotlab.service.interfaces.MotorController;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.simpleframework.xml.Element;
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
public class SaberTooth extends Service implements MotorController {

	private static final long serialVersionUID = 1L;

	public final static int PACKETIZED_SERIAL_MODE = 4;
	
	int mode = PACKETIZED_SERIAL_MODE;
	
	public final static Logger log = LoggerFactory.getLogger(SaberTooth.class);
		
	public SerialDeviceService serial;
	
	private Integer address = 128	;
	
	// range mapping
	@Element
	private float minX = 0;
	@Element
	private float maxX = 180;
	@Element
	private float minY = 0;
	@Element
	private float maxY = 180;
	
	boolean setSaberToothBaud = false;
	
	class MotorData implements Serializable {
		private static final long serialVersionUID = 1L;
		transient MotorControl motor = null;
		int motorPort = -1;
	}

	HashMap<String, MotorData> motors = new HashMap<String, MotorData>();

	
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
	
	public boolean disconnect(){
		if (serial != null){
			return serial.disconnect();
		}
		return true;
	}
	
	public void setAddress(int address){
		this.address = address;
	}
	
	@Override
	public String getDescription() {
		return "used as a general template";
	}
	
	public void sendPacket(int command, int data) throws IOException {
		// NEW WAY - if "throws IOException" explodes outward - does thread die ?
		// shouldn't .. if it does.. needs fixin ;)
		
		if (serial == null || !serial.isConnected()){
			error("serial device not connected");
			return;
		}
		
		if (!setSaberToothBaud){
			serial.write(170);
			setSaberToothBaud = true;
		}
		
		serial.write(address);
		serial.write(command);
		serial.write(data);
		serial.write((address + 0 + data) & 0b01111111);
	}

	//----------MotorController Interface Begin --------------
	
	
	/**
	 * Motor Controller specific method for attaching a motor
	 * In the case of a SaberTooth - we will need the motor name (of course)
	 * and the motor port number - SaberTooth supports 2 (M1 & M2)
	 * @param motorName
	 * @param motorPort
	 * @return
	 */
	public boolean motorAttach(String motorName, int motorPort){
		return motorAttach(motorName, new Object[]{motorPort});
	}
	
	@Override
	public boolean motorAttach(String motorName, Object... motorData) {
		ServiceInterface sw = Runtime.getService(motorName);
		if (!sw.isLocal()) {
			error("motor is not in the same MRL instance as the motor controller");
			return false;
		}
		
		ServiceInterface service = sw;
		MotorControl motor = (MotorControl) service; // BE-AWARE - local
														// optimization ! Will
														// not work on remote
														// !!!
		if (motor == null || motorData == null) {
			error("null data or motor - can't attach motor");
			return false;
		}

		if (motorData.length != 1 || motorData[0] == null || motorData[0].getClass() != Integer.class) {
			error("motor data must be of the folowing format - motorAttach(motorName, (1 || 2))");
			return false;
		}

		MotorData md = new MotorData();
		md.motor = motor;
		motors.put(motor.getName(), md);
		motor.setController(this);
		return true;

	}

	@Override
	public ArrayList<Pin> getPinList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void motorMoveTo(String name, Integer position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void motorMove(String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean motorDetach(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object[] getMotorData(String motorName) {
		// TODO Auto-generated method stub
		return null;
	}

	//----------MotorController Interface End --------------
	
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
