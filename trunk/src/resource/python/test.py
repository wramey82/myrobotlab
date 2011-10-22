from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Clock
from org.myrobotlab.service import Logging

clock = Clock("clock")
clock.startService()

log = Logging("log")
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