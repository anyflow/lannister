#!/bin/sh

cd ${lannister.home}

processor=${project.build.finalName}

for i in 0 1 2 3 4 5
do
        PID=`ps -ef | grep $processor | grep -v grep | awk '{print $2}'`
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