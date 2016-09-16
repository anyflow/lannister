#!/bin/sh

echo "Lannister shutdown started"

cd "$(dirname "$0")"

echo 'current directory :'
pwd

LOG_FILE="${log.path}/output.log"
TAIL_PID=0
if [ -f "$LOG_FILE" ];
then
        tail -f -n0 $LOG_FILE &
        TAIL_PID=$!
fi

processor=${project.build.finalName}

for i in {0..5}
do
        PID=`ps -ef | grep $processor | grep -v grep | awk '{print $2}'`
        echo 'PID='$PID
        
        if [ ${#PID} -gt 0 ]; then
                if [ $i -gt 5 ]; then
                        kill -9 $PID
                        echo $processor " killing..."
                else
                        kill -15 $PID
                        echo $processor " shutdowning..."
                fi
                sleep 5
        else
                break
        fi
done

if [ $TAIL_PID -gt 0 ]; then
        kill -9 $TAIL_PID
fi

echo "Lannister shutdown completed"
