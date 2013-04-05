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

	// FIXME !!! - single HashMap !!! cast for convienent methods - can cast and catch for graceful return of null
	// SOOOOO much more simple to have single HashMap - tricky part is serializations
	
	@ElementMap(entry="images", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, SerializableImage> images = new HashMap<String, SerializableImage>();
	@ElementMap(entry="boundingBoxes", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, ArrayList<Rectangle>> boundingBoxes = new HashMap<String, ArrayList<Rectangle>>();
	@ElementMap(entry="points", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, ArrayList<Point2Df>> points = new HashMap<String, ArrayList<Point2Df>>();
	@ElementMap(entry="attributes", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, String> attributes = new HashMap<String, String>();
	
	@Element
	public String name;
	@Element
	String filterName;
	@Element
	private long timestamp;
	
	public OpenCVData(){}
	
	public Set<String> keySet()
	{
		return images.keySet();
	}
	
	public OpenCVData(String name)
	{
		this.timestamp = System.currentTimeMillis();
		this.name = name;
	}
	
	public OpenCVData(SerializableImage image)
	{
		images.put(filterName, image);
	}
	
	
	
	/**
	 * OpenCV VideoProcessor will set this data collection to the last
	 * filtername - when asked for an "image" it will give the last filter's image
	 * as this is defaulted to auto-load into this set
	 * @return
	 */
	public SerializableImage getImage()
	{
		//if (images.containsKey(OUTPUT_KEY))
		if (images.containsKey(filterName))
		{
			return ((SerializableImage)images.get(filterName));
		} else return null;
	}
	
	public SerializableImage getInputImage()
	{
		if (images.containsKey(INPUT_KEY))
		{
			return ((SerializableImage)images.get(INPUT_KEY));
		} else return null;
	}
	
	public SerializableImage put(String key, SerializableImage image)
	{
		return images.put(key, image);
	}
	
	public boolean containsKey(String key)
	{
		return images.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public void add(Rectangle boundingBox) {

		//String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		ArrayList<Rectangle> list;
		if (!boundingBoxes.containsKey(filterName))
		{
			list = new ArrayList<Rectangle>();
			boundingBoxes.put(filterName, list);
		} else {
			list = boundingBoxes.get(filterName);
		}
		
		list.add(boundingBox);
	}
	
	public ArrayList<Rectangle> getBoundingBoxArray()
	{
		/*
		String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY);
		return boundingBoxes.get(key);
		*/
		return boundingBoxes.get(filterName);
	}

	
	public void add(ArrayList<Point2Df> pointArray) {

		//String key = String.format("%s.%s", filterName, KEY_BOUNDING_BOX_ARRAY); overcomplicating it just use filtername
		points.put(filterName, pointArray);
	}
	
	
	public ArrayList<Point2Df> getPointArray()
	{
		//String key = String.format("%s.%s", filterName, KEY_POINT_ARRAY);
		return points.get(filterName);
	}
	
	public void setFilterName(String name) {
		this.filterName = name;
	}

	public ArrayList<Point2Df> set(ArrayList<Point2Df> pointsToPublish) {
		//String key = String.format("%s.%s", filterName, KEY_POINT_ARRAY);
		return points.put(filterName, pointsToPublish);
	}

	public boolean containsAttribute(String name)
	{
		return attributes.containsKey(name);
	}
	
	public String getAttribute(String name)
	{
		return attributes.get(name);
	}
	
	public String putAttribute(String name)
	{
		return attributes.put(name,null);
	}

	public String putAttribute(String name, String value)
	{
		return attributes.put(name,value);
	}

	public HashMap<String, SerializableImage> getImages() {
		return images;
	}

	public ArrayList<Rectangle> put(ArrayList<Rectangle> bb) {
		return boundingBoxes.put(filterName, bb);
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
