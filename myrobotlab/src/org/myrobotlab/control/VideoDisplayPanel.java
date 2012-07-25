package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.interfaces.GUI;

// TODO - too big for inner class
public class VideoDisplayPanel implements ActionListener
{
	public final static Logger log = Logger.getLogger(VideoDisplayPanel.class.getCanonicalName());

	VideoWidget parent;
	String boundFilterName;
	
	public final String boundServiceName;
	final GUI myService;

	JPanel myDisplay = new JPanel();
	JComboBox sources = new JComboBox();
	JLabel screen = new JLabel();
	JLabel mouseInfo = new JLabel("mouse x y");
	JLabel resolutionInfo = new JLabel("width x height");
	JLabel deltaTime = new JLabel("0");

	JButton attach = new JButton("attach");
	JButton fork = new JButton("fork");
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
			Object[] d = new Object[1];
			d[0] = e; // TODO - "invokeFilterMethod" to mouseClick - not OpenCV specific
			myService.send(boundServiceName, "invokeFilterMethod", sourceNameLabel.getText(), "samplePoint", d); 
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


	VideoDisplayPanel(String boundFilterName, VideoWidget p, GUI myService, String boundServiceName)
	{
		this(boundFilterName, p, myService, boundServiceName, null);
	}

	
	VideoDisplayPanel(String boundFilterName, VideoWidget parent, GUI myService, String boundServiceName, ImageIcon icon)
	{
		this.myService = myService;
		this.boundServiceName = boundServiceName;
		this.parent = parent;
		this.boundFilterName = boundFilterName;
		
		myDisplay.setLayout(new BorderLayout());
		
		if (icon == null)
		{
			screen.setIcon(Util.getResourceIcon("mrl_logo.jpg"));	
		}

		screen.addMouseListener(vml);
		myIcon.setImageObserver(screen); // Good(necessary) Optimization

		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " " + boundFilterName + " video widget");
		myDisplay.setBorder(title);


		JPanel north = new JPanel();
		fork.addActionListener(this);

		if (!parent.allowFork)
		{
			fork.setVisible(false);
		}
		
		sources.setVisible(false);
		attach.addActionListener(this);

		attach.setVisible(false);
		north.add(fork);
		north.add(sources);
		north.add(attach);
		
		myDisplay.add(BorderLayout.NORTH, north);
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
		
		/* got new frame - check if a screen exists for it
		 * or if i'm in single screen mode
		 * 
		 * img.source is the name of the bound filter
		 */
		if(img.source == null)
		{
			img.source = "output";
		}
		 
		if (parent.allowFork && !parent.displays.containsKey(img.source)) {
			parent.addVideoDisplayPanel(img.source);// dynamically spawn a display if a new source is found
			getSources();
		}

		if (lastImage != null) {
			screen.setIcon(lastIcon);
		}
		
		if (!sourceNameLabel.getText().equals(img.source))
		{
			sourceNameLabel.setText(img.source);
		}
		
		myIcon.setImage(img.getImage());
		screen.setIcon(myIcon);
		if (lastImage != null) {
			if (img.timestamp != null)
				deltaTime.setText(""
						+ (img.timestamp.getTime() - lastImage.timestamp.getTime()));
		}
		
		lastImage = img;
		lastIcon.setImage(img.getImage());

		if (parent.exports.size() > 0) {
			for (int i = 0; i < parent.exports.size(); ++i) {
//				exports.get(i).displayFrame(filterName, img); FIXME
			}
		}
		
		if (lastImageWidth != img.getImage().getWidth())
		{
			screen.invalidate();
			myService.pack();
			lastImageWidth = img.getImage().getWidth();
			resolutionInfo.setText(" " + lastImageWidth + " x " + img.getImage().getHeight());
		}

		img = null;

	}

	public void getSources()
	{

		Map<String, VideoDisplayPanel> sortedMap = new TreeMap<String, VideoDisplayPanel>(parent.displays);
		Iterator<String> it = sortedMap.keySet().iterator();

		sources.removeAllItems();
		
		while (it.hasNext()) {
			String serviceName = it.next();
			sources.addItem(serviceName);
		}
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		String id = ((JButton)e.getSource()).getText(); 
		if (id.equals("fork")) {
			String filter = (String)sources.getSelectedItem();
			parent.addVideoDisplayPanel(filter);
			myService.send(boundServiceName, "fork", filter); 

		} else {
			log.error("unhandled button event - " + id);
		}
		
	}
	
	
} // VideoDisplayPanel


