headPort = "COM7"
rightHandPort = "COM8"
leftHandPort = "COM7"

i01 = Runtime.createAndStart("i01", "InMoov")
head = i01.startHead(headPort)
rightHand = i01.startRightHand(rightHandPort)
leftHand = i01.startLeftHand(leftHandPort)
neck = i01.getHeadTracking()
neck.faceDetect()
#eyes = i01.getEyesTracking()
#eyes.faceDetect()
