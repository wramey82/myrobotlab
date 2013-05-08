# a minimal tracking script - this will start all peer 
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details

tracker = Runtime.createAndStart("tracker","Tracking")
tracker.setRestPosition(90, 90)
tracker.setSerialPort("COM12")

# setXMinMax & setYMinMax (min, max) - this will set the min and maximum
# x value it will send the servo - typically this is not needed
# because the tracking service will pull the min and max positions from 
# the servos it attaches too
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# setServoPins (x, y) set the servo of the pan and tilt repectively
tracker.setServoPins(13,12)
# tracker.setCameraIndex(1) #change cameras if necessary
 
tracker.trackLKPoint()

#tracker.learnBackground()