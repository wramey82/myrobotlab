package org.myrobotlab.framework;

import java.util.ArrayList;
import java.util.TreeMap;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

/**
 * a data class for ServiceInfo
 * 
 */

@Root
public class ServiceData {

	/**
	 * list of relationships from Service to dependency key. Dependency
	 * information is stored in a normalized (TreeMap) list of Service types
	 * Each Service type can have a list of serviceInfo (many to many). The
	 * relationships can be many to many but the actual serviceInfo have to be
	 * normalized
	 */
	@ElementMap(entry = "serviceType", value = "dependsOn", attribute = true, inline = true, required = false)
	public TreeMap<String, ServiceDescriptor> serviceInfo = new TreeMap<String, ServiceDescriptor>();
	/**
	 * master list of serviceInfo
	 */

	@ElementMap(entry = "categories", value = "category", attribute = true, inline = true, required = false)
	public TreeMap<String, CategoryList> categories = new TreeMap<String, CategoryList>();

	// Master source is ONLY ivy resolved xml files ! no need to serialize or
	// deserialize
	@ElementMap(entry = "org", value = "dependency", attribute = true, inline = true, required = false)
	public TreeMap<String, Dependency> thirdPartyLibs = new TreeMap<String, Dependency>();

	public static class CategoryList {
		@ElementList(entry = "include", inline = true)
		public ArrayList<String> services = new ArrayList<String>();

		public int size() {
			return services.size();
		}

		public String get(int x) {
			return services.get(x);
		}
	}
}
