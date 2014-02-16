package org.myrobotlab.opencv;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * @author GRPERRY
 * 
 *         The single source for all OpenCV Video sources its a key'ed structure
 *         to allow filter to pick off and put on Images in a thread safe way
 *         TODO - create interface
 * 
 */
public class VideoSources2 {
	
	public final static Logger log = LoggerFactory.getLogger(VideoSources2.class);
	private long timeout = 1000; 
	private int maxQueue = 2;
	private int errorMod = 1000;
	private int errorCount = 0;

	private final static HashMap<String, LinkedBlockingQueue<IplImage>> sources = new HashMap<String, LinkedBlockingQueue<IplImage>>();
	
	public void put(String name, String filtername, IplImage img) throws InterruptedException {
		
			String key = (String.format("%s.%s", name, filtername));	
			if (sources.containsKey(key)){
				LinkedBlockingQueue<IplImage> queue = sources.get(key);
				if (queue.size() != maxQueue){
					queue.put(img);
				} else {
					if (errorCount % errorMod == 0){
						log.warn(String.format("can not put image on %s - max queue size of %d has been reached", key, maxQueue));
						++errorCount;
					}
				}
				
			} else {
				LinkedBlockingQueue<IplImage> queue = new LinkedBlockingQueue<IplImage>();
				queue.put(img);
				sources.put(key, queue);
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
				//return sources.get(key);//.clone();
				return sources.get(key).poll(timeout , TimeUnit.MILLISECONDS);
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
