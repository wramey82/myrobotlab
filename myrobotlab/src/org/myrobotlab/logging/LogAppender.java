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
	}
}
