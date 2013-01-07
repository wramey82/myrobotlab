package org.myrobotlab.logging;

/**
 * Appenders to be used with logging.
 * 
 * @author christer
 * 
 */
public enum LogAppender {
	None {
		public String toString() {
			return "none";
		}
	},
	Console {
		public String toString() {
			return "console";
		}
	},
	ConsoleGui {
		public String toString() {
			return "console gui";
		}
	},
	File {
		public String toString() {
			return "file";
		}
	},
	Remote {
		public String toString() {
			return "remote";
		}
	};

	/**
	 * Safely parse the input parameter. Default: None.
	 * 
	 * @param str
	 * @return
	 */
	public static LogAppender tryParse(String str) {
		if (str == null || str.length() < 4) {
			return None;
		}
		if (str.equals(Console.toString())) {
			return Console;
		} else if (str.equals(ConsoleGui.toString())) {
			return ConsoleGui;
		} else if (str.equals(File.toString())) {
			return File;
		} else if (str.equals(Remote.toString())) {
			return Remote;
		} else {
			return None;
		}
	}

	/**
	 * Safely parse the input parameter. Default: None.
	 * 
	 * @param str
	 * @return
	 * @throws IllegalArgumentException
	 *             if there is no match
	 */
	public static LogAppender parse(String str) {
		if (str == null || str.length() < 4) {
			throw new IllegalArgumentException();
		}
		if (str.equals(Console.toString())) {
			return Console;
		}
		if (str.equals(ConsoleGui.toString())) {
			return ConsoleGui;
		}
		if (str.equals(File.toString())) {
			return File;
		}
		if (str.equals(Remote.toString())) {
			return Remote;
		}
		if (str.equals(None.toString())) {
			return None;
		}
		throw new IllegalArgumentException(str);
	}
}
