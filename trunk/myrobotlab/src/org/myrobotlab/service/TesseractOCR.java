package org.myrobotlab.service;

import net.sourceforge.tess4j.TesseractException;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;


public class TesseractOCR extends Service {

	private static final long serialVersionUID = 1L;
	

	public final static Logger log = LoggerFactory.getLogger(TesseractOCR.class.getCanonicalName());
	
	public TesseractOCR(String n) {
		super(n, TesseractOCR.class.getCanonicalName());	
	}

	@Override
	public String getToolTip() {
		return "Tesseract OCR Engine";
	}
	
	public String OCR(SerializableImage image){
			try {
				return net.sourceforge.tess4j.Tesseract.getInstance().doOCR(image.getImage());
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
