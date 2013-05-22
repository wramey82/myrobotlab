# a larger script for InMoov
inMoov = Runtime.createAndStart("inMoov", "InMoov")

# attach an arduino to InMoov
# possible board types include uno atmega168 atmega328p atmega2560 atmega1280 atmega32u4
# the MRLComm.ino sketch must be loaded into the Arduino for MyRobotLab control !
# set COM number according to the com of your Arduino board
inMoov.attachArduino("left","uno","COM12")
# if you have a laptop with a camera the one in InMoov is likely to be index #1
inMoov.setCameraIndex(1)
inMoov.attachHead("left")
inMoov.trackPoint(0.5,0.5)
