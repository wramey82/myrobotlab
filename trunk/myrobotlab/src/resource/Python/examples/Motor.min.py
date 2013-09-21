# minimum 

port = "COM10"

arduino = Runtime.createAndStart("arduino", "Arduino")
arduino.connect(port)

m1 = Runtime.createAndStart("m1","Motor")
m2 = Runtime.createAndStart("m2","Motor")

# connect motor m1 with pwm power pin 3, direction pin 4
arduino.motorAttach("m1", 3, 4) 
arduino.motorAttach("m2", 6, 7) 

# move both motors forward
# at 50% power
# for 2 seconds
m1.move(0.5)
m2.move(0.5)

sleep(2)

# move both motors backward
# at 50% power
# for 2 seconds
m1.move(-0.5)
m2.move(-0.5)

sleep(2)

# stop and lock m1
m1.stopAndLock()
m2.stop()

# m2 should move 
# but m1 should not
m1.move(0.5)
m2.move(0.5)

sleep(2)

# unlock m1 and move it
m1.unlock()
m1.move(0.5)

sleep(2)

m1.stop()
m2.stop()