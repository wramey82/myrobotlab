package org.myrobotlab.control;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.myrobotlab.framework.Message;
import org.myrobotlab.service.GUIService;

public class LoggingGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	JTextArea log = new JTextArea(20, 40);
	
	public LoggingGUI(String name, GUIService myService) {
		super(name, myService);
		
		gc.gridx = 0;
		gc.gridy = 0;

		log.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(log);
		
		display.add(scrollPane, gc);
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
		sendNotifyRequest("log", "log", Integer.class);		
	}

	@Override
	public void detachGUI() {
		removeNotifyRequest("log", "log", Message.class);
	}

}
