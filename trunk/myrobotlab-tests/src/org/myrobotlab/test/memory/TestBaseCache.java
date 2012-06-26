package org.myrobotlab.test.memory;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.memory.BaseCache;

public class TestBaseCache {
	private BaseCache guineaPig;

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
		guineaPig = new TestCache();
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	@Test
	public void testGetBool_NonExisting() {
		String name = "some odd name";
		boolean result;
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Boolean() {
		String name = "boolean";
		boolean result;
		guineaPig.put(name, true);
		result = guineaPig.getBool(name);
		assertTrue(result);
	}

	@Test
	public void testGetBool_Integer_Zero() {
		String name = "some odd name";
		boolean result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Integer_NonZero() {
		String name = "some odd name";
		boolean result;
		int value = 3;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
	}

	@Test
	public void testGetBool_Float_Zero() {
		String name = "some odd name";
		boolean result;
		float value = 0.0f;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Float_NonZero() {
		String name = "some odd name";
		boolean result;
		float value = 3.3f;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Double_Zero() {
		String name = "some odd name";
		boolean result;
		double value = 0.0d;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Double_NonZero() {
		String name = "some odd name";
		boolean result;
		double value = 5.8d;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_String_Filled() {
		String name = "some odd name";
		boolean result;
		String value = "some value";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_String_Empty() {
		String name = "some odd name";
		boolean result;
		String value = "";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_String_True() {
		String name = "some odd name";
		boolean result;
		String value = "true";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
		
		value = "True";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
		
		value = "TRUE";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
	}

	@Test
	public void testGetBool_String_False() {
		String name = "some odd name";
		boolean result;
		String value = "false";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
		
		value = "False";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
		
		value = "FALSE";
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_String_Null() {
		String name = "some odd name";
		boolean result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Short_Zero() {
		String name = "some odd name";
		boolean result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetBool_Short_NonZero() {
		String name = "some odd name";
		boolean result;
		short value = 43;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
	}

	@Test
	public void testGetBool_Byte_NonZero() {
		String name = "some odd name";
		boolean result;
		byte value = 3;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertTrue(result);
	}

	@Test
	public void testGetBool_Byte_Zero() {
		String name = "some odd name";
		boolean result;
		byte value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getBool(name);
		assertFalse(result);
	}

	@Test
	public void testGetByte_BooleanFalse() {
		String name = "some odd name";
		byte result;
		boolean value = false;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_BooleanTrue() {
		String name = "some odd name";
		byte result;
		boolean value = true;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_ByteZero() {
		String name = "some odd name";
		byte result;
		byte value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(value, result);
	}

	@Test
	public void testGetByte_ByteNonZero() {
		String name = "some odd name";
		byte result;
		byte value = 14;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(value, result);
	}

	@Test
	public void testGetByte_IntZero() {
		String name = "some odd name";
		byte result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_IntNonZero() {
		String name = "some odd name";
		byte result;
		int value = 14;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_DoubleZero() {
		String name = "some odd name";
		byte result;
		double value = 0.0d;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_DoubleNonZero() {
		String name = "some odd name";
		byte result;
		double value = 14.0d;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_FloatZero() {
		String name = "some odd name";
		byte result;
		float value = 0.0f;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_FloatNonZero() {
		String name = "some odd name";
		byte result;
		float value = 14.0f;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_ShortZero() {
		String name = "some odd name";
		byte result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_ShortNonZero() {
		String name = "some odd name";
		byte result;
		short value = 14;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(14, result);
	}

	@Test
	public void testGetByte_String_Invalid() {
		String name = "some odd name";
		byte result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
		
		value = "invalid number";
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
		
		value = "23.43234";
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
		
		value = "765434567";
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetByte_String_Valid() {
		String name = "some odd name";
		byte result;
		String value = "3";
		guineaPig.put(name, value);
		result = guineaPig.getByte(name);
		assertEquals(3, result);
	}

	@Test
	public void testGetDouble_Double() {
		String name = "some odd name";
		double result;
		double value = 23.344d;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(value, result, 0.00001);
	}

	@Test
	public void testGetDouble_Float_Zero() {
		String name = "some odd name";
		double result;
		float value = 0.0f;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(value, result, 0.00001);
	}

	@Test
	public void testGetDouble_Float_NonZero() {
		String name = "some odd name";
		double result;
		float value = 23.344f;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(value, result, 0.00001);
	}

	@Test
	public void testGetDouble_Int_Zero() {
		String name = "some odd name";
		double result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_Int_NonZero() {
		String name = "some odd name";
		double result;
		int value = 23;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(23, result, 0.00001);
	}

	@Test
	public void testGetDouble_Byte_Zero() {
		String name = "some odd name";
		double result;
		byte value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_Byte_NonZero() {
		String name = "some odd name";
		double result;
		byte value = 23;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(23, result, 0.00001);
	}

	@Test
	public void testGetDouble_Short_Zero() {
		String name = "some odd name";
		double result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_Short_NonZero() {
		String name = "some odd name";
		double result;
		short value = 23;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(23, result, 0.00001);
	}

	@Test
	public void testGetDouble_Boolean_False() {
		String name = "some odd name";
		double result;
		boolean value = false;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_Boolean_True() {
		String name = "some odd name";
		double result;
		boolean value = true;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_String_Null() {
		String name = "some odd name";
		double result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_String_Invalid() {
		String name = "some odd name";
		double result;
		String value = "not a double";
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
		
		value = "";
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetDouble_String_Valid() {
		String name = "some odd name";
		double result;
		String value = "23";
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(23, result, 0.00001);
		
		value = "0";
		guineaPig.put(name, value);
		result = guineaPig.getDouble(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetFloat_Float() {
		String name = "some odd name";
		float result;
		float value = 25.5f;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(value, result, 0.00001);
		
		value = 25333666432.5f;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(value, result, 0.00001);
		
		value = 0.0f;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(value, result, 0.00001);
	}

	@Test
	public void testGetFloat_Int_Zero() {
		String name = "some odd name";
		float result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Int_NonZero() {
		String name = "some odd name";
		float result;
		int value = 44532;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(value, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Double_Zero() {
		String name = "some odd name";
		float result;
		double value = 0.0d;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Double_NonZero() {
		String name = "some odd name";
		float result;
		double value = 44532d;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Short_Zero() {
		String name = "some odd name";
		float result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Short_NonZero() {
		String name = "some odd name";
		float result;
		short value = 44;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(44, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Byte_Zero() {
		String name = "some odd name";
		float result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);	
	}

	@Test
	public void testGetFloat_Byte_NonZero() {
		String name = "some odd name";
		float result;
		short value = 44;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(44, result, 0.00001);	
	}

	@Test
	public void testGetFloat_String_Invalid() {
		String name = "some odd name";
		float result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);
		
		value = "";
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);
		
		value = "some bad value";
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetFloat_String_Valid() {
		String name = "some odd name";
		float result;
		String value = "44.3344";
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(44.3344, result, 0.00001);
		
		value = "44";
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(44, result, 0.00001);
		
		value = "0.0";
		guineaPig.put(name, value);
		result = guineaPig.getFloat(name);
		assertEquals(0.0, result, 0.00001);
	}

	@Test
	public void testGetInt_Double_Zero() {
		String name = "some odd name";
		int result;
		double value = 0.0d;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = 0.1d;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_Double_NonZero() {
		String name = "some odd name";
		int result;
		double value = 2333.344d;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = 2333.5334d;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_Float_Zero() {
		String name = "some odd name";
		int result;
		float value = 0.0f;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = 0.1f;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_Float_NonZero() {
		String name = "some odd name";
		int result;
		float value = 2333.344f;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = 2333.5334f;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_Integer() {
		String name = "some odd name";
		int result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(value, result);
		
		value = 133043;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(value, result);
	}

	@Test
	public void testGetInt_Byte_NonZero() {
		String name = "some odd name";
		int result;
		byte value = 23;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(23, result);
	}

	@Test
	public void testGetInt_Byte_Zero() {
		String name = "some odd name";
		int result;
		byte value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_Short_NonZero() {
		String name = "some odd name";
		int result;
		short value = 23;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(23, result);
	}

	@Test
	public void testGetInt_Short_Zero() {
		String name = "some odd name";
		int result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetInt_String_Valid() {
		String name = "some odd name";
		int result;
		String value = "23";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(23, result);
		
		value = "0";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = "2343423";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(2343423, result);
	}

	@Test
	public void testGetInt_String_Invalid() {
		String name = "some odd name";
		int result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = "";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = "0.0";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
		
		value = "234.045";
		guineaPig.put(name, value);
		result = guineaPig.getInt(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Short() {
		String name = "some odd name";
		short result;
		short value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
		
		value = 32;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(value, result);
	}

	@Test
	public void testGetShort_Int_Zero() {
		String name = "some odd name";
		short result;
		int value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Int_NonZero() {		
		String name = "some odd name";
		short result;
		int value = 32;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Double_Zero() {
		String name = "some odd name";
		short result;
		double value = 0d;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Double_NonZero() {		
		String name = "some odd name";
		short result;
		double value = 32d;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Float_Zero() {
		String name = "some odd name";
		short result;
		float value = 0f;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Float_NonZero() {		
		String name = "some odd name";
		short result;
		float value = 32f;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Byte_Zero() {
		String name = "some odd name";
		short result;
		byte value = 0;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_Byte_NonZero() {		
		String name = "some odd name";
		short result;
		byte value = 32;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(32, result);
	}

	@Test
	public void testGetShort_String_Invalid() {
		String name = "some odd name";
		short result;
		String value = null;
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
		
		value = "";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
		
		value = "invalid number";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
		
		value = "23.4333";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
		
		value = "23432434324";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	@Test
	public void testGetShort_String_Valid() {		
		String name = "some odd name";
		short result;
		String value = "32";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(32, result);
		
		value = "0";
		guineaPig.put(name, value);
		result = guineaPig.getShort(name);
		assertEquals(0, result);
	}

	private class TestCache extends BaseCache {
		private HashMap<String, Object> cache = new HashMap<String, Object>();
		
		@Override
		protected void addToCache(String name, Object value) {
			cache.put(name, value);
		}

		@Override
		protected boolean contains(String name) {
			return cache.containsKey(name);
		}

		@Override
		protected Object getFromCache(String name) {
			return cache.get(name);
		}

		@Override
		protected void removeFromCache(String name) {
			cache.remove(name);
		}

		@Override
		public void clear() {
			cache.clear();
		}

		@Override
		public void timeout() {
			// do nothing for now
		}

		@Override
		protected void clearCache() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void expireItem(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void timeoutCache() {
			// TODO Auto-generated method stub
			
		}
		
	}
}
