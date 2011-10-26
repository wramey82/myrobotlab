from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Clock
from org.myrobotlab.service import Logging

# inputTest.py
# example script for MRL showing Jython Service
# input method.  Input is a hook which allows
# other services to send data to your script.

# Create a running instance of the Clock Service.
# <<URL>>
# Name it "clock".
clock = ServiceFactory.createService("clock","Clock")

# Create a running instance of the Logging Service.
# <<URL>>
# Name it "log".
clock = ServiceFactory.createService("log","Logging")


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


def input(object):
    print 'object is ', object
    return object


clock.setPulseDataString('new clock data !!')

clock.notify("pulse", "jython", "input", String().getClass()); 
clock.notify("pulse", "log", "log", String().getClass());

clock.setPulseDataType(clock.PulseDataType.string)
clock.startClock()

# FIXME - is there anyway to get String.class?
# TODO - make an opencv class - template match
