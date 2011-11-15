from org.myrobotlab.service import Speech
from java.lang import String
from org.myrobotlab.service import SpeechRecognition
from org.myrobotlab.framework import ServiceFactory

# create the grammar you would like recognized
SpeechRecognition.createGrammar("ball | hand | box | beer")
# start an ear
ear = ServiceFactory.createService("ear","SpeechRecognition")
ear.startService()

def input():
    # here is where recognized data from the ear will be sent
    data = msg_ear_publish.data[0]
    print 'ear data is ', data
    if (data == "beer"):
       print("robot gets a beer")
    elif (data == "box"):
       print ("robot gets a box")
    # ... etc

# set up a message route from the ear to the scripting engine
ear.notify("publish", jython.name, "input", String().getClass()); 
# start a jython monitor to see the results of recognized speech
jython.monitor()