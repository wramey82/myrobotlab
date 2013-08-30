arduino = Runtime.createAndStart("arduino","Arduino")
joystick = runtime.createAndStart("joystick","Joystick")
hand  = Runtime.createAndStart("hand","Servo")
arduino.setSerialDevice("COM3", 57600, 8, 1, 0)
sleep(4)
arduino.attach(hand.getName() , 2)

b = 100
print b
hand.moveTo(b)
 
def x():
    global b
    x = msg_joystick_XAxisRaw.data[0]
    print x
    if (x == 1):
     b += 1
     print b
     hand.moveTo(b)
     
    elif (x == -1):
     b -= 1
     print b
     hand.moveTo(b)
    return
   
#create a message route from joy to python so we can listen for button
joystick.addListener("XAxisRaw", python.name, "x")
