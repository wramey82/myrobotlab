########################################################
# authors : GroG & michael96_27
# references:
# http://myrobotlab.org/content/color-tracking-mrl-using-opencv-jython-and-arduino-services
# File colorTracking.py
# //////////BEGIN PYTHON SCRIPT////////////////////////////
# //////////OPENCV////////////////////////////////////////

from java.lang import String
from java.lang import Class
from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from com.googlecode.javacv.cpp.opencv_core import CvPoint;
from org.myrobotlab.service import OpenCV

# create or get a handle to an OpenCV service
opencv = Runtime.createAndStart("opencv","OpenCV")

# add the desired filters
opencv.addFilter("PyramidDown1", "PyramidDown")
opencv.addFilter("InRange1", "InRange")
opencv.addFilter("Dilate1", "Dilate")
opencv.addFilter("FindContours1", "FindContours")

# change values of the InRange filter
opencv.setFilterCFG("InRange1","hueMin", "3")
opencv.setFilterCFG("InRange1","hueMax", "33")
opencv.setFilterCFG("InRange1","saturationMin", "87")
opencv.setFilterCFG("InRange1","saturationMax", "256")
opencv.setFilterCFG("InRange1","valueMin", "230")
opencv.setFilterCFG("InRange1","valueMax", "256")
opencv.setFilterCFG("InRange1","useHue", True)
opencv.setFilterCFG("InRange1","useSaturation", True)
opencv.setFilterCFG("InRange1","useValue", True)

# configuration variables
# GroG's config
#panServoPin = 9
#tiltServoPin = 10
#comPort = 'COM10'
# michael's config
panServoPin = 2
tiltServoPin = 3
comPort = 'COM7'

# change value of the FindContours filter
# opencv.setFilterCFG("FindContours1","minArea", "150")
# opencv.setFilterCFG("FindContours1","maxArea", "-1")
# opencv.setFilterCFG("FindContours1","useMinArea", True)
# opencv.setFilterCFG("FindContours1","useMaxArea", false)

# ----------------------------------
# input
# ----------------------------------
# the "input" method is where Messages are sent to this Service
# from other Services. The data from these messages can
# be accessed on based on these rules:
# Details of a Message structure can be found here
# http://myrobotlab.org/doc/org/myrobotlab/framework/Message.html 
# When a message comes in - the input function will be called
# the name of the message will be msg_++_+
# In this particular case when the service named "opencv" finds a face it will publish
# a CvPoint.  The CvPoint can be access by msg_opencv_publish.data[0]
def input():
    #print 'found face at (x,y) ', msg_opencv_publish.data[0].x(), msg_opencv_publish.data[0].y()
    arrayOfPolygons = msg_opencv_publish.data[0]
    print arrayOfPolygons
    if (arrayOfPolygons.size() > 0):
      print arrayOfPolygons.get(0).centeroid.x(),arrayOfPolygons.get(0).centeroid.y()
    return object

# create a message route from opencv to jython so we can see the coordinate locations
opencv.addListener("publish", jython.name, "input", CvPoint().getClass()); 

# set the input source to the first camera
opencv.capture()

# ///////////////////ARDUINO/////////////////////////////////////////////////////

from time import sleep
from org.myrobotlab.service import Arduino

arduino = runtime.createAndStart('arduino','Arduino')

# set and open the serial device 
# arduino.setSerialDevice('/dev/ttyUSB0', 57600, 8, 1, 0)
arduino.setSerialDevice(comPort, 57600, 8, 1, 0)

sleep(3) # sleep because even after initialization the serial port still takes time to be ready
arduino.pinMode(16, Arduino.INPUT)
# arduino.digitalReadPollingStop(7)
arduino.analogReadPollingStart(16) # A2


# //////////////SERVOS////////////////////////////////////////////
from org.myrobotlab.service import Servo

pan = runtime.createAndStart('pan','Servo')
tilt = runtime.createAndStart('tilt','Servo')

# attach the pan servo to the Arduino on pin 2
pan.attach(arduino.getName(),panServoPin) 
# attach the pan servo to the Arduino on pin 3
tilt.attach(arduino.getName(),tiltServoPin) 

pan = runtime.createAndStart('pan','Servo')
tilt = runtime.createAndStart('tilt','Servo')

for pos in range(0,3):
	pan.moveTo(10)
	tilt.moveTo(10)
	sleep(1)
	pan.moveTo(60)
	tilt.moveTo(60)
	sleep(1)
	pan.moveTo(130)
	tilt.moveTo(130)
	sleep(1)

# ////////////////////END PYTHON  SCRIPT/////////////////////////////////////////
# ////////////////////END PYTHON  SCRIPT/////////////////////////////////////////
