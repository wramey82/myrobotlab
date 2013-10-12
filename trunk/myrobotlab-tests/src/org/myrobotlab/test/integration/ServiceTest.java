/**
 * 
 * ServiceDirectory is GLOBAL - so when junit unexpectedly created
 * multiple instances of this class - multiple services were created
 * with the same service name (a BIG NO NO) - there needs to be
 * something which will prevent multiple named instances in the 
 * ServiceDirectory - e.g. NameCollisionException
 * 
 * 
 */
package org.myrobotlab.test.integration;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.framework.StopWatch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Python;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;
import org.myrobotlab.service.interfaces.ServiceInterface;

/**
 * @author GroG TODO - timing based and NON timing based testing TODO - global
 *         time base determined on speed of computer
 */

/*
 * Dependencies - 
 * All Systems	
 * 		OpenCV binaries and appropriate pathing
 * 		RXTXSerial (Arduino) - and appropriate pathing
 * Microsoft
 * 		microsoft runtime libraries http://www.microsoft.com/download/en/confirmation.aspx?id=5555
 * 		
 */

public class ServiceTest {

	public final static Logger log = Logger.getLogger(ServiceTest.class.getCanonicalName());
	
	final static StopWatch stopwatch = new StopWatch();
	
	Service guineaPig;
	static String guineaPigName;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		guineaPigName = "test name";
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		guineaPig = new TestThrower(guineaPigName);
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	public static void main(String args[]) {
		org.junit.runner.JUnitCore.main("org.myrobotlab.test.junit.ServiceTest");
	}
	
	String host = "localhost";
	int port = 6767;
		
	@Test
	public final void testSimpleMessage() {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		log.debug("testSimpleMessage begin-------------");
		
		// create services
		TestThrower thrower01 = (TestThrower) Runtime.createAndStart("thrower02", "TestThrower");
		TestCatcher catcher01 = (TestCatcher) Runtime.createAndStart("catcher01", "TestCatcher");

		// set addListener list
		thrower01.addListener("throwInteger", "catcher01", "catchInteger", Integer.class);

		// send 10 messages
		int cnt = 10;
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwInteger", new Integer(7));
		}

		catcher01.waitForCatches(cnt, 5000);
		stopwatch.end();

		// check results
		log.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(cnt, catcher01.catchList.size());
		assertEquals(catcher01.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher01.catchList.get(0), new Integer(7));

		catcher01.catchList.clear();

		Runtime.releaseAll();
		log.warn("testSimpleMessage end-------------");
	}

	@Test
	public final void stressTest() {

		log.warn("stressTest begin-------------");

		// create services
		TestThrower thrower01 = new TestThrower("thrower02");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		// start services
		thrower01.startService();
		catcher01.startService();

		// set addListener list
		thrower01.addListener("throwInteger", "catcher01", "catchInteger", Integer.class);

		// send 1 message
		stopwatch.start();
		int cnt = 100;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwInteger", new Integer(7));
		}

		catcher01.waitForCatches(cnt, 5000);
		stopwatch.end();

		// check results
		log.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(cnt, catcher01.catchList.size());
		assertEquals(0, catcher01.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"));
		assertEquals(new Integer(7), catcher01.catchList.get(0));

		catcher01.catchList.clear();

		// send n messages
		stopwatch.start();
		cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwInteger", new Integer(7));
		}
		catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		// check results
		log.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(catcher01.catchList.size(), cnt);
		assertEquals(catcher01.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher01.catchList.get(0), new Integer(7));

		Runtime.releaseAll();
		log.warn("stressTest end-------------");
	}
	

	

	@Test
	public final void blockingTest() {
		log.warn("blockingTest begin-------------");		
		
		// create services
		TestCatcher catcher01 = new TestCatcher("catcher01");
		TestThrower thrower01 = new TestThrower("thrower01");

		// start services
		catcher01.startService();
		thrower01.startService();

		Object[] data = new Object[1];

		int ret = 0;
		int cnt = 100;
		
		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			data[0] = new Integer(i);
			ret = (Integer) thrower01.sendBlocking(catcher01.getName(), "catchInteger", data);
			assertEquals(ret, i);
		}
		stopwatch.end();

		// test results
		log.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		log.info(catcher01.catchList.size());

		Object o = (Object) thrower01.sendBlocking(catcher01.getName(), "returnNull", (Object[])null);
		assertEquals(null, o);
		
		// release all
		Runtime.releaseAll();
		
		// testing robustness of releasing after releasing all
		catcher01.releaseService();
		thrower01.releaseService();
		log.warn("blockingTest end-------------");
	}
	
	
	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html

	@Test
	public final void testDoubleNotifyMessage() {
		log.warn("testDoubleNotifyMessage begin-------------");

		// create services
		TestThrower thrower01 = new TestThrower("thrower03");
		TestCatcher catcher01 = new TestCatcher("catcher01");
		TestCatcher catcher02 = new TestCatcher("catcher02");

		thrower01.startService();
		catcher01.startService();
		catcher02.startService();

		thrower01.addListener("throwInteger", "catcher01", "catchInteger",Integer.class);
		thrower01.addListener("throwInteger", "catcher02", "catchInteger",Integer.class);

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwInteger", new Integer(7));
		}
		catcher01.waitForCatches(cnt, 1000);
		catcher02.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), cnt);
		assertEquals(catcher02.catchList.size(), cnt);
		
		Runtime.releaseAll();
		log.warn("testDoubleNotifyMessage end-------------");
	}

	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html
	@Test
	public final void testNullAndEmptyParamNotify() {
		log.warn("testNullAndEmptyParamNotify begin-------------");

		// create services
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		thrower01.startService();
		catcher01.startService();

		thrower01.addListener("throwNothing", "catcher01", "catchNothing");

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwNothing");
		}
		catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(cnt, catcher01.catchList.size());
		
		Runtime.releaseAll();
		log.warn("testNullAndEmptyParamNotify end-------------");

	}

	@Test
	public final void testremoveListener() {
		log.warn("testremoveListener begin-------------");

		// create services
		ConfigurationManager cfg = new ConfigurationManager();
		cfg.clear();
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		thrower01.startService();
		catcher01.startService();

		thrower01.addListener("catcher01", "throwNothing");
		thrower01.removeListener("catcher01", "throwNothing");

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwNothing");
		}
		// catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), 0);

		Runtime.releaseAll();
		log.warn("testremoveListener begin-------------");
	}
