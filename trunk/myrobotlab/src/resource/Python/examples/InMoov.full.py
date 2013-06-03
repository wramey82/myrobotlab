# a basic script for starting the InMoov service
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis

# ADD SECOND STAGE CONFIRMATION
#  instead of saying: you said... it would say: did you say...? and I would confirm with yes or give the voice command again
#  face tracking in InMoov ... activated by voice ...

inMoov = Runtime.createAndStart("inMoov", "InMoov")

rightSerialPort = "COM8"
leftSerialPort = "COM7"
cameraIndex = 1

# attach an arduinos to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control
inMoov.attachArduino("right","uno",rightSerialPort)
inMoov.attachHand("right")
inMoov.attachArm("right")

inMoov.attachArduino("left","atmega1280", leftSerialPort)
inMoov.attachHand("left")
inMoov.attachArm("left")
inMoov.attachHead("left")


# setting speech's language
# regrettably voice recognition is only in
# English
# inMoov.setLanguage("fr")
# inMoov.setLanguage("it")
# inMoov.setLanguage("en")


# system check
inMoov.systemCheck()
inMoov.rest()

# listen for these key words
inMoov.startListening("rest | open hand | close hand | manual | voice control| capture gesture | track | freeze track | hello | giving | fighter | fist hips | look at this | victory | arms up | arms front | da vinci")

# voice control
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data
  
  mouth.speak("you said " + data)
  
  if (data == "rest"):
    inMoov.rest() 
  elif (data == "open hand"):
    inMoov.handOpen("both")
  elif (data == "close hand"):
    inMoov.handClose("both")
  elif (data == "manual"):
    inMoov.lockOutAllGrammarExcept("voice control")
  elif (data == "voice control"):
    inMoov.clearGrammarLock()
  elif (data == "capture gesture"):
    inMoov.captureGesture();
  elif (data == "track"):
    inMoov.trackPoint(0.5, 0.5)
  elif (data == "freeze track"):
    inMoov.clearTrackingPoints()
  elif (data == "hello"):
    inMoov.hello()
  elif (data == "giving"):
    inMoov.giving()
  elif (data == "fighter"):
    inMoov.fighter()
  elif (data == "fist hips"):
    inMoov.fistHips() 
  elif (data == "look at this"):
    inMoov.lookAtThis()
  elif (data == "victory"):
    inMoov.victory()
  elif (data == "arms up"):
    inMoov.armsUp() 
  elif (data == "arms front"):
    inMoov.armsFront()
  elif (data == "da vinci"):
    inMoov.daVinci()
