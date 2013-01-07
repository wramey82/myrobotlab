/**
 * Wrapper class that allows us to stay neutral when it comes to selecting
 * a logging facility.
 */
package org.myrobotlab.logging;

/**
 * Wrapper for any logging system we want to use. Initial implementation is
 * simply a wrapper for log4j.
 * 
 * @author SwedaKonsult
 * 
 */
public class Logger {
	/**
	 * The logger we're wrapping.
	 */
	private final org.apache.log4j.Logger logger;

	/**
	 * Constructor.
	 * 
	 * @param name
	 */
	public Logger(String name) {
		logger = org.apache.log4j.Logger.getLogger(name);
	}

	/**
	 * DEBUG log.
	 * 
	 * @param message
	 */
	public void debug(Object message) {
		logger.debug(message);
	}

	/**
	 * ERROR log.
	 * 
	 * @param message
	 */
	public void error(Object message) {
		logger.error(message);
	}

	/**
	 * ERROR log with Exception.
	 * 
	 * @param message
	 * @param t
	 */
	public void error(Object message, Throwable t) {
		logger.error(message, t);
	}

	/**
	 * Get a logger handle.
	 * 
	 * @param name
	 * @return
	 */
	public static Logger getLogger(String name) {
		return new Logger(name);
	}

	/**
	 * Get a logger handle.
	 * 
	 * @param name
	 * @return
	 */
	public static Logger getLogger(Class<?> name) {
		return new Logger(name.getName());
	}

	/**
	 * INFO log.
	 * 
	 * @param message
	 */
	public void info(Object message) {
		logger.info(message);
	}

	/**
	 * WARN log.
	 * 
	 * @param message
	 */
	public void warn(Object message) {
		logger.warn(message);
	}

	/**
	 * WARN log with Exception.
	 * 
	 * @param message
	 * @param t
	 */
	public void warn(Object message, Throwable t) {
		logger.warn(message, t);
	}

}
