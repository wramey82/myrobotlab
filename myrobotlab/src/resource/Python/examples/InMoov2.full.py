# this will start all the services of InMoov
# WebGUI and XMPP can be added durning runtime if desired

leftPort = "COM15"
rightPort = "COM12"

inMoov = Runtime.createAndStart("inMoov", "InMoov")

# starts everything
#inMoov.startAll(leftPort, rightPort)

# starts only the right hand
#rightHand = inMoov.startRightHand(rightPort)

# starts only the right hand
#rightArm = inMoov.startRightArm(rightPort)

# starts only the left hand
#leftHand  = inMoov.startLeftHand(leftPort)

# starts only the left arm
#leftArm  = inMoov.startLeftArm(leftPort)

# starts only the head
head = inMoov.startHead(leftPort)


def delicategrab():
  inMoov.setHandSpeed("left", 0.60, 0.60, 1.0, 1.0, 1.0, 1.0)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("left", 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(21,98)
  inMoov.moveArm("left",30,72,77,10)
  inMoov.moveArm("right",0,91,28,17)
  inMoov.moveHand("left",131,130,4,0,0,180)
  inMoov.moveHand("right",86,51,133,162,153,180)

def perfect():
  inMoov.setHandSpeed("left", 0.80, 0.8, 1.0, 1.0, 1.0, 1.0)
  inMoov.setHandSpeed("right", 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
  inMoov.setArmSpeed("left", 0.75, 0.75, 0.75, 0.95)
  inMoov.setArmSpeed("right", 1.0, 1.0, 1.0, 1.0)
  inMoov.setHeadSpeed(0.65, 0.75)
  inMoov.moveHead(88,79)
  inMoov.moveArm("left",89,75,93,11)
  inMoov.moveArm("right",0,91,28,17)
  inMoov.moveHand("left",131,130,4,0,0,34)
  inMoov.moveHand("right",86,51,133,162,153,180)
  
  
  