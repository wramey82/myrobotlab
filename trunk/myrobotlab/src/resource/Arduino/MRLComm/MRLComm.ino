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
* References :
*      http://www.arduino.cc/en/Reference/Constants
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
#define SET_SERVO_SPEED           12
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

// --VENDOR DEFINE SECTION BEGIN--
// --VENDOR DEFINE SECTION END--

// -- FIXME - modified by board type BEGIN --
#define ANALOG_PIN_COUNT 4
#define DIGITAL_PIN_COUNT 13
// -- FIXME - modified by board type END --

long debounceDelay = 50; // in ms
long lastDebounceTime[DIGITAL_PIN_COUNT];

Servo servos[MAX_SERVOS];
int servoSpeed[MAX_SERVOS];    // 0 - 100 corresponding to the 0.0 - 1.0 Servo.setSpeed - not a float at this point
int servoTargetPosition[MAX_SERVOS];  // when using a fractional speed - servo's must remember their end destination
int servoCurrentPosition[MAX_SERVOS]; // when using a fractional speed - servo's must remember their end destination
int movingServos[MAX_SERVOS];		  // array of servos currently moving at some fractional speed
int movingServosCount = 0;            // number of servo's currently moving at fractional speed

unsigned long loopCount     = 0;
int byteCount               = 0;
unsigned char newByte 		= 0;
unsigned char ioCommand[4];  // most io fns can cleanly be done with a 4 byte code
int readValue;

int digitalReadPin[DIGITAL_PIN_COUNT];        // array of pins to read from
int digitalReadPollingPinCount = 0;           // number of pins currently reading
int lastDigitalInputValue[DIGITAL_PIN_COUNT]; // array of last input values
int digitalPinService[DIGITAL_PIN_COUNT];     // the services this pin is involved in
bool sendDigitalDataDeltaOnly	= false;      // send data back only if its different

int analogReadPin[ANALOG_PIN_COUNT];          // array of pins to read from
int analogReadPollingPinCount = 0;            // number of pins currently reading
int lastAnalogInputValue[ANALOG_PIN_COUNT];   // array of last input values
int analogPinService[ANALOG_PIN_COUNT];       // the services this pin is involved in
bool sendAnalogDataDeltaOnly = false;         // send data back only if its different

unsigned long retULValue;

unsigned int errorCount = 0;

void setup() {
	Serial.begin(57600);        // connect to the serial port

	softReset();

	// --VENDOR SETUP BEGIN--
	// --VENDOR SETUP END-- 
}

