package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.myrobotlab.control.PinComponent;
import org.myrobotlab.control.VideoWidget;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.GUIService;

public class Oscope {

	JPanel display = null;
	VideoWidget screen = null;
	Dimension size = new Dimension(320, 512);

	Graphics g = null;

	HashMap<String, ArrayList<PinComponent>> allPins = new HashMap<String, ArrayList<PinComponent>>();

	ArrayList<PinComponent> pinList = null;
	String source = null;

	SerializableImage sensorImage = null;

	String boundServiceName;
	GUIService myService;

	/*
	 * source selector - todo trigger interface - ???
	 */

	public Oscope(GUIService myService, String boundServiceName) {
		this.myService = myService;
		this.boundServiceName = boundServiceName;
	}

	public void clearScreen() // TODO - static - put in oscope/image package
	{
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, size.width, size.height); // TODO - ratio - to expand
													// or reduce view
	}

	public void drawGrid() // TODO - static & put in oscope/image package
	{
		g.setColor(Color.DARK_GRAY);
		g.drawLine(0, size.height - 25, size.width - 1, size.height - 25);
		g.drawString("50", 10, size.height - 25);
		g.drawLine(0, size.height - 50, size.width - 1, size.height - 50);
		g.drawString("100", 10, size.height - 50);
		g.drawLine(0, size.height - 100, size.width - 1, size.height - 100);
		g.drawString("200", 10, size.height - 100);
		g.drawLine(0, size.height - 200, size.width - 1, size.height - 200);
		g.drawString("400", 10, size.height - 200);
		g.drawLine(0, size.height - 300, size.width - 1, size.height - 300);
		g.drawString("600", 10, size.height - 300);
		g.drawLine(0, size.height - 400, size.width - 1, size.height - 400);
		g.drawString("800", 10, size.height - 400);

	}

	public Color getColor(int pos) {
		if (pinList == null)
			return null;

		float gradient = 1.0f / pinList.size();
		Color hsv = new Color(Color.HSBtoRGB((pos * (gradient)), 0.8f, 0.7f));
		return hsv;
	}

	public JPanel getOscopePanel() {

		display = new JPanel(new BorderLayout());
		GridBagConstraints opgc = new GridBagConstraints();

		JPanel tracePanel = new JPanel(new GridBagLayout());

		opgc.fill = GridBagConstraints.HORIZONTAL;
		opgc.gridx = 0;
		opgc.gridy = 0;
		float gradient = 1.0f / pinList.size();

		// pinList.size() mega 60 deuo 20
		for (int i = 0; i < pinList.size(); ++i) {
			PinComponent p = pinList.get(i);
			if (!p.isAnalog) { // digital pins -----------------
				p.trace.setText("D " + (i));
				p.trace.onText = "D " + (i);
				p.trace.offText = "D " + (i);
			} else {
				// analog pins ------------------
				p.trace.setText("A " + (i - 14));
				p.trace.onText = "A " + (i - 14);
				p.trace.offText = "A " + (i - 14);
			}
			tracePanel.add(p.trace, opgc);
			Color hsv = new Color(Color.HSBtoRGB((i * (gradient)), 0.8f, 0.7f));
			p.trace.setBackground(hsv);
			p.trace.offBGColor = hsv;
			++opgc.gridy;
			if (opgc.gridy % 20 == 0) {
				opgc.gridy = 0;
				++opgc.gridx;
			}
		}

		screen = new VideoWidget(boundServiceName, myService, false);
		screen.init();
		sensorImage = new SerializableImage(new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB), "output");
		g = sensorImage.getImage().getGraphics();
		screen.displayFrame(sensorImage);

		display.add(tracePanel, opgc);
		++opgc.gridx;
		display.add(screen.display, opgc);

		JFrame top = myService.getFrame();
		myService.getFrame().pack();

		return display;
	}

}
