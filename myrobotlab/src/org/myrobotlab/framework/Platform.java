package org.myrobotlab.framework;

import java.io.Serializable;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.runtime.ProcParser;

public class Platform implements Serializable {

	private static final long serialVersionUID = 1L;

	// VM Names
	public final static String VM_DALVIK = "dalvik";
	public final static String VM_HOTSPOT = "hotspot";

	// OS Names
	public final static String OS_LINUX = "linux";
	public final static String OS_MAC = "mac";
	public final static String OS_WINDOWS = "windows";

	public final static String UNKNOWN = "unknown";
	
	// arch names
	public final static String ARCH_X86 = "x86";
	public final static String ARCH_ARM = "arm";

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
		if ((os.indexOf(OS_LINUX) >= 0)) {
			return OS_LINUX;
		} else if ((os.indexOf(OS_MAC) >= 0)) {
			return OS_MAC;
		} else if ((os.indexOf("win") >= 0)) {
			return OS_WINDOWS;
		} else {
			return UNKNOWN;
		}
	}

	public static String getVMName() {
		String vmname = System.getProperty("java.vm.name").toLowerCase();

		if (vmname.equals(VM_DALVIK)) {
			return VM_DALVIK;
		} else {
			return VM_HOTSPOT;
		}
	}

	public static boolean isDavlik() {
		return VM_DALVIK.equals(getVMName());
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
		
		if ("arm".equals(arch)){
			Integer armv = ProcParser.getArmInstructionVersion();
			if (armv != null){
				arch = String.format("armv%d",armv);
			}
			//arch = "armv6"; // assume its version 6 instruction set
			
		}
		return arch;
	}

	public static boolean isMac() {
		return getOS().equals(OS_MAC);
	}

	public static boolean isLinux() {
		return getOS().equals(OS_LINUX);
	}

	public static boolean isWindows() {
		return getOS().equals(OS_WINDOWS);
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
	
	public static boolean isArm() {
		return getArch().startsWith(ARCH_ARM);
	}
	
	public static boolean isX86() {
		return getArch().equals(ARCH_X86);
	}
	
	public String toString()
	{
		return String.format("%s.%d.%s", arch, bitness, os);
	}
	


}
