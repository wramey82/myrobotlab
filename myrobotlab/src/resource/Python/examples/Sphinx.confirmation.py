# a two stage confirmation of commands
# first a command is given - when a command is heard

command = ""

mouth = Runtime.createAndStart("mouth", "Speech")

ear = Runtime.createAndStart("ear", "Sphinx")
ear.addListener("recognized", "python", "heard", String.class);

# flat grammar with confirmation
ear.startListening("rest | hand open | hand close | manual | voice control| capture gesture | track | freeze tracking "
                      "| hello | giving | fighter | fist hips | look at this | victory | arms up | arms front | da vinci "
                      "| yes | correct | yeah | no | incorrect | nope ")

# attaching the mouth to the ear
# prevents listening when speaking
# which causes an undesired feedback loop
ear.attach(mouth)

def heard():
  data = msg_ear_recognized.data[0]
  print "heard ", data         
  # if the command is blank and its not a confirmation
  # it will be the first stage command
  if (command != "" and (data != "yes" or data != "correct" or data != "yeah" or data != "no" or data != "incorrect" or data != "nope"):
    command = data # command is assigned