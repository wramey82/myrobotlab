package org.myrobotlab.serial.gnu;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.myrobotlab.serial.SerialDevice;
import org.myrobotlab.serial.SerialDeviceFactory;
import org.myrobotlab.serial.SerialDeviceIdentifier;

public class SerialDeviceIdentifierGNU  implements SerialDeviceIdentifier {

	public final static Logger log = Logger.getLogger(SerialDeviceFactory.class.getCanonicalName());
	private CommPortIdentifier commPort;
	

	public SerialDeviceIdentifierGNU(CommPortIdentifier commPort)
	{
		this.commPort = commPort;
	}
	
	public static ArrayList<SerialDeviceIdentifierGNU> getSerialDeviceIdentifiers()
	{
		ArrayList<SerialDeviceIdentifierGNU> ret = new ArrayList<SerialDeviceIdentifierGNU>();
		CommPortIdentifier portId;
		// getPortIdentifiers - returns all ports "available" on the machine -
		// ie not ones already used
		Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			portId = (CommPortIdentifier) portList.nextElement();
			String inPortName = portId.getName();
			log.info(inPortName);
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				ret.add(new SerialDeviceIdentifierGNU(portId));
			}
		}
		return ret;
	}

	@Override
	public String getCurrentOwner() {
		return commPort.getCurrentOwner();
	}

	@Override
	public String getName() {
		return commPort.getName();
	}

	@Override
	public int getPortType() {
		return commPort.getPortType();
	}

	@Override
	public boolean isCurrentlyOwned() {
		return commPort.isCurrentlyOwned();
	}

	@Override
	public SerialDevice open(FileDescriptor f) throws Exception {
		return new SerialDeviceGNU((SerialPort)commPort.open(f));
	}

	@Override
	public SerialDevice open(String TheOwner, int i) throws Exception {
		return new SerialDeviceGNU((SerialPort)commPort.open(TheOwner, i));
	}

}
