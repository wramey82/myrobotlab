package org.myrobotlab.control;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;


public class Test {

	//RSyntaxTextArea editor = new RSyntaxTextArea();
	 RSyntaxTextArea editor = new RSyntaxTextArea();
	public void display()
	{
		RTextScrollPane sp = new RTextScrollPane(editor);
		
	    JFrame f = new JFrame("A JFrame");
	    f.setSize(250, 250);
	    f.setLocation(300,200);
	    f.getContentPane().add(BorderLayout.CENTER, new JTextArea(10, 40));
	    
	    //JPanel p = new JPanel();
	    //p.add(editor);
	    
	    JPanel cp = new JPanel(new BorderLayout());
	    cp.add(sp);	    
	    f.getContentPane().add(cp);
	    
	    f.setVisible(true);

	}
	
	 public static void main(String[] args) {
		 Test test = new Test();
		 test.display();
		    
		  }
}
