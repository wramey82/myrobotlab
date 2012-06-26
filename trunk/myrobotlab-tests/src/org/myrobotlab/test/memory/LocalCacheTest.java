package org.myrobotlab.test.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentMap;

import javax.swing.JPanel;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.memory.LocalCache;
import org.myrobotlab.util.TestHelpers;

public class LocalCacheTest {
	@SuppressWarnings("unused")
	private Logger log = Logger.getLogger("JythonTest");
	String name = "sometestname";
	LocalCache guineaPig = null;
	

	String name1 = "name1", name2 = "name2", name3 = "name3", name4 = "name4", invalidName = "some invalid name";
	boolean value1 = true;
	String value2 = "value2";
	JPanel value3 = new JPanel();
	Double value4 = 0.882d;

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
		loadDefaults();
		boolean val1 = guineaPig.get(name1, Boolean.class);
		assertTrue(val1);
		String val2 = guineaPig.get(name2, String.class);
		assertEquals(value2, val2);
		JPanel val3 = guineaPig.get(name3, JPanel.class);
		assertEquals(value3, val3);
		Double val4 = guineaPig.get(name4, Double.class);
		assertEquals(value4, val4);
	}

	@Test
	public void testGet_InvalidStoredType_Double() {
		loadDefaults();
		Double val5 = guineaPig.get(name3, Double.class);
		assertEquals(0.0d, val5, 0.001);
		val5 = guineaPig.get(name2, Double.class);
		assertEquals(0.0d, val5, 0.001);
		val5 = guineaPig.get(name1, Double.class);
		assertEquals(0.0d, val5, 0.001);
	}

	@Test
	public void testGet_NoMatching_Double() {
		loadDefaults();
		Double val5 = guineaPig.get(invalidName, Double.class);
		assertEquals(0.0d, val5, 0.001);
	}
	
	@Test
	public void testGet_InvalidStoredType_Boolean() {
		loadDefaults();
		Boolean val6 = guineaPig.get(name3, Boolean.class);
		assertFalse(val6);
		val6 = guineaPig.get(name2, Boolean.class);
		assertFalse(val6);
		val6 = guineaPig.get(name4, Boolean.class);
		assertFalse(val6);
	}
	
	@Test
	public void testGet_NoMatching_Boolean() {
		loadDefaults();
		Boolean val6 = guineaPig.get(invalidName, Boolean.class);
		assertFalse(val6);
	}
	
	@Test
	public void testGet_InvalidStoredType_JPanel() {
		loadDefaults();
		JPanel val7 = guineaPig.get(name1, JPanel.class);
		assertNull(val7);
		val7 = guineaPig.get(name2, JPanel.class);
		assertNull(val7);
		val7 = guineaPig.get(name4, JPanel.class);
		assertNull(val7);
	}
	
	@Test
	public void testGet_NoMatching_JPanel() {
		loadDefaults();
		JPanel val7 = guineaPig.get(invalidName, JPanel.class);
		assertNull(val7);
	}
	
	@Test
	public void testGet_InvalidStoredType_String() {
		loadDefaults();
		String val8 = guineaPig.get(name1, String.class);
		assertNull(val8);
		val8 = guineaPig.get(name3, String.class);
		assertNull(val8);
		val8 = guineaPig.get(name4, String.class);
		assertNull(val8);
	}
	
	@Test
	public void testGet_NoMatching_String() {
		loadDefaults();
		String val8 = guineaPig.get(invalidName, String.class);
		assertNull(val8);
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
		ConcurrentMap cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "items");
		assertEquals(1, cache.size());
		assertEquals(value1, cache.get(name1));
		
		guineaPig.put(name2, value2);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "items");
		assertEquals(2, cache.size());
		assertEquals(value2, cache.get(name2));
		assertEquals(value1, cache.get(name1));
		
		guineaPig.put(name3, value3);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "items");
		assertEquals(3, cache.size());
		assertEquals(value3, cache.get(name3));
		assertEquals(value2, cache.get(name2));
		assertEquals(value1, cache.get(name1));
		
		guineaPig.put(name4, value4);
		cache = TestHelpers.<ConcurrentMap>getField(guineaPig, "items");
		assertEquals(4, cache.size());
		assertEquals(value4, cache.get(name4));
		assertEquals(value3, cache.get(name3));
		assertEquals(value2, cache.get(name2));
		assertEquals(value1, cache.get(name1));
	}

	private void loadDefaults() {
		guineaPig.put(name1, value1);
		guineaPig.put(name2, value2);
		guineaPig.put(name3, value3);
		guineaPig.put(name4, value4);
	}
}
