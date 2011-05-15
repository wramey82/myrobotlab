#!/bin/sh

# actual start script - this does not work directly from 
# the file manager (nautilus) so if anyone has an idea
# as to why I'd love to know - 
 
APPDIR="$(dirname -- "${0}")"

cd $APPDIR
 
for LIB in \
    myrobotlab.jar \
    lib/*.jar \
    ;
do
    CLASSPATH="${CLASSPATH}:${APPDIR}/${LIB}"
done
export CLASSPATH

LD_LIBRARY_PATH=`pwd`/bin:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH

# export PATH="${APPDIR}/java/bin:${PATH}"
# exporting PATH will not work LD_LIBRARY_PATH is necessary (at least in Fedora)


# java -d32  force 32 bit 
# -classpath ":myrobotlab.jar:./lib/*"  - make note : on Linux ; on Windows ! 
# -Djava.library.path="./bin" - can not change or modify LD_LIBRARY_PATH after jvm starts 
# The shell itself need 
# org.myrobotlab.service.Invoker -service Invoker services GUIService gui > log.txt
 

java -classpath ":myrobotlab.jar:./lib/*" org.myrobotlab.service.Invoker -service Invoker services GUIService gui > log.txt

