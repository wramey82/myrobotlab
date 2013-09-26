# a minimal tracking script - this will start all peer
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details
# all commented code is not necessary but allows custom
# options
 
port = "COM8"
xServoPin = 3;
yServoPin = 6;
 
 
tracker = Runtime.create("tracker", "Tracking")
 
# naming - binding of peer services is done with service names
# the Tracking service will use the following default names
# arduinoName = "arduino" - the arduino controller - used to control the servos
# xpidName = "xpid" - the PID service to control X tracking
# ypidName = "ypid" - the PID service to control Y tracking
# xName = "x" - the x servo (pan)
# yName = "y" - the y servo (tilt)
# opencvName = "opencv" - the camera
 
# after the Tracking service is "created" you may create peer service
# and change values of that service - for example if we want to invert a
# servo :
tilt = Runtime.create("tilt", "Servo")
tilt.setInverted(True)
# now we bind it to the Tracking service by changing the name of Tracking's xName :
tracker.yName = "tilt"
# this must be done before tracker.startService() is called
 
# initialization 
tracker.connect(port)
tracker.attachServos(xServoPin, yServoPin)
 
# set limits if necessary
# default is servo limits
tracker.setServoLimits(65, 90, 22, 85) 
 
# set rest position default is 90 90
tracker.setRestPosition(80, 47) 
 
#tracker.setPIDDefaults()
# changing PID values 
# setXPID(Kp, Ki, Kd, Direction 0=direct 1=reverse, Mode 0=manual 1= automatic, minOutput, maxOutput, sampleTime, setPoint);
# defaults look like this_AUTOMATIC
tracker.setXPID(10.0, 1, 0.1, 0, 1, -10, 10, 30, 0.5)
tracker.setYPID(10.0, 1, 0.1, 0, 1, -10, 10, 30, 0.5)
tracker.startService()
 
# set a point and track it
# there are two interfaces one is float value
# where 0.5,0.5 is middle of screen
tracker.trackPoint(0.5, 0.5)
 
# don't be surprised if the point does not
# stay - it needs / wants a corner in the image
# to presist - otherwise it might disappear
# you can set points manually by clicking on the
# opencv screen
 
# face tracking from face detection filter
tracker.faceDetect()