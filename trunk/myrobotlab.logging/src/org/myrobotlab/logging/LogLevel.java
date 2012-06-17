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
	Warning {
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
	}
}
