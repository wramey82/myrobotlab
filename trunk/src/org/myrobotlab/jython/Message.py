from org.myrobotlab.framework import MessageType
# Building object that subclasses a Java interface

class Message(MessageType):

    def __init__(self):
        self.name = None
	self.data = None

    def getData(self):
        return self.data

    def setData(self, data):
        self.data = data;

