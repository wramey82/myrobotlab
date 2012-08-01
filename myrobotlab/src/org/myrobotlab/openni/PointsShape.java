package org.myrobotlab.openni;

// PointsShape.java
// Andrew Davison, September 2011, ad@fivedots.coe.psu.ac.th

/* PointsShape is a Java 3D shape which is drawn as a collection
   of colored points stored in a PointsArray. These points are
   calculated from the Kinect's current depth buffer.

   The points' coordinates and colors are represented
   by two arrays: coords[] and colors[]

   The points are stored in the PointArray as a BY_REFERENCE geometry,
   which means that only the coords[] and colors[] arrays need
   to be changed in order to affect the PointArray. Once changed, Java 3D
   automatically redraws the PointArray in the 3D scene.

   When a new depth buffer is passed to updateDepthCoords(), a request
   is made to Java 3D to update the PointArray, which is does by calling
   updateData() which updates the coords[] and colors[] arrays.

   PointsShape implements GeometryUpdater so it can update
   the PointArray by having the system call it's updateData() method.

   The mapping from 8-bits to colour is done
   using the ColorUtils library methods 
   (http://code.google.com/p/colorutils/)
*/


import java.awt.Color;
import java.awt.Point;
import java.nio.ShortBuffer;
import java.util.concurrent.Semaphore;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;

import edu.scripps.fl.color.ColorUtils;



public class PointsShape extends Shape3D implements GeometryUpdater
{
  // resolution of depth image; change to match setting in DepthReader
  private static final int IM_WIDTH = 640;  
  private static final int IM_HEIGHT = 480;  

  /* display volume for particles inside the 3D scene; arrived at 
     by trial-and-error testing to see what looked 'good' in the scene 
  */
  private static final int X_WIDTH = 12;
  private static final int Y_WIDTH = 6;
  private static final int Z_WIDTH = 50;

  private static final int SAMPLE_FREQ = 10;
              // the gap between depth positions being sampled

  private static final int MAX_POINTS = 32000;
      /* make sure that MAX_POINTS*SAMPLE_FREQ >= IM_WIDTH*IM_HEIGHT
         otherwise the coords[] array will not be 
         big enough for all the sampled points
      */

  private final static int POINT_SIZE = 3;
  private static final int NUM_COLORS = 256;  


  private float xScale, yScale, zScale;
      // scaling from Kinect coords to 3D scene coords

  // for coloring the points (created with the ColorUtils library)
  private Color[] colorMap;

  private ShortBuffer depthBuf;

  private PointArray pointParts;     // Java 3D geometry holding the points 
  private float[] coords, colors;    // holds (x,y,z) and (R,G,B) of the points

  private Semaphore sem;    
     /* used to make updateDepthCoords() wait until 
        GeometryUpdater.updateData() has finished an update
     */


  public PointsShape() 
  {
    // BY_REFERENCE PointArray storing coordinates and colors
    pointParts = new PointArray(MAX_POINTS, 
				PointArray.COORDINATES | PointArray.COLOR_3 |
                                    PointArray.BY_REFERENCE );


    // the data structure can be read and written at run time
    pointParts.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
    pointParts.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

    // colorMap = ColorUtils.getRedtoBlueSpectrum(NUM_COLORS);
    // colorMap = ColorUtils.getRedtoYellowSpectrum(NUM_COLORS);
    // colorMap = ColorUtils.getColorsBetween(Color.RED, Color.GREEN, NUM_COLORS);
    colorMap = ColorUtils.getSpectrum(NUM_COLORS, 0, 1);

    // calculate x- and y- scaling from Kinect coords to 3D scene coords
    xScale = ((float)X_WIDTH)/IM_WIDTH;
    yScale = ((float)Y_WIDTH)/IM_HEIGHT;

    sem = new Semaphore(0);  

    // create PointsShape geometry and appearance
    createGeometry();
    createAppearance();
  } // end of PointsShape()



  private void createGeometry()
  /* Create and initialize coords and colors arrays for the depth
     points. Only sample every SAMPLE_FREQ point to reduce the arrays size.
     Each point requires 3 floats in the coords array (x, y, z) and
     3 floats in the colours array (R, G, B)

     The z-coordinates will change as the depths change, which will cause the
     points colors to change as well.
  */
  { 
    if (MAX_POINTS*SAMPLE_FREQ < IM_WIDTH*IM_HEIGHT) {
      System.out.println("Warning: coords[] is too small -");
      System.out.println("  some depth info will be lost");
    }

    coords = new float[MAX_POINTS*3];    // for (x,y,z) coords of a point
    colors = new float[MAX_POINTS*3];    // to store each a point's color (RGB)

    // initialize the two arrays
    int ptsCount = 0;
    for (int dpIdx=0; dpIdx < IM_WIDTH*IM_HEIGHT; dpIdx++) {
      if (dpIdx%SAMPLE_FREQ == 0) {    // only look at depth index that is to be sampled
        int ptIdx = (dpIdx/SAMPLE_FREQ)*3;   // calc point index
        if (ptIdx < MAX_POINTS*3) {   // is there enough space?
          Point pt = depthIdx2Point(dpIdx);
          coords[ptIdx] = pt.x * xScale;     // x coord
          coords[ptIdx+1] = pt.y * yScale;   // y coord
          coords[ptIdx+2] = 0f;              // z coord (will change later)

          // initial point colour is white (will change later)
          colors[ptIdx] = 1.0f;  
          colors[ptIdx+1] = 1.0f; 
          colors[ptIdx+2] = 1.0f;

          ptsCount++;
        }
      }
    }
    System.out.println("Initialized " + ptsCount + " points");

    // store the coordinates and colours in the PointArray
    pointParts.setCoordRefFloat(coords);    // use BY_REFERENCE
    pointParts.setColorRefFloat(colors);

    /* PointsShape is drawn as the collection
       of colored points stored in the PointsArray. */
    setGeometry(pointParts);
  }  // end of createGeometry()


