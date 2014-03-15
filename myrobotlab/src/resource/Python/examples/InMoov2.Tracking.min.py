headPort="COM6"
i01 = Runtime.createAndStart("i01", "InMoov")
i01.startMouth()
tracker = i01.startHeadTracking(headPort)
tracker.startLKTracking()