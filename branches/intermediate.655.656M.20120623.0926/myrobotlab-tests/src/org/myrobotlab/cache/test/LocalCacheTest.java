package org.myrobotlab.cache.test;

import static org.junit.Assert.*;

import java.util.concurrent.ConcurrentMap;

import javax.swing.JPanel;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.cache.LocalCache;
import org.myrobotlab.test.TestHelpers;

public class LocalCacheTest {
	@SuppressWarnings("unused")
	private Logger log = Logger.getLogger("JythonTest");
	String name = "sometestname";
	LocalCache guineaPig = null;

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
		guineaPig = new LocalCache(1);
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	@Test
	public void testGet() {
		String name1 = "name1", name2 = "name2", name3 = "name3", name4 = "name4";
		boolean value1 = true;
		String value2 = "value2";
		JPanel value3 = new JPanel();
		Double value4 = 0.882d;
		
		guineaPig.put(name1, value1);
		boolean val1 = guineaPig.<Boolean>get(name1);
		Assert.assertTrue(val1);
		
		guineaPig.put(name2, value2);
		val1 = guineaPig.<Boolean>get(name1);
		Assert.assertTrue(val1);
		String val2 = guineaPig.<String>get(name2);
		Assert.assertEquals(value2, val2);
		
		guineaPig.put(name3, value3);
		val1 = guineaPig.<Boolean>get(name1);
		Assert.assertTrue(val1);
		val2 = guineaPig.<String>get(name2);
		Assert.assertEquals(value2, val2);
		JPanel val3 = guineaPig.<JPanel>get(name3);
		
		guineaPig.put(name4, value4);
		val1 = guineaPig.<Boolean>get(name1);
		Assert.assertTrue(val1);
		val2 = guineaPig.<String>get(name2);
		Assert.assertEquals(value2, val2);
		val3 = guineaPig.<JPanel>get(name3);
		Assert.assertEquals(value3, val3);
		Double val4 = guineaPig.<Double>get(name4);
		Assert.assertEquals(value4, val4);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testPut() {
		String name1 = "name1", name2 = "name2", name3 = "name3", name4 = "name4";
		boolean value1 = true;
		String value2 = "value2";
		JPanel value3 = new JPanel();
		Double value4 = 0.882d;
		
		guineaPig.put(name1, value1);
		ConcurrentMap cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "cache");
		Assert.assertEquals(1, cache.size());
		Assert.assertEquals(value1, cache.get(name1));
		
		guineaPig.put(name2, value2);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "cache");
		Assert.assertEquals(2, cache.size());
		Assert.assertEquals(value2, cache.get(name2));
		
		guineaPig.put(name3, value3);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "cache");
		Assert.assertEquals(3, cache.size());
		Assert.assertEquals(value3, cache.get(name3));
		
		guineaPig.put(name4, value4);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "cache");
		Assert.assertEquals(4, cache.size());
		Assert.assertEquals(value4, cache.get(name4));
	}

}
