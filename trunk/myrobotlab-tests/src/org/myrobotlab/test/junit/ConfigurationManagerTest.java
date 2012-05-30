/**
 * 
 */
package org.myrobotlab.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.ConfigurationManager.CFGError;

/**
 * @author gperry
 * 
 */
public class ConfigurationManagerTest {
	public final static Logger log = Logger
			.getLogger(ConfigurationManagerTest.class.getCanonicalName());

	public class TestClass {
		public int x = 0;
		public int y = 0;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#ConfigurationManager(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testConfigurationManager() {
		ConfigurationManager cfg = new ConfigurationManager("app0", "service0");
		cfg.clear();

		// simplest set/get
		cfg.set("str0", "val0");
		assertEquals("val0", cfg.get("str0"));

		// test root configuration access
		ConfigurationManager cfg1 = new ConfigurationManager();
		ConfigurationManager cfg2 = new ConfigurationManager("", "");

		// accessable to root cfg
		assertEquals("val0", cfg2.get("app0/service0/str0"));
		assertEquals("notSet", cfg1.get("str0", "notSet"));
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#setServiceEntry()}.
	 */
	/*
	@Test
	public final void setServiceEntry() {

		// service level config
		ConfigurationManager servicecfg = new ConfigurationManager("app0","service0"); // change to "app0/service0" ??
		ConfigurationManager hostcfg = new ConfigurationManager("app0");
		ConfigurationManager cfg1 = new ConfigurationManager();
		cfg1.clear();

		String service = new String("Service");
		hostcfg.setServiceEntry("app0", "service0", "com.myrobot.framework.MyService", 777, new Date(), service, null);
		hostcfg.setServiceEntry("app0", "service1", "com.myrobot.framework.MyService", 222, new Date(), null, null);
		hostcfg.setServiceEntry("app0", "service2",
				"com.myrobot.framework.MyService", 222, new Date(), service, null);

		hostcfg.save("cfg.txt");

		// null cfg manager test
		Set<String> s = cfg1.keySet("app0/service");
		assertEquals(3, s.size());

		// host level cfg manager test
		ServiceEntry se0 = hostcfg.getServiceEntry("service0");
		assertEquals("app0", se0.host);
		assertEquals("com.myrobot.framework.MyService", se0.serviceClass);
		assertEquals(777, se0.servicePort);

		se0 = hostcfg.getServiceEntry("blah");
		assertEquals(null, se0);

		String myService = (String) hostcfg.getLocalServiceHandle("service0");
		assertEquals("Service", myService);
		myService = (String) hostcfg.getLocalServiceHandle("nullTest");
		assertEquals(null, myService);

		Set<String> serviceNames = hostcfg.keySet("service");
		assertEquals(3, serviceNames.size());

		hostcfg.save("cfg1.txt");

		HashMap<String, ServiceEntry> sem = hostcfg.getServiceMap();
		assertEquals(3, sem.size());

		// hostcfg.remove("app0/service/service0");
		// hostcfg.remove("app0/service","service0");

		hostcfg.removeServiceEntry("service0");

		sem = hostcfg.getServiceMap();
		assertEquals(2, sem.size());

		hostcfg.removeServiceEntries("app0", 222);
		hostcfg.save("cfg2.txt");
		sem = hostcfg.getServiceMap();
		assertEquals(0, sem.size());
	}
	*/
	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getLocalServiceEntries()}
	 * .
	 */
	/*
	@Test
	public final void getLocalServiceEntries() {
		ConfigurationManager hostcfg = new ConfigurationManager("app0");
		hostcfg.clear();

		String service = new String("Service");
		hostcfg.setServiceEntry("app0", "service0",
				"com.myrobot.framework.MyService", 111, new Date(), service, null);
		hostcfg.setServiceEntry("app0", "service1",
				"com.myrobot.framework.MyService", 222, new Date(), null, null);
		hostcfg.setServiceEntry("app0", "service2",
				"com.myrobot.framework.MyService", 333, new Date(), service, null);

		ArrayList<ServiceEntry> sel = hostcfg.getLocalServiceEntries();
		log.info(sel);
		assertEquals(2, sel.size());

	}
	*/
	
