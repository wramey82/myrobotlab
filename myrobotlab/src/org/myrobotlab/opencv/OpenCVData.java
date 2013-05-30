package org.myrobotlab.opencv;

import static org.myrobotlab.opencv.VideoProcessor.INPUT_KEY;

import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.data.Point2Df;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

// FIXME !!! - single HashMap !!! cast for convienent methods - can cast and catch for graceful return of null
// SOOOOO much more simple to have single HashMap - tricky part is serializations

@Root
public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Element
	private int x;
	@Element
	private int y;
	
	// TODO GEOTAG - GPS-TIME OFFSET LAT LONG ALT DIRECTION LOCATION

	@ElementMap(entry="data", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, Object> data = new HashMap<String, Object>();
	
	@Element
	public String name;
	@Element
	String filtername;
	@Element
	private long timestamp;
	
	public OpenCVData(){}
	
	public Set<String> keySet()
	{
		return data.keySet();
	}
	
	public OpenCVData(String name)
	{
		this.timestamp = System.currentTimeMillis();
		this.name = name;
	}
	
	public Integer getWidth()
	{
		return (Integer)data.get(String.format("%s.width", filtername));
	}

	public void setWidth(Integer width)
	{
		data.put(String.format("%s.width", filtername), width);
	}
	
	public Integer getHeight()
	{
		return (Integer)data.get(String.format("%s.height", filtername));
	}

	public void setHeight(Integer height)
	{
		data.put(String.format("%s.height", filtername), height);
	}
	
	/**
	 * OpenCV VideoProcessor will set this data collection to the last
	 * filtername - when asked for an "image" it will give the last filter's image
	 * as this is defaulted to auto-load into this set
	 * @return
	 */
	public SerializableImage getImage()
	{
		//if (data.containsKey(OUTPUT_KEY))
		if (data.containsKey(filtername))
		{
			return ((SerializableImage)data.get(filtername));
		} else return null;
	}
	
	public SerializableImage getInputImage()
	{
		if (data.containsKey(INPUT_KEY))
		{
			return ((SerializableImage)data.get(INPUT_KEY));
		} else return null;
	}
	
	// FIXME - lPlimage needs to do the same thing
	public void put(String key, SerializableImage image)
	{
		
		data.put(key, image);
	}
	
	public boolean containsKey(String key)
	{
		return data.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public void add(Rectangle boundingBox) {

		//String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		ArrayList<Rectangle> list;
		if (!data.containsKey(String.format("%s.boundingboxes", filtername)))
		{
			list = new ArrayList<Rectangle>();
			data.put(String.format("%s.boundingboxes", filtername), list);
		} else {
			list = (ArrayList<Rectangle>)data.get(String.format("%s.boundingboxes", filtername));
		}
		
		list.add(boundingBox);
	}

	
	public void add(ArrayList<Point2Df> pointArray) {

		//String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY); overcomplicating it just use filtername
		data.put(filtername, pointArray);
	}
	
	
	public ArrayList<Point2Df> getPoints()
	{
		return (ArrayList<Point2Df>)data.get(String.format("%s.points", filtername));
	}
	
	public void setFilterName(String name) {
		this.filtername = name;
	}

	public void set(ArrayList<Point2Df> pointsToPublish) {
		data.put(String.format("%s.points", filtername), pointsToPublish);
	}

	public boolean containsAttribute(String name)
	{
		return data.containsKey(String.format("%s.attribute.%s", filtername, name));
	}
	
	public String getAttribute(String name)
	{
		return (String)data.get(String.format("%s.attribute.%s", filtername, name));
	}
	
	public String putAttribute(String name)
	{
		return (String)data.put(String.format("%s.attribute.%s", filtername, name),(Object)null);
	}

	public String putAttribute(String name, String value)
	{
		return (String)data.put(String.format("%s.attribute.%s", filtername, name),value);
	}

	public void put(ArrayList<Rectangle> bb) {
		data.put(String.format("%s.boundingboxes", filtername), bb);
	}
	
	public ArrayList<Rectangle> getBoundingBoxArray()
	{
		return (ArrayList<Rectangle>)data.get(String.format("%s.boundingboxes", filtername));
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
