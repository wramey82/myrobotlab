# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a right hand or finger box
inMoov = Runtime.createAndStart("inMoov", "InMoov")

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control !
# set COM number according to the com of your Arduino board
inMoov.attachArduino("right","uno","COM12")

# attach the right hand
inMoov.attachHand("right")

# system check
inMoov.systemCheck()

# listen for these key words
# to get voice to work - you must be attached to the internet for
# at least the first time
inMoov.startListening("rest | open hand | close hand | manual | voice control")

# voice control
def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data
  #mouth.setLanguage("fr")
  mouth.speak("you said " + data)
  if (data == "rest"):
    inMoov.rest() 
  elif (data == "open hand"):
    inMoov.handOpen("right")
  elif (data == "close hand"):
    inMoov.handClose("right")
  elif (data == "manual"):
    inMoov.lockOutAllGrammarExcept("voice control")
  elif (data == "voice control"):
    inMoov.clearGrammarLock()