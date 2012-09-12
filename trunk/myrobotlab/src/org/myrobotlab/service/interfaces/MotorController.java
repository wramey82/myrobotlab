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


public interface MotorController {

	/**
	 * createMotor - creates a new Motor service. The data
	 * parameter is used for any necessary initialization info
	 * 
	 * @param data
	 *            - name of the Motor
	 * @return Motor
	 */
	 Motor createMotor(String name);


	/**
	 * moveTo - move the Motor a relative amount the amount can be negative or
	 * positive an integer value is expected
	 * 
	 * @param name
	 *            - name of the Motor
	 * @param position
	 *            - positive or negative absolute amount to move the Motor
	 * @return void
	 */
	void motorMoveTo(String name, Integer position);

	/**
	 * MotorMove - move the Motor a relative amount the amount can be negative
	 * or positive an integer value is expected
	 * 
	 * @param name
	 *            - name of the Motor
	 * @param amount
	 *            - positive or negative relative amount to move the Motor
	 * @return void
	 */
	void motorMove(String name, Integer amount);


	/**
	 * MotorDetach - detach the Motor from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Motor
	 * @return void
	 */
	void releaseMotor (String data);

	

}
