# simple script to show how to send a message to and recieve a message from
# using a robot with the XMPP service 

# create ear and mouth
xmpp = Runtime.createAndStart("xmpp","XMPP")

# adds the python service as a listener for messages
xmpp.addListener("python","publishMessage")

# there is a big list of different xmpp/jabber servers out there
# but we will connect to the big one - since that is where our robots account is
xmpp.connect("gmail.com")
xmpp.login("robot01@myrobotlab.org", "mrlRocks!")

# gets list of all the robots friends
print xmpp.getRoster()

xmpp.setStatus(True, "online all the time")

# send a message
xmpp.sendMessage("hello this is robot01 - the current heatbed temperature is 40 degrees celcius", "supertick@gmail.com")

def publishMessage():
	msg = msg_xmpp_publishMessage.data[0]
	print msg.getFrom(), " says " , msg.getBody()