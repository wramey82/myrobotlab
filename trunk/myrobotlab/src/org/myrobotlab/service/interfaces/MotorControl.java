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

package org.myrobotlab.service.interfaces;

public interface MotorControl {
	
	public String getName();
	
	/**
	 *  attach a motor controller to the motor
	 */
	public boolean attach(MotorController controller);
	
	/**
	 *  detaches the motor from the motor controller
	 * @return
	 */
	public boolean detach();
	
	/**
	 *  reports if a motor is attached to a motor controller
	 */
	public boolean isAttached();
	
	/**
	 * get the current power level of the motor
	 * @return
	 */
	public float getPowerLevel();
	
	public boolean isDirectionInverted();
	
	public void invertDirection(boolean invert);
	
	public void stopAndLock();
	
	public void stop();
	
	public void lock();

}
