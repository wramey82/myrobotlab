# a very minimal script for InMoov
# although this script is very short you can still
# do voice control of a right hand or finger box

inmoov = Runtime.createAndStart("inmoov", "InMoov")
rhand = inmoov.startHand("COM12", "right")
Runtime.createAndStart("webgui", "WebGUI")


