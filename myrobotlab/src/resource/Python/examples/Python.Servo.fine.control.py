# system specific variables
port = "COM9"
servoPin = 7
joystickIndex = 2

joystick = Runtime.createAndStart("joystick", "Joystick")
arduino = Runtime.createAndStart("arduino", "Arduino")
servo = Runtime.createAndStart("servo", "Servo")

if (not joystick.isPolling()):
  joystick.setController(2)
  joystick.startPolling()

if (not arduino.isConnected()):
  print "connecting"
  arduino.connect(port)
  print "attaching servo"
  arduino.servoAttach(servo.getName(), servoPin)
  joystick.addListener("YAxisRaw", python.name, "y")
  joystick.addListener("button1", python.name, "a")

servoDirection = 0

# set servo speed 
# if you are going to do "stops" on a servo
# it MUST have speed < 1.0
servo.setSpeed(0.7)

def y():
    global servoDirection
    servoDirection = msg_joystick_YAxisRaw.data[0]
    print "servoDirection ", servoDirection

def moveServo():
    global servoDirection
    # API is now sissy 1 based for those who don't know how to deal with 0 index :)
    button1 = msg_joystick_button1.data[0]
    if (button1 == 1):
      if (servoDirection == 1):
        print 'is moving up to ', upperLimit
        servo.moveTo(upperLimit)
      elif (servoDirection == -1):
         servo.moveTo(lowerLimit)
         print 'is moving down to ' lowerLimit
      else:
         print 'no direction'
    else:
     print 'is not moving'

servo.moveTo(0)
sleep(0.5)
servo.moveTo(90)
sleep(0.5)
servo.moveTo(180)
sleep(0.5)
servo.moveTo(90)

