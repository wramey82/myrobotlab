/**
 * 
 */
package org.myrobotlab.cache;

import java.util.HashSet;

/**
 * @author SwedaKonsult
 *
 */
public abstract class BaseCache implements Cache {

	/**
	 * Allow for checking if a boxed primitive is being used.
	 */
	private final static HashSet<Class<?>> primitiveTypes = new HashSet<Class<?>>();
	
	static {
	    primitiveTypes.add(Boolean.class);
	    primitiveTypes.add(Character.class);
	    primitiveTypes.add(Byte.class);
	    primitiveTypes.add(Short.class);
	    primitiveTypes.add(Integer.class);
	    primitiveTypes.add(Long.class);
	    primitiveTypes.add(Float.class);
	    primitiveTypes.add(Double.class);
	}

	/**
	 * Internal method for BaseCache to actually add items to the implementing cache.
	 * 
	 * @param name
	 * @param value
	 */
	protected abstract void addToCache(String name, Object value);
	/**
	 * Internal method for BaseCache to actually check if the name exists in the implementing cache.
	 * @param name
	 * @return
	 */
	protected abstract boolean contains(String name);
	/**
	 * Internal method for BaseCache to actually retrieve items from the implementing cache.
	 * @param name
	 */
	protected abstract Object getFromCache(String name);
	/**
	 * Internal method for BaseCache to actually remove items from the implementing cache.
	 * @param name
	 */
	protected abstract void removeFromCache(String name);

	/**
	 * Expire an item in the cache.
	 * 
	 * @param name
	 */
	public void expire(String name) {
		if (name == null || name.isEmpty() || !contains(name)) {
			return;
		}
		removeFromCache(name);
	}
	
	/**
	 * Get a value from the cache.
	 * 
	 * @param name the name of the value to retrieve
	 * @return null if the name does not exist or if the type could not be cast to T
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String name, Class<? extends T> cls) {
		if (name == null || !contains(name)) {
			if (!primitiveTypes.contains(cls)) {
				return null;
			}
			// else return whichever primitive they're asking for
			if (cls.isAssignableFrom(Integer.class)) {
				return (T) new Integer(0);
			}
			if (cls.isAssignableFrom(Byte.class)) {
				return (T) new Byte(Byte.MIN_VALUE);
			}
			if (cls.isAssignableFrom(Short.class)) {
				return (T) new Short(Short.MIN_VALUE);
			}
			if (cls.isAssignableFrom(Double.class)) {
				return (T) new Double(0d);
			}
			if (cls.isAssignableFrom(Float.class)) {
				return (T) new Float(0f);
			}
			if (cls.isAssignableFrom(Long.class)) {
				return (T) new Long(0l);
			}
			if (cls.isAssignableFrom(Boolean.class)) {
				return (T) Boolean.FALSE;
			}
			return (T) new Character('\u0000');
		}
		Object value = getFromCache(name);

		if (value != null && cls.isInstance(value)) {
			return (T) value;
		}
		if (!primitiveTypes.contains(cls)) {
			return null;
		}
		// else return whichever primitive they're asking for
		if (cls.isAssignableFrom(Integer.class)) {
			return (T) new Integer(0);
		}
		if (cls.isAssignableFrom(Byte.class)) {
			return (T) new Byte(Byte.MIN_VALUE);
		}
		if (cls.isAssignableFrom(Short.class)) {
			return (T) new Short(Short.MIN_VALUE);
		}
		if (cls.isAssignableFrom(Double.class)) {
			return (T) new Double(0d);
		}
		if (cls.isAssignableFrom(Float.class)) {
			return (T) new Float(0f);
		}
		if (cls.isAssignableFrom(Long.class)) {
			return (T) new Long(0l);
		}
		if (cls.isAssignableFrom(Boolean.class)) {
			return (T) Boolean.FALSE;
		}
		return (T) new Character('\u0000');
	}
	
	/**
	 * Add a value to the cache.
	 * 
	 * @param name cannot be null or empty
	 * @param value
	 */
	public void put(String name, Object value) {
		if (name == null || name.isEmpty()) {
			return;
		}
		addToCache(name, value);
	}
}
