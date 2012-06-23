/**
 * Cache class that can be used by any code.
 */
package org.myrobotlab.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of the Cache interface that stores information in local memory.
 * 
 * @author SwedaKonsult
 *
 */
public class LocalCache implements Cache {
	/**
	 * Default concurrency level - grabbed from ConcurrentHashMap.
	 */
	public static final int DEFAULT_CONCURRENCY_LEVEL = 16;
	/**
	 * Default load factor - grabbed from ConcurrentHashMap.
	 */
	public static final float DEFAULT_LOAD_FACTOR = 0.75f;
	/**
	 * The cache of this instance.
	 */
	private final ConcurrentMap<String, Object> cache;
	
	/**
	 * Constructor.
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements.
     * @param loadFactor  the load factor threshold, used to control resizing.
     * Resizing may be performed when the average number of elements per
     * bin exceeds this threshold.
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation performs internal sizing
     * to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * non-positive.
	 */
	public LocalCache(int initialSize, float loadFactor, int concurrencyLevel) {
		cache = new ConcurrentHashMap<String, Object>(initialSize, loadFactor, concurrencyLevel);
	}

    /**
     * Constructor. Default load factor (0.75) and concurrencyLevel (16).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative.
     */
    public LocalCache(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
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
}
