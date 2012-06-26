package org.myrobotlab.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Code downloaded from:
 * <ul>
 * 	<li>http://www.jroller.com/CoBraLorD/entry/junit_testing_private_fields_and</li>
 * </ul>
 * 
 * @author SwedaKonsult
 *
 */
public class TestHelpers {
	/**
	 * Gets the field value from an instance.  The field we wish to retrieve is
	 * specified by passing the name.  The value will be returned, even if the
	 * field would have private or protected access.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getField( Object instance, String name )
	{
		Class<?> c = instance.getClass();

		// Retrieve the field with the specified name
		Field f = null;
		try {
			f = c.getDeclaredField( name );
		} catch (NoSuchFieldException e1) {
			e1.printStackTrace();
			return null;
		} catch (SecurityException e1) {
			e1.printStackTrace();
			return null;
		}

		// *MAGIC* make sure the field is accessible, even if it
		// would be private or protected
		f.setAccessible( true );

		// Return the value of the field for the instance
		try {
			return (T) f.get( instance );
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Executes a method on an object instance.  The name and parameters of
	 * the method are specified.  The method will be executed and the value
	 * of it returned, even if the method would have private or protected access.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T executeMethod( Object instance, String name, Object... params )
	{
		Class<?> c 	= instance.getClass();

		// Fetch the Class types of all method parameters
		Class<?>[] types = new Class[params.length];

		for ( int i = 0; i < params.length; i++ ) {
			types[i] = params[i].getClass();
		}

		Method m = null;
		try {
			m = c.getDeclaredMethod( name, types );
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		// *MAGIC* make sure the method is accessible
		m.setAccessible( true );

		try {
			return (T) m.invoke( instance, params );
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
