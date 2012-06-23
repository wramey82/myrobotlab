/**
 * Cache class that can be used by any code.
 */
package org.myrobotlab.cache;

import java.util.HashMap;

/**
 * Singleton for caching any data.
 * 
 * @author SwedaKonsult
 *
 */
public class Cache {
	// singleton
	private final static transient Cache me = new Cache();
	
	// the cache
	private final HashMap<String, Object> cache;
	
	private Cache() {
		cache = new HashMap<String, Object>();
	}
	
	/**
	 * Get a value from the cache.
	 * 
	 * @param name the name of the value to retrieve
	 * @return null if the name does not exist or if the type could not be cast to T
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		if (name == null) {
			return null;
		}
		if (!cache.containsKey(name)) {
			return null;
		}
		Object value = cache.get(name);
		try {
			return (T) value;
		} catch (ClassCastException e) {}
		return null;
	}
	
	/**
	 * Add a value to the cache.
	 * 
	 * @param name
	 * @param value
	 */
	@SuppressWarnings("null")
	public <T> void put(String name, Object value) {
		T type = null;
		cache.put(type.getClass().getName() + name, value);
	}
	
	/**
	 * Get a handle to this singleton.
	 * 
	 * @return
	 */
	public static Cache getInstance() {
		return me;
	}
}
