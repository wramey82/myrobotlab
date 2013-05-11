package org.myrobotlab.logging;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.framework.Service;

public class LoggingLog4J extends Logging {

	public final static Logger log = Logger.getLogger(Logging.class.getCanonicalName());

	@Override
	public void configure() {
		org.apache.log4j.BasicConfigurator.configure();
	}

	@Override
	public void setLevel(String level) {

		if ("DEBUG".equals(level)) { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
		} else if ("TRACE".equals(level)) { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.TRACE);
		} else if ("WARN".equals(level)) { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
		} else if ("ERROR".equals(level)) { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.ERROR);
		} else if ("FATAL".equals(level)) { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.FATAL);
		} else { // && log4j {
			org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.INFO);
		}
	}
	
	/**
	 * 
	 * @param type
	 */
	public void addAppender(String type) {
		addAppender(type, null, null);
	}

	/**
	 * 
	 * @param type
	 * @param host
	 * @param port
	 */
	public void addAppender(String type, String host, String port) {
		// same format as .configure()
		PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x - %m%n");
		org.apache.log4j.Appender appender = null;

		// TODO the type should be an enumeration so that we can make this a
		// switch statement (unless Python dependencies don't allow for it)
		try {
			if (Appender.CONSOLE.equals(type)) {
				appender = new ConsoleAppender(layout);
				appender.setName(type);
			} else if (Appender.REMOTE.equals(type)) {
				appender = new SocketAppender(host, Integer.parseInt(port));
				appender.setName(type);
			} else if (Appender.FILE.equals(type)) {
				SimpleDateFormat TSFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
				TSFormatter.setCalendar(cal);
				
				appender = new RollingFileAppender(layout, String.format("%s%smyrobotlab.%s.log", System.getProperty("user.dir"), File.separator, TSFormatter.format(new Date())), false);
				appender.setName(type);
				
			} else {
				log.error(String.format("attempting to add unkown type of Appender %1$s", type));
				return;
			}
		} catch (Exception e) {
			System.out.println(Service.stackToString(e));
		}

		if (appender != null) {
			Logger.getRootLogger().addAppender(appender);
		}

		if (type.equals(Appender.NONE)) {
			Logger.getRootLogger().removeAllAppenders();
		}

	}

	/**
	 * 
	 * @param name
	 */
	public void removeAppender(String name) {
		Logger.getRootLogger().removeAppender(name);
	}


	public void removeAllAppenders() {
		Logger.getRootLogger().removeAllAppenders();
	}

	@Override
	public String getLevel() {
		Logger root = Logger.getRootLogger();
		String level = root.getLevel().toString();
		// FIXME - normalize
		// if "something"  return Level.INFO
		return level;
	}

	@Override
	public void addAppender(Object console) {
		Logger.getRootLogger().addAppender((AppenderSkeleton)console);
	}

	@Override
	public void removeAppender(Object console) {
		Logger.getRootLogger().removeAppender((AppenderSkeleton)console);
	}
	
	


}
