package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;

public class SystemInformation extends Service {

	// TODO - always return values without formatting - formatting can be
	// applied with other functions
	
	// Process and runtime
	// http://stackoverflow.com/questions/636367/java-executing-a-java-application-in-a-separate-process

	private static final long serialVersionUID = 1L;
	public final static Logger LOG = Logger.getLogger(SystemInformation.class
			.getCanonicalName());

	// public SystemInformationConfig config;
	/*
	 * public void setConfig(SystemInformationConfig config) { this.config =
	 * config; }
	 */
	public SystemInformation(String n) {
		super(n, SystemInformation.class.getCanonicalName());
	}

	public void loadDefaultConfiguration() {
	}

	static int getPID(Process process)
	throws IllegalAccessException, IllegalArgumentException,
	NoSuchFieldException, SecurityException
	{
		Field field = process.getClass().getDeclaredField("pid");
		field.setAccessible(true);
		return field.getInt(process);
	}	
	
	
	
	// http://stackoverflow.com/questions/25552/using-java-to-get-os-level-system-information
	public String getSystemInfoString() {
		String ret = "";
		ret += "hello dave. ";
		ret += "see pee you temperature is " + getCPUTemp() + " celcius. ";
		ret += "total memory is 2,630 mega bytes. ";
		ret += "free memory is 780 mega bytes. ";
		ret += "That is a blue thingy.. ";
		ret += "That is a green thingy.. ";
		ret += "I can see out of my right eye.. ";
		ret += "I can see out of my left eye.. ";
		ret += " And all my circuits are functioning perfectly dave ";
		return ret;
	}

	// TODO - use fileLib
	public Integer getFreeMem() {
		String record = "";
		StringBuffer ret = new StringBuffer("");
		int recCount = 0;
		// return "30 C 87 F";
		String cpuTempFile = "/proc/meminfo";
		FileReader fr;
		try {
			fr = new FileReader(cpuTempFile);
			BufferedReader br = new BufferedReader(fr);

			while ((record = br.readLine()) != null) {
				recCount++;
				System.out.println(recCount + ": " + record);
				ret.append(record);
			}

			record = ret.toString().substring(24, 27).trim();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret.append("FileNotFoundException " + cpuTempFile);
		} catch (IOException e) {
			ret.append("IOException ");
		}

		return new Integer(Integer.parseInt(record));

	}

	// TODO - use fileLib
	public Integer getCPUTemp() {
		String record = "";

		String acpi = FileIO
				.fileToString("/proc/acpi/thermal_zone/THM0/temperature");
		record = acpi.substring(24, 27).trim();

		return new Integer(Integer.parseInt(record));

	}

	// TODO - return Message? to GWT?
	public String getCPUTemp(Message m) {

		// String helloTo = msg;
		String record = "";
		StringBuffer ret = new StringBuffer("");
		// return "30 C 87 F";
		String acpi = FileIO
				.fileToString("/proc/acpi/thermal_zone/THM0/temperature");
		record = acpi.substring(24, 27).trim();

		Double f = 1.8 * Integer.parseInt(record) + 32;
		ret.setLength(0);
		ret.append(record + " C " + f.intValue() + " F");

		return ret.toString();

	}

	@Override
	public String getToolTip() {
		return "<html>service for retrieving information regarding the system</html>";
	}

	
	public static void main(String[] args) throws ClassNotFoundException {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

	
	Runtime r = Runtime.getRuntime();
    long mem1, mem2;
    Integer someints[] = new Integer[10000];

    System.out.println("Total memory is: " + r.totalMemory());

    mem1 = r.freeMemory();
    System.out.println("Initial free memory: " + mem1);
    r.gc();
    mem1 = r.freeMemory();
    System.out.println("Free memory after garbage collection: " + mem1);

    for (int i = 0; i < someints.length; i++)
      someints[i] = new Integer(i); // allocate integers

    mem2 = r.freeMemory();
    System.out.println("Free memory after allocation: " + mem2);
    System.out.println("Memory used by allocation: " + (mem1 - mem2));

    for (int i = 0; i < someints.length; i++)
      someints[i] = null;

    r.gc(); // request garbage collection

    mem2 = r.freeMemory();
    System.out.println("Free memory after collecting" + " discarded Integers: "
        + mem2);

	
	}	
}
