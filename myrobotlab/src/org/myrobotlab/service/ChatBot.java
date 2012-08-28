package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;

/**
 * @author GroG
 * 
 * References :
 * 	http://www.codeproject.com/Articles/36106/Chatbot-Tutorial
 *  http://cleverbot.com/
 *  http://courses.ischool.berkeley.edu/i256/f06/projects/bonniejc.pdf
 *  http://www.infradrive.com/downloads/articles/Article1.pdf
 *  http://www.chatterbotcollection.com/
 *
 */
public class ChatBot extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(ChatBot.class.getCanonicalName());

	public ChatBot(String n) {
		super(n, ChatBot.class.getCanonicalName());
	}
	
	@Override
	public void loadDefaultConfiguration() {
		
	}
	
	@Override
	public String getToolTip() {
		return "used as a general template";
	}
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		ChatBot template = new ChatBot("chatbot");
		template.startService();
		/*
		GUIService gui = new GUIService("gui");
		gui.startService();
		gui.display();
		*/
	}


}
