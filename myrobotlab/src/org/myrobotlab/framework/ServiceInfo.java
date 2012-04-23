package org.myrobotlab.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ivy.Main;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.cli.CommandLineParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.ServiceData.CategoryList;
import org.myrobotlab.net.HTTPRequest;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

// TODO - command line refresh - repo management & configuration options "latest" etc

public class ServiceInfo implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String ivyFileName = "ivychain.xml";
	public final static Logger LOG = Logger.getLogger(ServiceInfo.class.toString());
	
	private ServiceData serviceData = new ServiceData();
	private ServiceData serviceDataFromRepo = new ServiceData();
	private ArrayList<String> errors = new ArrayList<String>(); 
	
	private static ServiceInfo instance = null;	
		
	private ServiceInfo()
	{
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
	
	public void clearErrors()
	{
		errors.clear();
	}
	
	public boolean hasErrors()
	{
		return errors.size() > 0;
	}
	
	public ArrayList<String> getErrors()
	{
		return errors;
	}
	
	public Set<String> getKeySet()
	{
		return serviceData.serviceInfo.keySet();
	}
		
	/**
	 * default save saves the memory serviceData to .myrobotlab/serviceData.xml
	 */
	public boolean save(ServiceData data, String filename )
	{
		Serializer serializer = new Persister();

		try {
			File cfgdir = new File(Service.getCFGDir());
			if (!cfgdir.exists())
			{
				cfgdir.mkdirs();
			}
			File cfg = new File(Service.getCFGDir() + File.separator + filename);
			serializer.write(data, cfg);
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}
	
	/**
	 * this method looks in the .ivy cache directory for resolved dependencies
	 * and builds a master map of third party libs which are on the local system
	 */
	public boolean getLocalResolvedDependencies()
	{
		boolean ret = false;
		
		// clear local resolved serviceInfo
		serviceData.thirdPartyLibs.clear();
		
		// load .ivy cache		
		try {
									
			List<File> files = FindFile.find(".ivy", "resolved.*\\.xml$");
			
			for (File file : files) {
				String org = file.getName();
				org = org.substring(org.indexOf("-")+1);
				org = org.substring(0, org.indexOf("-"));
				
				String module = org.substring(org.lastIndexOf(".")+1);
				
				String contents = FileIO.fileToString(file.getPath());
				
				// hack - more cheesy parsing
				String version = contents.substring(contents.indexOf("rev=\"")+5); 
				version = version.substring(0, version.indexOf("\""));
				LOG.info("adding dependency " + org + " " + version + " to local thirdPartyLib");
				Dependency d = new Dependency(org, module, version, false);
				d.released = true;
				d.resolved = true;
				serviceData.thirdPartyLibs.put(org, d);
			}			
			
		} catch (FileNotFoundException e) {
			Service.logException(e);
			return false;
		}

		return ret;
	}
	
	/**
	 * default load, loads both the serviceData.xml file and the local
	 * .ivy cache into memory.
	 */
	public boolean getLocalServiceData()
	{
		boolean ret = loadXML(serviceData, "serviceData.xml");;
		ret &= getLocalResolvedDependencies();
		return ret;
	}
	
	
	public boolean loadXML(Object o, String inCfgFileName)
	{
		String filename = null;
		if (inCfgFileName == null)
		{
			filename = Service.getCFGDir() + File.separator + inCfgFileName + ".xml";
		} else {
			filename = Service.getCFGDir() + File.separator + inCfgFileName;
		}
		if (o == null)
		{
			o = this;
		}
		Serializer serializer = new Persister();
		try {
			File cfg = new File(filename);
			if (cfg.exists()){
				serializer.read(o, cfg);
			} else {
				LOG.warn("cfg file "   + filename + " does not exist");
			}
		} catch (Exception e) {
			Service.logException(e);
			return false;
		}
		return true;
	}

	
	public boolean hasUnfulfilledDependencies(String fullServiceName)
	{
		boolean ret = false;
		LOG.debug("inspecting " + fullServiceName + " for unfulfilled dependencies");
		
		// no serviceInfo
		if (!serviceData.serviceInfo.containsKey(fullServiceName))
		{
			LOG.error("need full service name ... got " + fullServiceName);
			return false;
		}
		
		ServiceDescriptor d = serviceData.serviceInfo.get(fullServiceName);
		for (int i = 0; i < d.size(); ++i)
		{
			if (serviceData.thirdPartyLibs.containsKey(d.get(i)))
			{
				Dependency dep = serviceData.thirdPartyLibs.get(d.get(i));
				if (!dep.resolved)
				{
					LOG.debug("hasUnfulfilledDependencies exit true");
					return true;
				}
			} else {
				LOG.debug(d.get(i) + " can not be found in current thirdPartyLibs");
				LOG.debug("hasUnfulfilledDependencies exit true");
				return true;
			}			
		}
		LOG.debug("hasUnfulfilledDependencies exit " + ret);		
		return ret;
	}
		
	public String[] getShortClassNames()
	{
		return getShortClassNames(null);
	}	
	
	public String[] getShortClassNames(String filter)
	{
		ArrayList<String> sorted = new ArrayList<String>();
		
		Iterator<String> it = serviceData.serviceInfo.keySet().iterator();
		while (it.hasNext()) {
			String sn = it.next();
			if (filter != null)
			{
				ServiceData.CategoryList cats = serviceData.categories.get(sn);
				if (cats != null) {
					for (int i = 0; i < cats.size(); ++i)
					{
						if (filter.equals(cats.get(i)))
						{
							sorted.add(sn.substring(sn.lastIndexOf('.') + 1));
						}
					}
				}
			} else {
				sorted.add(sn.substring(sn.lastIndexOf('.') + 1));
			}
		}
		Collections.sort(sorted);
		return sorted.toArray(new String[sorted.size()]);
		//return serviceInfo.keySet().toArray(new String[serviceInfo.keySet().size()]);
	}

	/*
	public void addDependency (String shortName, String org, String version)
	{
		addDependency(shortName, org, version, true);
	}
	
	public void addDependency (String shortName, String org, String version, boolean released)
	{
		String fullname = "org.myrobotlab.service." + shortName;
		String module = org.substring(org.lastIndexOf(".")+1);		

		ServiceDescriptor list = null;
		if (serviceData.serviceInfo.containsKey(fullname))
		{
			list =  serviceData.serviceInfo.get(fullname);
		} else {
			list = new ServiceDescriptor();
			serviceData.serviceInfo.put(fullname, list);
		}
		
		// check to see if it is in the master list
		// if not add it
		Dependency d = null;
		if (serviceData.thirdPartyLibs.containsKey(org))
		{
			d = serviceData.thirdPartyLibs.get(org);
		} else {
			d = new Dependency(org, module, version, released);
			serviceData.thirdPartyLibs.put(org, d);
		}
		list.addDependency(org);		
	}
	*/
	
	public String[] getUniqueCategoryNames ()
	{
		ArrayList<String> sorted = new ArrayList<String>();
		HashMap<String,String> normal = new HashMap<String,String>();
		Iterator<String> it = serviceData.categories.keySet().iterator();
		while (it.hasNext()) {
			String sn = it.next();
			ServiceData.CategoryList al = serviceData.categories.get(sn);
			for (int i = 0; i < al.size(); ++i)
			{
				normal.put(al.get(i), null);
			}
		}

		it = normal.keySet().iterator();
		while (it.hasNext()) {
			String sn = it.next();
			sorted.add(sn);
		}		
		
		Collections.sort(sorted);
		return sorted.toArray(new String[sorted.size()]);		
	}

	public boolean getRepoServiceData(String localFileName) {
		try {
			HTTPRequest http = new HTTPRequest("http://myrobotlab.googlecode.com/svn/trunk/myrobotlab/thirdParty/repo/serviceData.xml");
			String s = http.getString();
			if (s != null && localFileName != null){
				FileIO.stringToFile(Service.getCFGDir() + File.separator + localFileName, s);
				return true;
			}
		} catch (Exception e) {
			Service.logException(e);
			errors.add(e.getMessage());
		}
		return false;
	}

	// TODO - implement and remove from Runtime
	public void determineInstalledServices()
	{
		
	}
	
	
	public void addCategory(String shortName, String category)
	{
		// TODO - bury all this in ServiceData
		if (serviceData.categories == null)
		{
			serviceData.categories = new TreeMap<String, CategoryList>();
		}
		
		String fullname = "org.myrobotlab.service." + shortName;
		//ArrayList<String>list = null;
		ServiceData.CategoryList list = null;
		if (serviceData.categories.containsKey(shortName))
		{
			
			list =  serviceData.categories.get(fullname);
		} else {
			list = new ServiceData.CategoryList();
			serviceData.categories.put(fullname, list);
		}
		list.services.add(category);	
	}

	public boolean isServiceInstalled(String name)
	{
		//if ()
		return false;
	}
	
	
	/** 
	 * gets thirdPartyLibs of a Service using Ivy
	 * interfaces with Ivy using its command parameters
	 * @param fullTypeName
	 */
	// TODO - interface to Ivy2 needs to be put into ServiceInfo
	// resolve here "means" retrieve
	public boolean resolve(String fullTypeName) 
	{
		LOG.debug("getDependencies " + fullTypeName);
		boolean ret = true;

		File ivysettings = new File(ivyFileName);
		if (!ivysettings.exists())
		{
			LOG.warn(ivyFileName +  " does not exits - will not try to resolve dependencies");
			return false;
		}
		
		try {

			ArrayList<String> d = getRequiredDependencies(fullTypeName);

			if (d != null)
			{
				LOG.info(fullTypeName + " found " + d.size() + " needed dependencies");
				for (int i=0; i < d.size(); ++i)
				{				
					// java -jar libraries\jar\ivy.jar -cache .ivy -settings ivychain.xml -dependency org.myrobotlab myrobotlab "latest.integration"
					// java -jar libraries\jar\ivy.jar -cache .ivy -retrieve libraries/[type]/[artifact].[ext] -settings ivychain.xml -dependency org.myrobotlab myrobotlab "latest.integration"
										
					String dep = d.get(i);					
					
					ArrayList<String> cmd = new ArrayList<String>();
					
					cmd.add("-cache");
					cmd.add(".ivy");
	
					cmd.add("-retrieve");
					cmd.add("libraries/[type]/[artifact].[ext]");
	
					cmd.add("-settings");
					//cmd.add("ivysettings.xml");
					cmd.add(ivyFileName);
	
					//cmd.add("-cachepath");
					//cmd.add("cachefile.txt");					
					
					cmd.add("-dependency");
					cmd.add(dep); // org
					String module = dep.substring(dep.lastIndexOf(".")+1);
					cmd.add(module); 		// module		
					//cmd.add(dep.version); 	// version
					cmd.add("latest.integration");
					
					cmd.add("-confs");
					String confs = "runtime,"+Platform.getArch()+"."+
							Platform.getBitness()+"." + 
							Platform.getOS();
					cmd.add(confs);
					
					// show cmd params
					StringBuilder sb = new StringBuilder();
					for (int k = 0; k < cmd.size(); ++k)
					{
						sb.append(cmd.get(k));
						sb.append(" ");
					}
					
					// FIXME - generate Ivy xml file
					//dependencyCommandLine += sb.toString() + "\n"; 
					LOG.info(sb.toString());
					//debug = true;
					//if (debug) continue;

					
					CommandLineParser parser = Main.getParser();
					
					try {
						Ivy2.run(parser, cmd.toArray(new String[cmd.size()]));
						ResolveReport report = Ivy2.getReport();
			            if (report.hasError()) {
			            	ret = false;
			            	//dep.resolved = false; - not needed since it will not be in cache
			                // System.exit(1);
			            	LOG.error("Ivy resolve error");
			            	// invoke Dependency Error - 
			            	List<String> l = report.getAllProblemMessages();
			            	for (int j = 0; j < l.size(); ++j)
			            	{
			            		/* TODO - interface for generating events ???
				            	
				    			if (INSTANCE != null)
				    			{
				    				INSTANCE.invoke("failedDependency", l.get(j));
				    			}
				    			*/
			            		LOG.error(l.get(j));
			            	}
			            } else {
			            	//dep.resolved = true;
			    			//save();
			            }
					} catch (Exception e)
					{
						Service.logException(e);
					}
					
					// local config - 
					
					// if the Service is downloaded we have to dynamically 
					// load the classes - if we are not going to restart
					// http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
				}
			} else {
				if (d == null)
				{
					LOG.info(fullTypeName + " returned no dependencies");
				}
			}
		} catch (Exception e) {
			Service.logException(e);
			ret = false;
		}		
		
		return ret;
	}

	/**
	 * function to return an array of serviceInfo for the Runtime
	 * So that Ivy can download, cache, and manage all the appropriate 
	 * serviceInfo for a Service.  TODO - make this function abstract and
	 * force implementation.
	 * 
	 * @return Array of serviceInfo to be retrieved from the repo
	 */
	public ArrayList<String> getRequiredDependencies (String fullname)
	{
		if (serviceData.serviceInfo.containsKey(fullname))
		{
			ServiceDescriptor d = serviceData.serviceInfo.get(fullname);
			ArrayList<String> ret = new ArrayList<String>(); 
			for (int i = 0; i < d.size(); ++i)
			{
				String org = d.get(i);
				if (serviceData.thirdPartyLibs.containsKey(org))
				{
					LOG.info(org + " already in cache - skipping");
				} else {
					LOG.error(org + " required - will need to resolve");
					ret.add(org);
				}
			}
			
			return ret;
		}
		
		return null;
	}

	// FIXME - need update(fullTypeName); !!!
	public boolean update() {

		// load up the serviceData.xml and .ivy cache
		// NOTE - it is the responsibility of some other system
		// to call getRepoServiceData - if a new service & categories
		// definition is wanted
		getLocalServiceData();

		// ask for resolution without retrieving
		Iterator<String> it = getKeySet().iterator();
		while (it.hasNext()) {
			String s = it.next();
			resolve(s);
		}

		// examine local cache
		
		// display report
		// accept changes in default upgrade from user
		// FIXME - display report !
		
		// retrieve updates
		it = getKeySet().iterator();
		while (it.hasNext()) {
			String s = it.next();
			resolve(s);
		}
		
		// TODO - return list object - for event processing on caller
		// TODO - re-check local after processing Ivy - so new Services can be
		// loaded or
		// after reboot -
		return false;
	}

	public Dependency getRepoLatestDependencies(String org) {
		
		String module = org.substring(org.lastIndexOf(".")+1);
		
		try {
			HTTPRequest http = new HTTPRequest("http://myrobotlab.googlecode.com/svn/trunk/myrobotlab/thirdParty/repo/" + org + "/" + module+ "/");
			String s = http.getString();
			if (s != null){
				//---- begin fragile & ugly parsing -----
				// reverse pos from bottom to find the end of the list of directories
				// to start the pos
				String latestVersion = s.substring(s.lastIndexOf("<li><a href=\"") + 13);
				latestVersion = latestVersion.substring(0, latestVersion.indexOf("/\">"));
				return new Dependency(org, module, latestVersion, true);
				//---- end fragile & ugly parsing -----
			}
		} catch (Exception e) {
			Service.logException(e);
			errors.add(e.getMessage());
		}

		return null;
		
	}

	public boolean getRepoData()
	{
		// TODO - populate errors - make event
		// clear errors ? this is the beginning of a high level method
		
		// first get repo's serviceData.xml (do not cache it !)
		String repoFileName = "serviceData.repo.xml";
		
		// iterate through it and get all latest dependencies
		boolean ret = getRepoServiceData(repoFileName);
		ret &= loadXML(serviceDataFromRepo, repoFileName);
		
		// iterate through services's dependencies
		Iterator<String> it = serviceDataFromRepo.serviceInfo.keySet().iterator();
		while (it.hasNext()) {
			String sn = it.next();
			ServiceDescriptor sd = serviceDataFromRepo.serviceInfo.get(sn);
			
			// iterate through dependencies - adding to thirdparty libs
			for (int i = 0; i < sd.size(); ++i)
			{
				String dependencyRef = sd.get(i);
				if (!serviceDataFromRepo.thirdPartyLibs.containsKey(dependencyRef))
				{
					Dependency d = getRepoLatestDependencies(dependencyRef);
					serviceDataFromRepo.thirdPartyLibs.put(d.organisation, d);
				}
				
			}
		}
		
		save(serviceDataFromRepo, "serviceData.repo.processed.xml");
		
		return ret;
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		boolean update = true;
		ServiceInfo info = ServiceInfo.getInstance();

		// set defaults [update all | update none] depending on context		
		
		// get local data 
		info.getLocalServiceData();
		
		info.save(info.serviceData, "serviceData.processed.xml");
		
		// get remote data
		info.getRepoData();
		
		
		// generate update report / dialog
		
		// get user input (or accept defaults [update all | update none])
		
		// perform actions
		
		LOG.info(info.getRepoLatestDependencies("org.myrobotlab"));
		LOG.info(info.getRepoLatestDependencies("org.apache.log4j"));
		LOG.info(info.getRepoLatestDependencies("edu.cmu.sphinx"));
		LOG.info(info.getRepoLatestDependencies("org.apache.ivy"));
		
		
		try {
			java.lang.Runtime.getRuntime().exec("cmd /c start myrobotlab.bat");
			java.lang.Runtime.getRuntime().exec("myrobotlab.sh");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		// load the possibly recent serviceData
		info.getLocalServiceData(); 
		
		// add the local ivy cache - TODO - rename to thirdPartyLibs
		info.getLocalResolvedDependencies();

		// TODO - verify all keys !
		//info.save();
	}


}
