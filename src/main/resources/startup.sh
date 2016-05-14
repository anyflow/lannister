#!/bin/sh
EXEC_FILE="$0"
BASE_NAME=`basename "$EXEC_FILE"`
if [ "$EXEC_FILE" = "./$BASE_NAME" ] || [ "$EXEC_FILE" = "$BASE_NAME" ]; then
    FULL_PATH=`pwd`
else
    FULL_PATH=`echo "$EXEC_FILE" | sed 's/'"${BASE_NAME}"'$//'`
    cd "$FULL_PATH" > /dev/null 2>&1
    FULL_PATH=`pwd`
fi
cd $FULL_PATH/../
echo 'Current Path: '`pwd`

tail -f -n0 ${log.path}/output.log &
TAIL_PID=$!

nohup java -Dname=${project.build.finalName} ${project.build.startOption} -jar "${project.build.finalName}.jar" >/dev/null 2>&1 &

sleep 5

echo "Bootstrapping finished."

kill $TAIL_PID

exit 0
