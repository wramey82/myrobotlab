package org.myrobotlab.control;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import org.myrobotlab.service.Arduino;
import org.myrobotlab.service.interfaces.GUI;

// TODO - too big for inner class
public class VideoDisplayPanel
{
	public final static Logger LOG = Logger.getLogger(VideoDisplayPanel.class.getCanonicalName());

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

	
	VideoDisplayPanel(String boundFilterName, VideoWidget p, GUI myService, String boundServiceName, ImageIcon icon)
	{
		this.myService = myService;
		this.boundServiceName = boundServiceName;
		this.parent = p;
		this.boundFilterName = boundFilterName;	// TODO - boundSourceName - not OpenCV specific		
		
		myDisplay.setLayout(new GridBagLayout());
		
		if (icon == null)
		{
			screen.setIcon(Util.getResourceIcon("mrl_logo.jpg"));	
		}

		GridBagConstraints gc = new GridBagConstraints();

		screen.addMouseListener(vml);
		myIcon.setImageObserver(screen); // Good(necessary) Optimization

		TitledBorder title;
		title = BorderFactory.createTitledBorder(boundServiceName + " " + boundFilterName + " video widget");
		myDisplay.setBorder(title);

		gc.gridx = 0;
		gc.gridy = 0;

		parent.localSources = parent.getServices(null); // FIXME - should be local, global, static?
		parent.localSources.setVisible(false);
		myDisplay.add(parent.localSources, gc);

		++gc.gridx;
		fork.addActionListener(new ButtonListener());
		myDisplay.add(fork, gc);
		if (!parent.allowFork)
		{
			fork.setVisible(false);
		}
		
		++gc.gridx;
		sources.setVisible(false);
		myDisplay.add(sources, gc);
		
		++gc.gridx;
		attach.addActionListener(new ButtonListener());
		myDisplay.add(attach, gc);
		attach.setVisible(false);

		
		gc.gridx = 0;
		gc.gridwidth = 5;
		++gc.gridy;
		myDisplay.add(screen, gc);
		
		// add the sub-text components
		gc.gridwidth = 1;
		++gc.gridy;
		myDisplay.add(mouseInfo, gc);
		++gc.gridx;
		myDisplay.add(resolutionInfo, gc);
		++gc.gridy;
		myDisplay.add(deltaTime, gc);
		
		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 5;
		myDisplay.add(sourceNameLabel, gc);		

		gc.gridx = 0;
		++gc.gridy;
		gc.gridwidth = 5;
		myDisplay.add(extraDataLabel, gc);		
		
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
//			screens.put(img.source, new JLabel());
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
		// screen.repaint(); - was in other function - performance hit remove if works in GraphicServiceGUI
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
		
		// resize gui if necessary
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
		
		// String [] namesAndClasses = new String[sortedMap.size()];
		while (it.hasNext()) {
			String serviceName = it.next();
			sources.addItem(serviceName);
		}
	}

	class ButtonListener implements ActionListener {
		ButtonListener() {
		}

		public void actionPerformed(ActionEvent e) {
			String id = ((JButton)e.getSource()).getText(); 
			if (id.equals("attach"))
			{
				parent.attachLocalGUI();
			} else if (id.equals("fork")) {
				String filter = (String)sources.getSelectedItem();
				parent.addVideoDisplayPanel(filter);
				myService.send(boundServiceName, "fork", filter); 

			} else {
				LOG.error("unhandled button event - " + id);
			}
		}
	}
	
	
} // VideoDisplayPanel


