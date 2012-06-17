package org.myrobotlab.logging;

/**
 * The log levels available.
 * 
 * @author christer
 *
 */
public enum LogLevel { 
	Debug {
		public String toString() {
			return "DEBUG";
		}
	},
	Info {
		public String toString() {
			return "INFO";
		}
	},
	Warn {
		public String toString() {
			return "WARN";
		}
	},
	Error {
		public String toString() {
			return "ERROR";
		}
	},
	Fatal {
		public String toString() {
			return "FATAL";
		}
	};
	
	/**
	 * Safely parse the input parameter. Default: Info.
	 * 
	 * @param str
	 * @return
	 */
	public static LogLevel tryParse(String str) {
		if (str == null || str.length() < 4) {
			return LogLevel.Info;
		}
		StringBuilder formattedString = new StringBuilder(str.length())
			.append(Character.toUpperCase(str.charAt(0)))
			.append(str.substring(1).toLowerCase());
		try {
			return LogLevel.valueOf(formattedString.toString());
		} catch (IllegalArgumentException ex) {
			return LogLevel.Info;
		}
	}
	
	/**
	 * Parse the input parameter.
	 * 
	 * @param str
	 * @return
	 * @throws IllegalArgumentException if there is no match
	 */
	public static LogLevel parse(String str) {
		if (str == null || str.length() < 4) {
			throw new IllegalArgumentException(str);
		}
		// change the string to pascal casing
		StringBuilder formattedString = new StringBuilder(str.length())
			.append(Character.toUpperCase(str.charAt(0)))
			.append(str.substring(1).toLowerCase());
		return LogLevel.valueOf(formattedString.toString());
	}
}
