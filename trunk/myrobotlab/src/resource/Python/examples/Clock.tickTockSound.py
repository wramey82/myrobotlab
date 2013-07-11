#create a clock service named clock
clock = Runtime.createAndStart("clock","Clock")
#create a log service named log
log = Runtime.createAndStart("log","Log")
#create an audio file player service named audio
audio = Runtime.createAndStart("audio","AudioFile")
#create a message between clock and log, so you can read date and time on log
clock.addListener("pulse","log","log")
def ticktock():
    #if tick.mp3 is not in the main folder (myrobotlab)
    #it should be replaced with the full file-path eg. "C:\\myrobotlab\\src\\resource\\Clock\\tick.mp3"
    audio.playFile("tick.mp3")
#create a message between clock and audio, so a tick tock sound could be played
clock.addListener("pulse",python.name,"ticktock")
#start clock
clock.startClock()
