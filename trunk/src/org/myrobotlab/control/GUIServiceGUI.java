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

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import org.myrobotlab.framework.ConfigurationManager;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.framework.RuntimeEnvironment;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.interfaces.GUI;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;

public class GUIServiceGUI extends ServiceGUI implements KeyListener {

	static final long serialVersionUID = 1L;
	
	final int PORT_DIAMETER = 20;
	final int PORT_RADIUS = PORT_DIAMETER / 2;	
	//ConfigurationManager hostCFG;
	
	// notify structure begin -------------
	public JLabel srcServiceName = new JLabel("             ");
	public JLabel srcMethodName = new JLabel("             ");
	public JLabel parameterList  = new JLabel("             ");
	public JLabel dstMethodName  = new JLabel();
	public JLabel dstServiceName  = new JLabel();		
	public JLabel period0 = new JLabel(" ");
	public JLabel period1 = new JLabel(" ");
	public JLabel arrow0 = new JLabel(" ");
	//public JLabel arrow1 = new JLabel(" ");
	// notify structure end -------------

	
	public GUIServiceGUI(final String boundServiceName, final GUI myService) {
		super(boundServiceName, myService);
	}
	
	
	public mxGraph graph = new mxGraph() {
		
		// Ports are not used as terminals for edges, they are
		// only used to compute the graphical connection point
		public boolean isPort(Object cell)
		{
			mxGeometry geo = getCellGeometry(cell);
			
			return (geo != null) ? geo.isRelative() : false;
		}
		
		// Implements a tooltip that shows the actual
		// source and target of an edge
		public String getToolTipForCell(Object cell)
		{
			if (model.isEdge(cell))
			{
				return convertValueToString(model.getTerminal(cell, true)) + " -> " +
					convertValueToString(model.getTerminal(cell, false));
			}
			
			mxCell m = (mxCell)cell;
			
			//String serviceName = super.getToolTipForCell(cell);
			//ServiceEntry se = myService.getHostCFG().getServiceEntry(serviceName);
			
			GUIServiceGraphVertex se = (GUIServiceGraphVertex)m.getValue();
			if (se != null)
			{
				return se.toolTip;
			} else {
				return "<html>port node<br>click to drag and drop static routes</html>";
			}
		}
		
		// Removes the folding icon and disables any folding
		public boolean isCellFoldable(Object cell, boolean collapse)
		{
			//return true;
			return false;
		}
	};
	
	mxCell currentlySelectedCell = null;
	
	public void init() {

		//hostCFG = myService.getHostCFG();
		
		// build input begin ------------------
		JPanel input = new JPanel();
		input.setBorder(BorderFactory.createTitledBorder("input"));

		input.add(getRefreshServicesButton());
		input.add(getDumpCFGButton());
		
		JPanel newRoute = new JPanel(new GridBagLayout());
		newRoute.setBorder(BorderFactory.createTitledBorder("new route"));
		newRoute.add(srcServiceName);
		newRoute.add(period0);
		newRoute.add(srcMethodName);
		newRoute.add(arrow0);
		//newRoute.add(parameterList);
		//newRoute.add(arrow1);
		newRoute.add(dstServiceName);
		newRoute.add(period1);
		newRoute.add(dstMethodName);

		JPanel graphPanel = new JPanel();
		graphPanel.setBorder(BorderFactory.createTitledBorder("graph"));

		// -------------------------BEGIN PURE JGRAPH ----------------------------
		graph.setMinimumGraphSize(new mxRectangle(0, 0, 620, 480)); // TODO - get # of services to set size?

		
		// Sets the default edge style
		Map<String, Object> style = graph.getStylesheet().getDefaultEdgeStyle();
		style.put(mxConstants.STYLE_EDGE, mxEdgeStyle.ElbowConnector);
				
		graph.getModel().beginUpdate();
		try
		{
			buildLocalServiceGraph();
			buildLocalServiceRoutes();
		} 
		finally
		{
			graph.getModel().endUpdate();
		}

		//graphPanel.addKeyListener(this);
		
		final mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphPanel.add(graphComponent);
		
		graphComponent.addKeyListener(this);
		//graphComponent.getGraphControl().addKeyListener(this);
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter()
		{
		
			public void mouseReleased(MouseEvent e)
			{
				Object cell = graphComponent.getCellAt(e.getX(), e.getY());
				
				currentlySelectedCell = (mxCell)cell;
				
				if (cell != null)
				{
					mxCell m = (mxCell)cell;
					System.out.println("cell="+graph.getLabel(cell) + ", " + m.getId() + ", " + graph.getLabel(m.getParent()));
					if (m.isVertex())
					{
						// TODO - edges get filtered through here too - need to process - (String) type
						GUIServiceGraphVertex v = (GUIServiceGraphVertex)m.getValue();// zod zod zod
						if (v.type == Type.OUTPORT)
						{
							new GUIServiceOutMethodDialog(myService, "out method", v); 
						} else if (v.type == Type.INPORT)
						{
							new GUIServiceInMethodDialog(myService, "in method", v); 
						}
					} else if (m.isEdge()) {
						LOG.error("isEdge");
					}
					
				}
			}
		});		
		
		graphComponent.setToolTips(true);		

		// -------------------------END PURE JGRAPH--------------------------------------

		gc.gridx = 0;
		gc.gridy = 0;
		display.add(input, gc);
		
		++gc.gridy;		
		display.add(newRoute, gc);
		
		++gc.gridy;
		graphPanel.setVisible(true);
		
