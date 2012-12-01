#!/bin/sh

CLASSPATH=.; export CLASSPATH=$CLASSPATH$(find "$PWD/libraries/jar" -name '*.jar' -type f -printf ':%p\n' | sort -u | tr -d '\n'); echo $CLASSPATH

# Mac's don't use LD_LIBRARY_PATH yet its 
# required to load shared objects on Linux systems
LD_LIBRARY_PATH=`pwd`/libraries/native/arm.32.linux:`pwd`/libraries/native/x86.32.linux:`pwd`/libraries/native/x86.64.linux:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH

# java -d32  force 32 bit 
# -classpath ":myrobotlab.jar:./lib/*"  - make note : on Linux ; on Windows ! 
# -Djava.library.path=./bin - can not change or modify LD_LIBRARY_PATH after jvm starts 
# LD_LIBRARY_PATH needed by Linux systems
# -Djava.library.path= needed by mac

java -classpath "./libraries/jar/*" -Djava.library.path="./libraries/native/arm.32.linux:./libraries/native/x86.32.linux:./libraries/native/x86.64.linux:./libraries/native/x86.32.mac" org.myrobotlab.service.Runtime -service gui GUIService python Python 

