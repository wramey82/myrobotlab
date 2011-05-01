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

	public final static String digitalWrite = "digitalWrite";

	/**
	 * createMotor - creats a new service - probably through the interface
	 * hopefully much of the managing through the interface will be hidden
	 * 
	 * @param name
	 *            - name of the Motor
	 * @return Motor
	 */
	// GenericMotor createMotor(String name);

	/**
	 * createMotor - creats a new service - probably through the interface
	 * hopefully much of the managing through the interface will be hidden
	 * 
	 * @param name
	 *            - name of the Motor
	 * @param PWMPin
	 *            - power pin in a DPDT motor controller
	 * @param DIRPin
	 *            - direction pin in a DPDT motor controller
	 * @return Motor
	 */
	// GenericMotor createMotor(String name, int PWMPin, int DIRPin);
	// GenericMotor createHSmokeMotor(String name, int PWMPin, int DIRPin);

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
	 * MotorAttach - attach the Motor to a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Motor
	 * @param pin
	 *            - pin number
	 * @return void
	 */
	void motorAttach(String name, Integer PWMPin, Integer DIRPin);

	/**
	 * MotorDetach - detach the Motor from a specific pin on the controller
	 * 
	 * @param name
	 *            - name of the Motor
	 * @return void
	 */
	void motorDetach(String name);

}
