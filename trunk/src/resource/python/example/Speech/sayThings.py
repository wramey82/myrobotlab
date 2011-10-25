from time import sleep
from org.myrobotlab.service import Speech
from org.myrobotlab.framework import ServiceFactory

# the preferred method of creating services is
# through the ServiceFactory - this will allow
# the script to be rerun without name/service clashing
# the delay on initial creation can be large, however
# after creation the service should be immediately 
# responsive
speech = ServiceFactory.createService("speech","Speech")
speech.speak("hello brave new world")

# Google must have network connectivity
# the back-end will cache a sound file
# once it is pulled from Goole.  So the 
# first time it is slow but subsequent times its very 
# quick.
speech.setBackendType("GOOGLE"); 
speech.setLanguage("en");
speech.speak("Hello World From Google.");
speech.setLanguage("pt");
speech.speak("Hello World From Google.");
speech.setLanguage("de");
speech.speak("Hello World From Google.");
speech.setLanguage("ja");
speech.speak("Hello World From Google.");
speech.setLanguage("da");
speech.speak("Hello World From Google.");

sleep(3)
speech.setBackendType("FREETTS"); 
speech.speak("Hello World From Free TTS.");


