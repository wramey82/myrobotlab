# a minimal tracking script - this will start all peer# services and attach everything appropriately# change parameters depending on your pan tilt, pins and# Arduino detailstracker = Runtime.create("tracker","Tracking")
# setXMinMax & setYMinMax (min, max)
# this will set the min and maximum
# values of the servos 
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# set rest x,ytracker.setRestPosition(90, 90)# set serial port of the Arduinotracker.setSerialPort("COM12")# se the pan and tilt pins
tracker.setServoPins(13,12)
#change cameras if necessary
tracker.setCameraIndex(1)
# start the tracking servicetracker.startService()
# set a point and track it# there are two interfaces one is float value
# where 0.5,0.5 is middle of screen
tracker.trackPoint(0.5, 0.5)
# don't be surprised if the point does not
# stay - it needs / wants a corner in the image
# to presist - otherwise it might disappear
# you can set points manually by clicking on the
# opencv screen
