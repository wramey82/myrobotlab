headPort = "COM4"

i01 = (InMoov) Runtime.createAndStart("i01", "InMoov")
head = i01.startHead(headPort)
neck = i01.getHeadTracking()
neck.faceDetect()