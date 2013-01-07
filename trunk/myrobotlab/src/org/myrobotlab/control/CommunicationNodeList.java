package org.myrobotlab.control;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

public class CommunicationNodeList extends JPanel {

	private static final long serialVersionUID = 1L;

	public DefaultListModel model = new DefaultListModel();
	public JList nodeList;

	public CommunicationNodeList() {
		setLayout(new BorderLayout());

		model = new DefaultListModel();
		nodeList = new JList(model);
		nodeList.setCellRenderer(new CommunicationNodeRenderer());
		nodeList.setVisibleRowCount(8);
		JScrollPane pane = new JScrollPane(nodeList);
		add(pane, BorderLayout.NORTH);
		// add(button, BorderLayout.SOUTH);
	}

	public final static Logger log = Logger.getLogger(OpenCV.class.getCanonicalName());

	/*
	 * // An inner class to respond to clicks on the Print button class
	 * PrintListener implements ActionListener { public void
	 * actionPerformed(ActionEvent e) { int selected[] =
	 * nodeList.getSelectedIndices();
	 * System.out.println("Selected Elements:  ");
	 * 
	 * for (int i = 0; i < selected.length; i++) { CommunicationNodeEntry
	 * element = (CommunicationNodeEntry) nodeList.getModel()
	 * .getElementAt(selected[i]); System.out.println("  " +
	 * element.getTitle()); } } }
	 */

	public static void main(String s[]) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		JFrame frame = new JFrame("List Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		CommunicationNodeList cl = new CommunicationNodeList();

		cl.model.add(0, (Object) new CommunicationNodeEntry("0.0.0.0:6432 -> 192.168.0.5:6767 latency 32ms rx 30 tx 120 msg 5 UDP", "3.gif"));
		cl.model.add(0, (Object) new CommunicationNodeEntry("192.168.0.3:6767 disconnected ", "3.gif"));
		cl.model.add(0, (Object) new CommunicationNodeEntry("0.0.0.0:6432 -> 192.168.0.4:6767 latency 14ms rx 12 tx 430 msg 5 UDP", "3.gif"));
		cl.model.add(0, (Object) new CommunicationNodeEntry("0.0.0.0:6432 -> 192.168.0.7:6767 latency 05ms rx 14 tx 742 msg 5 UDP", "3.gif"));

		frame.setContentPane(cl);
		frame.pack();
		frame.setVisible(true);

	}

}