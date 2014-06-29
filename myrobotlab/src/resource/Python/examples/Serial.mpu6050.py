import time
serial = Runtime.createAndStart("serial","Serial")
serial.connect("COM3", 38400, 8, 1, 0)
#have python listening to serial
serial.addListener("publishByteArray", python.name, "input") 
 
 
def input():
 byte1= msg_serial_publishByteArray.data[0]
 byte2= msg_serial_publishByteArray.data[1]
 print 'byte 1 is' , byte1
 print 'byte 2 is' , byte2