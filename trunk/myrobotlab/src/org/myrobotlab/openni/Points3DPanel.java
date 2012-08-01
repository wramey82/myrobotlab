package org.myrobotlab.openni;

// Points3DPanel.java
// Andrew Davison, September 2011, ad@fivedots.coe.psu.ac.th

/* This class builds a Java 3D scene consisting of a dark green 
   and blue tiled surface with labels along the X and Z axes, 
   a blue background, lit from two different directions. 

   The user (viewer) can move through the scene by moving the mouse.

   A points cloud of the Kinect's depth map is displayed spread out
   along the -z axis, with different colors assigned to different
   depths. The points cloud is implemented as an instance of the
   PointsShape class, a subclass of Shape3D.

   All of the scene graph, apart from the PointsShape object,
   comes from the Checkers3D example in Chapter 15,
   "Killer Game Programming in Java"
   (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/), and is explained
   in detail there.
*/


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;



public class Points3DPanel extends JPanel
// Holds the 3D canvas
{
  private static final int PWIDTH = 512;   // size of panel
  private static final int PHEIGHT = 512; 
  private static final int BOUNDSIZE = 100;  // larger than world
  private static final Point3d USERPOSN = new Point3d(0,7,17);
    // initial user position


  private SimpleUniverse su;
  private BranchGroup sceneBG;
  private BoundingSphere bounds;   // for environment nodes


  public Points3DPanel(PointsShape ptsShape)
  // construct the 3D canvas
  {
    setLayout( new BorderLayout() );
    setOpaque( false );

    setPreferredSize( new Dimension(PWIDTH, PHEIGHT));

    GraphicsConfiguration config =
					SimpleUniverse.getPreferredConfiguration();
    Canvas3D canvas3D = new Canvas3D(config);
    add("Center", canvas3D);
    canvas3D.setFocusable(true);
    canvas3D.requestFocus();    // the canvas now has focus, so receives key events

    su = new SimpleUniverse(canvas3D);

    createSceneGraph(ptsShape);
    initUserPosition();        // set user's viewpoint
    orbitControls(canvas3D);   // controls for moving the viewpoint
    
    su.addBranchGraph( sceneBG );
  } // end of Points3DPanel()



  private void createSceneGraph(PointsShape ptsShape) 
  // initilise the scene
  { 
    sceneBG = new BranchGroup();
    bounds = new BoundingSphere(new Point3d(0,0,0), BOUNDSIZE);   

    lightScene();         // add the lights
    addBackground();      // add the sky
    sceneBG.addChild( new CheckerFloor().getBG() );  // add the floor

    addPointsShape(ptsShape);

    sceneBG.compile();   // fix the scene
  } // end of createSceneGraph()


  private void lightScene()
  /* One ambient light, 2 directional lights */
  {
    Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

    // Set up the ambient light
    AmbientLight ambientLightNode = new AmbientLight(white);
    ambientLightNode.setInfluencingBounds(bounds);
    sceneBG.addChild(ambientLightNode);

    // Set up the directional lights
    Vector3f light1Direction  = new Vector3f(-1.0f, -1.0f, -1.0f);
       // left, down, backwards 
    Vector3f light2Direction  = new Vector3f(1.0f, -1.0f, 1.0f);
       // right, down, forwards

    DirectionalLight light1 = 
            new DirectionalLight(white, light1Direction);
    light1.setInfluencingBounds(bounds);
    sceneBG.addChild(light1);

    DirectionalLight light2 = 
        new DirectionalLight(white, light2Direction);
    light2.setInfluencingBounds(bounds);
    sceneBG.addChild(light2);
  }  // end of lightScene()



  private void addBackground()
  // A blue sky
  { Background back = new Background();
    back.setApplicationBounds( bounds );
    back.setColor(0.17f, 0.65f, 0.92f);    // sky colour
    sceneBG.addChild( back );
  }  // end of addBackground()



  private void orbitControls(Canvas3D c)
  /* OrbitBehaviour allows the user to rotate around the scene, and to
     zoom in and out.
  */
  { OrbitBehavior orbit = 
		new OrbitBehavior(c, OrbitBehavior.REVERSE_ALL);
    orbit.setSchedulingBounds(bounds);

    ViewingPlatform vp = su.getViewingPlatform();
    vp.setViewPlatformBehavior(orbit);	    
  }  // end of orbitControls()



  private void initUserPosition()
  // Set the user's initial viewpoint using lookAt()
  {
    ViewingPlatform vp = su.getViewingPlatform();
    TransformGroup steerTG = vp.getViewPlatformTransform();

    Transform3D t3d = new Transform3D();
    steerTG.getTransform(t3d);

    // args are: viewer posn, where looking, up direction
    t3d.lookAt( USERPOSN, new Point3d(0,0,0), new Vector3d(0,1,0));
    t3d.invert();

    steerTG.setTransform(t3d);
  }  // end of initUserPosition()



  private void addPointsShape(PointsShape ptsShape)
  /* This is the only method different from the Checkers3D example
     in Chapter 15 of "Killer Game Programming in Java"
     (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/).

     All the hard work is done inside the PointsShape object. The
     transform group is used to position (and perhaps scale) the points
     cloud.
  */
  {
    // scale and move start position to (-4,0,0)  // change later
    TransformGroup posnTG = new TransformGroup();
    Transform3D t3d = new Transform3D();
    // t3d.setScale(0.5);
    t3d.setTranslation( new Vector3d(-4.0f, 0.0f, 0.0f) );
    posnTG.setTransform(t3d);
    posnTG.addChild(ptsShape); 
    sceneBG.addChild( posnTG );
  } // end of addPointsShape()



} // end of Points3DPanel class