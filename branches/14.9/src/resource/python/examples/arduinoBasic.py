# header - initialization script - header.py
# for more information regarding the Arduino service check
# http://myrobotlab.org/service/arduino

arduino = runtime.createAndStart('arduino','Arduino')

# set board's port
#arduino.setPort('/dev/ttyS50');
arduino.setPort('COM10')

#set the serial parameters of the connection
arduino.setSerialPortParams (57600, 8, 1, 0)

# open a sketch in the GUI
arduino.openSketch('C:\\mrl\\myrobotlab\\arduino\\libraries\\MyRobotLab\\examples\\MRLComm\\MRLComm.ino')

# upload the program to the board
arduino.uploadSketch(False);

# broadcast a state change - which will update gui
arduino.broadcastState()


arduino.pinMode(44, 0)

arduino.digitalWrite(44, 1)
