# a safe tracking script - servos are created seperately
# and their limits are programmed, they are then "bound" to
# the tracking service
tracker = Runtime.create("tracker","Tracking")
arduino = Runtime.create("arduino","Arduino")

# create servos BEFORE starting the tracking service
# so we can specify values for the servos and specify names
# before it starts tracking

rotation = Runtime.create("rotation","Servo")
neck = Runtime.create("neck","Servo")

# set safety limits - servos
# will not go beyond these limits
rotation.setPositionMin(50)
rotation.setPositionMin(170)

neck.setPositionMin(50)
neck.setPositionMin(170)

# here we are binding are new servos with different names
# to the tracking service.  If not specified the tracking service
# will create a servo named x and y

tracker.xName = "rotation"
tracker.yName = "neck"
tracker.arduinoName = "arduino"

tracker = Runtime.create("tracker","Tracking")
tracker.setRestPosition(90, 90)
tracker.setSerialPort("COM7")

# setXMinMax & setYMinMax (min, max) - this will set the min and maximum
# x value it will send the servo - typically this is not needed
# because the tracking service will pull the min and max positions from 
# the servos it attaches too
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# setServoPins (x, y) set the servo of the pan and tilt repectively
tracker.setServoPins(13,12)
# tracker.setCameraIndex(1) #change cameras if necessary

tracker.startService()
 
tracker.trackLKPoint()

#tracker.learnBackground()