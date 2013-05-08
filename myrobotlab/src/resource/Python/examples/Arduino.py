# Connects a serial device on Windows this would COMx 
# You will need MRLComm.ino loaded on the Arduino
from time import sleep
from org.myrobotlab.service import Arduino

# create an Arduino service named arduino
arduino = runtime.createAndStart("arduino","Arduino")

#you have to replace COMX with your arduino serial port number
arduino.setSerialDevice("COM3",57600,8,1,0)

# give it a second for the serial device to get ready
sleep(1)

# update the gui with configuration changes
arduino.publishState()

# set the pinMode of pin 13 to output (you can change the pin number if you want)
arduino.pinMode(13, Arduino.OUTPUT)

# turn pin 13 on and off 10 times
for x in range(0, 10):
	arduino.digitalWrite(8,1)
	sleep(1) # sleep a second
	arduino.digitalWrite(8,0)
	sleep(1) # sleep a second