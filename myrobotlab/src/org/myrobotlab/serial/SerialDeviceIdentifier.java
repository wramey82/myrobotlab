package org.myrobotlab.serial;
import java.io.FileDescriptor;

public interface SerialDeviceIdentifier {

	public static final int PORT_SERIAL = 1; // rs232 Port
	public static final int PORT_PARALLEL = 2; // Parallel Port
	public static final int PORT_I2C = 3; // i2c Port
	public static final int PORT_RS485 = 4; // rs485 Port
	public static final int PORT_RAW = 5; // Raw Port

	/*------------------------------------------------------------------------------
		addPortOwnershipListener()
		accept:
		perform:
		return:
		exceptions:
		comments:   
	------------------------------------------------------------------------------*/
	//public abstract void addPortOwnershipListener(CommPortOwnershipListener c);

	/*------------------------------------------------------------------------------
		getCurrentOwner()
		accept:
		perform:
		return:
		exceptions:
		comments:    
	------------------------------------------------------------------------------*/
	public abstract String getCurrentOwner();

	/*------------------------------------------------------------------------------
		getName()
		accept:
		perform:
		return:
		exceptions:
		comments:
	------------------------------------------------------------------------------*/
	public abstract String getName();

	/*------------------------------------------------------------------------------
		getPortType()
		accept:
		perform:
		return:
		exceptions:
		comments:
	------------------------------------------------------------------------------*/
	public abstract int getPortType();

	/*------------------------------------------------------------------------------
		isCurrentlyOwned()
		accept:
		perform:
		return:
		exceptions:
		comments:    
	------------------------------------------------------------------------------*/
	public abstract boolean isCurrentlyOwned();

	/*------------------------------------------------------------------------------
		open()
		accept:
		perform:
		return:
		exceptions:
		comments:
	------------------------------------------------------------------------------*/
	public abstract SerialDevice open(FileDescriptor f)
			throws Exception;

	public abstract SerialDevice open(String TheOwner, int i)
			throws Exception;

	/*------------------------------------------------------------------------------
		removePortOwnership()
		accept:
		perform:
		return:
		exceptions:
		comments:
	------------------------------------------------------------------------------*/
	//public abstract void removePortOwnershipListener(CommPortOwnershipListener c);

}