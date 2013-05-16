# a safe tracking script - servos are created seperately
# and their limits are programmed, they are then attached
# the tracking service

# create a tracking service
tracker = Runtime.create("tracker","Tracking")

# we can create all the peer services for tracker manually
# giving them new names and different values
# then we can attach them - this must be done before
# starting the tracking service

rotation = Runtime.create("rotation","Servo")
neck = Runtime.create("neck","Servo")
arduino = Runtime.create("arduino","Arduino")
xpid = Runtime.create("xpid","PID");
ypid = Runtime.create("ypid","PID");

# adjust values
arduino.setSerialDevice("COM12")
eye = Runtime.create("eye","OpenCV")
eye.setCameraIndex(1)

# flip the pid if needed
xpid.invert()
xpid.setOutputRange(-3, 3)
xpid.setPID(10.0, 0, 1.0)
xpid.setControllerDirection(1)
xpid.setSetpoint(0.5) # we want the target in the middle of the x

# flip the pid if needed
# ypid.invert()
ypid.setOutputRange(-3, 3)
ypid.setPID(10.0, 0, 1.0)
ypid.invert()
ypid.setSetpoint(0.5)

# set safety limits - servos
# will not go beyond these limits
rotation.setPositionMin(50)
rotation.setPositionMax(170)

neck.setPositionMin(50)
neck.setPositionMax(170)

# here we are attaching to non-default
# manually created peer services

tracker.attach(arduino)
tracker.attachServos(rotation, 13, neck, 12)
tracker.attach(eye)
tracker.attach(xpid, ypid)

tracker.setRestPosition(90, 90)

tracker.startService()
tracker.trackLKPoint()