/*
	@Test
	public final void remoteThrow() throws InterruptedException {
		log.warn("remoteThrow begin-------------");
		//Logger.getRootLogger().setLevel(Level.DEBUG);
		
		TestThrower thrower01 = new TestThrower("thrower01");
		RemoteAdapter remote01 = new RemoteAdapter("remote01", "http://" + host + ":" + port);
		TestCatcher catcher01 = new TestCatcher("catcher01", "http://" + host + ":" +port);
				
		remote01.startService();
		catcher01.startService();
		thrower01.startService();
		
		// set addListener list
		thrower01.addListener("throwInteger", "catcher01", "catchInteger",Integer.class);

		// prepare data
		Integer param1 = new Integer(1);
		int cnt = 100;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			
			try { // have to slow down for windows, it will drop UDP in a loopback (crazy no?)
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			thrower01.invoke("throwInteger", param1);
		}
		log.warn("waiting for " + cnt + " catcher has " + catcher01.catchList.size());
		assertEquals(cnt, catcher01.waitForCatches(cnt, 100));
		stopwatch.end();

		// test results
		log.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		assertEquals(catcher01.catchList.get(0).getClass().getCanonicalName().compareTo(Integer.class.getCanonicalName()), 0);
		assertEquals(catcher01.catchList.get(0), new Integer(1));

		// set new notifies - different functions
		thrower01.addListener("lowPitchInteger", "catcher01", "lowCatchInteger", Integer.class);
		thrower01.addListener("highPitchInteger", "catcher01", "catchInteger", Integer.class);
		thrower01.addListener("noPitchInteger", "catcher01", "catchInteger",
				Integer.class);

		// send messages
		stopwatch.start();
		thrower01.invoke("highPitchInteger", param1);
		thrower01.invoke("noPitchInteger", param1);
		thrower01.invoke("lowPitchInteger", param1);
		log.info(catcher01.lowCatchList.size());
		catcher01.waitForLowCatches(1, 100);
		catcher01.waitForCatches(2, 100);
		stopwatch.end();

		// check results
		log.info(" messages sent in " + stopwatch.elapsedMillis() + " ms");
		log.info(catcher01.lowCatchList.size());
		assertEquals(catcher01.lowCatchList.size(), 1);

		// send messages
		stopwatch.start();
		thrower01.invoke("noPitchInteger", param1);
		catcher01.waitForCatches(5, 100);
		stopwatch.end();

		// check results
		log.info(" messages sent in " + stopwatch.elapsedMillis() + " ms");
		log.info(catcher01.catchList.size());
		// assertEquals(catcher01.catchList.size(), 5);
		
		Runtime.releaseAll();
		Thread.sleep(1000); // wait a second for OS to free bound udp port
		log.warn("remoteThrow end-------------");
	}
	*/
	

	@Test
	public final void bothHandsCatchIntegerTest() {
		log.warn("bothHandsCatchInteger begin-------------");

		// create services
		TestCatcher catcher01 = new TestCatcher("catcher01");

		// start services
		catcher01.startService();
		log.info(catcher01.catchList.size());

		// set addListener list

		// prepare data
		int param1 = 1;
		int param2 = 2;
		int cnt = 1;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			catcher01.invoke("bothHandsCatchInteger", param1, param2);
		}
		catcher01.waitForCatches(2 * cnt, 100);
		stopwatch.end();

		// test results
		log
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		log.info(catcher01.catchList.size());
		assertEquals(catcher01.catchList.size(), (2 * cnt));
		assertEquals(catcher01.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher01.catchList.get(0), new Integer(1));
		assertEquals(catcher01.catchList.get(1), new Integer(2));

		Runtime.releaseAll();
		log.warn("bothHandsCatchIntegerTest end-------------");
	}


	@SuppressWarnings("deprecation")
	@Test
	public final void doubleHandedRemoteThrow() throws InterruptedException {
		log.warn("doubleHandedRemoteThrow begin-------------");

		// clear globals
		ConfigurationManager catchercfg = new ConfigurationManager(Service
				.getHostName(null));
		ConfigurationManager throwercfg = new ConfigurationManager("localhost");
		catchercfg.clear();

		// create services
		// create the test thrower01 on a different host
		TestThrower thrower01 = new TestThrower("thrower01", "localhost");
		RemoteAdapter remote01 = new RemoteAdapter("remote01");
		TestCatcher catcher01 = new TestCatcher("catcher01");
		GUIService gui01 = new GUIService("gui01");
		remote01.setCFG("servicePort", "6565");

		// manually setting an entry for the catcher01 in thrower01's config
		ServiceEntry se = new ServiceEntry();
		se.host = "localhost";
		se.lastModified = new Date();
		se.name = "catcher01";
		se.serviceClass = TestCatcher.class.getCanonicalName();
		se.servicePort = 6565;

		throwercfg.setServiceEntry(se);
		throwercfg.save("cfg.txt");

		// start services
		remote01.startService();
		catcher01.startService();
		thrower01.startService();
		gui01.startService();
		// gui01.display();

		// set addListener list
		// thrower01.addListener("throwInteger", "catcher01",
		// "bothHandsCatchInteger", "java.lang.Integer");
		thrower01.addListener("throwInteger", "catcher01", "bothHandsCatchInteger",
				Integer.class, Integer.class);

		// prepare data
		int param1 = 1;
		int param2 = 2;
		int cnt = 100;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			thrower01.send("catcher01", "bothHandsCatchInteger", param1, param2);
			try {
				Thread.sleep(30); 
				// FIXME - this is to kludge to work with UDP (hopefully) - 
				// a "valid" test would be to see if at least 1 message got through for
				// UDP & all messages got through for TCP

			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
		}
		catcher01.waitForCatches(2 * cnt, 100);
		stopwatch.end();

		// test results
		log.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		log.info(catcher01.catchList.size());
		assertEquals(2 * cnt, catcher01.catchList.size());
		assertEquals(catcher01.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher01.catchList.get(0), new Integer(1));

		// check results
		log.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		
		Runtime.releaseAll();
		Thread.sleep(1000);
		log.warn("doubleHandedRemoteThrow end-------------");
	}

	@Test
	public final void remoteInterfaceTest () throws InterruptedException
	{
		log.warn("remoteInterfaceTest begin-------------");
		// The following services would be running in a remote
		// instance of MRL - creating them here is only for demonstrative 
		// purposes
		RemoteAdapter remote = new RemoteAdapter("remote");
		TestCatcher catcher01 = new TestCatcher("catcher01");
		remote.startService();
		catcher01.startService();
		
		// begin client sending status message (or any message for that matter)
		// create message
		Message msg = new Message();
		msg.name = "catcher01";
		msg.method = "bothHandsCatchInteger";
		msg.sender = "test";
		msg.setData(5,10);
		
		// send it
		try {

			DatagramSocket socket = new DatagramSocket(); 
			ByteArrayOutputStream b_out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(b_out);

			oos.writeObject(msg);
			oos.flush();
			byte[] b = b_out.toByteArray();

			DatagramPacket packet = new DatagramPacket(b, b.length, 
					InetAddress.getByName("localhost"), 6767);
			
			socket.send(packet);
			oos.reset();

		} catch (Exception e) {
			log.warn("threw [" + e.getMessage() + "]");
		}		
		
		Runtime.releaseAll();
		
		Thread.sleep(1000);
		log.warn("remoteInterfaceTest end-------------");
	}

	
	@Test
	public final void serialize() {
		log.warn("serializing begin--------------------");
		String[] serviceNames = Runtime.getInstance().getServiceSimpleNames();
		
		for (int i=0;i < serviceNames.length; ++i)
		{
			
			ServiceInterface s = Runtime.create(serviceNames[i], i + "");
			
			if (i == 29)
			{
				log.info("here");
			}
			log.info("serializing " + serviceNames[i]);
			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			try
			{
			       fos = new FileOutputStream("junit.serialize.bin");
			       out = new ObjectOutputStream(fos);
			       out.writeObject(s);
			       out.close();
			    
			       /*
			       FileInputStream fis = new FileInputStream("test.backup");
			       ObjectInputStream in = new ObjectInputStream(fis);
			       Logging log = (Logging)in.readObject();
			       Clock clock = (Clock)in.readObject();
			       GUIService gui = (GUIService)in.readObject();
			       in.close();
			       
			       log.startService();

			       clock.startService();
			       clock.startClock();
			       
			       gui.startService();
			       gui.display();
			       */
			    
			       s.releaseService();
			} catch (Exception e)
			{
				log.warn(e.getMessage());
				log.warn(Service.stackToString(e));
			} 
		}
						
		Runtime.releaseAll();
		log.warn("serializing end--------------------");	
	}
	/*
	 * FIXME - MRLClient needs to be tested in it own jar only - now has its own project
	@Test
	public final void clientAPI() {
		log.warn("clientAPI begin-------------");

		
		// setting 2 services in different Service Environment
		// all communication which does not have the Service Environment
		// will be done using remote communication (UDP)
		// FIXME - the Runtime will still come in as LOCAL !!!!
		RemoteAdapter remote01 = new RemoteAdapter("remote01", "http://localhost:6767");
		TestCatcher catcher01 = new TestCatcher("catcher01", "http://localhost:6767");		
		TestThrower thrower01 = new TestThrower("thrower01", "http://localhost:6767");		
		log.info(Runtime.dump());
		
		remote01.startService();
		catcher01.startService();
		thrower01.startService();
		
		// set addListener list
		thrower01.addListener("throwInteger", "catcher01", "catchInteger", Integer.class);
		thrower01.invoke("throwInteger", new Integer(7));
		
		// creating a client which uses the remote API to communicate with MRL
		ClientAPITester client  = new ClientAPITester();
		client.init();
		//log.info(Runtime.getInstance().dump());
		client.test1();
		// check results
		client.test2();
		// check results


		log.debug("check sending from RA Client API to RA service");		
		remote01.send("catcher01", "catchInteger", 5);
		catcher01.waitForCatches(8, 2000);
		assertEquals(8, catcher01.catchList.size());
		catcher01.catchList.clear();

		log.debug("check sending from RA Client API to RA service (bothHandsCatchInteger -double parameter)");		
		remote01.send("catcher01", "bothHandsCatchInteger", 8, 9);
		catcher01.waitForCatches(1, 100);
		assertEquals(catcher01.catchList.size(), 2);
		catcher01.catchList.clear();
		
		Runtime.releaseAll();
		
		log.warn("clientAPI end-------------");
	}
	*/
	/*
	@Test
	public final void JythonTest() {
		Python jython = (Python)Runtime.createAndStart("jython", "Jython");
		TestCatcher catcher = (TestCatcher)Runtime.createAndStart("catcher", "TestCatcher");
		
		String code = "jython.send(\"catcher\", \"catchInteger\", 10)\n";
		jython.exec(code);
		catcher.waitForCatches(1, 100);
		assertEquals(1, catcher.catchList.size());
		assertEquals(10, (int)catcher.catchList.get(0));
		Runtime.releaseAll();
	}
	*/
	@Test
	public void cleanUp ()
	{
		log.warn("cleanUp begin-------------");
		try {
			// wait a second for all 
			// listening network sockets to close
			Thread.sleep(1000);
			
			// time clean up
			Runtime.releaseAll();

			File f = new File("junit.serialize.bin");
			f.delete();
			f = new File("cfg.txt");
			f.delete();
			f = new File(".myrobotlab" + File.separator + "serviceData.xml");
			f.delete();
			f = new File(".myrobotlab");
			f.delete();
			} catch (Exception e) {
			e.printStackTrace();
		} 
		
		log.warn("cleanUp end-------------");
	}

}
