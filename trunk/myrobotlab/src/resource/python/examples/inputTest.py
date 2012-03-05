from jarray import array
from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Clock
from org.myrobotlab.service import Logging
from org.myrobotlab.service import Runtime
from org.myrobotlab.framework import Message

# inputTest.py
# example script for MRL showing Jython Service
# input method.  Input is a hook which allows
# other services to send data to your script.
# This script will also show a "Message" which
# is the basic form of communication between all
# Services in MRL.  Additionally it will show how to 
# extract data from the message to be used in the 
# script.

# Create a running instance of the Clock Service.
# <<URL>>
# Name it "clock".
clock = Runtime.create("clock","Clock")
clock.startService()
# Create a running instance of the Logging Service.
# <<URL>>
# Name it "log".
log = Runtime.create("log","Logging")
log.startService()

# ----------------------------------
# input
# ----------------------------------
# the "input" method is a Message input sink 
# currently String is the only
# parameter supported - possibly the future
# non-primitive Java data types could be dynamically
# constructed through reflection and sent to the 
# interpreter
#
# The Python way to invoke the method
# input ('hello there input !')
# 
# Within the MRL Jython serivce the method is invoked
# when messages are sent to Jython#input(String)

def input():
    # print 'python object is ', msg_clock_pulse
    print 'python data is ', msg_clock_pulse.data[0]


clock.setPulseDataString('new clock data !!')

# send a data to the log service and the jython service 
# you should be able to see the data in the log gui or the jython console
clock.notify("pulse", jython.name, "input", String().getClass()); 
clock.notify("pulse", "log", "log", String().getClass());

clock.setPulseDataType(clock.PulseDataType.string)
clock.startClock()

# start a jython console
jython.console()
