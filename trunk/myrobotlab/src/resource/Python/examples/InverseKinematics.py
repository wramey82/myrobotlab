inversekinematics = Runtime.createAndStart("inversekinematics", "InverseKinematics")
# insert coordinates of the point to reach (x,y,z)
inversekinematics.setCoordinates(50,50,0)
# insert rods lenght of your arm
inversekinematics.setLengths(100,100)
inversekinematics.computeAngles()

print 'First rod angle is :' , inversekinematics.getTeta1()
print 'Second rod angle is:' , inversekinematics.getTeta2()
print 'Base rotation angle is :' , inversekinematics.getTeta3()