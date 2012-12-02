package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ShortBuffer;

import javax.swing.JFrame;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.GeneralException;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.OutArg;
import org.OpenNI.ScriptNode;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.openni.UserTracker;

public class GestureRecognition extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(GestureRecognition.class.getCanonicalName());

	//public UserTracker viewer;

	private OpenNIThread openniThread;
	
	
	private OutArg<ScriptNode> scriptNode;
    private Context context;
    private DepthGenerator depthGen;
    private byte[] imgbytes;
    private float histogram[];
	private static final int IM_WIDTH = 640;
	private static final int IM_HEIGHT = 480;

    

	public GestureRecognition(String n) {
		super(n, GestureRecognition.class.getCanonicalName());
	}

	@Override
	public void loadDefaultConfiguration() {

	}

	@Override
	public String getToolTip() {
		return "used as a general template";
	}

	int width;
	int height;
	private DepthMetaData depthMD;

	
	public void configOpenNI() // FIXME - move most to constructor
	// create context and depth generator
	{
		try {
			context = new Context();

			// add the NITE License
			License license = new License("PrimeSense","0KOIk2JeIBYClPWVnMoRKn5cdY4=");
			// vendor, key
			context.addLicense(license);

			depthGen = DepthGenerator.create(context);
			depthMD = depthGen.getMetaData();

			MapOutputMode mapMode = new MapOutputMode(IM_WIDTH, IM_HEIGHT, 30);
			// xRes, yRes, FPS
			depthGen.setMapOutputMode(mapMode);

			// set Mirror mode for all
			context.setGlobalMirror(true);

			context.startGeneratingAll();
			
            histogram = new float[10000];
            width = depthMD.getFullXRes();
            height = depthMD.getFullYRes();
            
            imgbytes = new byte[width*height];
            
            DataBufferByte dataBuffer = new DataBufferByte(imgbytes, width*height);
            Raster raster = Raster.createPackedRaster(dataBuffer, width, height, 8, null);
            bimg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            bimg.setData(raster);

			System.out.println("Started context generating...");

//			depthMD = depthGen.getMetaData();
			// use depth metadata to access depth info (avoids bug with
			// DepthGenerator)
		} catch (Exception e) {
			logException(e);
		}
	} // end of configOpenNI()
	
	@Override
	public void startService() {
		super.startService();
		configOpenNI();
	}

	SerializableImage simg = new SerializableImage();
    private BufferedImage bimg;

	
	class OpenNIThread extends Thread {

		private boolean shouldRun = true;

		public OpenNIThread(String string) {
			super(string);

		}

		public void run() {
			while (shouldRun) {
				updateDepth();  // viewer.updateDepth()
//				viewer.repaint(); // publish
				
			     DataBufferByte dataBuffer = new DataBufferByte(imgbytes, width*height);
			     Raster raster = Raster.createPackedRaster(dataBuffer, width, height, 8, null);
			     bimg.setData(raster);
			     simg.setImage(bimg);				
				invoke("publishFrame", simg);
			}
			//frame.dispose();
		}

	}
	
	public void capture()
	{
		if (openniThread != null)
		{
			openniThread.shouldRun = false;
		}
		
		openniThread = new OpenNIThread("openniThread");
		openniThread.start();
		
	}
	
	public SerializableImage publishFrame(SerializableImage frame)
	{
		return frame;
	}
	

	public void stopCapture()
	{
		openniThread.shouldRun = false;
		openniThread = null;
	}
    
    private void calcHist(DepthMetaData depthMD)
    {
        // reset
        for (int i = 0; i < histogram.length; ++i)
            histogram[i] = 0;
        
        ShortBuffer depth = depthMD.getData().createShortBuffer();
        depth.rewind();

        int points = 0;
        while(depth.remaining() > 0)
        {
            short depthVal = depth.get();
            if (depthVal != 0)
            {
                histogram[depthVal]++;
                points++;
            }
        }
        
        for (int i = 1; i < histogram.length; i++)
        {
            histogram[i] += histogram[i-1];
        }

        if (points > 0)
        {
            for (int i = 1; i < histogram.length; i++)
            {
                histogram[i] = (int)(256 * (1.0f - (histogram[i] / (float)points)));
            }
        }
    }


	
    void updateDepth()
    {
        try {
            DepthMetaData depthMD = depthGen.getMetaData();

            context.waitAnyUpdateAll();
            
            calcHist(depthMD);
            ShortBuffer depth = depthMD.getData().createShortBuffer();
            depth.rewind();
            
            while(depth.remaining() > 0)
            {
                int pos = depth.position();
                short pixel = depth.get();
                imgbytes[pos] = (byte)histogram[pixel];
            }
        } catch (GeneralException e) {
            e.printStackTrace();
        }
    }

	public static void main(String s[]) {
		org.apache.log4j.BasicConfigurator.configure();

		GestureRecognition gr = new GestureRecognition("gr");
		gr.startService();
		
		GUIService gui = new GUIService("gui");
		gui.startService();
		
		/*
		 * JFrame f = new JFrame("OpenNI User Tracker"); f.addWindowListener(new
		 * WindowAdapter() { public void windowClosing(WindowEvent e)
		 * {System.exit(0);} }); UserTrackerApplication app = new
		 * UserTrackerApplication(f);
		 * 
		 * app.viewer = new UserTracker(); f.add("Center", app.viewer);
		 * f.pack(); f.setVisible(true); app.run();
		 */
	}

}
