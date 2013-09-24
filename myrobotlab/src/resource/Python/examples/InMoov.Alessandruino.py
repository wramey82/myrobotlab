# InMoov Head for MakerFair
from org.myrobotlab.opencv import OpenCVFilterLKOpticalTrack

# system specific variables
port = "COM3"
actRothead = 90
actNeck = 90

# create 2 tracking services for 4 PID
eyes = Runtime.create("eyes","Tracking")
head = Runtime.create("head","Tracking")

eyeX = Runtime.create("eyeX","Servo")
eyeY = Runtime.create("eyeY","Servo")

# initialization
arduino = Runtime.create("arduino","Arduino")
arduino.connect(port)

# name services so they will bind appropriately
# head names
head.xName = "neck"

# eyes names
eyeY = eyes.yName = "eyeY"
eyes.xName = "eyeX"
eyes.xpidName = "eyeXPID"
eyes.ypidName = "eyeYPID"
 
neck = Runtime.create("neck","Servo")
xpidb = Runtime.create("xpidb","PID");
ypidb = Runtime.create("ypidb","PID");

xpidb.setPID(5, 0, 0.1) # head is less responsive than eye, so 8 instead of 10 and 0 instead of 0.1
ypidb.setPID(5, 0, 0.1) # head is less responsive than eye, so 8 instead of 10 and 0 instead of 0.1
xpidb.setOutputRange(-1, 1) #the output is of 1, instead of 3 because the head moves less fast than eye 
ypidb.setOutputRange(-1, 1) #the output is of 1, instead of 3 because the head moves less fast than eye
 
# create all the peer services

xpid = Runtime.create("xpid","PID");
ypid = Runtime.create("ypid","PID");
 
# adjust values

#eye = Runtime.create("eye","OpenCV")
# eye.addListener("publishOpenCVData", python.name, "input")
#eye.setCameraIndex(0)
 
# flip the pid if needed
# xpid.invert()
xpid.setOutputRange(-1, 1)
xpid.setPID(10.0, 0, 0.1)
xpid.setSetpoint(0.5) # we want the target in the middle of the x
 
# flip the pid if needed
# ypid.invert()
ypid.setOutputRange(-1, 1)
ypid.setPID(10.0, 0, 0.1)
ypid.setSetpoint(0.5)
 
# set safety limits - servos
# will not go beyond these limits
eyeX.setPositionMin(65)
eyeX.setPositionMax(90)
 
eyeY.setPositionMin(95)
eyeY.setPositionMax(158)
 
# here we are attaching to the
# manually created peer services
 
eyes.attach(arduino)
eyes.attachServos(eyeX, 3, eyeY, 6)
arduino.attach(rothead.getName() , 10)
arduino.attach(neck.getName(), 9)
rothead.moveTo(90)
neck.moveTo(90)
eyes.attach(eye)
eyes.attachPIDs(xpid, ypid)
 
eyes.setRestPosition(80, 133)

def input ():
  points = msg_eye_publishOpenCVData.data[0].getPoints()
  if  (not points == None):
    if (points.size() > 0):
      global pointX
      global pointY
      pointX = points.get(0).x
      pointY = points.get(0).y
      print pointX , pointY
      global newRothead
      global newNeck
      xpidb.setInput(pointX)
      xpidb.compute()
      ypidb.setInput(pointY)
      ypidb.compute()
      valx = xpidb.getOutput()
      valy = ypidb.getOutput()
      global actRothead
      newRothead = (actRothead + valx)
      global actNeck
      newNeck = (actNeck + valy) 
      print 'x servo' , int(newRothead)
      print 'y servo' , int(newNeck)
      rothead.moveTo(int(newRothead))
      neck.moveTo(int(newNeck))
      actRothead = newRothead
      actNeck = newNeck
 
eyes.startService()
eyes.trackPoint(0.5,0.5)