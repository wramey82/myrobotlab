arduino = Runtime.createAndStart("arduino","Arduino")
dx 	= Runtime.createAndStart("dx","Servo")
sx	= Runtime.createAndStart("sx","Servo")
arduino.setSerialDevice("COM3", 57600, 8, 1, 0)
sleep(4)
arduino.attach(dx.getName() , 3)
arduino.attach(sx.getName(), 6)
def forward():
 dx.moveTo(120)
 sx.moveTo(60)
 return
def back():
 dx.moveTo(60)
 sx.moveTo(120)
 return
def turnR ():
 dx.moveTo(120)
 sx.moveTo(120)
 return
def turnL ():
 dx.moveTo(60)
 sx.moveTo(60)
 return
def stop ():
 dx.moveTo(90)
 sx.moveTo(90)
 return

turnR()
sleep(2)
turnL()
sleep(2)
forward()
sleep(2)
back()
sleep(2)
stop()