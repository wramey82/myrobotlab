import time
 
#create a Serial service named serial
jf = Runtime.createAndStart("jf","JFugue")
serial = Runtime.createAndStart("serial","Serial")
count = 0
def input():
 global word
 global count
 word = int(serial.readByte())
 print word
 #count is to avoid lagging and continuous playing
 if ((word < 20) and (count == 0)) :
  count = 1
  jf.play('A')
 elif ((word > 20) and (count == 1)) :
  count = 0
#have python listening to serial
serial.addListener("publishByte", python.name, "input") 
#connect to a serial port COM4 57600 bitrate 8 data bits 1 stop bit 0 parity
serial.connect("COM3", 9600, 8, 1, 0)
#sometimes its important to wait a little for hardware to get ready
sleep(1)
