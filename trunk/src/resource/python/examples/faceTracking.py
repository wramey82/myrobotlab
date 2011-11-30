from java.lang import String
from java.lang import Class
from org.myrobotlab.framework import ServiceFactory
from org.myrobotlab.service import OpenCV
from org.myrobotlab.service import GUIService
from com.googlecode.javacv.cpp.opencv_core import CvPoint;
from org.myrobotlab.service import OpenCV

# create or get a handle to an OpenCV service
opencv = ServiceFactory.create("opencv","OpenCV")
opencv.startService()
# reduce the size - face tracking doesn't need much detail
# the smaller the faster
opencv.addFilter("PyramidDown1", "PyramidDown")
# add the face detect filter
opencv.addFilter("FaceDetect1", "FaceDetect")


# get a handle on the GUIService - probably already created if your
# using the editor
gui = ServiceFactory.create("gui","GUIService")

# rebuild the gui since we have added a new OpenCV service
gui.rebuild()

# ----------------------------------
# input
# ----------------------------------
# the "input" method is where Messages are sent to
# from other Services. The data from these messages can
# be accessed on based on these rules:
# Details of a Message structure can be found here
# http://myrobotlab.org/doc/org/myrobotlab/framework/Message.html 
# When a message comes in - the input function will be called
# the name of the message will be msg_+<sending service name>+_+<sending method name>
# In this particular case when the service named "opencv" finds a face it will publish
# a CvPoint.  The CvPoint can be access by msg_opencv_publish.data[0]
def input():
    print 'found face at (x,y) ', msg_opencv_publish.data[0].x(), msg_opencv_publish.data[0].y()
    return object

# create a message route from opencv to jython so we can see the coordinate locations
opencv.notify("publish", jython.name, "input", CvPoint().getClass()); 

# set the input source to the first camera
opencv.capture()
# start a Jython console so we can see the data from opencv
jython.console()

