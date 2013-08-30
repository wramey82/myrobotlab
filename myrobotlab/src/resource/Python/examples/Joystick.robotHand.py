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
def a():
    #button 0 because there is a strange thing... button = "button you see" - 1..so in this case i'm pressing button1
    #but i have to write button 0 (start ccounting by 0) - TODO adjust.. we are men not machine :D
    a = msg_joystick_button0.data[0]
    print a
    if (a == 1):
     print 'button pressed'
    elif ( a == 0):
     print 'button not pressed'
   
#create a message route from joy to python so we can listen for button
joystick.addListener("XAxisRaw", python.name, "x")
joystick.addListener("button0", python.name, "a")
