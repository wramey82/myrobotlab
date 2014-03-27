port = "COM5"

i01 = Runtime.createAndStart("i01", "InMoov")
mouth = i01.startMouth()
head = i01.startHead(port)
arduino = head.arduino
arduino.addListener("publishPin", python.getName(), "input")
arduino.setSampleRate(8000)
head.setSpeed(0.5, 0.5, 0.5, 0.5, 0.5)

def input():
    # print 'python object is ', msg_clock_pulse
    pin = msg_i01_left_publishPin.data[0]
    print 'pin data is ', pin.pin, pin.value
    if (pin.value == 1):
      mouth.speak("howdy partner")
      head.neck.moveTo(85)
      sleep(2)
      head.neck.moveTo(90)