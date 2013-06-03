# a larger script for InMoov
inMoov = Runtime.createAndStart("inMoov", "InMoov")

# variables to adjust
leftSerialPort = "COM7"
cameraIndex = 1

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control !
# set COM number according to the com of your Arduino board

inMoov.attachArduino("left","uno", leftSerialPort)
# if you have a laptop with a camera the one in InMoov is likely to be index #1
inMoov.setCameraIndex(cameraIndex)
inMoov.attachHead("left")
inMoov.trackPoint(0.5,0.5)

# system check
inMoov.systemCheck()
 
# listen for these key words
# to get voice to work - you must be attached to the internet for
# at least the first time
inMoov.startListening("track | freeze track | manual | voice control")
 
# voice control
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data

  mouth.speak("you said " + data)
  if (data == "rest"):
    inMoov.rest() 
  elif (data == "track"):
    inMoov.trackPoint(0.5,0.5)
  elif (data == "freeze track"):
    inMoov.clearTrackingPoints();
  elif (data == "manual"):
    inMoov.lockOutAllGrammarExcept("voice control")
  elif (data == "voice control"):
    inMoov.clearGrammarLock()