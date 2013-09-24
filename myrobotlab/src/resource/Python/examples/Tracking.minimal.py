# a minimal tracking script - this will start all peer# services and attach everything appropriately# change parameters depending on your pan tilt, pins and# Arduino details# all commented code is not necessary but allows custom# optionsport = "COM12"xServoPin = 3;yServoPin = 6;tracker = Runtime.create("tracker", "Tracking")tracker.connect(port)tracker.attachServos(xServoPin, yServoPin)# set limits if necessary# default is servo limits# tracker.setServoLimits(0, 180, 0, 180) # set rest position default is 90 90# tracker.setRestPosition(90, 90) tracker.setPIDDefaults()tracker.startService()
# set a point and track it# there are two interfaces one is float value
# where 0.5,0.5 is middle of screen
tracker.trackPoint(0.5, 0.5)
# don't be surprised if the point does not
# stay - it needs / wants a corner in the image
# to presist - otherwise it might disappear
# you can set points manually by clicking on the
# opencv screen
