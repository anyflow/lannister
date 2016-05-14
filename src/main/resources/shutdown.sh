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
cd $FULL_PATH
echo 'Current Path: '`pwd`

processor=${project.build.finalName}

for i in 0 1 2 3
do
        PID=`ps -ef | grep $processor | grep $USER | grep -v grep | awk '{print $2}'`
        if [ $PID > 0 ]
        then
                if [ $i -gt 2 ]
                then
                        kill -9 $PID
                        echo $processor " kill..."
                else
                        kill -15 $PID
                        echo $processor " stopping..."
                fi
                sleep 5
        else
                break
        fi
done

echo $processor " stopped"