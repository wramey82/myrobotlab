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
# start the tracking servicetracker.startService()# put it in LK track modetracker.trackLKPoint()