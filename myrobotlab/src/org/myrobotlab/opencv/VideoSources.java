package org.myrobotlab.opencv;

import java.util.HashMap;
import java.util.Set;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Logging;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * @author GRPERRY
 * 
 *         The single source for all OpenCV Video sources its a key'ed structure
 *         to allow filter to pick off and put on Images in a thread safe way
 *         TODO - create interface
 * 
 */
public class VideoSources {

	// ConcurrentHashMap not needed - filters removed leave the last image !!!
	// :)	
	private final static HashMap<String, IplImage> sources = new HashMap<String, IplImage>();

	public void put(String name, String filtername, SerializableImage img) {
		IplImage iplImage = IplImage.createFrom(img.getImage());
		this.put(name,filtername,iplImage);
	}
	
	public void put(String name, String filtername, IplImage img) {
		try {
			String key = (String.format("%s.%s", name, filtername));		
			sources.put(key, img);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}
	
	public IplImage get(String serviceName, String filtername)
	{
		String key = (String.format("%s.%s", serviceName, filtername));
		return get(key);
	}
	
	public IplImage get(String key) {
		try {
			
			// hmmm is this right?
			if (sources.containsKey(key)) {
				return sources.get(key);//.clone();
			} 
			
		} catch (Exception e) {
			Logging.logException(e);
		}
		
		return null;
	}
	
	public Set<String> getKeySet()
	{
		return sources.keySet();
	}
	
}
