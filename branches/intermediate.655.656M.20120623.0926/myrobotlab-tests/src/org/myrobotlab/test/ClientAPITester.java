package org.myrobotlab.test;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.RemoteAdapter;

/**
 * @author GRPERRY
 *
 */
public class ClientAPITester {

	// FIXME - needs work - have to control a different process
	
	public final static Logger log = Logger.getLogger(ClientAPITester.class.getCanonicalName());

	RemoteAdapter api = null;
		
	/**
	 * Initialization of client api / remote adapter 
	 * FIXME - use MRLCLient.jar
	 */
	public void init()
	{
		api = new RemoteAdapter("api","http://localhost:6767");
		
		// registering for messages from remote MRL
//		api.registerForMsgs("localhost", 6767, this);
		// we will listen to different port since MRL is on 6767
		api.setUDPPort(6768); 
		api.setTCPPort(7777);
		api.setUDPStringPort(7775);
		api.startService();
		//api.register("localhost", 6767, "catcher01");
	}
	
	/**
	 * Test sending messages to MRL.
	 * Junit ServiceTest has a TestCatcher and RemoteAdapter
	 * named catcher01 & remote01 respectively.
	 * We are going to send msgs to MRL over UDP port 6767.
	 * This is a simple test of sending control messages to a 
	 * MRL instance.
	 * 
	 * We don't have to register in this case, because we are not 
	 * receiving any messages from MRL
	 */
	public void test1()
	{				
		api.send("catcher01", "catchInteger", 1);
		api.send("catcher01", "catchInteger", 2);
		api.send("catcher01", "catchInteger", 3);		
	}
	
	/**
	 * In this test we will set a subscription to the catcher. So when we
	 * send a message to the catcher, the catcher will send a message to our
	 * receive function.  
	 * 
	 * FIXME - use MRLClient.jar
	 * 
	 */
	public void test2()
	{
		// subscribe to the "catchInteger(Integer x)" method in service catch01
		// FIXME - inbound method not needed everthing goes to receive
		// FIXME - inbound method overloaded for routing ??? Bad Idea ???
	//	api.subscribe("catchInteger", "catcher01", "receive", Integer.TYPE);

		api.send("catcher01", "catchInteger", 1);
		api.send("catcher01", "catchInteger", 2);
		api.send("catcher01", "catchInteger", 3);					
	}
	
	public void receive(Message msg) {

		log.info("***** recieved msg " + msg + " *******");
		// echo the msg back as data
		//api.send("localhost", 6767, "catcher01", "catchMsg", msg);
	}

}
