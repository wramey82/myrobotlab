package org.myrobotlab.opencv;

import static org.myrobotlab.opencv.VideoProcessor.INPUT_KEY;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.data.Point2Df;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * This is the data returned from a single pass of an OpenCV pipeline of
 * filters. The amount of data can be changed depending on individual
 * configuration of the filters. The filters had some limited ability to add a
 * copy of the image and add other data structures such as arrays of point,
 * bounding boxes, masks and other information.
 * 
 * The default behavior is to return the INPUT image from the start of the
 * pipeline, all non-image data - bounding boxes, points etc and a refrence to
 * the last image coming from the pipeline
 * 
 * Do to thread restrictions and serializable capabilities only NON-JAVCV
 * objects will be used !!! Preferably, no Swing either as not all JVMs support
 * swing (Android, etc)
 * 
 * @author GroG
 * 
 */
@Root
public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;

	// TODO GEOTAG - GPS-TIME OFFSET LAT LONG ALT DIRECTION LOCATION
	// TODO - base keys for Ilpimages - serializable = .image keys
	// TODO - removeIlpimages() strips all
	// TODO - different forms of serialization

	@ElementMap(entry = "data", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, Object> data = new HashMap<String, Object>();

	/**
	 * name of the service which produced this data
	 */
	@Element
	public String name;
	/**
	 * the filter's name - used as a key to get or put data associated with a
	 * specific filter
	 */
	@Element
	String filtername;
	@Element
	private long timestamp;

	/**
	 * constructed by the 'name'd service
	 * 
	 * @param name
	 */
	public OpenCVData(String name) {
		this.timestamp = System.currentTimeMillis();
		this.name = name;
	}

	/**
	 * sets the key - used to access the various data of a particular filter -
	 * first set the filter name the access images, points, etc
	 * 
	 * @param name
	 */
	public void setFilterName(String name) {
		this.filtername = name;
	}

	/**
	 * used by VideoProcessor to initialize this object for a particular filter
	 * 
	 * @param filter
	 */
	public void setFilter(OpenCVFilter filter) {
		this.filtername = filter.name;
		data.put(String.format("%s.class", filtername), filter.getClass().getSimpleName());
	}

	/**
	 * basic HashMap functionality
	 * 
	 * @return
	 */
	public Set<String> keySet() {
		return data.keySet();
	}

	public void setWidth(Integer width) {
		data.put(String.format("%s.width", filtername), width);
	}

	public Integer getWidth() {
		return (Integer) data.get(String.format("%s.width", filtername));
	}

	public void setHeight(Integer height) {
		data.put(String.format("%s.height", filtername), height);
	}

	public Integer getHeight() {
		return (Integer) data.get(String.format("%s.height", filtername));
	}

	/**
	 * OpenCV VideoProcessor will set this data collection to the last
	 * filtername - when asked for an "image" it will give the last filter's
	 * image as this is defaulted to auto-load into this set
	 * 
	 * @return
	 */
	public SerializableImage getImage(String key) {
		return ((SerializableImage) data.get(key));
	}

	/**
	 * paramaterless tries to retrieve image based on current filtername
	 * 
	 * @return
	 */
	public SerializableImage getImage() {
		return getImage(filtername);
	}

	/**
	 * get the original "camera" image - or the image which started the pipeline
	 * 
	 * @return
	 */
	public SerializableImage getInputImage() {
		return getImage(String.format("%s.%s", name, INPUT_KEY));
	}

	/**
	 * the only reference to IplImage - since it loads data it "should" not be
	 * used after this object is published keeping it "safe" on systems where
	 * IplImage is not available (hack)
	 * 
	 * @param key
	 * @param image
	 */
	public void put(String name, String key, IplImage image) {
		this.name = name;
		data.put(String.format("%s.%s", name, key), new SerializableImage(image.getBufferedImage(), filtername));
	}

	public boolean containsKey(String key) {
		return data.containsKey(key);
	}

	// // -----------continue------------------
	@SuppressWarnings("unchecked")
	public void add(Rectangle boundingBox) {

		// String key = String.format("%s.%s", filterName,
		// KEY_BOUNDING_BOX_ARRAY);
		ArrayList<Rectangle> list;
		if (!data.containsKey(String.format("%s.boundingboxes", filtername))) {
			list = new ArrayList<Rectangle>();
			data.put(String.format("%s.boundingboxes", filtername), list);
		} else {
			list = (ArrayList<Rectangle>) data.get(String.format("%s.boundingboxes", filtername));
		}

		list.add(boundingBox);
	}

	public ArrayList<Rectangle> getBoundingBoxArray() {
		return (ArrayList<Rectangle>) data.get(String.format("%s.boundingboxes", filtername));
	}
	
	public void set(ArrayList<Point2Df> pointsToPublish) {
		data.put(String.format("%s.points", filtername), pointsToPublish);
	}

	public ArrayList<Point2Df> getPoints() {
		return (ArrayList<Point2Df>) data.get(String.format("%s.points", filtername));
	}
	
	public Point2Df getFirstPoint() {
		ArrayList<Point2Df> points = (ArrayList<Point2Df>) data.get(String.format("%s.points", filtername));
		if (points != null && points.size() > 0)
			return points.get(0);
		return null;
	}

	public boolean containsAttribute(String name) {
		return data.containsKey(String.format("%s.attribute.%s", filtername, name));
	}

	public String getAttribute(String name) {
		return (String) data.get(String.format("%s.attribute.%s", filtername, name));
	}

	public String putAttribute(String name) {
		return (String) data.put(String.format("%s.attribute.%s", filtername, name), (Object) null);
	}

	public String putAttribute(String name, String value) {
		return (String) data.put(String.format("%s.attribute.%s", filtername, name), value);
	}

	public void put(ArrayList<Rectangle> bb) {
		data.put(String.format("%s.boundingboxes", filtername), bb);
	}

	public Integer getX() {
		return (Integer) data.get(String.format("%s.x", filtername));
	}

	public void setX(int x) {
		data.put(String.format("%s.x", filtername), x);
	}

	public Integer getY() {
		return (Integer) data.get(String.format("%s.y", filtername));
	}

	public void setY(int y) {
		data.put(String.format("%s.y", filtername), y);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public ArrayList<SerializableImage> crop() {
		return cropBoundingBoxArray(String.format(filtername));
	}

	public ArrayList<SerializableImage> cropBoundingBoxArray() 
	{
		return cropBoundingBoxArray(filtername);
	}
	public ArrayList<SerializableImage> cropBoundingBoxArray(String key) {
		SerializableImage img = getImage(key);
		ArrayList<Rectangle> bbxs = getBoundingBoxArray();
		ArrayList<SerializableImage> ret = new ArrayList<SerializableImage>();
		if (bbxs != null) {
			for (int i = 0; i < bbxs.size(); ++i) {
				Rectangle r = bbxs.get(i);
				//ret.add(new SerializableImage(img.getImage().getSubimage(r.x, r.y, r.width, r.height), filtername));
				ret.add(new SerializableImage(deepCopy(img.getImage()).getSubimage(r.x, r.y, r.width, r.height), filtername));
			}
		}
		return ret;
	}
	
	static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
	public ArrayList<SerializableImage> cropPoints()
	{
		return cropPoints();
	}
	
	public ArrayList<SerializableImage> cropPoints(String key) {
		SerializableImage img = getImage(key);
		ArrayList<Point2Df> pts = getPoints();
		ArrayList<SerializableImage> ret = new ArrayList<SerializableImage>();
		int x = 0;
		int y = 0;
		if (pts != null) {
			for (int i = 0; i < pts.size(); ++i) {
				Point2Df p = pts.get(i);
				x = (int)(p.x * (float)img.getWidth() -  55);
				y = (int)(p.y * (float)img.getWidth() -  55);
				ret.add(new SerializableImage(img.getImage().getSubimage(x,y,x+(2*55),y+(2*55)), filtername));
			}
		}
		return ret;
	}


}