// 
void softReset()
{

	for (int i = 0; i < MAX_SERVOS - 1; ++i)
	{
		servoSpeed[i] = 100;
		servos[i].detach();
	}

	for (int j = 0; j < DIGITAL_PIN_COUNT - 1; ++j)
	{
		pinMode(j, OUTPUT);
	}


	digitalReadPollingPinCount = 0;
	analogReadPollingPinCount = 0;
	loopCount = 0;


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
			// TODO - call modulus error method - notify sender
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

		switch (ioCommand[1])
		{
		case DIGITAL_WRITE:
			digitalWrite(ioCommand[2], ioCommand[3]);
			break;
		case ANALOG_WRITE:
			analogWrite(ioCommand[2], ioCommand[3]);
			break;
		case PINMODE:
			pinMode(ioCommand[2], ioCommand[3]);
			break;
		case PULSE_IN:
			retULValue = pulseIn(ioCommand[2], ioCommand[3]);
			break;
		case SERVO_ATTACH:
			servos[ioCommand[2]].attach(ioCommand[3]);
			break;
		case SERVO_WRITE:
			if (servoSpeed[ioCommand[2]] == 100) // move at regular/full 100% speed
			{   
				// move at regular/full 100% speed
				// although not completely accurate
				// target position & current position are
				// updated immediately
				servos[ioCommand[2]].write(ioCommand[3]);
				servoTargetPosition[ioCommand[2]] = ioCommand[3];
				servoCurrentPosition[ioCommand[2]] = ioCommand[3];
			} else if (servoSpeed[ioCommand[2]] < 100 && servoSpeed[ioCommand[2]] > 0) {
				// start moving a servo at fractional speed 
				servoTargetPosition[ioCommand[2]] = ioCommand[3];
				movingServos[movingServosCount]=ioCommand[2];
				++movingServosCount;
			} else {
				// NOP - 0 speed - don't move
			}
			break;
		case SET_SERVO_SPEED:
			// setting the speed of a servo
			servoSpeed[ioCommand[2]]=ioCommand[3];
			break;
		case SERVO_SET_MAX_PULSE:
			//servos[ioCommand[1]].setMaximumPulse(ioCommand[2]);    TODO - lame fix hardware
			break;
		case SERVO_DETACH:
			servos[ioCommand[2]].detach();
			break;
		case SET_PWM_FREQUENCY:
			setPWMFrequency (ioCommand[2], ioCommand[3]);
			break;
		case ANALOG_READ_POLLING_START:
			analogReadPin[analogReadPollingPinCount] = ioCommand[2]; // put on polling read list
			analogPinService[ioCommand[2]] |= POLLING_MASK;
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT - if already set don't increment
			++analogReadPollingPinCount;
			break;
		case ANALOG_READ_POLLING_STOP:
			// TODO - MAKE RE-ENRANT
			removeAndShift(analogReadPin, analogReadPollingPinCount, ioCommand[2]);
			analogPinService[ioCommand[2]] &= ~POLLING_MASK;
			break;
		case DIGITAL_READ_POLLING_START:
			// TODO - MAKE RE-ENRANT
			digitalReadPin[digitalReadPollingPinCount] = ioCommand[2]; // put on polling read list
			++digitalReadPollingPinCount;
			break;
		case DIGITAL_READ_POLLING_STOP:
			// TODO - MAKE RE-ENRANT
			removeAndShift(digitalReadPin, digitalReadPollingPinCount, ioCommand[2]);
			digitalPinService[ioCommand[1]] &= ~POLLING_MASK;
			break;
		case SET_ANALOG_TRIGGER:
			// TODO - if POLLING ALREADY DON'T RE-ADD - MAKE RE-ENTRANT
			analogReadPin[analogReadPollingPinCount] = ioCommand[2]; // put on polling read list
			analogPinService[ioCommand[2]] |= TRIGGER_MASK;
			++analogReadPollingPinCount;
			break;
		case SOFT_RESET:
			softReset();
			break;

			// --VENDOR CODE BEGIN--
			// --VENDOR CODE END-- 

		case NOP:
			// No Operation
			break;
		default:
			//             Serial.print("unknown command!\n");
			break;
		}

		// reset buffer
		ioCommand[0] = -1; // MAGIC_NUMBER 
		ioCommand[1] = -1; // FUNCTION
		ioCommand[2] = -1; // PARAM 1
		ioCommand[3] = -1; // PARAM 2
		byteCount = 0;

	} // if getCommand()

	// digital polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
	for (int i  = 0; i < digitalReadPollingPinCount; ++i)
	{
		if (debounceDelay)
		{
		  if (millis() - lastDebounceTime[digitalReadPin[i]] < debounceDelay)
		  {
		    continue;
		  } 
		} 
	
		// read the pin
		readValue = digitalRead(digitalReadPin[i]);

		// if my value is different from last time  && config - send it
		if (lastDigitalInputValue[digitalReadPin[i]] != readValue  || !sendDigitalDataDeltaOnly)
		{
			Serial.write(MAGIC_NUMBER);
			Serial.write(DIGITAL_VALUE);
			Serial.write(digitalReadPin[i]);// Pin# 
			Serial.write(readValue >> 8);   // MSB
			Serial.write(readValue); 	// LSB

		        lastDebounceTime[digitalReadPin[i]] = millis();
		}

		// set the last input value of this pin
		lastDigitalInputValue[digitalReadPin[i]] = readValue;
	}


	// analog polling read - send data for pins which are currently in INPUT mode only AND whose state has changed
	for (int i  = 0; i < analogReadPollingPinCount; ++i)
	{
		// read the pin
		readValue = analogRead(analogReadPin[i]);

		// if my value is different from last time - send it
		if (lastAnalogInputValue[analogReadPin[i]] != readValue   || !sendAnalogDataDeltaOnly) //TODO - SEND_DELTA_MIN_DIFF
		{
			Serial.write(MAGIC_NUMBER);
			Serial.write(ANALOG_VALUE);
			Serial.write(analogReadPin[i]);
			Serial.write(readValue >> 8);   // MSB
			Serial.write(readValue & 0xFF);	// LSB
		}
		// set the last input value of this pin
		lastAnalogInputValue[analogReadPin[i]] = readValue;
	}

	// handle the servos going at fractional speed
	for (int i = 0; i < movingServosCount; ++i)
	{
		int servoIndex = movingServos[i];
		int speed = servoSpeed[servoIndex];
		if (servoCurrentPosition[servoIndex] != servoTargetPosition[servoIndex])
		{
			// caclulate the appropriate modulus to drive
			// the servo to the next position
			// TODO - check for speed > 0 && speed < 100 - send ERROR back?
			int speedModulus = (100 - speed) * 10;
			if (loopCount % speedModulus == 0)
			{
				int increment = (servoCurrentPosition[servoIndex]<servoTargetPosition[servoIndex])?1:-1;
				// move the servo an increment
				servos[servoIndex].write(servoCurrentPosition[servoIndex] + increment);
				servoCurrentPosition[servoIndex] = servoCurrentPosition[servoIndex] + increment;
			}
		} else {
			removeAndShift(movingServos, movingServosCount, servoIndex);
		}
	}



} // loop
