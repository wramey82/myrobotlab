# a minimal tracking script - this will start all peer
# setXMinMax & setYMinMax (min, max)
# this will set the min and maximum
# values of the servos 
tracker.setXMinMax(10, 170)
tracker.setYMinMax(10, 170)

# set rest x,y
tracker.setServoPins(13,12)
#change cameras if necessary
tracker.setCameraIndex(1)
# start the tracking service