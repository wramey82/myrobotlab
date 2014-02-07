package org.myrobotlab.pickToLight;

import java.util.HashMap;
import java.util.Map;

public class Controller2 {

	private String name;
	private String version;
	private String ipAddress;
	private String macAddress;
	
	private HashMap <String, Module2> modules = new HashMap <String, Module2>();
	
	public Controller2(){
	}

	public String getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public HashMap<String, Module2> getModules() {
		return modules;
	}

	public void setModules(HashMap<String, Module2> modules) {
		this.modules = modules;
	}

}
