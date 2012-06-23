/**
 * Cache interface
 */
package org.myrobotlab.cache;

/**
 * Interface for a single cache.
 * Should be retrieved from CacheManager.
 * 
 * @author SwedaKonsult
 *
 */
public interface Cache {
	/**
	 * Get a value.
	 * 
	 * @param name
	 * @return
	 */
	<T> T get(String name);
	
	/**
	 * Cache a value.
	 * 
	 * @param name
	 * @param value
	 */
	<T> void put(String name, Object value);
}
