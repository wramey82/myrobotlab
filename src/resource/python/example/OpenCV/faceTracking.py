from java.lang import String
from java.lang import Class
from org.myrobotlab.service import OpenCV

opencv = OpenCV("opencv")
opencv.startService()

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


opencv.addFilter("PyramidDown1", "PyramidDown")

#opencv.notify("pulse", "jython", "input", String().getClass()); 

opencv.setUseInput("camera")
opencv.capture()
