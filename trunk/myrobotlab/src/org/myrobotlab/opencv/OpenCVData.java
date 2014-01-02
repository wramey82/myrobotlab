package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_highgui.cvEncodeImage;
import static org.myrobotlab.opencv.VideoProcessor.INPUT_KEY;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.imageio.ImageIO;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.data.Rectangle;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_legacy.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;


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
 * choices of images are "by filter name", the "input", the display, and the "last filter" == "output"
 * choices of return types are IplImage, CVMat, BufferedImage, ByteBuffer, ByteArrayOutputStream, byte[
 * 
 * method naming conventions (get|set) (display | input | filter) (format - IplImage=image CVMat | BufferedImage | ByteBuffer | Bytes
 * 
 * @author GroG
 * 
 */
@Root
public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(OpenCVData.class);

	@ElementMap(entry = "data", key = "key", value = "data", attribute = true, inline = true, required = false)
	private HashMap<String, Object> data = new HashMap<String, Object>();

	/**
	 * name of the service which produced this data
	 */
	@Element
	public final String service;
	/**
	 * the filter's name - used as a key to get or put data associated with a
	 * specific filter
	 */
	@Element
	private String filter;
	
	/**
	 * display filter name
	 */
	private String display;
	@Element
	private long timestamp;
	
	public int frameIndex;

	/**
	 * constructed by the 'name'd service
	 * 
	 * @param service
	 */
	public OpenCVData(String service) {
		this(service, 0, null);
	}

	public OpenCVData(String service, int frameIndex, String display) {
		this.timestamp = System.currentTimeMillis();
		this.frameIndex = frameIndex;
		this.service = service;
		this.display = display;
	}

	/**
	 * sets the key - used to access the various data of a particular filter -
	 * first set the filter name the access images, points, etc
	 * 
	 * @param name
	 */
	public void setFilterName(String name) {
		this.filter = name;
	}
	
	public void setFilter(OpenCVFilter inFilter) {
		this.filter = inFilter.name;
		data.put(String.format("%s.class", filter), inFilter.getClass().getCanonicalName());
	}
	
	public String getFilterName(){
		return filter;
	}

	public void setWidth(Integer width) {
		data.put(String.format("%s.width", filter), width);
	}

	public Integer getWidth() {
		return (Integer) data.get(String.format("%s.width", filter));
	}

	public void setHeight(Integer height) {
		data.put(String.format("%s.height", filter), height);
	}

	public Integer getHeight() {
		return (Integer) data.get(String.format("%s.height", filter));
	}

	// -------- IplImage begin ----------------
	
	/**
	 * OpenCV VideoProcessor will set this data collection to the last
	 * filtername - when asked for an "image" it will give the last filter's
	 * 
	 * @return the filter's IplImage
	 */
		
	public IplImage getImage(String filtername) {
		return ((IplImage) data.get(String.format("%s", filtername)));
	}

	/**
	 * paramaterless tries to retrieve image based on current filtername
	 * 
	 * @return
	 */
	public IplImage getImage() {
		return getImage(filter);
	}

	/**
	 * get the original "camera" image - or the image which started the pipeline
	 * 
	 * @return
	 */
	public IplImage getInputImage() {
		return getImage(String.format("%s", INPUT_KEY));
	}
	
	public void put(String key, IplImage image) {
		data.put(String.format("%s", key), image);
	}
	// -------- IplImage end ----------------

	// -------- BufferedImage begin ----------------
	public BufferedImage getBufferedImageDisplay()
	{
		return getBufferedImage(display);
	}
	
	public BufferedImage getBufferedImage()
	{
		return getBufferedImage(filter);
	}
	
	public BufferedImage getBufferedImage(String filterName){
		if (data.containsKey(String.format("%s.BufferedImage", filterName)))
		{
			return (BufferedImage) data.get(String.format("%s.BufferedImage", filterName));
		} else {
			IplImage img = getImage(filterName);
			if (img == null) return null;
			
			BufferedImage image = img.getBufferedImage();
			data.put(String.format("%s.BufferedImage", filterName), image);
			return image;
		}
	}

	// -------- ByteBuffer begin ----------------
	public ByteBuffer getByteBufferImage(String filtername){
		IplImage img = getImage(filtername);
		return img.getByteBuffer();
	}
	// -------- ByteBuffer end ----------------

	// -------- JPG to file begin ----------------
	public String writeImage()
	{
		return writeImage(filter);
	}
	
	public String writeDisplay()
	{
		return writeImage(display);
	}

	public String writeInput()
	{
		return writeImage(INPUT_KEY);
	}
	
	public String writeImage(String filter){
		String filename = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			BufferedImage bi = getBufferedImage(filter);
			if (bi == null) return null;
			// FIXME OPTIMIZE - USE CONVERT & OPENCV !!!
			ImageIO.write(bi , "jpg", baos);
			filename = String.format("%s.%s.%d.jpg", service, filter, frameIndex );
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(baos.toByteArray());
			fos.close();
			
		} catch (IOException e) {
			Logging.logException(e);
		}
		
		return filename;
	}
	
	// FIXME FIXME FIXME - always push result back into data structure
	public byte[] getJPGBytes(String filterName)
	{
		CvMat mat = getJPG(filterName);

		ByteBuffer byteBuffer = mat.getByteBuffer(); 
		byte[] barray = new byte[byteBuffer.remaining()]; 
		byteBuffer.get(barray); 
		return barray;
	}
	
	public ByteBuffer getJPGByteBuffer(String filterName)
	{
		CvMat mat = getJPG(filterName);
		ByteBuffer byteBuffer = mat.getByteBuffer(); 
		return byteBuffer;
	}
	
	public CvMat getJPG(String filterName)
	{
		// FIXME FIXME FIXME - before doing ANY CONVERSION EVER - ALWAYS CHECK CACHE !!
		CvMat mat = getEncoded(filterName, ".jpg");
		return mat;
	}
	
	public CvMat getEncoded(String filterName, String encoding){
		
		// should you go to CvMat ?? - or ByteBuffer ???
		if (data.containsKey(String.format("%s.%s", filterName, encoding)))
		{
			return (CvMat) data.get(String.format("%s.%s", filterName, encoding));
		} else {
			IplImage img = getImage(filterName);
			if (img == null) return null;
			
			try {
				String e = encoding.toLowerCase();
				CvMat encodedImg = cvEncodeImage(e, img);
				return encodedImg;
				/*
				 * 
				ByteBuffer byteBuffer = encodedImg.getByteBuffer(); 
				byte[] barray = new byte[byteBuffer.remaining()]; 
				byteBuffer.get(barray); 
				log.info(String.format("%d size", barray.length));
				 
				FileOutputStream fos = new FileOutputStream("memoryEncoded.jpg");
				fos.write(barray);
				fos.close();
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bos.write(encodedImg.data_ptr().getStringBytes());
				byte[] b = bos.toByteArray();
				log.info("%d size", barray.length);
				*/

			} catch(Exception e) {
				Logging.logException(e);
			}
			cvSaveImage("direct.jpg", img);
			cvSaveImage("direct.png", img);
			
			
		/*	
			ByteBuffer bb = encodedImg.asByteBuffer();
			
			byte[] b = new byte[bb.remaining()];
			bb.get(b);
			
			data.put(String.format("%s.JPG", filterName), b);
			*/
			return null;
		}
		
	}
	
	// -------- JPG to file end ----------------
	// -------- HashMap begin ----------------

	public boolean containsKey(String key) {
		return data.containsKey(key);
	}

	public Set<String> keySet() {
		return data.keySet();
	}
	// -------- HashMap end ----------------
	
	// // -----------continue------------------
	@SuppressWarnings("unchecked")
	public void add(Rectangle boundingBox) {

		// String key = String.format("%s.%s", filterName,
		// KEY_BOUNDING_BOX_ARRAY);
		ArrayList<Rectangle> list;
		if (!data.containsKey(String.format("%s.boundingboxes", filter))) {
			list = new ArrayList<Rectangle>();
			data.put(String.format("%s.boundingboxes", filter), list);
		} else {
			list = (ArrayList<Rectangle>) data.get(String.format("%s.boundingboxes", filter));
		}

		list.add(boundingBox);
	}

	public ArrayList<Rectangle> getBoundingBoxArray() {
		return (ArrayList<Rectangle>) data.get(String.format("%s.boundingboxes", filter));
	}
	
	public void set(ArrayList<Point2Df> pointsToPublish) {
		data.put(String.format("%s.points", filter), pointsToPublish);
	}

	public ArrayList<Point2Df> getPoints() {
		return (ArrayList<Point2Df>) data.get(String.format("%s.points", filter));
	}
	
	public Point2Df getFirstPoint() {
		ArrayList<Point2Df> points = (ArrayList<Point2Df>) data.get(String.format("%s.points", filter));
		if (points != null && points.size() > 0)
			return points.get(0);
		return null;
	}

	public boolean containsAttribute(String name) {
		return data.containsKey(String.format("%s.attribute.%s", filter, name));
	}

	public String getAttribute(String name) {
		return (String) data.get(String.format("%s.attribute.%s", filter, name));
	}

	public String putAttribute(String name) {
		return (String) data.put(String.format("%s.attribute.%s", filter, name), (Object) null);
	}

	public String putAttribute(String name, String value) {
		return (String) data.put(String.format("%s.attribute.%s", filter, name), value);
	}

	public void put(ArrayList<Rectangle> bb) {
		data.put(String.format("%s.boundingboxes", filter), bb);
	}

	public Integer getX() {
		return (Integer) data.get(String.format("%s.x", filter));
	}

	public void setX(int x) {
		data.put(String.format("%s.x", filter), x);
	}

	public Integer getY() {
		return (Integer) data.get(String.format("%s.y", filter));
	}

	public void setY(int y) {
		data.put(String.format("%s.y", filter), y);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/*
	public ArrayList<SerializableImage> crop() {
		return cropBoundingBoxArray(String.format(filtername));
	}
	*/

	/*
	public ArrayList<SerializableImage> cropBoundingBoxArray() 
	{
		return cropBoundingBoxArray(filtername);
	}
	*/
	
	/*
	public ArrayList<IplImage> cropBoundingBoxArray(String key) {
		IplImage img = getImage(key);
		ArrayList<Rectangle> bbxs = getBoundingBoxArray();
		ArrayList<SerializableImage> ret = new ArrayList<SerializableImage>();
		if (bbxs != null) {
			for (int i = 0; i < bbxs.size(); ++i) {
				Rectangle r = bbxs.get(i);
				//ret.add(new SerializableImage(img.getImage().getSubimage(r.x, r.y, r.width, r.height), filtername));
				// expand to use pixel values - 
				int width = img.width();
				int height = img.height();
				int sx = (int)(r.x * width);
				int sy = (int)(r.y * height);
				int swidth = (int)(r.width * width);
				int sheight = (int)(r.height * height);
				ret.add(new SerializableImage(deepCopy(img.getImage()).getSubimage(sx, sy, swidth, sheight), filtername));
			}
		}
		return ret;
	}
	*/
	
	static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
	/*
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
	*/

	/*
	public void saveToDirectory(String folderName) {
		File f = new File(folderName);
		f.mkdirs();
		for (Map.Entry<String, Object> d : data.entrySet()) {
			// Map.Entry<String,SerializableImage> pairs = o;
			String key = d.getKey();
			Object o = d.getValue();
			log.error(String.format("saving %s of type %s", key, o.getClass().getSimpleName()));
			try {
			if (o.getClass() == SerializableImage.class)
			{
				SerializableImage img = (SerializableImage)o;
				String imageFile = String.format("%s%s%d.%s.png", folderName, File.separator, timestamp, img.getSource());
				ImageIO.write(img.getImage(), "png",new File(imageFile));
			} else if (o.getClass() == ArrayList.class){
				
				// FIXME - not exact
				ArrayList<SerializableImage> dump = crop();
				for (int i = 0; i < dump.size(); ++i)
				{
					SerializableImage img = dump.get(i);
					String imageFile = String.format("%s%s%d.%s.%d.png", folderName, File.separator, timestamp, img.getSource(), i);
					ImageIO.write(img.getImage(), "png",new File(imageFile));
				}
			}
			} catch (Exception e) {
				Logging.logException(e);
			}
		}
		
	}
*/
	
	public String getDisplayName() {
		return display;
	}

	public void setDisplayName(String displayname) {
		this.display = displayname;
	}


}
