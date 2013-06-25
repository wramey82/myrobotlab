package org.myrobotlab.service.interfaces;


public interface StepperControl {
	
	public void attach(StepperController arduino, Integer pin1, Integer pin2);
	
	public void attach(StepperController arduino, Integer pin1, Integer pin2, Integer pin3, Integer pin4);
	
	public void setSpeed(Integer rpm);
	
	public void step(Integer steps);
	

}
