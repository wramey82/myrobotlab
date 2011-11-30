package org.myrobotlab.control;

import java.awt.Component;
import java.util.Properties;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;

// http://www.javaworld.com/javaworld/jw-12-2004/jw-1220-toolbox.html?page=5
public class Console extends AppenderSkeleton {
	
	static public JTextArea textArea = null;
	
	public Console () {
	}
	
	static public JTextArea getRootConsole()
	{
		// This code attaches the appender to the text area
		JTextArea text = new JTextArea();
		Console.setTextArea(text);
		//Logger.getRootLogger().addAppender(appender);
		
		// Normally configuration would be done via a log4j.properties
		// file found on the class path, but here we will explicitly set
		// values to keep it simple.
		//
		// Great introduction to Log4J at http://logging.apache.org/log4j/docs/manual.html
		//
		// Could also have used straight code like: app.logger.setLevel(Level.INFO);
		Properties logProperties = new Properties();
		logProperties.put("log4j.rootLogger", "INFO, TEXTAREA");
/*		logProperties.put("log4j.rootLogger", "INFO, CONSOLE, TEXTAREA");
		logProperties.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender"); // A standard console appender
		logProperties.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout"); //See: http://logging.apache.org/log4j/docs/api/org/apache/log4j/PatternLayout.html
		logProperties.put("log4j.appender.CONSOLE.layout.ConversionPattern", "%d{HH:mm:ss} [%12.12t] %5.5p %40.40c: %m%n");
*/
		logProperties.put("log4j.appender.TEXTAREA", "org.myrobotlab.control.Console");  // Our custom appender
		logProperties.put("log4j.appender.TEXTAREA.layout", "org.apache.log4j.PatternLayout"); //See: http://logging.apache.org/log4j/docs/api/org/apache/log4j/PatternLayout.html
		logProperties.put("log4j.appender.TEXTAREA.layout.ConversionPattern", "%d{HH:mm:ss} %5.5p %40.40c: %m%n");
		
		PropertyConfigurator.configure(logProperties);
		return text;
	}
	
	/** Set the target JTextArea for the logging information to appear. */
	static public void setTextArea(JTextArea textArea) {
		Console.textArea = textArea;
	}
	/**
	 * Format and then append the loggingEvent to the stored
	 * JTextArea.
	 */
	public void append(LoggingEvent loggingEvent) {
		final String message = this.layout.format(loggingEvent);

		// Append formatted message to textarea using the Swing Thread.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.append(message);
			}
		});
	}
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean requiresLayout() {
		// TODO Auto-generated method stub
		return true;
	}


	public Component getTextArea() {
		return textArea;
	}
}