# a minimal tracking script - this will start all peer 
# services and attach everything appropriately
# change parameters depending on your pan tilt, pins and
# Arduino details

tracker = Runtime.createAndStart("tracker","Tracking")
tracker.setRestPosition(90, 90)
tracker.setSerialPort("COM12")
# setServoPins (x, y) set the servo of the pan and tilt repectively
tracker.setServoPins(13,12)
# tracker.setCameraIndex(1) #change cameras if necessary
 
tracker.trackLKPoint()

#tracker.learnBackground()