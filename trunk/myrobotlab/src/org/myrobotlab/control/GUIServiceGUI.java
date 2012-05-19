/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.control;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.control.GUIServiceGraphVertex.Type;
import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.GUI;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.w3c.dom.Document;

import com.mxgraph.io.mxCellCodec;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;

public class GUIServiceGUI extends ServiceGUI {

	static final long serialVersionUID = 1L;

	final int PORT_DIAMETER = 20;
	final int PORT_RADIUS = PORT_DIAMETER / 2;

	// notify structure begin -------------
	public JLabel srcServiceName = new JLabel("             ");
	public JLabel srcMethodName = new JLabel("             ");
	public JLabel parameterList = new JLabel("             ");
	public JLabel dstMethodName = new JLabel();
	public JLabel dstServiceName = new JLabel();
	public JLabel period0 = new JLabel(" ");
	public JLabel period1 = new JLabel(" ");
	public JLabel arrow0 = new JLabel(" ");
	// public JLabel arrow1 = new JLabel(" ");
	// notify structure end -------------

	boolean showRoutes = true;
	public HashMap<String, mxCell> serviceCells = new HashMap<String, mxCell>();
	public mxGraph graph = null;
	mxCell currentlySelectedCell = null;
	mxGraphComponent graphComponent = null;
	JPanel graphPanel = new JPanel();

	private JButton rebuildButton = new JButton("rebuild");
	private JButton showRoutesButton = new JButton("hide routes");

