package org.myrobotlab.opencv;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.image.SerializableImage;

public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;

	// sub-keys
	public static final String KEY_IMAGE = "KEY_IMAGE";
	public static final String KEY_IPLIMAGE = "KEY_IPLIMAGE";
	public static final String KEY_BOUNDING_BOX_ARRAY = "KEY_BOUNDING_BOX_ARRAY";
	
	private HashMap<String, Object> data = new HashMap<String, Object>();
	String filterName;
	
	long timestamp;
	
	public OpenCVData()
	{
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

		String key = String.format("%s_%s", filterName, KEY_BOUNDING_BOX_ARRAY);
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
		String key = String.format("%s_%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		return (ArrayList<Rectangle>)data.get(key);
	}

	public void setFilterName(String name) {
		this.filterName = name;
	}

}
