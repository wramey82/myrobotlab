package org.myrobotlab.openni;

// PointCloud.java
// Andrew Davison, Feb. 2012, ad@fivedots.coe.psu.ac.th

/* Render the Kinect's changing depth buffer as a point cloud
 in a Java 3D scene.

 The points have varying colours depending on their depth,
 which are spread out along the -z axis.

 The user's viewpoint can be rotated, zoomed, and translated 
 through the scene using standard java 3D mouse controls.

 Almost all of the 3D scene, aside from the point cloud, is
 based on the Checkers3D example in Chapter 15,
 "Killer Game Programming in Java"
 (http://fivedots.coe.psu.ac.th/~ad/jg/ch8/), and is explained
 in detail there.
 */

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public class PointCloud extends JFrame {

	private static final long serialVersionUID = 1L;
	private DepthReader depthReader;

	public PointCloud(String args[]) {
		super("Point Cloud");

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		PointsShape ptsShape = new PointsShape();
		depthReader = new DepthReader(ptsShape);
		Points3DPanel panel3d = new Points3DPanel(ptsShape);

		c.add(panel3d, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				depthReader.closeDown();
				// System.exit(0);
			}
		});

		pack();
		setResizable(false); // fixed size display
		setLocationRelativeTo(null);
		setVisible(true);
	} // end of PointCloud()

	// -----------------------------------------

	public static void main(String[] args) {
		new PointCloud(args);
	}

} // end of PointCloud class

