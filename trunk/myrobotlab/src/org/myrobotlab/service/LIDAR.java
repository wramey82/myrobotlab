package org.myrobotlab.service;

import java.io.ByteArrayOutputStream;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class LIDAR extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(LIDAR.class.getCanonicalName());

	public static final String MODEL_SICK_LMS200 = "SICK LMS200";
	
	public String serialName;

	public transient Serial serial;
	
	public ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	String model;

	// states
	public static final String STATE_PRE_INITIALIZATION = "state pre initialization";
	public static final String STATE_INITIALIZATION_STAGE_1 = "state initialization stage 1";
	public static final String STATE_INITIALIZATION_STAGE_2 = "state initialization stage 2";
	public static final String STATE_INITIALIZATION_STAGE_3 = "state initialization stage 3";
	public static final String STATE_INITIALIZATION_STAGE_4 = "state initialization stage 4";

	String state = STATE_PRE_INITIALIZATION;

	public LIDAR(String n) {
		super(n, LIDAR.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "The LIDAR service";
	}

	public void startService() {
		super.startService();

		try {
			serial = getSerial();
			// setting callback / message route
			serial.addListener("publishByte", getName(), "byteReceived");
			serial.startService();
			if (model == null) {
				model = MODEL_SICK_LMS200;
			}

			// start LIDAR hardware initialization here 
			// data coming back from the hardware will be in byteRecieved
			if (MODEL_SICK_LMS200.equals(model)) {
				serial.write(new byte[] { 1, 38, 32, 43 });
			}
			state = STATE_INITIALIZATION_STAGE_1;
		} catch (Exception e) {
			error(e.getMessage());
		}
	}
	
	public void byteReceived(Byte b)
	{
		try {
		buffer.write(b);
		// so a byte was appended
		// now depending on what model it was and
		// what stage of initialization we do that funky stuff
		if (MODEL_SICK_LMS200.equals(model) && STATE_INITIALIZATION_STAGE_1.equals(state) && buffer.size() == 32)
		{
			// we have the right model & the correct stage & the right size of response
			// send the next initialization sequence
			serial.write(new byte[] { 1, 38, 32, 43 });
			state = STATE_INITIALIZATION_STAGE_2;
		}
		
		if (MODEL_SICK_LMS200.equals(model) && STATE_INITIALIZATION_STAGE_2.equals(state) && buffer.size() == 32)
		{
			// we have the right model & the correct stage & the right size of response
			// send the next initialization sequence
			serial.write(new byte[] { 1, 38, 32, 43 });
			state = STATE_INITIALIZATION_STAGE_3;
		}
		
		// ... etc ..
		
		} catch(Exception e){
			error(e.getMessage());
		}
	}

	public boolean connect(String port) {
		serial = getSerial();
		serial.connect(port);
		return serial.isConnected();
	}

	public Serial getSerial() {
		if (serialName == null) {
			serialName = String.format("%s_serial", getName());
		}
		serial = (Serial) Runtime.create(serialName, "Serial");
		return serial;
	}

	public void setModel(String m) {
		model = m;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		LIDAR template = new LIDAR("template");
		template.startService();

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
