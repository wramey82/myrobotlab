/**
 *
 * @author greg (at) myrobotlab.org
 *
 * This file is part of MyRobotLab.
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
 * arduinoSerial
 * -----------------
 * Purpose: translate serial commands into Arduino language commands,
 * mostly relating to IO.  This would allow a computer to easily take
 * advantage of Arduino's great IO capabilities, while cpu number crunching
 * could be done on the computer (e.g. Video processing)
 *
 * Created 2 Februrary 2009
 *
 * http://myrobotlab.org
 *
 * TODO - create analogSensitivity (delta) & analogGain (scalar)
 *
 */

#include <Servo.h>

#define DIGITAL_WRITE        0
#define DIGITAL_VALUE        1
#define ANALOG_WRITE         2
#define ANALOG_VALUE         3
#define PINMODE              4
#define PULSE_IN             5
#define SERVO_ATTACH         6
#define SERVO_WRITE          7
#define SERVO_SET_MAX_PULSE  8
#define SERVO_DETACH         9
#define SET_PWM_FREQUENCY    11
#define SERVO_READ           12
#define ANALOG_READ_POLLING_START	 13
#define ANALOG_READ_POLLING_STOP	 14
#define DIGITAL_READ_POLLING_START	 15
#define DIGITAL_READ_POLLING_STOP	 16
#define SET_ANALOG_TRIGGER               17
#define REMOVE_ANALOG_TRIGGER            18
#define SET_DIGITAL_TRIGGER              19
#define REMOVE_DIGITAL_TRIGGER           20

#define COMMUNICATION_RESET	   252
#define SOFT_RESET			   253
#define SERIAL_ERROR           254
#define NOP  255

#define MAGIC_NUMBER    170 // 10101010

// pin services
#define POLLING_MASK 1
#define TRIGGER_MASK 2
// TODO #define SERVO_SWEEP

Servo servos[MAX_SERVOS];

unsigned long loopCount = 0;
int byteCount 		= 0;
unsigned char newByte 		= 0;
unsigned char ioCommand[4]; 	// most io fns can cleanly be done with a 4 byte code
int readValue;
int nibbleCount = 0;        // wii-led protocol

int digitalReadPin[13];          	// array of pins to read from
int digitalReadPollingPinCount = 0;     // number of pins currently reading
int lastDigitalInputValue[13];   	// array of last input values
int digitalPinService[13];              // the services this pin is involved in
bool sendDigitalDataDeltaOnly	= false; // send data back only if its different

int analogReadPin[4];          		// array of pins to read from
int analogReadPollingPinCount = 0;     	// number of pins currently reading
int lastAnalogInputValue[4];   		// array of last input values
int analogPinService[4];                // the services this pin is involved in
bool sendAnalogDataDeltaOnly = false;	// send data back only if its different

unsigned long retULValue;

unsigned int errorCount = 0;

void setup() {
  Serial.begin(57600);        // connect to the serial port
}

void setPWMFrequency (int address, int prescalar)
{
  int clearBits = 0x07;
  if (address == 0x25)
  {
    TCCR0B &= ~clearBits;
    TCCR0B |= prescalar;
  } else if (address == 0x2E)
  {
    TCCR1B &= ~clearBits;
    TCCR1B |= prescalar;
  } else if (address == 0xA1)
  {
    TCCR2B &= ~clearBits;
    TCCR2B |= prescalar;
  }

}

void removeAndShift (int array [], int& len, int removeValue)
{
  int pos = -1;

  if (len == 0)
  {
	  return;
  }

  // find position of value
  for (int i = 0; i < len; ++i)
  {
	if (removeValue == array[i])
	{
	  pos = i;
	  break;
	}
  }
  // if at the end just decrement size
  if (pos == len - 1)
  {
	  --len;
	  return;
  }

  // if found somewhere else shift left
  if (pos < len && pos > -1)
  {
	  for (int j = pos; j < len - 1; ++j)
	  {
		array[j] = array[j+1];
	  }
	  --len;
  }
}


/*
 * getCommand - retrieves a command message
 * input messages are in the following format
 *
 * command message - (4 byte protocol) MAGICNUMBER|FUNCTION|DATA0|DATA1
 * e.g. digitalWrite (13, 1)     = DIGITAL_WRITE|13|1 = 170|0|13|1
 *
 * return message  - (5 byte protocol) MAGICNUMBER|FUNCTION|DATA0|MSB|LSB
 * e.g. results of analogRead = ANALOG_READ|3|1|1  = 170|3|3|257
 *
 */


boolean getCommand ()
{
    // handle serial data begin
    if (Serial.available() > 0)
    {
      // read the incoming byte:
      newByte = Serial.read();
      
      if (byteCount == 0 && newByte != MAGIC_NUMBER)
      {
         // ERROR !!!!!
         // TODO - call error method - notify sender
         ++errorCount;
         return false;
      }
      
      ioCommand[byteCount] = newByte;
      ++byteCount;
      
      if (byteCount > 3)
      {
        return true;
      }
    } // if Serial.available

  return false;

}

