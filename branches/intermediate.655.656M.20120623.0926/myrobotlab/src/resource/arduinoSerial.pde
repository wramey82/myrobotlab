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
 */

#include <Servo.h>

// TODO - determine details of Arduino conflicts between NewSoftSerial, VirtualWire, Servo, & SoftwareServo

//#include <Servo.h> //conflicts with NewSoftSerial
//#include <SoftwareServo.h> // does not remove pwm from pin 9 & 10, but does conflict with NewSoftSerial
//#include <ServoTimer2.h>  // the servo timer library
//#include <Servo2.h>  // still another servo library

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

#define NOP  255

//ServoTimer2 servos[2];
//Servo servos[2]; 
//SoftwareServo servos[2];// - conflicts with NewSoftSerial.h
Servo servos[MAX_SERVOS]; //TODO - difference between Servo.h SoftwareServo.h MegaServo.h ????

unsigned long loopCount = 0;
int byteCount 		= 0;
unsigned char newByte 		= 0;
unsigned char ioCommand[3]; 	// most io fns can cleanly be done with a 3 byte code
int readValue; 
int nibbleCount = 0;        // wii-led protocol

int digitalReadPin[13];          	// array of pins to read from
int digitalReadPollingPinCount = 0;     // number of pins currently reading
int lastDigitalInputValue[13];   	// array of last input values

int analogReadPin[4];          		// array of pins to read from
int analogReadPollingPinCount = 0;     	// number of pins currently reading
int lastAnalogInputValue[4];   		// array of last input values

unsigned long retULValue;  
int prescalerVal = 0;

// for wiicom
int strobeState = 0;
boolean readySteady = false;



void setup() {
  // First clear all three prescaler bits:
  // prescalerVal = 0x07; //create a variable called prescalerVal and set it equal to the binary                                                       number "00000111"
  // TCCR0B &= ~prescalerVal; //AND the value in TCCR0B with binary number "11111000"

  // Now set the appropriate prescaler bits:
  // int prescalerVal = 1; //set prescalerVal equal to binary number "00000001"
  // TCCR0B |= prescalerVal; //OR the value in TCCR0B with binary number "00000001"

  // pinMode(ledPin,OUTPUT);    // declare the LED's pin as output
  // TODO init all as OUTPUT ! - or find documentation that say
  // 9600 57600 115200 - 1000000 refer to http://www.arduino.cc/cgi-bin/yabb2/YaBB.pl?num=1235580746
  Serial.begin(115200);        // connect to the serial port    


#define MIN_PULSE_WIDTH 544     // the shortest pulse sent to a servo  
#define MAX_PULSE_WIDTH 2400     // the longest pulse sent to a servo 


  // TODO - what a mess - have to set these low cause the 754410 seems to want to go full speed when enable is high impedence
//    pinMode(3, OUTPUT);
//    digitalWrite(3, 0);

//    pinMode(6, OUTPUT);
//    digitalWrite(6, 0);

/* What a mess! - reconcile setup of all servo libraries
  servos[0].attach(13);
  servos[0].setMinimumPulse(MIN_PULSE_WIDTH);
  servos[0].setMaximumPulse(MAX_PULSE_WIDTH);
  servos[0].write(90);    
  
  servos[1].attach(12);
  servos[1].setMinimumPulse(MIN_PULSE_WIDTH);
  servos[1].setMaximumPulse(MAX_PULSE_WIDTH);
  servos[1].write(90);    
*/  
    
  
  
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
 * command message - (3 byte protocol) FUNCTION|DATA0|DATA1
 * e.g. digitalWrite (13, 1)     = DIGITAL_WRITE|13|1 = 0|13|1
 *
 * return message  - (4 byte protocol) FUNCTION|DATA0|MSB|LSB
 * e.g. results of analogRead = ANALOG_READ|3|1|1  = 3|3|257
 *
 */


boolean getCommand ()
{
    // handle serial data begin 
    if (Serial.available() > 0)
    {
      // read the incoming byte:
      newByte = Serial.read();
      ioCommand[byteCount] = newByte;
      ++byteCount;

      if (byteCount > 2)
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
             // what a mess - reconcile all servo libraries
/*           
               servos[ioCommand[1]].attach(ioCommand[2]);
               servos[ioCommand[1]].setMinimumPulse(MIN_PULSE_WIDTH);
               servos[ioCommand[1]].setMaximumPulse(MAX_PULSE_WIDTH);
               servos[ioCommand[1]].write(90);               
*/               
               
           break;
           case SERVO_WRITE:
               servos[ioCommand[1]].write(ioCommand[2]);
//               servos[ioCommand[1]].write(ioCommand[2] * 10 + 544); - appropriate for ServoTimer2
//              servos[ioCommand[1]].writeMicroseconds(ioCommand[2] * 10 + 544);
           break;
           case SERVO_READ:
//             Serial.write(servos[ioCommand[1]].read() >> 8);   // MSB
//             Serial.write(servos[ioCommand[1]].read()); // LSB
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
             ++analogReadPollingPinCount;
           break;
           case ANALOG_READ_POLLING_STOP:
             removeAndShift(analogReadPin, analogReadPollingPinCount, ioCommand[1]);
           break;
           case DIGITAL_READ_POLLING_START:
             digitalReadPin[digitalReadPollingPinCount] = ioCommand[1]; // put on polling read list
             ++digitalReadPollingPinCount;
           break;
           case DIGITAL_READ_POLLING_STOP:
             removeAndShift(digitalReadPin, digitalReadPollingPinCount, ioCommand[1]);
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
        byteCount = 0;           
        nibbleCount = 0;
     
  } // if getCommand()

  // polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
  for (int i  = 0; i < digitalReadPollingPinCount; ++i)
  {
    // read the pin
    readValue = digitalRead(digitalReadPin[i]);
      
    // if my value is different then last time - send it
    if (lastDigitalInputValue[digitalReadPin[i]] != readValue)
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
    

  // polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
  for (int i  = 0; i < analogReadPollingPinCount; ++i)
  {
    // read the pin
    readValue = analogRead(analogReadPin[i]);
      
    // if my value is different then last time - send it
    if (lastAnalogInputValue[analogReadPin[i]] != readValue)// - POLLING DIFS VS POLLING !! COMMAND
    {       
      Serial.write(ANALOG_VALUE);
      Serial.write(analogReadPin[i]); // TODO - have to encode it to determine where it came from
      Serial.write(readValue >> 8);   // MSB
      Serial.write(readValue & 0xFF);        // LSB
    }
    // set the last input value of this pin
    lastAnalogInputValue[analogReadPin[i]] = readValue;
  }

  //SoftwareServo::refresh();// - conflicts with NewSoftSerial
  delay(20);
 
} // loop

