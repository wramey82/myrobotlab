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

LD_LIBRARY_PATH=`pwd`/lib:${LD_LIBRARY_PATH}
export LD_LIBRARY_PATH

export PATH="${APPDIR}/java/bin:${PATH}"

java -d32 -Djava.library.path=./bin org.myrobotlab.service.Invoker -service Invoker services GUIService gui > log.txt

