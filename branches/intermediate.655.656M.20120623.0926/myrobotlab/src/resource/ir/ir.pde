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


#include <NewSoftSerial.h>

NewSoftSerial irSerial(11, 3);

void setup()  
{
  Serial.begin(57600);
  Serial.println("Goodnight moon!");

  // set the data rate for the NewSoftSerial port
  irSerial.begin(2400);
  irSerial.println("Hello, world?");
}

void loop()                     // run over and over again
{

  if (irSerial.available()) {
      char x = (char)irSerial.read();
      if (x != 0xFF)
      {
        Serial.print(x,BYTE);
        Serial.print("\n");
      }
  }

  //if (Serial.available()) {
  //    mySerial.print((char)Serial.read());
  //}
}