	@Test
	public void Simple() throws Exception {
		String processID = "hyperparasite:6666";
		String host = processID;
		ConfigurationManager cfg = new ConfigurationManager(host);
		cfg.clear();
		assertTrue(cfg.getRoot().compareTo(host) == 0);

		// TODO ADD MOD DELETE ALL FORMS + SERIALIZE LOAD all types
		// TODO - change all paramters to key vs name

		// NULL tests begin ----------------------------------------------------
		// set null String test

		// get null String test
		// String value = cfg.get("nullString");
		// assertEquals(null, value);

		boolean threw = false;

		// set bad Integer test
		cfg.set("badInteger", "bad");
		try {
			cfg.getInt("badInteger");
		} catch (CFGError error) {
			threw = true;
		}
		assertTrue(threw);

		// set bad Boolean test
		threw = false;
		boolean boolTest = false;
		cfg.set("badBoolean", "bad");
		try {
			boolTest = cfg.getBoolean("notset");
		} catch (CFGError error) {
			threw = true;
		}
		assertTrue(threw);

		// basic type tests begin
		// ----------------------------------------------------
		// set string
		cfg.set("test1", "value1");
		assertEquals(cfg.get("test1"), "value1");
		cfg.set("test1", "");
		assertEquals(cfg.get("test1"), "");
		log.info(cfg.size("test1"));
		assertEquals(cfg.size("test1"), 0);

		// boolean test
		cfg.set("nullBoolean", "true");
		boolTest = cfg.getBoolean("nullBoolean");
		assertEquals(true, boolTest);
		cfg.set("nullBoolean", "false");
		boolTest = cfg.getBoolean("nullBoolean");
		assertEquals(false, boolTest);

		// int test
		cfg.set("test1", 7676);
		int intTest = cfg.getInt("test1");
		assertEquals(intTest, 7676);

		// TODO - finish types
		// TODO - test split

		// map testing
		cfg.set("typeMap/Sonar", "sonarInfo");
		cfg.set("typeMap/Welcome", "PilotGUI");
		cfg.set("typeMap/SystemInformation", "PilotGUI");
		cfg.set("typeMap/SODHAR", "PilotGUI");

		cfg.set("typeMap/type", "otherGarbage");
		log.info(cfg.toString());

		// map search
		Set<String> keys = cfg.keySet("typeMap");
		assertEquals(5, keys.size());

		// all search
		keys = cfg.keySet();
		assert (keys.size() == 5);

		// TODO get distinct key list

		// TODO Service Entry Examples
		// TODO Service.gui01 is . or / delimeter?
		cfg.set("gui01", ""); // using HashMap as an array
		cfg.set("sodar", "");
		cfg.set("arduino01", "");
		cfg.set("arduino02", "");

		// cfg.set("Service.Names", "gui01, sodar, arduino01, arduino02"); TODO
		// - support tricky spits?

		// TODO - root configs litterals can be stored in the object
		// Service will store SERVICE_PORT = "Port" ???

		// context is gui01
		cfg.set("hostname", ""); // remote hostname
		cfg.set("servicePort", "6767");
		cfg.set("class", "org.myrobotlab.service.GUIService");
		cfg.set("category", "");

		cfg.set("catchInteger", "");
		cfg.set("catchObject", "");
		cfg.set("catch", "");

		cfg.set("direction", "GUIService");
		cfg.set("method", "GUIService");

		// TODO - getArray - ARRAY_DELIMETER

		cfg.set("name0", "value0");
		assert (cfg.get("name0").compareTo("value0") == 0);
		// cfg.add("name0", "value1", "comment");
		assert (cfg.get("name0").compareTo("value0") == 0);

	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#ConfigurationManager(java.lang.String)}
	 * .
	 */
	@Test
	public final void testConfigurationManager2() {
		/*
		 * 
		 * TODO - read the current documentation on ConfigurationManager 1.
		 * handle like HashMap in general - with the exception of allowing nulls
		 * for values 2. if a different function (ie set vs put) is needed allow
		 * nulls and objects to be passed 3. nulls can be valid - set throwable
		 * if null or throwable if empty 4. casting is boring - cast as a
		 * boolean or int should throw 5. most .get with a default can resolve
		 * to the correct type and not necessary to throw 6. make .get default
		 * and make private any master get 7. need a function (main function)
		 * which does not affect the root when getting a value - "threadsafe"
		 * !!!! 8. support Objects but not primitives? 9. any piece of config
		 * can be supplied a default even if its null or ""
		 * 
		 * Since it uses ConcurrentHashMap for thread safety - it will not
		 * accept null as a key NOR null as a value
		 * 
		 * Questions : What happens when you set a value in a hashmap to null ??
		 * I think it throws - should it only throw if set to throwable? and if
		 * its null then what?? you could not set it and the get would
		 * appropriatly return null - but it would also not be in the keyset -
		 * would this be correct - ?? I think so
		 * 
		 * configuration which must be defaulted in place will lead to
		 * non-centralization of setting and getting config - this will lead to
		 * defaults not being localized in loadDefaultConfiguration but
		 * sprinkled through the code - i have introduced a single paramter (non
		 * default) get for string - and this is used to keep things centralized
		 * - if it is NOT set the get will return null - String is only
		 * supported as that will be the most common and duplicate function
		 * prototypes would exist with a single paramter
		 */

		// How a regular HashMap behaves
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		String key = "key";
		hashmap.put(key, null);
		Integer z = (Integer) hashmap.get(key);
		assertEquals(null, z);
		Iterator<String> it = hashmap.keySet().iterator();
		assertTrue(it.hasNext());
		assertEquals("key", it.next());

		z = (Integer) hashmap.get("nada");

		ConfigurationManager c = new ConfigurationManager("testRoot");
		key = "key";
		c.set(key, "");
		z = c.get(key, 0);
		String e = c.get("nada", null);
		String d = c.get(key, "");

		// test multiple constructions
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		ConfigurationManager cfg1 = new ConfigurationManager("");
		ConfigurationManager cfg2 = new ConfigurationManager("testRoot2");
		ConfigurationManager cfg3 = new ConfigurationManager(null);

		cfg.clear();
		cfg1.clear();
		cfg2.clear();
		cfg3.clear();

		assertEquals("", cfg1.getRoot());
		assertEquals("", cfg3.getRoot());

		// root of null or "" is not supported
		// therefore after climbing one branch
		// the root is assumed to be that branch
		/*
		 * TODO - getRootKeySet() cfg3.setRoot(null); cfg3.climb("testRoot");
		 * assertEquals("testRoot",cfg3.getRoot()); assertFalse(cfg3.drop());
		 * cfg3.climb("branch0"); cfg3.dropToRoot();
		 * assertEquals("testRoot",cfg3.getRoot());
		 * 
		 * assertEquals("testRoot",cfg.getRoot());
		 * assertEquals("",cfg1.getRoot());
		 * assertEquals("testRoot2",cfg2.getRoot());
		 * 
		 * // setting a value in one should be accessible in the other
		 * cfg.set("test0", "value0"); assertFalse(cfg2.drop()); // can't drop
		 * below root cfg2.setRoot("testRoot");
		 * 
		 * assertEquals("value0",cfg2.get("test0", ""));
		 */
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getRoot()}.
	 */
	@Test
	public final void testGetRoot2() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		ConfigurationManager cfg2 = new ConfigurationManager("testRoot2");
		cfg.clear();
		cfg2.clear();

		assertFalse(cfg.getRoot().compareTo(cfg2.getRoot()) == 0);
		assertEquals("testRoot", cfg.getRoot());
		assertEquals("testRoot2", cfg2.getRoot());

		// cfg2.setRoot("testRoot");
		// assertEquals("testRoot", cfg2.getRoot());

	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#containsKey(java.lang.String)}
	 * .
	 */
	@Test
	public final void testContainsKey2() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		ConfigurationManager cfg2 = new ConfigurationManager("");
		cfg.clear();
		cfg2.clear();

		cfg.set("test0", "value0");

		assertFalse(cfg.containsKey(null));
		assertFalse(cfg.containsKey(""));
		assertTrue(cfg.containsKey("test0"));
		// assertTrue(cfg2.containsKey("testRoot"));

	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#size()}. Size is
	 * always the number of branches this config has
	 */
	@Test
	public final void testSize2() {

		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		cfg.clear();
		cfg.set("branch0/branch1/branch2/branch3/v0", 0);
		cfg.set("branch0/branch1/branch2/branch3/v1", 1);
		cfg.set("branch0/branch1/branch2/branch3/v2", 2);
		cfg.set("branch0/branch1/branch2/branch3/v3", 3);
		log.info(cfg.size()); // put in 1 branch with 4 leaves
		assertEquals(1, cfg.size());

		log.info(cfg.size());
		assertEquals(1, cfg.size());

		cfg.set("v5", 5); // setting the leaves on the root branch 1 root branch
							// with 3 leaves
		cfg.set("v6", 6);
		cfg.set("v7", 7);
		cfg.set("newbranch/v7", 7);
		assertEquals(3, cfg.size());// {testRoot={v6=6, v7=7, v5=5},
									// testRoot/branch0/branch1/branch2/branch3={v0=0,
									// v3=3, v1=1, v2=2}}

		cfg.set("v8", 8); // adding the first leaf creates a new branch
		log.info(cfg.size());
		// assertEquals(4, cfg.size());

		cfg.set("branch0/branch1/branch2/branch3", 4);
		log.info(cfg.size());
		assertEquals(4, cfg.size());

	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#size(java.lang.String)}
	 * .
	 */
	@Test
	public final void testSizeString2() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		cfg.clear();
		cfg.set("gui01", "org.myrobotlab.Service.GUIService");
		cfg.set("arduino01", "org.myrobotlab.Service.Arduino");
		cfg.set("joy01", "org.myrobotlab.Service.JoystickService");
		cfg.set("catcher01", "org.myrobotlab.Service.TestCatcher");
		cfg.set("thrower01", "org.myrobotlab.Service.TestThrower");
		cfg.set("motor01", "org.myrobotlab.Service.Motor");
		cfg.set("newBranch/thrower01", "org.myrobotlab.Service.TestThrower");
		cfg.set("newBranch/motor01", "org.myrobotlab.Service.Motor");
		cfg.set("newBranch/motor02", "org.myrobotlab.Service.Motor");
		assertEquals(2, cfg.size());
		assertEquals(3, cfg.size("newBranch"));
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGet2() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		cfg.clear();

		cfg.set("voiceName", "kevin16");
		assertEquals("kevin16", cfg.get("voiceName"));
		String notset = null;
		boolean throwOnNotSet = false;
		try {
			notset = cfg.get("noKeyWillReturnNull");
		} catch (CFGError error) {
			log.error(error);
			throwOnNotSet = true;
		}
		assertTrue(throwOnNotSet);
		assertEquals(null, notset);
		cfg.set("volume", 75);
		cfg.get("volume"); // TODO - should THROW IF NOT SET !!!! IF SET TO
							// THROWABLE !!!
		int int0 = cfg.getInt("volume"); // TODO - should THROW IF NOT SET !!!!
											// all PRIMITIVE retrieval - only
											// object is String and Object??
											// which will return null - unless
											// set to throwable
		int int1 = cfg.get("volume", 50);
		assertEquals(75, int0);
		assertEquals(75, int1);
		cfg.remove("volume"); // TODO the notation here looks like remove value
								// HOWEVER its remove map
		int int2 = cfg.get("volume", 50);
		assertEquals(50, cfg.get("volume", 50));

		// TODO - class defaults
		TestClass t0 = new TestClass();
		t0.x = 1;
		t0.y = 2;

		cfg.set("t0", (Object) t0);
		TestClass t1 = (TestClass) cfg.get("t0", (Object) null);
		assertEquals(1, t1.x);
		t1 = (TestClass) cfg.get("xxx", new TestClass());
		assertEquals(0, t1.x);

		Object[] params = new Object[2];
		TestClass tc = new TestClass();
		tc.x = 12;
		params[0] = tc;
		params[1] = new TestClass();
		cfg.set("xxx", params);
		Object[] t3 = (Object[]) cfg.get("xxx", params);
		assertEquals(((TestClass) t3[0]).x, 12);

		Properties p = new Properties();
		// p.setProperty("abc", null); // can't do this TODO - needs to throw if
		// throwable
		String t = (String) p.get("abc");
		t = p.getProperty("abc", null);

		// NULL tests begin ----------------------------------------------------
		// set null String test
		String nullValue = null;
		boolean threwError = false;
		try {
			// code
			cfg.set("nullString", nullValue);
		} catch (CFGError x) {
			threwError = true;
		} catch (java.lang.Throwable ex) {
		}

		assertTrue(threwError);
		assertEquals(null, cfg.get("nullString", null));
		assertEquals("", cfg.get("nullString", ""));
		t = cfg.get("nullStringx", null);

		// set null Integer test
		assertEquals(6, cfg.get("nokey", 6));
		assertEquals(null, cfg.get("nokey", null));

		// TODO testing defaults
		// set null Boolean test
		cfg.set("myKey", true);
		assertEquals(true, cfg.get("myKey", false));
		cfg.set("myKey", false);
		assertEquals(false, cfg.get("myKey", true));

		// TODO - internal type conversions
		cfg.set("stringKey", "xxx");
		boolean stringToBoolean = cfg.get("stringKey", true);
		assertFalse(stringToBoolean);
		cfg.set("stringKey", "true");
		stringToBoolean = cfg.get("stringKey", false);
		assertTrue(stringToBoolean);
		boolean threw = false;
		// set null Date test
		// OUCH!!!!
		try {
			Date nullDate = null;
			cfg.set("nullDate", (Object) nullDate);
			Date dateTest = cfg.getDate("nullDate");
			assertEquals(null, dateTest);
		} catch (CFGError error) {
			threw = true;
		}
		assertTrue(threw);

		// basic type tests begin ------------------------------------
		// set string
		cfg.set("test1", "value1");
		assertEquals(cfg.get("test1", ""), "value1");
		cfg.set("test1", "");
		assertEquals(cfg.get("test1", null), "");
		log.info(cfg.size("test1"));
		assertEquals(cfg.size("test1"), 0);

		// boolean test
		cfg.set("nullBoolean", "true");
		boolean boolTest = cfg.get("nullBoolean", false);
		assertEquals(true, boolTest);

		// long keys ------------------------------------
		cfg.set("branch0/branch1/branch2/branch3/namekey", "myValue");
		String x = cfg.get("branch0/branch1/branch2/branch3/namekey", null);
		assertEquals(x, "myValue");

		// int test
		cfg.set("test1", 7676);
		int intTest = cfg.get("test1", 700);
		assertEquals(intTest, 7676);

		// TODO throw tests ----------------------------------------------
		cfg.setThrowable(true);
		cfg.clear();
		threw = false;

		try {
			Boolean b = null;
			cfg.set("newkey", b);
		} catch (CFGError e) {
			threw = true;
		}

		assertTrue(threw);

		cfg.set("gui01", "org.myrobotlab.Service.GUIService");
		cfg.set("arduino01", "org.myrobotlab.Service.Arduino");
		cfg.set("joy01", "org.myrobotlab.Service.JoystickService");
		cfg.set("catcher01", "org.myrobotlab.Service.TestCatcher");
		cfg.set("thrower01", "org.myrobotlab.Service.TestThrower");
		cfg.set("motor01", "org.myrobotlab.Service.Motor");

		/*
		Date d = new Date();
		for (Iterator<String> it = cfg.keySet().iterator(); it.hasNext();) {
			String name = (String) it.next();
			log.info(cfg.get(name, null));
			cfg.set("host", "localhost");
			cfg.set("servicePort", 6666);
			cfg.set("name", name);
			// cfg.set("class", cfg.get(name, null));
			cfg.set("status", "A");
			cfg.set("category", "");
			cfg.set("method", "");
			cfg.set("direction", "");
			cfg.set("lastModified", d);
			cfg.set("localServiceHandle", "");
			cfg.set("dataClass", "");

			assertEquals("localhost", cfg.get("host", null));
			assertEquals(6666, cfg.get("servicePort", 5555));
			assertEquals(d, cfg.getDate("lastModified"));
			assertEquals(null, cfg.get("class", null));
			// log.info(cfg.get("lastModified"));
		}
		*/
		
		// TODO - add asserts
		cfg.get("host", "");
		cfg.get("host", "notLocalhost");

		// log.info(cfg);
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#split(java.lang.String)}
	 * .
	 */
	@Test
	public final void testSplit2() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		cfg.clear();

		cfg.set("names", "gui01,arduino01,joy01,catch01,thrower01,motor01");
		String[] names = cfg.split("names");
		assertEquals("gui01", names[0]);
		assertEquals("arduino01", names[1]);
		assertEquals("motor01", names[5]);
		assertEquals(6, names.length);

		Date d = new Date();

		for (int i = 0; i < names.length; ++i) {
			String name = names[i];
			cfg.set("host", "localhost");
			cfg.set("servicePort", 6666);
			cfg.set("name", name);
			// cfg.set("class", cfg.get(name, null));
			cfg.set("status", "A");
			cfg.set("category", "");
			cfg.set("method", "");
			cfg.set("direction", "");
			cfg.set("lastModified", d);
			// cfg.set("localServiceHandle", "");
			cfg.set("dataClass", "");

		}
	}

	/**
	 * Integer type test
	 */
	@Test
	public final void boxingTypeTest() {
		ConfigurationManager cfg = new ConfigurationManager("testRoot");
		cfg.clear();

		// All primitives are stored as string
		// All primitives do not have .toString because they are not Objects
		// setting an Integer which will fit in set(String name, int
		// defaultValue) will use the set(String name, Object defaultValue)
		// When requesting getInt(name) - will blow up in a cast because getInt
		// assumes it was a primitive stored as a String
		// Recommendation - NO Primitives in getInt only Object support
		// getInteger

		// now test for Integer !!
		Integer killer = 5;
		cfg.set("pin", killer);
		int pin = cfg.getInt("pin");
		assertEquals(pin, 5);

		Boolean b = true;
		cfg.set("yesNo", b);
		boolean pb = cfg.getBoolean("yesNo");
		assertEquals(pb, true);

		Float f = (float) 2.35;
		cfg.set("float", f);
		float t = cfg.getFloat("float");
		log.info(f.floatValue());
		log.info(t);
		String a = "" + t;
		String z = f.toString();
		assertEquals(z, a);

	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#clear()}.
	 */
	@Test
	public final void testClear() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getRoot()}.
	 */
	@Test
	public final void testGetRoot() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getServiceName()}.
	 */
	@Test
	public final void testGetServiceName() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#containsKey(java.lang.String)}
	 * .
	 */
	@Test
	public final void testContainsKey() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#size()}.
	 */
	@Test
	public final void testSize() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#size(java.lang.String)}
	 * .
	 */
	@Test
	public final void testSizeString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#split(java.lang.String)}
	 * .
	 */
	@Test
	public final void testSplitString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#split(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testSplitStringString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String, java.lang.Object)}
	 * .
	 */
	@Test
	public final void testGetStringObject() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetStringString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getInt(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetInt() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getBoolean(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetBoolean() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getFloat(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetFloat() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getServiceEntry(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetServiceEntry() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String, float)}
	 * .
	 */
	@Test
	public final void testGetStringFloat() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String, int)}
	 * .
	 */
	@Test
	public final void testGetStringInt() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#get(java.lang.String, boolean)}
	 * .
	 */
	@Test
	public final void testGetStringBoolean() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#set(java.lang.String, java.lang.Object)}
	 * .
	 */
	@Test
	public final void testSetStringObject() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#set(java.lang.String, boolean)}
	 * .
	 */
	@Test
	public final void testSetStringBoolean() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#set(java.lang.String, int)}
	 * .
	 */
	@Test
	public final void testSetStringInt() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#set(java.lang.String, java.util.Date)}
	 * .
	 */
	@Test
	public final void testSetStringDate() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getDate(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetDateString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getDate(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetDateStringString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#set(java.lang.String, float)}
	 * .
	 */
	@Test
	public final void testSetStringFloat() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#load(java.lang.String)}
	 * .
	 */
	@Test
	public final void testLoad() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#save(java.lang.String)}
	 * .
	 */
	@Test
	public final void testSave() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#remove(java.lang.String)}
	 * .
	 */
	@Test
	public final void testRemove() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#keySet(java.lang.String)}
	 * .
	 */
	@Test
	public final void testKeySetString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#keySet()}.
	 */
	@Test
	public final void testKeySet() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#toString(org.myrobotlab.framework.ConfigurationManager.OutputFormat)}
	 * .
	 */
	@Test
	public final void testToStringOutputFormat() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#toString()}.
	 */
	@Test
	public final void testToString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#loadFromXML(java.io.InputStream)}
	 * .
	 */
	@Test
	public final void testLoadFromXML() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#storeToXML(java.io.OutputStream, java.lang.String)}
	 * .
	 */
	@Test
	public final void testStoreToXMLOutputStreamString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#storeToXML(java.io.OutputStream, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public final void testStoreToXMLOutputStreamStringString() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getServiceRoot(org.myrobotlab.framework.Service)}
	 * .
	 */
	@Test
	public final void testGetServiceRoot() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#setThrowable(boolean)}
	 * .
	 */
	@Test
	public final void testSetThrowable() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.myrobotlab.framework.ConfigurationManager#getLocalServiceHandle(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetlocalServiceHandle() {
		// fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link java.lang.Object#toString()}.
	 */
	@Test
	public final void testToString1() {
		// fail("Not yet implemented"); // TODO
	}

}
