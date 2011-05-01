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

import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceEntry;
import org.myrobotlab.framework.StopWatch;
import org.myrobotlab.service.GUIService;
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

public class ServiceTest {

	public final static Logger LOG = Logger.getLogger(ServiceTest.class.getCanonicalName());
	
	final static StopWatch stopwatch = new StopWatch();

	public static void main(String args[]) {
		org.junit.runner.JUnitCore
				.main("org.myrobotlab.test.junit.ServiceTest");
	}

	@Test
	public final void blockingTest() {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		LOG.debug("blockingTest begin-------------");
		// clear globals
		ConfigurationManager cfg = new ConfigurationManager(Service
				.getHostName(null));
		cfg.clear();

		// create services
		TestCatcher catcher = new TestCatcher("catcher01");
		TestThrower thrower01 = new TestThrower("thrower01");

		// start services
		catcher.startService();
		thrower01.startService();

		// set notify list
		Object[] data = new Object[1];
		data[0] = new Integer(12);
		Integer ret = 12;
		// \ Integer ret = (Integer)thrower01.sendBlocking(catcher.name,
		// "catchInteger", data);
		// assertEquals(ret.intValue(), 12);

		int cnt = 100;
		// send messages
		stopwatch.start();
		for (int i = 0; i < cnt; ++i) {
			data[0] = new Integer(i);
			ret = (Integer) thrower01.sendBlocking(catcher.name,
					"catchInteger", data);
			assertEquals(ret.intValue(), i);
		}
		stopwatch.end();
		LOG.info(catcher.catchList.size());

		// test results
		LOG
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		LOG.info(catcher.catchList.size());

		Object o = (Object) thrower01.sendBlocking(catcher.name, "returnNull",
				null);
		assertEquals(null, o);
		LOG.debug("blockingTest end-------------");
	}

	@Test
	public final void testSingleThrow() {
		// LOG.setLevel(Level.WARN);

		LOG.debug("testSingleThrow begin-------------");

		// create services
		TestThrower thrower = new TestThrower("thrower01");
		TestCatcher catcher = new TestCatcher("catcher01");

		// thrower.listServices("fred"); // TODO - check bad name message

		// start services
		thrower.startService();
		catcher.startService();

		// set notify list
		thrower.notify("throwInteger", "catcher01", "catchInteger", Integer.class);

		// send 1 message
		stopwatch.start();
		int cnt = 1;
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
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
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
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(7));

		catcher.catchList.clear();

		catcher.stopService();
		thrower.stopService();
		LOG.debug("testSingleThrow end-------------");
	}

	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html

	@Test
	public final void testDoubleNotifyMessage() {
		LOG.debug("testDoubleNotifyMessage begin-------------");

		// create services
		ConfigurationManager cfg = new ConfigurationManager();
		cfg.clear();
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");
		TestCatcher catcher02 = new TestCatcher("catcher02");

		thrower01.startService();
		catcher01.startService();
		catcher02.startService();

		thrower01.notify("throwInteger", "catcher01", "catchInteger",
				Integer.class);
		thrower01.notify("throwInteger", "catcher02", "catchInteger",
				Integer.class);

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
	}

	// http://supportweb.cs.bham.ac.uk/documentation/tutorials/docsystem/build/tutorials/junit/junit.html
	@Test
	public final void testNullAndEmptyParamNotify() {
		LOG.debug("testDoubleNotifyMessage begin-------------");

		// create services
		ConfigurationManager cfg = new ConfigurationManager();
		cfg.clear();
		TestThrower thrower01 = new TestThrower("thrower01");
		TestCatcher catcher01 = new TestCatcher("catcher01");

		thrower01.startService();
		catcher01.startService();

		thrower01.notify("throwNothing", "catcher01", "catchNothing");

		// send n messages
		stopwatch.start();
		int cnt = 2;
		for (int i = 0; i < cnt; ++i) {
			thrower01.invoke("throwNothing", null);
		}
		catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), cnt);
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
			thrower01.invoke("throwNothing", null);
		}
		// catcher01.waitForCatches(cnt, 1000);
		stopwatch.end();

		assertEquals(catcher01.catchList.size(), 0);
		LOG.debug("testRemoveNotify begin-------------");
	}

	@Test
	public final void remoteThrow() {
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
		thrower01.notify("throwInteger", "catcher01", "catchInteger",
				Integer.class);

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
		LOG
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		assertEquals(catcher.catchList.size(), cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo(Integer.class.getCanonicalName()), 0);
		assertEquals(catcher.catchList.get(0), new Integer(1));

		// set new notifies - different functions
		thrower01.notify("lowPitchInteger", "catcher01", "lowCatchInteger",
				Integer.class);
		thrower01.notify("highPitchInteger", "catcher01", "catchInteger",
				Integer.class);
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

		catcher.stopService();
		thrower01.stopService();
		LOG.debug("remoteThrow end-------------");
	}

	@Test
	public final void bothHandsCatchIntegerTest() {
		LOG.debug("bothHandsCatchInteger begin-------------");
		// clear globals
		ConfigurationManager catchercfg = new ConfigurationManager(Service
				.getHostName(null));
		catchercfg.clear();

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

		LOG.debug("bothHandsCatchIntegerTest end-------------");
	}

	@Test
	public final void twoHandedPrimitiveCatchInt() {
		LOG.debug("twoHandedPrimitiveCatchInt begin-------------");
		// clear globals
		// not suppported - do to the fact "catcher.invoke("method", param1,
		// param2, param3) would need 5! function signatures
		// currently all params are upgraded to Object
		LOG.debug("remoteThrow end-------------");
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
		LOG
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		LOG.info(catcher.catchList.size());
		assertEquals(catcher.catchList.size(), 2 * cnt);
		assertEquals(catcher.catchList.get(0).getClass().getCanonicalName()
				.compareTo("java.lang.Integer"), 0);
		assertEquals(catcher.catchList.get(0), new Integer(1));

		// check results
		LOG
				.info(cnt + " messages sent in " + stopwatch.elapsedMillis()
						+ " ms");
		LOG.debug("doubleHandedRemoteThrow end-------------");
	}

}
