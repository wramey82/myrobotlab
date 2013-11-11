# a minimal tracking script - this will start all peer# services and attach everything appropriately# change parameters depending on your pan tilt, pins and# Arduino details# all commented code is not necessary but allows custom# optionsport = "COM12"xServoPin = 3yServoPin = 6
tracker = Runtime.create("tracker", "Tracking")
# tracker.getY().setMinMax(79, 127) - setting the min and max of servo Y

tracker.getX().setPin(xServoPin)
tracker.getY().setPin(yServoPin)
tracker.getOpenCV().setCameraIndex(1) # setting camera index to 1 default is 0
tracker.connect(port)
tracker.startService()
tracker.faceDetect()