package org.myrobotlab.control;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.interfaces.GUI;

public class LoggingGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	JTextArea log = new JTextArea(20, 40);
	
	public LoggingGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	public void init() {
		display.setLayout(new BorderLayout());

		log.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(log);
		
		display.add(scrollPane, BorderLayout.CENTER);
	}

	public void log (Message m)
	{
		
		StringBuffer data = null;
		
		if (m.data != null)
		{
			data = new StringBuffer();
			for (int i = 0; i < m.data.length; ++i)
			{
				data.append(m.data[i]);
				if (m.data.length > 1)
				{
					data.append(" ");
				}
			}
		}
		
		log.append(m.sender + "." + m.sendingMethod + " " + data + "\n");
		
		log.setCaretPosition(log.getDocument().getLength());
	}
	
	@Override
	public void attachGUI() {
		sendNotifyRequest("log", "log", Message.class);		
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("log", "log", Message.class);
	}

}
