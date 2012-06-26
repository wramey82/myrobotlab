package org.myrobotlab.test.reflection;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.reflection.Instantiator;

public class TestInstantiator {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetNewInstanceStringObjectArray() {
//		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetNewInstanceClassOfQObjectArray() {
//		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testInvokeMethod_Double() {
		Object object = 0d;
		String method = "parseDouble";
		Object[] params = {"1.34433"};
		Instantiator.invokeMethod(object, method, params);
	}

}
