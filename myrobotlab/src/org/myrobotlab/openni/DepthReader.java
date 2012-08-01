package org.myrobotlab.openni;

// DepthReader.java
// Andrew Davison, September 2011, ad@fivedots.psu.ac.th

/* Based on ViewerPanel (version 2) and on OpenNI's SimpleViewer example

   Wait for an update to the Kinect's depth map, then pass it to
   a PointsShape object for rendering in 3D.
*/

import java.nio.ShortBuffer;

import org.OpenNI.Context;
import org.OpenNI.DepthGenerator;
import org.OpenNI.DepthMetaData;
import org.OpenNI.License;
import org.OpenNI.MapOutputMode;
import org.OpenNI.StatusException;




public class DepthReader implements Runnable
{
  /* resolution of depth image; the same values 
     should be used in PointsShape */
  private static final int IM_WIDTH = 640;  
  private static final int IM_HEIGHT = 480;  


  private volatile boolean isRunning;
  
  // OpenNI
  private Context context;
  private DepthMetaData depthMD;

  private PointsShape ptsShape;   
            // renders the depth points in the Java 3D scene


  public DepthReader(PointsShape pps)
  {
    ptsShape = pps;
    configOpenNI();
    new Thread(this).start();   // start updating the depth info
  } // end of DepthReader()




  private void configOpenNI()
  // create context and depth generator
  {
    try {
      context = new Context();
      
      // add the NITE License 
      License license = new License("PrimeSense", "0KOIk2JeIBYClPWVnMoRKn5cdY4=");   
                                       // vendor, key
      context.addLicense(license); 
      
      DepthGenerator depthGen = DepthGenerator.create(context);

      MapOutputMode mapMode = new MapOutputMode(IM_WIDTH, IM_HEIGHT, 30);   
                                                        // xRes, yRes, FPS
      depthGen.setMapOutputMode(mapMode); 
      
      // set Mirror mode for all 
      context.setGlobalMirror(true);

      context.startGeneratingAll(); 
      System.out.println("Started context generating..."); 

      depthMD = depthGen.getMetaData();
           // use depth metadata to access depth info (avoids bug with DepthGenerator)
    } 
    catch (Exception e) {
      System.out.println(e);
      System.exit(1);
    }
  }  // end of configOpenNI()



  public void run()
  /* wait for the next depth map update, then pass it to ptsShape
  */
  {
    isRunning = true;
    while (isRunning) {
      try {
        context.waitAnyUpdateAll();
      }
      catch(StatusException e)
      {  System.out.println(e); 
         System.exit(1);
      }

      ShortBuffer depthBuf = depthMD.getData().createShortBuffer();
      ptsShape.updateDepthCoords(depthBuf);   
            // this call will not return until the 3D scene has been updated
    }
    // close down
    try {
      context.stopGeneratingAll();
    }
    catch (StatusException e) {}
    context.release();
    System.exit(0);
  }  // end of run()



  public void closeDown()
  {  isRunning = false;  } 


} // end of DepthReader class

