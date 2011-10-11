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
package org.myrobotlab.test.junit;

import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.framework.StopWatch;
import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.Invoker;
import org.myrobotlab.service.RemoteAdapter;
import org.myrobotlab.service.TestCatcher;
import org.myrobotlab.service.TestThrower;

/**
 * @author GroG TODO - timing based and NON timing based testing TODO - global
 *         time base determined on speed of computer
 */

// TODO - new up operators later - to test non-operator ServiceDirectory !!
// TODO - test start run remove Service
// TODO - decompose - use test cases which correspond to the Trac diagrams
// TODO - test ServiceDirectory
// TODO - test Configuration
// TODO - add non hardware dependent services

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

	public final static Logger LOG = Logger.getLogger(ServiceTest.class.getCanonicalName());
	
	final static StopWatch stopwatch = new StopWatch();

	public static void main(String args[]) {
		org.junit.runner.JUnitCore.main("org.myrobotlab.test.junit.ServiceTest");
	}

	@Test
	public final void blockingTest() {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		//Logger.getRootLogger().setLevel(Level.FATAL); // no logging

		LOG.debug("blockingTest begin-------------");

		
		try {
			URL url = new URL("http://localhost/127.0.0.1:655325");
			String h = url.getHost();
			String z = url.getQuery();
			int port = url.getPort();
			LOG.info(h + " " + port);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		
		// create services
		TestCatcher catcher = new TestCatcher("catcher01");
		TestThrower thrower01 = new TestThrower("thrower01");

		// start services
		catcher.startService();
		thrower01.startService();

		Object[] data = new Object[1];

		int ret = 0;
		int cnt = 100;
		
		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			data[0] = new Integer(i);
			ret = (Integer) thrower01.sendBlocking(catcher.name, "catchInteger", data);
			assertEquals(ret, i);
		}
		stopwatch.end();

		// test results
		LOG.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		LOG.info(catcher.catchList.size());

		Object o = (Object) thrower01.sendBlocking(catcher.name, "returnNull", null);
		assertEquals(null, o);
		
		// release all
		RuntimeEnvironment.releaseAll();
		
		// testing robustness of releasing after releasing all
		catcher.releaseService();
		thrower01.releaseService();

		LOG.debug("blockingTest end-------------");
	}

	@Test
	public final void testSingleThrow() {

		LOG.debug("testSingleThrow begin-------------");

		// create services
		TestThrower thrower = new TestThrower("thrower02");
		TestCatcher catcher = new TestCatcher("catcher02");

		// start services
		thrower.startService();
		catcher.startService();

		// set notify list
		thrower.notify("throwInteger", "catcher02", "catchInteger", Integer.class);

		// send 1 message
		stopwatch.start();
		int cnt = 100;
		for (int i = 0; i < cnt; ++i) {
			thrower.invoke("throwInteger", new Integer(7));
		}

		catcher.waitForCatches(cnt, 1000);
		stopwatch.end();

		// check results
		LOG.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(7));

		catcher.catchList.clear();

		// send n messages
		stopwatch.start();
		cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower.invoke("throwInteger", new Integer(7));
		}
		catcher.waitForCatches(cnt, 1000);
		stopwatch.end();

		// check results
		LOG.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(7));

		RuntimeEnvironment.releaseAll();
		
		LOG.debug("testSingleThrow end-------------");
	}

	@Test
	public final void stressTest() {

		LOG.debug("stressTest begin-------------");

		// create services
		TestThrower thrower = new TestThrower("thrower02");
		TestCatcher catcher = new TestCatcher("catcher02");

		// start services
		thrower.startService();
		catcher.startService();

		// set notify list
		thrower.notify("throwInteger", "catcher02", "catchInteger", Integer.class);

		// send 1 message
		stopwatch.start();
		int cnt = 100;
		for (int i = 0; i < cnt; ++i) {
			thrower.invoke("throwInteger", new Integer(7));
		}

		catcher.waitForCatches(cnt, 1000);
		stopwatch.end();

		// check results
		LOG.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(7));

		catcher.catchList.clear();

		// send n messages
		stopwatch.start();
		cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower.invoke("throwInteger", new Integer(7));
		}
		catcher.waitForCatches(cnt, 1000);
		stopwatch.end();

		// check results
		LOG.info(cnt + " message sent in " + stopwatch.elapsedMillis()
				+ " ms avg 1 msg per "
				+ (stopwatch.elapsedMillis() / (float) cnt) + " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName().compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(7));

		RuntimeEnvironment.releaseAll();
		
		LOG.debug("testSingleThrow end-------------");
	}
	
	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html

	@Test
	public final void testDoubleNotifyMessage() {
		LOG.debug("testDoubleNotifyMessage begin-------------");

		// create services
		TestThrower thrower01 = new TestThrower("thrower03");
		TestCatcher catcher01 = new TestCatcher("catcher04");
		TestCatcher catcher02 = new TestCatcher("catcher05");

		thrower01.startService();
		catcher01.startService();
		catcher02.startService();

		thrower01.notify("throwInteger", "catcher04", "catchInteger",Integer.class);
		thrower01.notify("throwInteger", "catcher05", "catchInteger",Integer.class);

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
		
		RuntimeEnvironment.releaseAll();
	}

	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html
	@Test
	public final void testNullAndEmptyParamNotify() {
		LOG.debug("testDoubleNotifyMessage begin-------------");

		// create services
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		thrower01.startService();
		catcher01.startService();

		thrower01.notify("throwNothing", "catcher01", "catchNothing");

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwNothing");
		}
		catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), cnt);
		
		RuntimeEnvironment.releaseAll();

	}

	@Test
	public final void testRemoveNotify() {
		LOG.debug("testRemoveNotify begin-------------");

		// create services
		ConfigurationManager cfg = new ConfigurationManager();
		cfg.clear();
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		thrower01.startService();
		catcher01.startService();

		thrower01.notify("catcher01", "throwNothing");
		thrower01.removeNotify("catcher01", "throwNothing");

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwNothing");
		}
		// catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), 0);

		RuntimeEnvironment.releaseAll();

		LOG.debug("testRemoveNotify begin-------------");
	}

	@Test
	public final void remoteThrow() {
		LOG.debug("remoteThrow begin-------------");

		// FIXME !!!
		TestThrower thrower01 = new TestThrower("thrower01", "http://localhost:0");
		//RemoteAdapter remote01 = new RemoteAdapter("remote01","http://0.0.0.0:6767");
		//TestCatcher catcher = new TestCatcher("catcher01","http://0.0.0.0:6767");
		RemoteAdapter remote01 = new RemoteAdapter("remote01");
		TestCatcher catcher = new TestCatcher("catcher01");
		remote01.servicePort = 6767;
		
		remote01.startService();
		catcher.startService();
		thrower01.startService();

		// set notify list
		thrower01.notify("throwInteger", "catcher01", "catchInteger",Integer.class);

		// prepare data
		Integer param1 = new Integer(1);
		int cnt = 100;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwInteger", param1);
		}
		catcher.waitForCatches(cnt, 100);
		stopwatch.end();

		// test results
		LOG.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName().compareTo(Integer.class.getCanonicalName()), 0);
		assertEquals(catcher.catchList.get(0), new Integer(1));

		// set new notifies - different functions
		thrower01.notify("lowPitchInteger", "catcher01", "lowCatchInteger", Integer.class);
		thrower01.notify("highPitchInteger", "catcher01", "catchInteger", Integer.class);
		thrower01.notify("noPitchInteger", "catcher01", "catchInteger",
				Integer.class);

		// send messages
		stopwatch.start();
		thrower01.invoke("highPitchInteger", param1);
		thrower01.invoke("noPitchInteger", param1);
		thrower01.invoke("lowPitchInteger", param1);
		LOG.info(catcher.lowCatchList.size());
		catcher.waitForLowCatches(1, 100);
		catcher.waitForCatches(2, 100);
		stopwatch.end();

		// check results
		LOG.info(" messages sent in " + stopwatch.elapsedMillis() + " ms");
		LOG.info(catcher.lowCatchList.size());
		assertEquals(catcher.lowCatchList.size(), 1);

		// send messages
		stopwatch.start();
		thrower01.invoke("noPitchInteger", param1);
		catcher.waitForCatches(5, 100);
		stopwatch.end();

		// check results
		LOG.info(" messages sent in " + stopwatch.elapsedMillis() + " ms");
		LOG.info(catcher.catchList.size());
		// assertEquals(catcher.catchList.size(), 5);
		
		RuntimeEnvironment.releaseAll();

		LOG.debug("remoteThrow end-------------");
	}

	@Test
	public final void bothHandsCatchIntegerTest() {
		LOG.debug("bothHandsCatchInteger begin-------------");

		// create services
		TestCatcher catcher = new TestCatcher("catcher01");

		// start services
		catcher.startService();
		LOG.info(catcher.catchList.size());

		// set notify list

		// prepare data
		int param1 = 1;
		int param2 = 2;
		int cnt = 1;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			catcher.invoke("bothHandsCatchInteger", param1, param2);
		}
		catcher.waitForCatches(2 * cnt, 100);
		stopwatch.end();

		// test results
		LOG
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		LOG.info(catcher.catchList.size());
		assertEquals(catcher.catchList.size(), (2 * cnt));
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(1));
		assertEquals(catcher.catchList.get(1), new Integer(2));

		RuntimeEnvironment.releaseAll();

		LOG.debug("bothHandsCatchIntegerTest end-------------");
	}


	@Test
	public final void doubleHandedRemoteThrow() {
		LOG.debug("remoteThrow begin-------------");

		// clear globals
		ConfigurationManager catchercfg = new ConfigurationManager(Service
				.getHostName(null));
		ConfigurationManager throwercfg = new ConfigurationManager("localhost");
		catchercfg.clear();

		// create services
		// create the test thrower on a different host
		TestThrower thrower01 = new TestThrower("thrower01", "localhost");
		RemoteAdapter remote01 = new RemoteAdapter("remote01");
		TestCatcher catcher = new TestCatcher("catcher01");
		GUIService gui01 = new GUIService("gui01");
		remote01.setCFG("servicePort", "6565");

		// manually setting an entry for the catcher in thrower's config
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
		catcher.startService();
		thrower01.startService();
		gui01.startService();
		// gui01.display();

		// set notify list
		// thrower01.notify("throwInteger", "catcher01",
		// "bothHandsCatchInteger", "java.lang.Integer");
		thrower01.notify("throwInteger", "catcher01", "bothHandsCatchInteger",
				Integer.class, Integer.class);

		// prepare data
		int param1 = 1;
		int param2 = 2;
		int cnt = 100;

		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			thrower01
					.send("catcher01", "bothHandsCatchInteger", param1, param2);
		}
		catcher.waitForCatches(2 * cnt, 100);
		stopwatch.end();

		// test results
		LOG.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		LOG.info(catcher.catchList.size());
		assertEquals(catcher.catchList.size(), 2 * cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(1));

		// check results
		LOG.info(cnt + " messages sent in " + stopwatch.elapsedMillis() + " ms");
		
		RuntimeEnvironment.releaseAll();
		LOG.debug("doubleHandedRemoteThrow end-------------");
	}

	@Test
	public final void remoteInterfaceTest ()
	{
		// TODO SOAP PROXY etc
		Message msg = new Message();
		RemoteAdapter remote01 = new RemoteAdapter("remote01");
		TestCatcher catcher = new TestCatcher("catcher01");

		msg.name = "catcher01";
		msg.method = "bothHandsCatchInteger";
		msg.sender = "test";
		msg.setData(5,10);
		
		
		
	}
	
	@Test
	public final void serialize() {
		String[] serviceNames = Invoker.getServiceShortClassNames();
		
		LOG.info("serializing");
		for (int i=0;i < serviceNames.length; ++i)
		{
			Service s = Invoker.addService(serviceNames[i], i + "");
			
			LOG.info("serializing " + serviceNames[i]);
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
				LOG.error(e.getMessage());
				LOG.error(Service.stackToString(e));
			}
			
		}
		
		
		
		
		RuntimeEnvironment.releaseAll();
		LOG.debug("doubleHandedRemoteThrow end-------------");
	}
	
	
}
