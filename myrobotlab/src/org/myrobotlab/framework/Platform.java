package org.myrobotlab.framework;

import java.io.Serializable;

public class Platform implements Serializable {
	private static final long serialVersionUID = 1L;
	final public String os;
	final public String arch;
	final public int bitness;
	final public String vmName;
	Platform(String os, String arch, int bitness, String vmName)
	{
		this.os = os;
		this.arch = arch;
		this.bitness = bitness;
		this.vmName = vmName;
	}
}
