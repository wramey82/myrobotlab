# a minimal tracking script - this will start all peer# services and attach everything appropriately# change parameters depending on your pan tilt, pins and# Arduino detailstracker = Runtime.create("tracker","Tracking")
# setXMinMax & setYMinMax (min, max) - this will set the min and maximum
# x value it will send the servo - typically this is not needed
# because the tracking service will pull the min and max positions from 
# the servos it attaches too
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# minimal configuration# set rest at x = 90 y = 90tracker.setRestPosition(90, 90)# set serial port of the Arduinotracker.setSerialPort("COM12")# setServoPins (x, y) set the servo of the pan and tilt repectivelytracker.setServoPins(13,12)
#change cameras if necessary
# tracker.setCameraIndex(1)
# start the tracking servicetracker.startService()# put it in LK track modetracker.trackLKPoint()