package org.myrobotlab.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.HTTPClient.HTTPData;
import org.slf4j.Logger;

public class Drupal extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Drupal.class.getCanonicalName());

	public Drupal(String n) {
		super(n, Drupal.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	// TODO post forum topic blog etc...
	// TODO seperate token from rest...
	// TODO - schedule 
	// check shout box - look for "new" comment' - if so -> take and send to chatterbox
	// TODO remove static methods...
	
	public void shout(String text)
	{
		shout(host, username, password, text);
	}
	
	public void shout(String host, String login, String password, String text) {
		// authenticate
		HashMap<String, String> fields = new HashMap<String, String>();

		fields.put("openid_identifier", "");
		fields.put("name", login);
		fields.put("pass", password);
		fields.put("op", "Log+in");
		fields.put("form_id", "user_login_block");
		fields.put("openid.return_to", "http%3A%2F%2F" + host + "%2Fopenid%2Fauthenticate%3Fdestination%3Dfrontpage%252Fpanel");
		HTTPData data = HTTPClient.post("http://" + host + "/node?destination=frontpage%2Fpanel", fields);

		// go to node page to get token
		HTTPClient.get("http://" + host + "/node", data);

		// get shoutbox token
		String form_token = null;
		try {
			form_token = HTTPClient.parse(data.method.getResponseBodyAsString(), "<input type=\"hidden\" name=\"form_token\" id=\"edit-shoutbox-add-form-form-token\" value=\"",
					"\"  />");
		} catch (IOException e) {
			e.printStackTrace();
		}

		// post comment
		fields.clear();
		fields.put("nick", login);
		fields.put("message", text);
		fields.put("ajax", "0");
		fields.put("nextcolor", "0");
		fields.put("op", "Shout");
		fields.put("form_token", form_token);
		fields.put("form_id", "shoutbox_add_form");

		HTTPClient.post("http://" + host + "/node", fields, data);

	}

	public String getShoutBox(String host) {
		try {
			HTTPData data = HTTPClient.get(String.format("http://%s/shoutbox/js/view?%d",host, System.currentTimeMillis()));
			String shoutbox = data.method.getResponseBodyAsString();
			log.info(shoutbox);
			return shoutbox;
		} catch (IOException e) {
			Logging.logException(e);
		}
		return null;
	}

	public String readLastShout(String host) {
		return null;
	}
	
	public static class UserShout
	{
		public String userName;
		public String shout;
	}
	
	String usernameTagBegin = "class=\\\"shoutbox-user-name\\\"\\x3e";
	String usernameTagEnd = "\\x3c";
	String shoutTagBegin = "class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3e";
	String shoutTagEnd = "\\x3c";
	
	/* letsmakerobots.com
	String usernameTagBegin = "click to view profile\">";
	String usernameTagEnd = "</a></b>";
	String shoutTagBegin = ": ";
	String shoutTagEnd = "</span>";
	*/

	public ArrayList<UserShout> parseShoutBox(String s)
	{
		ArrayList<UserShout> ret = new ArrayList<UserShout>();
		if (s == null)
		{
			return ret;
		}
		
		int pos0,pos1;
		pos0 = s.indexOf(usernameTagBegin);
		while (pos0 != -1)
		{
			UserShout shout = null;
			pos1 = s.indexOf(usernameTagEnd, pos0);
			if (pos1 != -1)
			{
				pos0 = pos0 + usernameTagBegin.length();
				shout = new UserShout();
				shout.userName = s.substring(pos0, pos1);
				
				pos0 = s.indexOf(shoutTagBegin, pos1);
				if (pos0 != -1){
					pos0 = pos0 + shoutTagBegin.length();
					pos1 = s.indexOf(shoutTagEnd, pos0);
					shout.shout = s.substring(pos0, pos1);
					
					log.info("{}-{}", shout.userName, shout.shout);
					ret.add(shout);
				}
			}
			
			pos0 = s.indexOf(usernameTagBegin, pos1);
			
		}
		
		return ret;
	}
	
	
	boolean doneChatting = false;
	public String host;
	public String username;
	public String password;
	public String chatResponseSearchString;
	public String cleverbotServiceName = String.format("%s_cleverbot",getName());
	public CleverBot cleverbot = new CleverBot(cleverbotServiceName);
	
	HashMap<String, String> usedContexts = new HashMap<String, String>();
	boolean useGreeting = false;
	
	// FIXME - NON context - when a name of someone online is addressed directly - its rude to respond 
	// although sometimes - random response to this would be ok
	
	public void startChatterBot()
	{
		boolean foundContext = false;
		
		if (!cleverbot.isRunning())
		{
			cleverbot.startService();
		}
		
		
		// FIXME - don't respond to myself
		// FIXME - don't respond if I'm the last shout
		// FIXME - way to come up with contextual based questions - e.g. robotics & electronic questions
		while (!doneChatting)
		{
			// wait a while
			usedContexts.put("@ mr.turing where are you from ?", null);
			Service.sleep(1000);
			
			String s = getShoutBox(host);
			ArrayList<UserShout> shouts = parseShoutBox(s);
			log.info("found {} shouts - looking for context", shouts.size());
			
			for(int i = 0; i < shouts.size(); ++i)
			{
				UserShout shout = shouts.get(i);
				if (shout.shout.indexOf(chatResponseSearchString) != -1 && !usedContexts.containsKey(shout.shout))
				{
					log.info("found context sending [{}] to cleverbot", shout.shout);
					usedContexts.put(shout.shout, shout.shout);
					foundContext = true;
					String response = cleverbot.chat(shout.shout);
					shout(response);
					
				}
			}
			
			// no context found - response to the last shout
			if (!foundContext && shouts.size() > 1)
			{
				String lastShout = null;
				if (useGreeting)
				{
					lastShout = "Hello";
					useGreeting = false;
				} else {
					UserShout shoutbox = shouts.get(0);
					if (shoutbox.userName.equals(username))
					{
						log.info("i'm the last to respond - i wont respond to myself");
						continue;
					}
					lastShout = shouts.get(0).shout;
				}
				//lastShout = shouts.get(0).shout;
				//lastShout = "good night";
				log.info("could not find context - sending last shout  - - [{}]", lastShout);
				String response = cleverbot.chat(lastShout);
				log.info("shouting [{}]", response);
				shout(response);
			}
				
			// scan through chats - find any addressed to us - from someone else - that has not be responded to 
			// if found ->
			// if not grab the most recent which is not us -> 
			
			// send target to chatterbox
			// get response
			// post response
		

		}
		
	}	
	
	
	
	public String getCleverBotResponse(String inMsg)
	{
		return null;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// Drupal.shout("letsmakerobots.com", "mr.turing", "zardoz7",
		// "Hello there.");
//		String s = Drupal.getShoutBox("myrobotlab.org");
		
		Drupal drupal = new Drupal("myrobotlab.org");
		drupal.host = "myrobotlab.org";
		drupal.username = "mr.turing";
		drupal.password = "zardoz7";
		drupal.chatResponseSearchString = "turing ";
		drupal.startChatterBot();
		
		/*
		String s = " { \"success\": true, \"data\": \"\\x3ctable\\x3e\\n\\x3ctbody\\x3e\\n \\x3ctr class=\\\"odd\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 09:23am by tinhead\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3etinhead\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3ewell yes everything is a file still you have to know how to use it\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"even\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 09:22am by tinhead\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3etinhead\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3eok back\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"odd\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:18am by tinhead\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3etinhead\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3esorry @work ... meeting\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"even\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:12am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3ecause you know everything in Unix is a file .. or so I\\'ve heard \\x3ca href=\\\"http://tinyurl.com/ajydw9\\\" title=\\\"http://tinyurl.com/ajydw9\\\"\\x3ehttp://tinyurl.com/ajydw9\\x3c/a\\x3e\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"odd\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:10am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3ethe I2C module puts in a /dev/ device? - is it possible to interface the bus using a /dev/smb device?\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"even\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:08am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3ethat instructable looks helpful - I\\'ll load the I2c driver (or try to) on the kernel I have now\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"odd\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:06am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3e.. so possibly python-smbus uses the kernel driver only...\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"even\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:06am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3eoh .. i2cdetect  looks like a \\\"i2c-tool\\\"\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"odd\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:05am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3eso - it might have .. blacklist i2c-bcm2708 .. but we don\\'t want it ?\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n \\x3ctr class=\\\"even\\\"\\x3e\\x3ctd\\x3e\\x3cdiv class=\\\"shoutbox-msg\\\" title=\\\"Posted 04/04/13 at 08:04am by GroG\\\"\\x3e\\x3cdiv class=\\\"shoutbox-admin-links\\\"\\x3e\\x3c/div\\x3e\\x3cspan class=\\\"shoutbox-user-name\\\"\\x3eGroG\\x3c/span\\x3e:\\x26nbsp;\\x3cspan class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3esheeet - blacklisting drivers .. stumbled into a vast array of flavors and versions :P\\x3c/p\\x3e\\n\\x3c/span\\x3e\\x3cspan class=\\\"shoutbox-msg-time\\\"\\x3e\\x3c/span\\x3e\\x3c/div\\x3e\\n\\x3c/td\\x3e \\x3c/tr\\x3e\\n\\x3c/tbody\\x3e\\n\\x3c/table\\x3e\\n\" }";
		ArrayList<UserShout> shouts = Drupal.parseShoutBox(s);
		for (int i = 0; i < shouts.size(); ++i)
		{
			UserShout shout = shouts.get(i);
			log.info("{} {}", shout.userName, shout.shout);
		}
		log.info(s);
		*/
		/*
		Object obj=JSONValue.parse(s);
		
		JSONTokener tokener = new JSONTokener(s);

		JSONObject jObj = new JSONObject(s); // this parses the json
		Iterator<String> it = jObj.keys(); //gets all the keys

		while(it.hasNext())
		{
		    String key = it.next(); // get key
		    Object o = jObj.get(key); // get value
		    log.info("{} = {}", key, o);
		    //session.putValue(key, o); // store in session
		}
		*/
		/*
		Object obj=JSONValue.parse(s);
		JSONObject object = (JSONObject)obj;
		
		Iterator<String> it = object.keySet().iterator(); //gets all the keys

		while(it.hasNext())
		{
		    String key = it.next(); // get key
		    Object o = object.get(key); // get value
		    log.info("{} = {}", key, o);
		    //session.putValue(key, o); // store in session
		}
		*/
		 //JSONArray array=(JSONArray)obj;
		
		//JSONArray finalResult = new JSONArray(tokener);
		
//		log.info("{}",finalResult);
		//Drupal.shout("myrobotlab.org", "mr.turing", "zardoz7", "fabshrimp");

		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

}
