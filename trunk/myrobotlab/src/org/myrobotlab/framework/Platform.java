package org.myrobotlab.framework;

import java.io.Serializable;

import org.myrobotlab.fileLib.FileIO;

public class Platform implements Serializable {

	private static final long serialVersionUID = 1L;

	// VM Names
	public final static String DALVIK = "dalvik";
	public final static String HOTSPOT = "hotspot";

	// OS Names
	public final static String LINUX = "linux";
	public final static String MAC = "mac";
	public final static String WINDOWS = "windows";

	public final static String UNKNOWN = "unknown";

	final public String os;
	final public String arch;
	final public int bitness;
	final public String vmName;
	final public String mrlVersion;

	public Platform(String os, String arch, int bitness, String vmName, String mrlVersion) {
		this.os = os;
		this.arch = arch;
		this.bitness = bitness;
		this.vmName = vmName;
		this.mrlVersion = mrlVersion;
	}

	// -------------pass through begin -------------------
	public static Platform getPlatform() {
		return new Platform(getOS(), getArch(), getBitness(), getVMName(), getMRLVersion());
	}

	private static String getMRLVersion() {
		return FileIO.getResourceFile("version.txt");
	}

	public static String getOS() {
		String os = System.getProperty("os.name").toLowerCase();
		if ((os.indexOf(LINUX) >= 0)) {
			return LINUX;
		} else if ((os.indexOf(MAC) >= 0)) {
			return MAC;
		} else if ((os.indexOf("win") >= 0)) {
			return WINDOWS;
		} else {
			return UNKNOWN;
		}
	}

	public static String getVMName() {
		String vmname = System.getProperty("java.vm.name").toLowerCase();

		if (vmname.equals(DALVIK)) {
			return DALVIK;
		} else {
			return HOTSPOT;
		}
	}

	public static boolean isDavlik() {
		return DALVIK.equals(getVMName());
	}

	public static int getBitness() {
		String model = System.getProperty("sun.arch.data.model");
		if ("64".equals(model)) {
			return 64;
		}
		return 32;
	}

	/**
	 * Returns only the bitness of the JRE hooked here in-case we need to
	 * normalize
	 * 
	 * @return hardware architecture
	 */
	public static String getArch() {
		String arch = System.getProperty("os.arch").toLowerCase();
		if ("i386".equals(arch) || "i686".equals(arch) || "i586".equals(arch) || "amd64".equals(arch) || arch.startsWith("x86")) {
			arch = "x86"; // don't care at the moment
		}
		return arch;
	}

	public static boolean isMac() {
		return getOS().equals(MAC);
	}

	public static boolean isLinux() {
		return getOS().equals(LINUX);
	}

	public static boolean isWindows() {
		return getOS().equals(WINDOWS);
	}
	
	public static String getClassPathSeperator()
	{
		if (isWindows())
		{
			return ";";
		} else {
			return ":";
		}
	}
	
	public String toString()
	{
		return String.format("%s.%d.%s", arch, bitness, os);
	}

}
