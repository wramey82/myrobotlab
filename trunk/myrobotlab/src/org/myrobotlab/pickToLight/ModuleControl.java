package org.myrobotlab.pickToLight;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.i2c.AdafruitLEDBackpack;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class ModuleControl {

	public final static Logger log = LoggerFactory.getLogger(ModuleControl.class);
	
	private ModuleType module;
	private ObjectFactory of = new ObjectFactory();
	
	private AdafruitLEDBackpack display;

	public String display(String str) {
		log.info(String.format("module %d displaying %s", module.getAddress().getI2CAddress(), str));
		if (display != null) {
			return display.display(str);
		}
		return str;
	}

	public int getI2CAddress() {
		return module.getAddress().getI2CAddress();
	}

	public int getI2CBus() {
		return module.getAddress().getI2CBus();
	}

	public ModuleType getModule() {
		return module;
	}

	public void blinkOff(String msg){
		log.info(String.format("blinkOff %s", msg));
		if (display != null) {
			display.blinkOff(msg);
		}
	}
	
	public void blinkOn(String msg){
		log.info(String.format("blinkOn %s", msg));
		if (display != null) {
			display.blinkOn(msg);
		}
	}
	
	public void cycle(String msg){
		display.cycle(msg, 300);
	}
	
	public void cycle(String msg, Integer delay){
		log.info(String.format("cycle %s %d", msg, delay));
		if (display != null) {
			display.cycle(msg, delay);
		}
	}
	
	public ModuleControl(int bus, int address) {
		module = of.createModuleType();
		module.setAddress(of.createAddressType());
		module.getAddress().setI2CBus(bus);
		module.getAddress().setI2CAddress(address);

		try {

			if (Platform.isArm()) {
				display = new AdafruitLEDBackpack(bus, address);
			}
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public void cycleStop() {
		log.info("cycleStop");
		if (display != null) {
			display.cycleStop();
		}
		
	}
	
	

	// FIXME - add PCFBLAHBLAH
}
