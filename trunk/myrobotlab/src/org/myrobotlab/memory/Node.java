package org.myrobotlab.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author GroG
 * a "very" generalized memory node - potentially used to grow associations with other nodes
 * uses the concept of attributes and free-form associations
 *
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;

	private HashMap<String,Object> data = new HashMap<String,Object>();
	
	public Node() {
	}
	
	public Object get(String key)
	{
		return data.get(key);
	}
	
	public void addToSet(String key, Object item)
	{
		ArrayList<Object> set;
		if (!data.containsKey(key))
		{
			set = new ArrayList<Object>();
		} else {
			set = (ArrayList<Object>)data.get(key);
		}
		
		set.add(item);
	}
	
	public ArrayList<Object> getSet(String key)
	{
		return (ArrayList<Object>)data.get(key);
	}
	
	public Object put(String key, Object value)
	{
		return data.put(key, value);
	}
	
	public int size()
	{
		return data.size();
	}
}
