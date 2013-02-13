package org.myrobotlab.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;


public abstract class Logging {
	
	public final static Logger log = LoggerFactory.getLogger(Logging.class.getCanonicalName());

	public abstract void configure(); // a basic configuration
	public abstract void setLevel(String level);
	

	public abstract void addAppender(Object type);
	public abstract void addAppender(String type);
	public abstract void addAppender(String type, String host, String port);
	
	public abstract void removeAppender(String name);

	public abstract void removeAllAppenders();
	public abstract String getLevel();
	
	public final static void logException(final Throwable e) {
		log.error(stackToString(e));
	}

	
	public final static String stackToString(final Throwable e) {
		StringWriter sw;
		try {
			sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
		} catch (Exception e2) {
			return "bad stackToString";
		}
		return "------\r\n" + sw.toString() + "------\r\n";
	}
	public abstract void removeAppender(Object console);

}
