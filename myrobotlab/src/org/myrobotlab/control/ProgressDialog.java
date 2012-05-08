package org.myrobotlab.control;

import java.awt.BorderLayout;
import java.awt.SystemColor;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class ProgressDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private String data = "";
	private JTextArea textArea = null;
	
	public ProgressDialog(JFrame frame) {
		super(frame, "new components");
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JLabel lblNewLabel = new JLabel();
		panel.add(lblNewLabel);
		lblNewLabel.setIcon(new ImageIcon(ProgressDialog.class.getResource("/resource/progressBar.gif")));
		
		JLabel lblDownloadingNewComponents = new JLabel("Downloading new components");
		panel.add(lblDownloadingNewComponents);
		
		textArea = new JTextArea("details", 5, 10);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(SystemColor.control);
		getContentPane().add(textArea, BorderLayout.SOUTH);	
	    setSize(320, 240);
	    setLocationRelativeTo(frame);
	}
	
	public void addInfo(String info)
	{
		data += "\n" + info;
		textArea.setText(data);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
