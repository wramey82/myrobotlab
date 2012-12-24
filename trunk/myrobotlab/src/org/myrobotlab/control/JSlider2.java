package org.myrobotlab.control;

import javax.swing.JLabel;
import javax.swing.JSlider;

public class JSlider2 extends JSlider {
	private static final long serialVersionUID = 1L;
	JLabel value = new JLabel();

	public JSlider2(int vertical, int i, int j, int k) {
		super (vertical, i, j, k);
		value.setText("" + k);
	}

	public JSlider2(int vertical, int i, int j, float k) {
		super (vertical, i, j, (int)k);
		value.setText("" + k);
	}
	
}
