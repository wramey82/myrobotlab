# The Adafruit16CServoDriver API is supported through Jython 

pwm =  Runtime.createAndStart("pwm", "Adafruit16CServoDriver")
		
pwm.setSerialDevice("COM9")

pwm.setPWM(0, 0, SERVOMIN)
pwm.setPWM(0, 0, SERVOMAX)