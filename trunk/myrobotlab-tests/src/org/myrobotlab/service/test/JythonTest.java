package org.myrobotlab.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.service.Jython;
import org.myrobotlab.test.TestHelpers;
import org.python.util.PythonInterpreter;

public class JythonTest {
	String name = "sometestname";
	Jython guineaPig = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.FATAL);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		guineaPig = new Jython(name);
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	@Test
	public void testGetToolTip() {
		String result = guineaPig.getToolTip();
		assertEquals("Jython IDE", result);
	}

	@Test
	public void testPreProcessHook() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testJython() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testCreatePythonInterpreter() {
		guineaPig.createPythonInterpreter();
		PythonInterpreter interpreter = TestHelpers.<PythonInterpreter>getField(guineaPig, "interp");
		assertNotNull(interpreter);
	}

	@Test
	public void testAttachJythonConsole() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testExecString() {
		// TODO set up a string logger to verify that the error message is written out for the failed exec() call
		String code = null;
		guineaPig.exec(code);
		String script = TestHelpers.<String>getField(guineaPig, "script");
		assertNull(script);
		
		code = "some code";
		guineaPig.exec(code);
		script = TestHelpers.<String>getField(guineaPig, "script");
		assertNotNull(script);
		assertEquals(code, script);
	}

	@Test
	public void testExecStringBoolean() {
		// TODO set up a string logger to verify that the error message is written out for the failed exec() call
		String code = null;
		boolean replace = false;
		guineaPig.exec(code, replace);
		String script = TestHelpers.<String>getField(guineaPig, "script");
		assertNull(script);
		
		code = "some code";
		replace = false;
		guineaPig.exec(code, replace);
		script = TestHelpers.<String>getField(guineaPig, "script");
		assertNull(script);
		
		code = "some code";
		replace = true;
		guineaPig.exec(code, replace);
		script = TestHelpers.<String>getField(guineaPig, "script");
		assertNotNull(script);
		assertEquals(code, script);
	}

	@Test
	public void testFinishedExecutingScript() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetScript() {
		String code = null;
		boolean replace = false;
		guineaPig.exec(code, replace);
		assertNull(guineaPig.getScript());
		
		code = "some code";
		replace = false;
		guineaPig.exec(code, replace);
		assertNull(guineaPig.getScript());
		
		code = "some code";
		replace = true;
		guineaPig.exec(code, replace);
		assertNotNull(guineaPig.getScript());
		assertEquals(code, guineaPig.getScript());
	}

	@Test
	public void testRestart() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testPublishStdOut() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testMain() {
		fail("Not yet implemented"); // TODO
	}

}
