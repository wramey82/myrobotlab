package org.myrobotlab.framework;

import java.io.Serializable;
import java.util.ArrayList;

import org.simpleframework.xml.ElementList;

/**
 * necessary for simplexml to handle HashMap<String, ArrayList<Dependency>>
 *
 */
public class DependencyList implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@ElementList(name="dependencies")
	ArrayList<Dependency> dependencies = new ArrayList<Dependency>(); 
	
	public Dependency get(int index)
	{
		return dependencies.get(index);
	}
	
	public void add(Dependency d)
	{
		dependencies.add(d);
	}
	
	public void clear()
	{
		dependencies.clear();
	}
	
	public int size()
	{
		return dependencies.size();
	}
	
	public ArrayList<Dependency> getDependencies()
	{
		return dependencies;
	}
	
}
