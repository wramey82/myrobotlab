package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class ServiceInfo {

	private static HashMap<String, ArrayList<Dependency>> dependencies = new HashMap<String, ArrayList<Dependency>>();
	private static HashMap<String, ArrayList<String>> categories = new HashMap<String, ArrayList<String>>();
	private static ServiceInfo instance = null;
	
	// TODO - command line refresh - repo management & configuration options "latest" etc
	static public Set<String> getKeySet()
	{
		return dependencies.keySet();
	}
	
	public void addBase(String shortServiceName)
	{
		addDependency(shortServiceName,"org.apache.log4j","1.2.14");
		addDependency(shortServiceName,"org.simpleframework.xml","2.5.3");	
	}
	
	private ServiceInfo()
	{

		addBase("Arduino");
		addDependency("Arduino","gnu.io.rxtx","2.1-7r2");		

		addBase("Arm");		
		addBase("AudioCapture");		
		addBase("AudioFile");
		addDependency("AudioFile","javazoom.jl.player","1.0.1");

		addBase("Chessgame");
		addDependency("Chessgame","org.op.chess","1.0.0");	
				
		addBase("Clock");
		addBase("DifferentialDrive");
		addBase("GoogleSTT");		
		addBase("GUIService");
		addDependency("GUIService","com.mxgraph.jgraph","1.6.1.2");	
		addDependency("GUIService","org.fife.rsyntaxtextarea","1.5.2");	

		addBase("Graphics");
		addBase("IPCamera");		
		
		addBase("JFugue");		
		addDependency("JFugue","org.jfugue.music","4.0.3");	

		addBase("Joystick");		
		addDependency("Joystick","com.centralnexus.joystick","0.7");	
		
		addBase("Jython");		
		addDependency("Jython","org.python.core","2.5.2");	

		addBase("Keyboard");		
		addBase("Motor");		

		addBase("OpenCV");		
		addDependency("OpenCV","com.googlecode.javacv","20111001");	
		addDependency("OpenCV","net.sourceforge.opencv","2.3.1a");	
		addDependency("OpenCV","com.sun.jna","3.2.2");	
		
		addBase("RemoteAdapter");		

		addBase("Roomba");		
		addDependency("Roomba","gnu.io.rxtx","2.1-7r2");
		
		addBase("SensorMonitor");		
		addBase("Servo");		

		addBase("Simbad");		
		addDependency("Simbad","net.sourceforge.simbad","1.4");	
		
		addBase("Speech");		
		addDependency("Speech","com.sun.speech.freetts","1.2");	
		
		/////////////////////CATEGORIES////////////////////
		addCategory  ("Arduino", "micro-controller");

		addCategory  ("AudioCapture", "sound");
		addCategory  ("AudioFile", "sound");
		addCategory  ("Speech", "sound");
		addCategory  ("JFugue", "sound");
		
		addCategory  ("ChumbyBot", "robots");
		addCategory  ("Roomba", "robots");
		addCategory  ("Magabot", "robots");
		
		addCategory  ("RemoteAdapter", "network");
		addCategory  ("HTTPClient", "network");
		addCategory  ("Jibble", "network");
	
		addCategory  ("Jython", "programming");
		addCategory  ("GUIService", "programming");

		addCategory  ("OpenCV", "vision");
		addCategory  ("IPCamera", "vision");
		
		addCategory  ("Motor", "actuators");
		addCategory  ("Servo", "actuators");

		addCategory  ("Simbad", "simulator");
		addCategory  ("PlayerStage", "simulator");
		
		addCategory  ("SendorMonitor", "sensors");

		addCategory  ("Speech", "speech");
		addCategory  ("GoogleSTT", "speech");
		addCategory  ("SpeechRecognition", "speech");
		
		//addCategory  ("Skype", "telerobotics");
		//addCategory  ("GoogleAPI", "telerobotics"); **
		//addCategory  ("GoogleGoggles", "telerobotics"); **
		
		addCategory  ("WiiDAR", "navigation");
		addCategory  ("SLAM", "navigation");
		
		//addCategory  ("ROS", "interoperability");
		//addCategory  ("Processing", "interoperability");
		
		addCategory  ("GeneticProgramming", "AI");
		//addCategory  ("NeuralNetwork", "AI");
		//addCategory  ("FSM", "AI");
		
		
		// addCategory  ("", "misc"); all which aren't defined
		// all
		
	}
	
	
	/**
	 * function to return an array of dependencies for the ServiceFactory
	 * So that Ivy can download, cache, and manage all the appropriate 
	 * dependencies for a Service.  TODO - make this function abstract and
	 * force implementation.
	 * 
	 * @return Array of dependencies to be retrieved from the repo
	 */
	public static ArrayList<Dependency> getDependencies (String fullname)
	{
		return dependencies.get(fullname);
	}
	
	
	/**
	 * static info - share it with a singleton
	 * @return
	 */
	public static  ServiceInfo getInstance()
	{
		if (instance == null)
		{
			instance = new ServiceInfo();
		}
		return instance;
	}
	
	public String[] getShortClassNames()
	{
		ArrayList<String> sorted = new ArrayList<String>();
		
		Iterator<String> it = dependencies.keySet().iterator();
		while (it.hasNext()) {
			String sn = it.next();
			sorted.add(sn.substring(sn.lastIndexOf('.') + 1));
		}
		Collections.sort(sorted);
		return sorted.toArray(new String[sorted.size()]);
		//return dependencies.keySet().toArray(new String[dependencies.keySet().size()]);
	}
	
	public void addDependency (String shortName, String org, String version)
	{
		String fullname = "org.myrobotlab.service." + shortName;
		String module = org.substring(org.lastIndexOf(".")+1);		
		ArrayList <Dependency> list = null;
		if (dependencies.containsKey(fullname))
		{
			list =  dependencies.get(fullname);
		} else {
			list = new ArrayList<Dependency>();
			dependencies.put(fullname, list);
		}
		Dependency d = new Dependency(org, module, version);
		list.add(d);
	}
	
	public void addCategory(String shortName, String category)
	{
		String fullname = "org.myrobotlab.service." + shortName;
		ArrayList<String>list = null;
		if (categories.containsKey(shortName))
		{
			
			list =  categories.get(fullname);
		} else {
			list = new ArrayList<String>();
			categories.put(fullname, list);
		}
		list.add(category);	
	}
	
}
