package org.myrobotlab.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.myrobotlab.service.GUIService;
import org.myrobotlab.service.data.Pin;

public class Motor_AdafruitMotorShieldGUI extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private GUIService myService;
	
	JLabel motorPortLabel = new JLabel("motor port");
	JComboBox motorPort = new JComboBox();
	JButton attachButton = new JButton("attach");
	String arduinoName;
	String motorName;

	ArrayList<Pin> pinList  = null;
	public Motor_AdafruitMotorShieldGUI(GUIService myService, String motorName, String controllerName)
	{
		this.myService = myService;
		this.arduinoName = arduinoName;
		this.motorName = motorName;

		// TODO - get list of motor ports from Adafruit which are currently free
		
		/*
		AdafruitMotorShield o = (AdafruitMotorShield) myService.sendBlocking(controllerName, "publishState", null);
		pinList = o.getPinList(); // ?? how to handle
		
		//setLayout(new BorderLayout());
		
		for (int i = 0; i < pinList.size(); ++i)
		{
			Pin pin = pinList.get(i);
			if (pin.type == Pin.PWM_VALUE) {
				motorPort.addItem(String.format("<html><font color=white bgcolor=green>%d</font></html>",pin.pin));
			} else {
				motorPort.addItem(String.format("%d",pin.pin));
			}
		}
		*/
		
		
		// TODO - fix up with only free ports offered in drop down
		
		for (int i = 0; i < 4; ++i)
		{
			motorPort.addItem(String.format("m%d",i));
		}
		
		setBorder(BorderFactory.createTitledBorder("type - Adafruit Motor Shield"));
		add(motorPortLabel);
		add(motorPort);
		add(attachButton);
		setEnabled(true);
		
		attachButton.addActionListener(this);


	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		
		if (o == attachButton)
		{
			Object[] motorData = null;// new Object[]{new Integer(powerPin.getSelectedIndex()), new Integer(directionPin.getSelectedIndex())};
			myService.send(arduinoName, "motorAttach", motorName, motorData);
		}
		
	}		
	

}
