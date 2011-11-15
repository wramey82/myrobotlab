from java.lang import String
from org.myrobotlab.service import Arduino
from org.myrobotlab.service import MagaBot
from org.myrobotlab.service import Speech
from org.myrobotlab.service import SpeechRecognition
from org.myrobotlab.framework import ServiceFactory


# create the grammar you would like recognized
SpeechRecognition.createGrammar("go | stop | left | right | back")
# start an ear
ear = ServiceFactory.createService("ear","SpeechRecognition")

# creat a magabot
magabot = ServiceFactory.createService("magabot","MagaBot")
magabot.init("COM8");  # initalize arduino on port specified to 9600 8n1

def input():
    # here is where recognized data from the ear will be sent
    data = msg_ear_publish.data[0]
    print 'ear data is ', data
    if (data == "stop"):
      print("stopping magabot")
      magabot.sendOrder('p');
    elif (data == "left"):
      print ("magabot goes left")
      magabot.sendOrder('a');
    elif (data == "right"):
      print ("magabot goes left")
      magabot.sendOrder('d');
    elif (data == "go"):
      print ("magabot goes forward")
      magabot.sendOrder('w');
    # ... etc

# set up a message route from the ear to the scripting engine
ear.notify("publish", jython.name, "input", String().getClass()); 
