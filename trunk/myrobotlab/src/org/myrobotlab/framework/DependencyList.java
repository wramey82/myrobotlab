package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.simpleframework.xml.ElementList;

/**
 * list of relations from a Service type to a Dependency key
 * the key is used to look up in the masterList - this keeps
 * the data normalized and if one Service fulfills its dependency and 
 * the dependency is shared with another Service type, it is fulfilled there
 * too
 * 
 * The dependency key is the "org" - no version is keyed at the moment.. this
 * would be something to avoid anyway (complexities of cross-versions - jar hell)
 *
 */
public class DependencyList implements Serializable {

	private static final long serialVersionUID = 1L;
	@ElementList(name="list")
	private ArrayList<String> list = new ArrayList<String>(); 
	
	public void add(String org)
	{
		list.add(org);
	}
	
	public int size()
	{
		return list.size();
	}
	
	public String get(int index)
	{
		return list.get(index);
	}
	
}
