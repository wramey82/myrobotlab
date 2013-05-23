package org.myrobotlab.service;

import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.sun.jna.Native;


public class TesseractOCR extends Service {

	private static final long serialVersionUID = 1L;
	

	public final static Logger log = LoggerFactory.getLogger(TesseractOCR.class.getCanonicalName());
	
	public TesseractOCR(String n) {
		super(n, TesseractOCR.class.getCanonicalName());	
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
