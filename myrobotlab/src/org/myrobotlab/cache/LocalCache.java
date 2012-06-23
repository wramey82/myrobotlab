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
public class LocalCache extends BaseCache {
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

	@Override
	protected void addToCache(String name, Object value) {
		cache.put(name, value);
	}

	@Override
	protected boolean contains(String name) {
		return cache.containsKey(name);
	}

	@Override
	protected Object getFromCache(String name) {
		return cache.get(name);
	}

	@Override
	protected void removeFromCache(String name) {
		cache.remove(name);
	}
}
