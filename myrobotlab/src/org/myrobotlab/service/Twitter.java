package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;


public class Twitter extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Twitter.class.getCanonicalName());
	
	twitter4j.Twitter twitter = null;
	
	public Twitter(String n) {
		super(n, Twitter.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}
	
	public void tweet(String msg)
	{
	    try {
			Status status = twitter.updateStatus(msg);
		} catch (TwitterException e) {
			Logging.logException(e);
		}
	}
	
	public String ck;
	
	public String cs;
	
	public String at;
	
	public String ats;
	
	public void setSecurity(String Ck, String Cs, String At, String Ats){
		
		ck = Ck;
		cs = Cs;
		at = At;
		ats = Ats;
		
	}
	
	
	public void configure()
	{
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(ck)
		  .setOAuthConsumerSecret(cs)
		  .setOAuthAccessToken(at)
		  .setOAuthAccessTokenSecret(ats);
		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Twitter twitter = new Twitter("twitter");
		twitter.startService();
		twitter.setSecurity("yourConsumerKey","yourConsumerSecret", "yourAccessToken", "yourAccessTokenSecret");
		twitter.configure();
		twitter.tweet("Ciao from MyRobotLab");
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
