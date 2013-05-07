from org.myrobotlab.service import Arduino
from org.myrobotlab.service import Servo
from org.myrobotlab.service import Runtime

from time import sleep

# create the services

arduino = Runtime.create("arduino","Arduino")
arduino.startService()
servo01 = Runtime.create("servo01","Servo")
servo01.startService()

# initialize arduino
arduino.setPort("/dev/ttyUSB0")
arduino.setSerialPortParams(115200, 8, 1, 0)

# attach servo
servo01.attach("arduino", 9)

#servo02 = Runtime.create("servo02","Servo")
#servo02.startService()

# fast sweep
servo01.moveTo(179)
sleep(0.5)
#servo02.moveTo(179)
#sleep(0.5)
servo01.moveTo(10)
sleep(0.5)
#servo02.moveTo(10)
#sleep(0.5)
servo01.moveTo(179)
sleep(0.5)
#servo02.moveTo(179)
#sleep(0.5)
servo01.moveTo(10)
sleep(0.5)
#servo02.moveTo(10)
#sleep(0.5)
servo01.moveTo(179)
sleep(0.5)
#servo02.moveTo(179)
#sleep(0.5)
servo01.moveTo(10)
sleep(0.5)
#servo02.moveTo(10)
#sleep(0.5)

# slow sweep
#for x in range(10,170):
#  servo01.moveTo(x)
#  sleep(0.05)
  

