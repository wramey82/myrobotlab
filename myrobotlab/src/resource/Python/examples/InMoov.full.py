# a basic script for starting the InMoov service
# and attaching the right hand
# an Arduino is required, additionally a computer
# with a microphone and speakers is needed for voice
# control and speech synthesis

# ADD SECOND STAGE CONFIRMATION
#  instead of saying: you said... it would say: did you say...? and I would confirm with yes or give the voice command again
#  face tracking in InMoov ... activated by voice ...

inMoov = Runtime.createAndStart("inMoov", "InMoov")

# attach an arduinos to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control
inMoov.attachArduino("right","uno","COM8")
inMoov.attachHand("right")
inMoov.attachArm("right")

inMoov.attachArduino("left","atmega1280","COM7")
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
inMoov.startListening("rest | camera on | camera off | camera enlarge | camera reduce | camera gray | camera color | hand open | hand close | manual | voice control| capture gesture | track | stop tracking")

# voice control
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data
  #mouth.setLanguage("fr")
  
  mouth.speak("you said " + data)
  
  if (data == "rest"):
    inMoov.rest() 
  elif (data == "camera on"):
    inMoov.cameraOn()
  elif (data == "camera off"):
    inMoov.cameraOff()
  elif (data == "camera enlarge"):
    inMoov.camerEnlarge()
  elif (data == "camera reduce"):
    inMoov.camerReduce()
  elif (data == "camera gray"):
    inMoov.camerGray()
  elif (data == "camera color"):
    inMoov.camerColor()
  elif (data == "hand open"):
    inMoov.handOpen("right")
  elif (data == "close hand"):
    inMoov.handClose("right")
  elif (data == "manual"):
    inMoov.lockOutAllGrammarExcept("voice control")
  elif (data == "voice control"):
    inMoov.clearGrammarLock()
  elif (data == "capture gesture"):
    inMoov.captureGesture();
  elif (data == "track"):
    inMoov.trackPoint(0.5, 0.5)
  elif (data == "stop tracking"):
    inMoov.stopTracking()