		display.add(graphPanel, gc);
	}

	
	public JButton getRefreshServicesButton() {
		JButton button = new JButton("refresh services");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				myService.loadTabPanels();
			}

		});

		return button;

	}

	public JButton getDumpCFGButton() {
		JButton button = new JButton("dump cfg");
		button.setEnabled(false);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// Execute when button is pressed // TODO send - message
				ConfigurationManager rootcfg = new ConfigurationManager();
				rootcfg.save(myService.getHost() + ".properties");
			}

		});

		return button;
	}


	public HashMap<String, mxCell> serviceCells = new HashMap<String, mxCell>(); 
	
	public void buildLocalServiceGraph() {

		
		HashMap<String, ServiceWrapper> services = RuntimeEnvironment.getRegistry();
		LOG.info("service count " + RuntimeEnvironment.getRegistry().size());
		
		TreeMap<String, ServiceWrapper>sortedMap = new TreeMap<String, ServiceWrapper>(services);
		Iterator<String> it = sortedMap.keySet().iterator();

		int i = 0;
		int x = 20;
		int y = 20;

		Object parent = graph.getDefaultParent();
		
		
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper se = services.get(serviceName);

			// get service type class name
			// String serviceClassName = se.serviceClass;
//			cells.put(serviceName, createVertex(serviceName, x, y, 80, 20, Color.ORANGE, false));
			String shortName[] = se.get().getClass().getCanonicalName().split("\\."); 
			String ret = shortName[shortName.length - 1] + "\n" + serviceName;

			String blockColor = null;
			if (se.host.accessURL == null)
			{
				blockColor = "orange";
			} else {
				blockColor = "0x99DD66";
			}
			
			mxCell v1 = (mxCell) graph.insertVertex(parent, null, 
					new GUIServiceGraphVertex(serviceName, 
							se.get().getClass().getCanonicalName(), 
							ret, se.get().getToolTip(), 
							GUIServiceGraphVertex.Type.SERVICE), x, y, 100, 50, 
							"ROUNDED;fillColor=" + blockColor);
			
			serviceCells.put(serviceName, v1);

			v1.setConnectable(false);
			mxGeometry geo = graph.getModel().getGeometry(v1);
			// The size of the rectangle when the minus sign is clicked
			geo.setAlternateBounds(new mxRectangle(20, 20, 100, 50));

			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER,PORT_DIAMETER);
			// Because the origin is at upper left corner, need to translate to
			// position the center of port correctly
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);
			
			mxCell inport = new mxCell(new GUIServiceGraphVertex(serviceName, 
					se.get().getClass().getCanonicalName(), 
					"in", se.get().getToolTip(), GUIServiceGraphVertex.Type.INPORT), geo1, 
					"shape=ellipse;perimter=ellipsePerimeter;fillColor=" + blockColor);
			
			inport.setVertex(true);

			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER,PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);

			mxCell outport = new mxCell(new GUIServiceGraphVertex(serviceName, 
					se.get().getClass().getCanonicalName(), 
					"out", se.get().getToolTip(), 
					GUIServiceGraphVertex.Type.OUTPORT), geo2, 
					"shape=ellipse;perimter=ellipsePerimeter;fillColor=" + blockColor);
			
			outport.setVertex(true);

			graph.addCell(inport, v1);
			graph.addCell(outport, v1);			

			x += 150;
			if (x > 400) {
				y += 150;
				x = 20;
			}

			++i;

		}

	}



	public void buildLocalServiceRoutes() {
		//Iterator<String> it = hostCFG.getServiceMap().keySet().iterator();
		Iterator<String> it = RuntimeEnvironment.getRegistry().keySet().iterator();

		Object parent = graph.getDefaultParent();
		
		
		while (it.hasNext()) {
			String serviceName = it.next();
			ServiceWrapper se = RuntimeEnvironment.getService(serviceName);

			//if (se.localServiceHandle != null) {
				Service s = RuntimeEnvironment.getService(serviceName).get();
				HashMap<String, ArrayList<NotifyEntry>> notifyList = s.getOutbox().notifyList;
				Iterator<String> ri = s.getOutbox().notifyList.keySet().iterator();
				while (ri.hasNext()) {
					ArrayList<NotifyEntry> nl = notifyList.get(ri.next());
					for (int i = 0; i < nl.size(); ++i) {
						NotifyEntry ne = nl.get(i);


						//createArrow(se.name, ne.name, methodString);
						//graph.getChildVertices(arg0)parent.
						//graph.getChildVertices(graph.getDefaultParent());
						graph.insertEdge(parent, null, formatMethodString(ne.outMethod_, ne.paramTypes, ne.inMethod_), serviceCells.get(s.name), serviceCells.get(ne.name));

					}
				}
			//}

		}

	}

	public static String formatMethodString (String out, Class[] paramTypes, String in)
	{
		// test if outmethod = in
		String methodString = out;
		//if (methodString != in) {
			methodString += "->" + in;
		//}

		// TODO FYI - depricate NotifyEntry use MethodEntry
		// These parameter types could always be considered "inbound" ? or returnType
		// TODO - view either full named paths or shortnames
		
		methodString += "(";

		if (paramTypes != null)
		{
			for (int j = 0; j < paramTypes.length; ++j)
			{								
				//methodString += paramTypes[j].getCanonicalName();
				Class c = paramTypes[j];
				String t[] = c.getCanonicalName().split("\\.");
				methodString += t[t.length -1];
					
				if (j < paramTypes.length - 1) {
					methodString += ",";
				}
			}
		}
			
		/*
		if (ne.paramType != null) {
			methodString += ne.paramType.substring(ne.paramType
					.lastIndexOf(".") + 1);
		}
		*/

		methodString += ")";
		
		return methodString;
	}
	
	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub

	}


	@Override
	public void keyTyped(KeyEvent e) {
		LOG.error("here");
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		LOG.error("here");
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		LOG.error("here");
	}
	
	// about begin

}
