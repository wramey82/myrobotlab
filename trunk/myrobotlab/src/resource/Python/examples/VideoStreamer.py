from org.myrobotlab.service import Runtime
from org.myrobotlab.service import OpenCV
from org.myrobotlab.service import VideoStreamer
from org.myrobotlab.net import BareBonesBrowserLaunch

# create a video source (opencv) & a video streamer
opencv = Runtime.createAndStart("opencv","OpenCV")
streamer = Runtime.createAndStart("streamer","VideoStreamer")

# attache them
streamer.attach(opencv)

# add a pyramid down filter and gray to minimize the data
opencv.addFilter("pyramidDown", "PyramidDown");
opencv.addFilter("gray", "Gray");

# start the camera
opencv.capture();

# go to http://localhost:9090/output
BareBonesBrowserLaunch.openURL("http://localhost:9090/output")
