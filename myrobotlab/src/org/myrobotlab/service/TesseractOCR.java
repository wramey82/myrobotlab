package org.myrobotlab.service;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;


public class TesseractOCR extends Service {

	private static final long serialVersionUID = 1L;
	

	public final static Logger log = LoggerFactory.getLogger(TesseractOCR.class.getCanonicalName());
	
	public TesseractOCR(String n) {
		super(n, TesseractOCR.class.getCanonicalName());	
		Map<String, String> env = System.getenv();
		File file = new File(".");
		Map<String, String> env1=new HashMap<String,String>(env);
		env1.put("TESSDATA_PREFIX", file.getAbsolutePath());
		setEnv(env);
        //TessAPI INSTANCE = (TessAPI) Native.loadLibrary("libtesseract302", TessAPI.class);
        //System.exit(0);

	}

	@Override
	public String getToolTip() {
		return "Tesseract OCR Engine";
	}
	
	public String OCR(SerializableImage image){
			try {
				String hh=Tesseract.getInstance().doOCR(image.getImage());
				System.out.println(hh);
				return hh;
			} catch (TesseractException e) {
				e.printStackTrace();
			}
			return null;
		
	
	};

	@Override 
	public void stopService()
	{
		super.stopService();
	}
	
	@Override
	public void releaseService()
	{
		super.releaseService();
	}
	
	private static void setEnv(Map<String, String> newenv)
	{
	  try
	    {
	        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
	        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
	        theEnvironmentField.setAccessible(true);
	        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
	        env.putAll(newenv);
	        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
	        theCaseInsensitiveEnvironmentField.setAccessible(true);
	        Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
	        cienv.putAll(newenv);
	    }
	    catch (NoSuchFieldException e)
	    {
	      try {
	        Class[] classes = Collections.class.getDeclaredClasses();
	        Map<String, String> env = System.getenv();
	        for(Class cl : classes) {
	            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
	                Field field = cl.getDeclaredField("m");
	                field.setAccessible(true);
	                Object obj = field.get(env);
	                Map<String, String> map = (Map<String, String>) obj;
	                map.clear();
	                map.putAll(newenv);
	            }
	        }
	      } catch (Exception e2) {
	        e2.printStackTrace();
	      }
	    } catch (Exception e1) {
	        e1.printStackTrace();
	    } 
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		TesseractOCR tesseract= new TesseractOCR("tesseract");
		tesseract.startService();			
		
		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}


}
