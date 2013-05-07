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

# fast sweep
servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

servo01.moveTo(179)
sleep(0.5)

servo01.moveTo(10)
sleep(0.5)

# slow sweep
#for x in range(10,170):
#  servo01.moveTo(x)
#  sleep(0.05)
  
servo01.setSpeed(0.99) # set speed to 99% of full speed
servo01.moveTo(90) 
sleep(0.5)
servo01.setSpeed(0.25) # set speed to 25% of full speed
servo01.moveTo(180)

