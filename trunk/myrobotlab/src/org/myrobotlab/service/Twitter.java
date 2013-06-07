package org.myrobotlab.service;

import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;


public class Twitter extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Twitter.class.getCanonicalName());
	
	private String consumerKey;
	private String consumerSecret;
	private String accessToken;
	private String accessTokenSecret;
	
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
	
	
	public void setSecurity(String consumerKey, String consumerSecret, String accessToken, String accessTokenSecret){
		
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.accessToken = accessToken;
		this.accessTokenSecret = accessTokenSecret;
		
	}
	
	public void uploadImage(SerializableImage image,String message){
		try{
	        StatusUpdate status = new StatusUpdate(message);
	        byte[] buffer = ((DataBufferByte)(image.getImage()).getRaster().getDataBuffer()).getData();
	        status.media("image", new ByteArrayInputStream(buffer) );
	        twitter.updateStatus(status);}
	    catch(TwitterException e){
	    	Logging.logException(e);
	    }
	}
	
	public void uploadPic(String filePath, String message) {
	    try{
	    	File file = new File(filePath);
	        StatusUpdate status = new StatusUpdate(message);
	        status.setMedia(file);
	        twitter.updateStatus(status);}
	    catch(TwitterException e){
	    	Logging.logException(e);
	    }
	}
	
	
	public void configure()
	{
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey(consumerKey)
		  .setOAuthConsumerSecret(consumerSecret)
		  .setOAuthAccessToken(accessToken)
		  .setOAuthAccessTokenSecret(accessTokenSecret);
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
		twitter.uploadPic("C:/Users/ALESSANDRO/Desktop/myrobotlab/opencv.jpg" , "here is the pic");
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
