import time
 
#create a Serial service named serial
jf = Runtime.createAndStart("jf","JFugue")
serial = Runtime.createAndStart("serial","Serial")

if not serial.isConnected():
    #connect to a serial port COM4 57600 bitrate 8 data bits 1 stop bit 0 parity
    serial.connect("COM3", 9600, 8, 1, 0)
    #have python listening to serial
    serial.addListener("publishByte", python.name, "input") 


def input():
 global count
 newByte = int(serial.readByte())
 #we have reached the end of a new line
 if (newByte == 10) :
    distanceString = ""
    while (newByte != 13):
        newByte = serial.readByte()
        distanceString += chr(newByte)
    
    distance = int(distanceString)
    print distance
 