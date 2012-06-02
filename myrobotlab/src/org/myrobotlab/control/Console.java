package org.myrobotlab.control;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.myrobotlab.framework.Service;

// http://www.javaworld.com/javaworld/jw-12-2004/jw-1220-toolbox.html?page=5
public class Console extends AppenderSkeleton {
	
	public JTextArea textArea = null;
	public JScrollPane scrollPane = null;
	public boolean logging = false;
	
	public Console () { // TODO boolean JFrame or component 
		textArea = new JTextArea();
		scrollPane = new JScrollPane(textArea);
		
		PatternLayout layout = new PatternLayout("%-4r [%t] %-5p %c %x - %m%n");
		setLayout(layout);
		setName(Service.LOGGING_APPENDER_CONSOLE_GUI);
		Logger.getRootLogger().addAppender(this);

	}
	
	/**
	 * Format and then append the loggingEvent to the stored
	 * JTextArea.
	 */
	public void append(LoggingEvent loggingEvent) {
		if (logging)
		{
			final String message = this.layout.format(loggingEvent);
	
			// Append formatted message to textarea using the Swing Thread.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
						textArea.append(message);
				}
			});
		}
	}
	@Override
	public void close() {
		Logger.getRootLogger().removeAppender(this);		
	}
	@Override
	public boolean requiresLayout() {
		return true;
	}

	public JScrollPane getScrollPane()
	{
		return scrollPane;
	}
	public Component getTextArea() {
		return textArea;
	}
}