  private Point depthIdx2Point(int depthIdx)
  /* convert index position in 1D depth buffer into 
     2D (x,yUp) point position, assuming a IM_WIDTH*IM_HEIGHT
     image dimensions, with x- axis across and y-axis **UP**
  */
  {
    int x = depthIdx%IM_WIDTH;
    int yUp = (IM_HEIGHT-1) - (depthIdx/IM_WIDTH);    // so y is up the axis 
    return new Point(x, yUp);
  }  // end of depthIdx2Point()



  private void createAppearance()
  {
    Appearance app = new Appearance();

    PointAttributes pa = new PointAttributes();
    pa.setPointSize( POINT_SIZE );  // fix point size
    app.setPointAttributes(pa);

    setAppearance(app);
  }  // end of createAppearance()


  public void updateDepthCoords(ShortBuffer dBuf)
  /* Use new depth buffer data to update the PointsArray inside the
     Java 3D scene. This method is repeatedly called by DepthReader
     as the depth buffer changes.
     This method will not return until the 3D scene has been updated.
  */
  {
    depthBuf = dBuf;
    zScale = ((float)Z_WIDTH)/getMaxDepth(depthBuf);   //adjust z-axis scaling

    pointParts.updateData(this);   // request an update of the geometry
    try {
      sem.acquire();     // wait for update to finish in updateData()
    }
    catch(InterruptedException e) {}
  }  // end of updateDepthCoords()


  private int getMaxDepth(ShortBuffer depthBuf)
  // scan buffer to find the largest depth
  {
    int maxDepth = 0;
    while (depthBuf.remaining() > 0) {
      short depthVal = depthBuf.get();
      if (depthVal > maxDepth)
        maxDepth = depthVal;
    }
    depthBuf.rewind();

    // System.out.println("Maximum depth: " + maxDepth);
    return maxDepth;
  }  // end of getMaxDepth()



  // -------------- GeometryUpdater methods ----------------

  public void updateData(Geometry geo)
  /* This method is called by the system some (short) time after 
     pointParts.updateData(this) is called in updateDepthCoords().
     An update of the geometry is carried out:
         the z-coord is changed in coords[], and the point's
         corresponding colour is updated
  */
  { 
    // GeometryArray ga = (GeometryArray) geo;
    // float pointCoords[] = ga.getCoordRefFloat();

    while (depthBuf.remaining() > 0) {
      int dpIdx = depthBuf.position();
      float zCoord = ((float) depthBuf.get())*zScale;   // convert to 3D scene coord
      if (dpIdx%SAMPLE_FREQ == 0) {    // save this z-coord
        int zCoordIdx = (dpIdx/SAMPLE_FREQ)*3 + 2;
        if (zCoordIdx < coords.length) {
          coords[zCoordIdx] = -zCoord;
              // negate so depths are spread out along -z axis, away from camera
          // printCoord(coords, zCoordIdx-2);
          updateColour(zCoordIdx-2, zCoord);
        }
      }
    }

    sem.release();    
        // signal that update is finished; now updateDepthCoords() can return
  }  // end of updateData()


  private void updateColour(int xCoordIdx, float zCoord)
  /* map z-coord to colormap key between 0 and 255, and store
     its color as the point's new color;
     similar to the depth coloring in version 5 of the ViewerPanel
     example: red in forground (and for no depth), changing to violet
     in the background
  */
  {
    int key = 0;
    if (zCoord != 0) {  // there is a depth for this point
      key = (int) Math.round((double)zCoord/Z_WIDTH*(NUM_COLORS-1));
      key = (NUM_COLORS-1) - key;
      // System.out.println("Color index: " + key);
    }
    Color col = colorMap[key];

    // assign colormap color to the point as a float between 0-1.0f
    colors[xCoordIdx] = col.getRed()/255.0f;  
    colors[xCoordIdx+1] = col.getGreen()/255.0f; 
    colors[xCoordIdx+2] = col.getBlue()/255.0f;
  }  // end of updateColour()



  private void printCoord(float[] coords, int xIdx)
  {  System.out.println("" + xIdx + ". depth coord (x,y,z): (" +
                 coords[xIdx] + ", " + coords[xIdx+1] +
                 ", " + coords[xIdx+2] + ")");
  }


} // end of PointsShape class
