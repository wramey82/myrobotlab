package org.myrobotlab.serial;


public class DeviceInUseException extends Exception {

	public DeviceInUseException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
