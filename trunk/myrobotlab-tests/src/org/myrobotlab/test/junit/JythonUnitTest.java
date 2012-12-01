package org.myrobotlab.test.junit;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Python;
import org.myrobotlab.util.TestHelpers;
import org.python.util.PythonInterpreter;

public class JythonUnitTest {
	private Logger log = Logger.getLogger("JythonTest");
	String name = "sometestname";
	Python guineaPig = null;

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
		guineaPig = new Python(name);
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	@Test
	public void testGetToolTip() {
		String result = guineaPig.getToolTip();
		assertNotNull(result);
		assertFalse(result.trim().isEmpty());
	}

	@Test
	public void testPreProcessHook() {
		boolean result = false;
		Message method = null;
		
		try {
			guineaPig.preProcessHook(method);
			fail("Should have thrown NullPointerException because input to method is null.");
		} catch (NullPointerException e) {}
		
		method = new Message();

		guineaPig.preProcessHook(method);

		method.sender = "JythonTest";
		guineaPig.preProcessHook(method);
		
		method.sendingMethod = "testPreProcessHook1";
		guineaPig.preProcessHook(method);

		method.method = "badmethod";
		guineaPig.preProcessHook(method);

		method.method = "preProcessHook";
		result = guineaPig.preProcessHook(method);
		assertTrue(result);

		// TODO need to have a case where the jython is successfully executed
	}

	@Test
	public void testJython() {
		Set<String> publicMethods = new HashSet<String>();
		Method[] methods = guineaPig.getClass().getMethods();
		for (Method m : methods) {
			if (Modifier.isPublic(m.getModifiers())) {
				publicMethods.add(m.getName());
				log.info(m.getName());
			}
		}
		Set<String> commandMap = TestHelpers.<Set<String>>getField(guineaPig, "commandMap");
		assertEquals(publicMethods.size(), commandMap.size());
	}

	@Test
	public void testCreatePythonInterpreter() {
		guineaPig.createPythonInterpreter();
		PythonInterpreter interpreter = TestHelpers.<PythonInterpreter>getField(guineaPig, "interp");
		assertNotNull(interpreter);
	}

	@Test
	public void testAttachJythonConsole() {
		guineaPig.attachPythonConsole();
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
		guineaPig.finishedExecutingScript();
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
	public void testStop() {
		guineaPig.createPythonInterpreter();
		guineaPig.stop();
		Object interpreter = TestHelpers.<Object>getField(guineaPig, "interp");
		assertNull(interpreter);
	}

	@Test
	public void testPublishStdOut() {
		String data = "some data";
		String result = guineaPig.publishStdOut(data);
		assertEquals(data, result);
	}
	
	/*
	@Test
	public void testPreProcessHook() {
		fail("Not yet implemented");
	}

	@Test
	public void testJython() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttachJythonConsole() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreatePythonInterpreter() {
		fail("Not yet implemented");
	}

	@Test
	public void testExecString() {
		fail("Not yet implemented");
	}

	@Test
	public void testExecStringBoolean() {
		fail("Not yet implemented");
	}

	@Test
	public void testFinishedExecutingScript() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetScript() {
		fail("Not yet implemented");
	}

	@Test
	public void testPublishStdOut() {
		fail("Not yet implemented");
	}

	@Test
	public void testRestart() {
		fail("Not yet implemented");
	}

	@Test
	public void testLoadPythonScript() {
		fail("Not yet implemented");
	}

	@Test
	public void testMain() {
		fail("Not yet implemented");
	}
 */

}
