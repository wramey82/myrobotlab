package org.myrobotlab.opencv;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

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
	private final static HashMap<String, LinkedBlockingQueue<IplImage>> sources = new HashMap<String, LinkedBlockingQueue<IplImage>>();

	public void put(String name, String filtername, IplImage img) {
		try {
			String key = (String.format("%s.%s", name, filtername));

			if (sources.containsKey(key)) {
				sources.get(key).put(img);
			} else {
				LinkedBlockingQueue<IplImage> q = new LinkedBlockingQueue<IplImage>();
				q.put(img);
				sources.put(key, q);
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
	}
	
	public IplImage get(String name, String filtername) {
		try {
			String key = (String.format("%s.%s", name, filtername));

			if (sources.containsKey(key)) {
				return sources.get(key).take();
			} 
			
		} catch (Exception e) {
			Logging.logException(e);
		}
		
		return null;
	}
	
	
	
}