	public GUIServiceGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}

	public void init() {

		JPanel newRoute = new JPanel(new FlowLayout());
		newRoute.setBorder(BorderFactory.createTitledBorder("new message route"));
		newRoute.add(srcServiceName);
		newRoute.add(period0);
		newRoute.add(srcMethodName);
		newRoute.add(arrow0);
		// newRoute.add(parameterList);
		// newRoute.add(arrow1);
		newRoute.add(dstServiceName);
		newRoute.add(period1);
		newRoute.add(dstMethodName);

		graphPanel.setBorder(BorderFactory.createTitledBorder("graph"));

		buildGraph();

		gc.gridx = 0;
		gc.gridy = 0;
		//gc.gridwidth = 2;
		display.add(newRoute, gc);
		//gc.gridwidth = 1;

		++gc.gridx;
		// begin graph view buttons
		JPanel filters = new JPanel(new GridBagLayout());
		GridBagConstraints fgc = new GridBagConstraints();
		fgc.fill = GridBagConstraints.HORIZONTAL;
		fgc.gridy = 0;
		fgc.gridx = 0;
		filters.add(rebuildButton, fgc);
		++fgc.gridy;
		filters.add(showRoutesButton, fgc);
		display.add(filters, gc);
		++gc.gridy;
		gc.gridx = 0;
		gc.gridwidth = 2;

		graphPanel.setVisible(true);
		display.add(graphPanel, gc);
		++gc.gridy;

		rebuildButton.setActionCommand("rebuild");
		rebuildButton.addActionListener(new ButtonListener());

		showRoutesButton.setActionCommand("hide routes");
		showRoutesButton.addActionListener(new ButtonListener());

	}

	class ButtonListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand();
			if (cmd.equals("rebuild")) {
				rebuildGraph();
			} else if (cmd.equals("show routes")) {
				JButton b = (JButton) e.getSource();
				b.setText("hide routes");
				b.setActionCommand("hide routes");
				showRoutes = true;
				rebuildGraph();
			} else if (cmd.equals("hide routes")) {
				JButton b = (JButton) e.getSource();
				b.setText("show routes");
				b.setActionCommand("show routes");
				showRoutes = false;
				rebuildGraph();
			}
		}
	}

	public void clearGraph() {
		graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
		buildGraph();
	}

	public void rebuildGraph() {
		graph.removeCells(graph.getChildCells(graph.getDefaultParent(), true, true));
		buildGraph();
	}

	public void buildGraph() {
		LOG.info("buildGraph");
		// -------------------------BEGIN PURE JGRAPH
		// ----------------------------

		if (myService.getGraphXML() == null || myService.getGraphXML().length() == 0) {
			if (graph == null) {
				graph = getNewMXGraph();
			}

			// new graph !
			graph.getModel().beginUpdate();
			try {
				buildLocalServiceGraph();
				if (showRoutes) {
					buildLocalServiceRoutes();
				}
			} finally {
				graph.getModel().endUpdate();
			}

		} else {
			// we have serialized version of graph...
			// de-serialize it

			// register
			mxCodecRegistry.addPackage("org.myrobotlab.control");
			mxCodecRegistry.register(new mxCellCodec(new org.myrobotlab.control.GUIServiceGraphVertex()));
			mxCodecRegistry.register(new mxCellCodec(Type.INPORT));

			// load
			Document document = mxUtils.parseXml(myService.getGraphXML());

			mxCodec codec2 = new mxCodec(document);
			graph = getNewMXGraph();
			codec2.decode(document.getDocumentElement(), graph.getModel());

			Object parent = graph.getDefaultParent();
			Object services[] = graph.getChildVertices(parent);
			LOG.info("serviceCount " + services.length);

			for (int i = 0; i < services.length; ++i) {
				// serviceCells
				Object s = services[i];
				LOG.info(s);

				mxCell m = (mxCell) services[i];
				GUIServiceGraphVertex v = (GUIServiceGraphVertex) m.getValue();// zod
																				// zod
																				// zod
				LOG.info(v.name);
				serviceCells.put(v.name, m);
				// serviceCells.put(arg0, s.);
			}
		}

		// TODO - get # of services to set size?
		graph.setMinimumGraphSize(new mxRectangle(0, 0, 600, 400));

		// Sets the default edge style
		// list of styles - http://libjgraphx-java.sourcearchive.com/documentation/1.7.0.7/classcom_1_1mxgraph_1_1util_1_1mxConstants.html
		Map<String, Object> style = graph.getStylesheet().getDefaultEdgeStyle();
		style.put(mxConstants.STYLE_EDGE, mxEdgeStyle.EntityRelation);// .ElbowConnector
		style.put(mxConstants.STYLE_STROKECOLOR, "black");// .ElbowConnector
		style.put(mxConstants.STYLE_EDITABLE, "0");// .ElbowConnector
		style.put(mxConstants.STYLE_MOVABLE, "0");// .ElbowConnector

		// creating JComponent
		if (graphComponent == null) {	
			graphComponent = new mxGraphComponent(graph);
			graphPanel.add(graphComponent);
			// graphComponent.addKeyListener(this);

			// graphComponent.getGraphControl().addMouseListener(this);

			graphComponent.getGraphControl().addMouseMotionListener(new MouseMotionListener() {

				@Override
				public void mouseMoved(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					LOG.info("dragged - mouseMoved - cell " + cell + " " + e.getX() + "," + e.getY());
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					LOG.info("dragged cell " + cell + " " + e.getX() + "," + e.getY());
				}
			});

			graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

				/*
				 * protected void mouseLocationChanged(MouseEvent e) {
				 * LOG.info(e.getX() + ", " + e.getY()); }
				 * 
				 * public void mouseDragged(MouseEvent e) { //
				 * http://forum.jgraph
				 * .com/questions/1343/mouse-coordinates-at-drop-event Object
				 * cell = graphComponent.getCellAt(e.getX(), e.getY());
				 * LOG.info(e.getX() + "," + e.getY()); }
				 */

				public void mouseReleased(MouseEvent e) {
					Object cell = graphComponent.getCellAt(e.getX(), e.getY());
					LOG.info("cell " + e.getX() + "," + e.getY());
					currentlySelectedCell = (mxCell) cell;

					if (cell != null) {
						mxCell m = (mxCell) cell;
						LOG.info("cell=" + graph.getLabel(cell) + ", " + m.getId() + ", "
								+ graph.getLabel(m.getParent()));
						if (m.isVertex()) {
							// TODO - edges get filtered through here too - need
							// to process - (String) type
							GUIServiceGraphVertex v = (GUIServiceGraphVertex) m.getValue();
							if (v.displayName.equals("out")) {
								new GUIServiceOutMethodDialog(myService, "out method", v);
							} else if (v.displayName.equals("in")) {
								new GUIServiceInMethodDialog(myService, "in method", v);
							}
						} else if (m.isEdge()) {
							LOG.error("isEdge");
						}

					}
				}
			});

			graphComponent.setToolTips(true);

		}

		// -------------------------END PURE
		// JGRAPH--------------------------------------

	}

	public mxGraph getNewMXGraph() {
		mxGraph g = new mxGraph() {

			// Ports are not used as terminals for edges, they are
			// only used to compute the graphical connection point
			public boolean isPort(Object cell) {
				mxGeometry geo = getCellGeometry(cell);

				return (geo != null) ? geo.isRelative() : false;
			}

			// Implements a tooltip that shows the actual
			// source and target of an edge
			public String getToolTipForCell(Object cell) {
				if (model.isEdge(cell)) {
					return convertValueToString(model.getTerminal(cell, true)) + " -> "
							+ convertValueToString(model.getTerminal(cell, false));
				}

				mxCell m = (mxCell) cell;

				GUIServiceGraphVertex sw = (GUIServiceGraphVertex) m.getValue();
				if (sw != null) {
					return sw.toolTip;
				} else {
					return "<html>port node<br>click to drag and drop static routes</html>";
				}
			}

			// Removes the folding icon and disables any folding
			public boolean isCellFoldable(Object cell, boolean collapse) {
				// return true;
				return false;
			}
		};

		return g;
	}

	// FIXME - no longer needed (remove)
	public JButton getRefreshServicesButton() {
		JButton button = new JButton("refresh services");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// myService.loadTabPanels(); FIXME - no longer needed ???
			}

		});

		return button;

	}

	public JButton getSaveButton() {
		JButton button = new JButton("save");
		button.setEnabled(true);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Execute when button is pressed // TODO send - message
				// serialize graph
				mxCodecRegistry.addPackage("org.myrobotlab.control");
				mxCodecRegistry.register(new mxCellCodec(new org.myrobotlab.control.GUIServiceGraphVertex()));
				mxCodecRegistry.register(new mxCellCodec(Type.INPORT));
				mxCodec codec = new mxCodec();
				String xml = mxUtils.getXml(codec.encode(graph.getModel()));

				myService.setGraphXML(xml);

				// save runtime
				Runtime.save("runtime.bin");
			}

		});

		return button;
	}

	public JButton getLoadButton() {
		JButton button = new JButton("load");
		button.setEnabled(true);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				Runtime.releaseAll();

				// load runtime
				Runtime.load("runtime.bin");

				Runtime.startLocalServices(); // FIXME - previously started gui
												// .display()

				// FIXME - startGUI

				// Execute when button is pressed // TODO send - message

			}

		});

		return button;
	}

	public JButton getDumpButton() {
		JButton button = new JButton("dump");
		button.setEnabled(true);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				FileIO.stringToFile("dump.xml", Runtime.dumpNotifyEntries());

			}

		});

		return button;
	}

	public void buildLocalServiceGraph() {

		HashMap<String, ServiceWrapper> services = Runtime.getRegistry();
		LOG.info("service count " + Runtime.getRegistry().size());

		TreeMap<String, ServiceWrapper> sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		int x = 20;
		int y = 20;

		Object parent = graph.getDefaultParent();
		serviceCells.clear();

		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper sw = services.get(serviceName);
			String ret;
			String toolTip;
			String canonicalName;

			if (sw.get() == null) {
				canonicalName = "unknown";
				ret = serviceName + "\n" + "unknown";
				toolTip = "";
			} else {
				canonicalName = sw.get().getShortTypeName();
				ret = serviceName + "\n\n\n\n\n.";// +
													// sw.get().getShortTypeName();
				toolTip = sw.getToolTip();
			}

			String blockColor = null;

			if (sw.host.accessURL == null) {
				blockColor = mxUtils.getHexColorString(Style.background);
			} else {
				blockColor = mxUtils.getHexColorString(Style.remoteBackground);
			}

			mxCell v1 = (mxCell) graph.insertVertex(parent, null, new GUIServiceGraphVertex(serviceName, canonicalName,
					ret, toolTip, GUIServiceGraphVertex.Type.SERVICE), x, y, 100, 50, "shape=image;image=/resource/"
					+ canonicalName.toLowerCase() + ".png");
			// "ROUNDED;fillColor=" + blockColor);

			// graphComponent.getGraphControl().scrollRectToVisible(new
			// Rectangle(0, 0, 900, 800), true);

			serviceCells.put(serviceName, v1);

			v1.setConnectable(false);
			mxGeometry geo = graph.getModel().getGeometry(v1);
			// The size of the rectangle when the minus sign is clicked
			geo.setAlternateBounds(new mxRectangle(20, 20, 100, 50));

			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			// Because the origin is at upper left corner, need to translate to
			// position the center of port correctly
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);

			mxCell inport = new mxCell(new GUIServiceGraphVertex(serviceName, canonicalName, "in", toolTip,
					GUIServiceGraphVertex.Type.INPORT), geo1, "shape=ellipse;perimter=ellipsePerimeter;fillColor="
					+ blockColor);

			inport.setVertex(true);

			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);

			mxCell outport = new mxCell(new GUIServiceGraphVertex(serviceName, canonicalName, "out", toolTip,
					GUIServiceGraphVertex.Type.OUTPORT), geo2, "shape=ellipse;perimter=ellipsePerimeter;fillColor="
					+ blockColor);

			outport.setVertex(true);

			graph.addCell(inport, v1);
			graph.addCell(outport, v1);

			x += 150;
			if (x > 400) {
				y += 150;
				x = 20;
			}
		}
	}

	public void buildLocalServiceRoutes() {
		Iterator<String> it = Runtime.getRegistry().keySet().iterator();

		Object parent = graph.getDefaultParent();

		while (it.hasNext()) {
			String serviceName = it.next();

			ServiceInterface s = Runtime.getService(serviceName).get();
			if (s != null) {
				Iterator<String> ri = s.getNotifyListKeySet().iterator();
				while (ri.hasNext()) {
					ArrayList<NotifyEntry> nl = s.getNotifyList(ri.next());
					for (int i = 0; i < nl.size(); ++i) {
						NotifyEntry ne = nl.get(i);

						// createArrow(sw.getName(), ne.getName(),
						// methodString);
						// graph.getChildVertices(arg0)parent.
						// graph.getChildVertices(graph.getDefaultParent());
/*						mxCell c = (mxCell)graph.insertEdge(parent, null, formatMethodString(ne.outMethod	, ne.paramTypes, ne.inMethod),
								serviceCells.get(s.getName()), serviceCells.get(ne.name));
*/								
						mxCell c = (mxCell)graph.insertEdge(parent, null, "",
								serviceCells.get(s.getName()), serviceCells.get(ne.name));
											
						//mxGeometry g = c.getGeometry();
						//c.setGeometry(g);
						LOG.info(c);

					}
				}
			} else {
				LOG.error("can not add graphic routes, since " + serviceName + "'s type is unknown");
			}
			// }

		}

	}

	public static String formatMethodString(String out, Class<?>[] paramTypes, String in) {
		// test if outmethod = in
		String methodString = out;
		// if (methodString != in) {
		methodString += "->" + in;
		// }

		// TODO FYI - depricate NotifyEntry use MethodEntry
		// These parameter types could always be considered "inbound" ? or
		// returnType
		// TODO - view either full named paths or shortnames

		methodString += "(";

		if (paramTypes != null) {
			for (int j = 0; j < paramTypes.length; ++j) {
				// methodString += paramTypes[j].getCanonicalName();
				Class c = paramTypes[j];
				String t[] = c.getCanonicalName().split("\\.");
				methodString += t[t.length - 1];

				if (j < paramTypes.length - 1) {
					methodString += ",";
				}
			}
		}

		/*
		 * if (ne.paramType != null) { methodString +=
		 * ne.paramType.substring(ne.paramType .lastIndexOf(".") + 1); }
		 */

		methodString += ")";

		return methodString;
	}

	// FIXME - should it hook to the Runtime ???
	@Override
	public void attachGUI() {
		// sendNotifyRequest("registerServices", "loadTabPanels");//(String
		// hostAddress, int port, Message msg)
		// sendNotifyRequest("registered", "loadTabPanels");//(String
		// hostAddress, int port, Message msg)
	}

	@Override
	public void detachGUI() {
		// removeNotifyRequest("registerServices", "loadTabPanels");
		// removeNotifyRequest("registered", "loadTabPanels");
	}

	/*
	 * @Override public void mouseDragged(MouseEvent e) { // TODO Auto-generated
	 * method stub LOG.info(e.getX() + "," + e.getY()); }
	 * 
	 * @Override public void mouseMoved(MouseEvent arg0) { // TODO
	 * Auto-generated method stub
	 * 
	 * }
	 */
	/*
	 * @Override public void keyTyped(KeyEvent e) { LOG.error("here"); }
	 * 
	 * @Override public void keyReleased(KeyEvent e) { LOG.error("here"); }
	 * 
	 * @Override public void keyPressed(KeyEvent e) { LOG.error("here"); }
	 */
	// about begin

}
