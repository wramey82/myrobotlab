from java.lang import String
from org.myrobotlab.service import Speech
from org.myrobotlab.service import SpeechRecognition
from org.myrobotlab.framework import ServiceFactory

# This demo is a basic speech recognition script.
#
# A set of commands needs to be defined before the recognizer starts
# Internet connectivity is needed initially to download the audio files
# of the Speech service (its default behavior interfaces with Google)
# once the phrases are spoken once, the files are used from that point on
# and internet connectivity is no longer used.  These cached files 
# can be found ./audioFile/google/<language code>/audrey/phrase.mpe
#
# A message route is created to NOT recognize speech when MRL is talking.
# Otherwise, initially amusing scenarios can occur such as infinite loops of
# the robot recognizing "hello", then saying "hello", then recognizing "hello"...
#
# The recognized phrase can easily be hooked to additional function such as
# changing the mode of the robot, or moving it.  Speech recognition is not
# the best interface to do finely controlled actuation.  But, it is very
# convenient to express high-level (e.g. go to center of the room) commands
#
# FYI - The memory requirements for Sphinx are a bit hefty and depending on the
# platform additional JVM arguments might be necessary e.g. -Xmx256m

# start an ear
ear = ServiceFactory.createService("ear","SpeechRecognition")
# create the grammar you would like recognized
# this must be done before the service is started
ear.createGrammar("hello | forward | back | stop | turn left | turn right | spin | power off")
ear.startService()

# start the mouth
mouth = ServiceFactory.createService("mouth","Speech")
mouth.startService()

speaking = False

def heard():
    # here is where recognized data from the ear will be sent
    # if (speaking == False):
    if (not speaking):
      data = msg_ear_recognized.data[0]
      mouth.speak("you said " + data)
      print "heard ", data
      if (data == "forward"):
         print "robot goes forward" 
      elif (data == "hello"):
         print  "robot says hello"
    # ... etc
    
def isSpeaking():
    speaking = msg_mouth_isSpeaking.data[0]
    print "is speaking " , speaking

# set up a message route from the ear to the scripting engine
ear.notify("recognized", jython.name, "heard", String().getClass()); 
# prevent infinite loop 
mouth.notify("isSpeaking", jython.name, "isSpeaking");

# start a jython monitor to see the results of recognized speech
jython.monitor()