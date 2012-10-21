package org.myrobotlab.test.integration;

import static org.junit.Assert.*;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.service.Jython;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.TestCatcher;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

public class JythonTest {

	public final static Logger log = Logger.getLogger(FileIO.class.getCanonicalName());

	@Test
	public void testMessagesFromScript() {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		log.info("testMessagesFromScript");
		/*
		Jython jython = (Jython)Runtime.createAndStart("jython", "Jython");
		TestCatcher catcher = (TestCatcher)Runtime.createAndStart("catcher", "TestCatcher");
		*/
		Jython jython = new Jython("jython");
		TestCatcher catcher = new TestCatcher("catcher");
		jython.startService();
		catcher.startService();
		
		String code = "jython.send(\"catcher\", \"catchInteger\", 10)\n";
		jython.exec(code);
		catcher.waitForCatches(1, 1000);
		assertEquals(1, catcher.catchList.size());
		assertEquals(10, (int)catcher.catchList.get(0));
		Runtime.releaseAll();		
	}

	@Test
	public void testMssageToAndFromJythonScript() {
		log.info("testMssageToAndFromJythonScript");
		/*
		Jython jython = (Jython)Runtime.createAndStart("jython", "Jython");
		TestCatcher catcher = (TestCatcher)Runtime.createAndStart("catcher", "TestCatcher");
		*/
		Jython jython = new Jython("jython");
		TestCatcher catcher = new TestCatcher("catcher");
		jython.startService();
		catcher.startService();
				
		// String code = "jython.send(\"catcher\", \"catchInteger\", 10)\n";
		jython.loadScriptFromResource("messageToAndFromJythonScript.py");
		log.info("pre exec");
		jython.exec();
		log.info("post exec");
		log.info(jython.getScript());
		
		catcher.waitForStringCatches(1, 1000);
		assertEquals(1, catcher.stringCatchList.size());
//		String s = catcher.stringCatchList.get(0);
		//assertEquals(10, (int)catcher.stringCatchList.get(0));
		Runtime.releaseAll();		
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