/*
set pollingRead = true if any of the pins are commanded to read
default behavior should be to poll read
*/

void loop () {

  ++loopCount;

  if (getCommand())
  {
/*
must comment out debugging - if serial control is used
all Serial, debugging assumes no
interference will occur due to serial
processing
*/

/*
Serial.print("c[");
Serial.print(ioCommand[0],HEX);
Serial.print("|");
Serial.print(ioCommand[1],HEX);
Serial.print("|");
Serial.print(ioCommand[2],HEX);
Serial.print("]\n");
*/

        switch (ioCommand[0])
        {
           case DIGITAL_WRITE:
             digitalWrite(ioCommand[1], ioCommand[2]);
           break;
           case ANALOG_WRITE:
             analogWrite(ioCommand[1], ioCommand[2]);
           break;
           case PINMODE:
             pinMode(ioCommand[1], ioCommand[2]);
           break;
           case PULSE_IN:
             retULValue = pulseIn(ioCommand[1], ioCommand[2]);
           break;
           case SERVO_ATTACH:
             servos[ioCommand[1]].attach(ioCommand[2]);
           break;
           case SERVO_WRITE:
               servos[ioCommand[1]].write(ioCommand[2]);
           break;
           case SERVO_READ:
             //Serial.write(servos[ioCommand[1]].read());
           break;
           case SERVO_SET_MAX_PULSE:
             //servos[ioCommand[1]].setMaximumPulse(ioCommand[2]);    TODO - lame fix hardware
           break;
           case SERVO_DETACH:
             servos[ioCommand[1]].detach();
           break;
           case SET_PWM_FREQUENCY:
             setPWMFrequency (ioCommand[1], ioCommand[2]);
           break;
           case ANALOG_READ_POLLING_START:
             analogReadPin[analogReadPollingPinCount] = ioCommand[1]; // put on polling read list
             analogPinService[ioCommand[1]] |= POLLING_MASK;
             // TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT - if already set don't increment
             ++analogReadPollingPinCount;
           break;
           case ANALOG_READ_POLLING_STOP:
             // TODO - MAKE RE-ENRANT
             removeAndShift(analogReadPin, analogReadPollingPinCount, ioCommand[1]);
             analogPinService[ioCommand[1]] &= ~POLLING_MASK;
           break;
           case DIGITAL_READ_POLLING_START:
             // TODO - MAKE RE-ENRANT
             digitalReadPin[digitalReadPollingPinCount] = ioCommand[1]; // put on polling read list
             ++digitalReadPollingPinCount;
           break;
           case DIGITAL_READ_POLLING_STOP:
             // TODO - MAKE RE-ENRANT
             removeAndShift(digitalReadPin, digitalReadPollingPinCount, ioCommand[1]);
             digitalPinService[ioCommand[1]] &= ~POLLING_MASK;
           break;
           case SET_ANALOG_TRIGGER:
             // TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
             analogReadPin[analogReadPollingPinCount] = ioCommand[1]; // put on polling read list
             analogPinService[ioCommand[1]] |= TRIGGER_MASK;
             ++analogReadPollingPinCount;
           break;

           case NOP:
             // No Operation
           break;
           default:
//             Serial.print("unknown command!\n");
           break;
        }

        // reset buffer
        ioCommand[0] = -1;
        ioCommand[1] = -1;
        ioCommand[2] = -1;
        ioCommand[3] = -1;
        byteCount = 0;

  } // if getCommand()

  // digital polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
  for (int i  = 0; i < digitalReadPollingPinCount; ++i)
  {
    // read the pin
    readValue = digitalRead(digitalReadPin[i]);

    // if my value is different then last time  && config - send it
    if (lastDigitalInputValue[digitalReadPin[i]] != readValue  || !sendDigitalDataDeltaOnly)
    {
      //++encoderValue;
      //if (encoderValue%300 == 0)
      //{
        Serial.write(DIGITAL_VALUE);
        Serial.write(digitalReadPin[i]); // TODO - have to encode it to determine where it came from
        Serial.write(readValue >> 8);   // MSB
        Serial.write(readValue); // LSB
      //}
    }

    // set the last input value of this pin
    lastDigitalInputValue[digitalReadPin[i]] = readValue;
  }


  // analog polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
  for (int i  = 0; i < analogReadPollingPinCount; ++i)
  {
    // read the pin
    readValue = analogRead(analogReadPin[i]);

    // if my value is different then last time - send it
    if (lastAnalogInputValue[analogReadPin[i]] != readValue   || !sendAnalogDataDeltaOnly) //TODO - SEND_DELTA_MIN_DIFF
    {
      Serial.write(ANALOG_VALUE);
      Serial.write(analogReadPin[i]);
      Serial.write(readValue >> 8);   	// MSB
      Serial.write(readValue & 0xFF);	// LSB
    }
    // set the last input value of this pin
    lastAnalogInputValue[analogReadPin[i]] = readValue;
  }

  delay(20); // necessary? TODO - onfigurable - SET_DELAY

} // loop

