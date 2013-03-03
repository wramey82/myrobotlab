package org.myrobotlab.opencv;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.data.Point2Df;

public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;

	// sub-keys
	public static final String KEY_IMAGE = "KEY_IMAGE";
	public static final String KEY_IPLIMAGE = "KEY_IPLIMAGE";
	public static final String KEY_BOUNDING_BOX_ARRAY = "KEY_BOUNDING_BOX_ARRAY";
	public static final String KEY_POINT_ARRAY = "KEY_POINT_ARRAY";
	
	private HashMap<String, Object> data = new HashMap<String, Object>();
	public String name;
	String filterName;
	
	long timestamp;
	
	public OpenCVData()
	{
		
	}
	
	public Set<String> keySet()
	{
		return data.keySet();
	}
	
	public OpenCVData(String name)
	{
		this.name = name;
	}
	
	public OpenCVData(SerializableImage image)
	{
		data.put(KEY_IMAGE, image);
	}
	
	public SerializableImage getImage()
	{
		return (SerializableImage)data.get(KEY_IMAGE);
	}
	
	public Object put(String key, Object object)
	{
		return data.put(key, object);
	}
	
	public boolean containsKey(String key)
	{
		return data.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public void add(Rectangle boundingBox) {

		String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		ArrayList<Rectangle> list;
		if (!data.containsKey(key))
		{
			list = new ArrayList<Rectangle>();
			data.put(key, list);
		} else {
			list = (ArrayList<Rectangle>)data.get(key);
		}
		
		list.add(boundingBox);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Rectangle> getBoundingBoxArray()
	{
		String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		return (ArrayList<Rectangle>)data.get(key);
	}

	
	public void add(ArrayList<Point2Df> pointArray) {

		String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		data.put(key, pointArray);
	}
	
	
	@SuppressWarnings("unchecked")
	public ArrayList<Point2Df> getPointArray()
	{
		String key = String.format("%s.%s", filterName, KEY_POINT_ARRAY);
		return (ArrayList<Point2Df>)data.get(key);
	}
	
	public void setFilterName(String name) {
		this.filterName = name;
	}

	public void set(ArrayList<Point2Df> pointsToPublish) {
		String key = String.format("%s.%s", filterName, KEY_POINT_ARRAY);
		data.put(key, pointsToPublish);
	}

	public SerializableImage getInputImage() {
		return (SerializableImage)data.get(KEY_IMAGE);
	}

}
