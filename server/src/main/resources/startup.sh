#!/bin/sh

echo 'Boostrapping Lannister started'

cd ${lannister.home}

echo 'current directory :'
pwd

echo 'Prepare log directory & file...'
LOG_DIRECTORY="./logs"
if [ -d "$LOG_DIRECTORY" ];
then
  echo $LOG_DIRECTORY' found'
else
  mkdir $LOG_DIRECTORY
  echo $LOG_DIRECTORY' created'
fi

LOG_FILE="./logs/output.log"
if [ -f "$LOG_FILE" ];
then
  echo $LOG_FILE' found'
else
  touch $LOG_FILE
  echo $LOG_FILE' created'
fi

echo 'Monitoring '$LOG_FILE'...'
tail -f -n0 $LOG_FILE &
TAIL_PID=$!

echo 'Execute Lannister execution file...'
EXECUTE_FILE='./lib/${project.build.finalName}.jar'

nohup java -Dname=${project.build.finalName} ${project.build.startOption} -jar $EXECUTE_FILE >/dev/null 2>&1 &

sleep 10

echo "Bootstrapping finished."

kill $TAIL_PID

exit 0
