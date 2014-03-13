# script for AdolphSmith
# http://myrobotlab.org/content/my-inmoov-parts-list-and-way-working

mouth = Runtime.createAndStart("mouth","Speech")
mouth.speakBlocking("Hello. I have powered up")
mouth.speakBlocking("And now I will start a Tracking service")

port = "COM15"
xServoPin = 13
yServoPin = 12

tracker = Runtime.createAndStart("tracker", "Tracking")

# set specifics on each Servo
servoX = tracker.getX()
servoX.setPin(xServoPin)
servoX.setMinMax(30, 150)

servoY = tracker.getY()
servoY.setPin(yServoPin)
servoY.setMinMax(30, 150)

# optional filter settings
opencv = tracker.getOpenCV()

# setting camera index to 1 default is 0
opencv.setCameraIndex(1) 

# connect to the Arduino
tracker.connect(port)

# Gray & PyramidDown make face tracking
# faster - if you dont like these filters - you
# may remove them before you select a tracking type with
# the following command
# tracker.clearPreFilters()

# diffrent types of tracking

# simple face detection and tracking
# tracker.faceDetect()

# lkpoint - click in video stream with 
# mouse and it should track
tracker.startLKTracking()

# scans for faces - tracks if found
# tracker.findFace() 
