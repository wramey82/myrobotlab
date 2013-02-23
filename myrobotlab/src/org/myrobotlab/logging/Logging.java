package org.myrobotlab.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

import org.slf4j.Logger;


public abstract class Logging {
	
	public final static Logger log = LoggerFactory.getLogger(Logging.class.getCanonicalName());
	// performance timing
	public static long startTimeMilliseconds = 0;
	public static boolean performanceTiming = false;
	public static HashMap<String, Long> timerMap = null;
	
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
	
	/**
	 * 
	 * @param tag
	 */
	static public void logTime(String tag) {
		if (startTimeMilliseconds == 0) {
			startTimeMilliseconds = System.currentTimeMillis();
		}
		if (performanceTiming) {
			log.error(String.format("performance clock :%1$d ms %2$s", System.currentTimeMillis() - startTimeMilliseconds, tag));
		}
	}

	/**
	 * 
	 * @param timerName
	 * @param tag
	 */
	static public void logTime(String timerName, String tag) {
		if (timerMap == null) {
			timerMap = new HashMap<String, Long>();
		}

		if (!timerMap.containsKey(timerName) || "start".equals(tag)) {
			timerMap.put(timerName, System.currentTimeMillis());
		}

		StringBuffer sb = new StringBuffer(40).append("timer ").append(timerName).append(" ").append(System.currentTimeMillis() - timerMap.get(timerName)).append(" ms ")
				.append(tag);

		log.error(sb.toString());
	}


}
