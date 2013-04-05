package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.interfaces.GUI;
import org.slf4j.Logger;

public class VideoDisplayPanel {
	public final static Logger log = LoggerFactory.getLogger(VideoDisplayPanel.class.getCanonicalName());

	VideoWidget parent;
	String boundFilterName;

	public final String boundServiceName;
	final GUI myService;

	JPanel myDisplay = new JPanel();
	JLabel screen = new JLabel();
	JLabel mouseInfo = new JLabel("mouse x y");
	JLabel resolutionInfo = new JLabel("width x height");
	JLabel deltaTime = new JLabel("0");

	JLabel sourceNameLabel = new JLabel("");
	public JLabel extraDataLabel = new JLabel("");

	public SerializableImage lastImage = null;
	public ImageIcon lastIcon = new ImageIcon();
	public ImageIcon myIcon = new ImageIcon();
	public VideoMouseListener vml = new VideoMouseListener();

	public int lastImageWidth = 0;

	public class VideoMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			mouseInfo.setText("clicked " + e.getX() + "," + e.getY());
			Object[] params = new Object[2];
			params[0] = e.getX();
			params[1] = e.getY();

			myService.send(boundServiceName, "invokeFilterMethod", sourceNameLabel.getText(), "samplePoint", params);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// mouseInfo.setText("entered");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// mouseInfo.setText("exit");

		}

		@Override
		public void mousePressed(MouseEvent e) {
			// mouseInfo.setText("pressed");
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// mouseInfo.setText("release");
		}

	}

	VideoDisplayPanel(String boundFilterName, VideoWidget p, GUI myService, String boundServiceName) {
		this(boundFilterName, p, myService, boundServiceName, null);
	}

	VideoDisplayPanel(String boundFilterName, VideoWidget parent, GUI myService, String boundServiceName, ImageIcon icon) {
		this.myService = myService;
		this.boundServiceName = boundServiceName;
		this.parent = parent;
		this.boundFilterName = boundFilterName;

		myDisplay.setLayout(new BorderLayout());

		if (icon == null) {
			screen.setIcon(Util.getResourceIcon("mrl_logo.jpg"));
		}

		screen.addMouseListener(vml);
		myIcon.setImageObserver(screen); // Good(necessary) Optimization
	
		myDisplay.add(BorderLayout.CENTER, screen);

		JPanel south = new JPanel();
		// add the sub-text components
		south.add(mouseInfo);
		south.add(resolutionInfo);
		south.add(deltaTime);
		south.add(sourceNameLabel);
		south.add(extraDataLabel);
		myDisplay.add(BorderLayout.SOUTH, south);

	}

	public void displayFrame(SerializableImage img) {

		/*
		 * got new frame - check if a screen exists for it or if i'm in single
		 * screen mode
		 * 
		 * img.source is the name of the bound filter
		 */
		String source = img.getSource();
		long timestamp = img.getTimestamp();

		if (lastImage != null) {
			screen.setIcon(lastIcon);
		}

		if (!sourceNameLabel.getText().equals(source)) {
			sourceNameLabel.setText(source);
		}

		if (parent.normalizedSize != null)
		{
			myIcon.setImage(img.getImage().getScaledInstance(parent.normalizedSize.width, parent.normalizedSize.height, 0));
		} else {
			myIcon.setImage(img.getImage());
		}
		screen.setIcon(myIcon);
		if (lastImage != null) {
				deltaTime.setText(String.format("%d", timestamp - lastImage.getTimestamp()));
		}

		lastImage = img;
		lastIcon.setImage(img.getImage());

		if (lastImageWidth != img.getImage().getWidth()) {
			screen.invalidate();
			myService.pack();
			lastImageWidth = img.getImage().getWidth();
			resolutionInfo.setText(" " + lastImageWidth + " x " + img.getImage().getHeight());
		}

		img = null;

	}


} // VideoDisplayPanel

