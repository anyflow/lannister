#!/bin/sh

cd ${lannister.home}

tail -f -n0 ${log.path}/output.log &
TAIL_PID=$!

nohup java -Dname=${project.build.finalName} ${project.build.startOption} -jar "${project.build.finalName}.jar" >/dev/null 2>&1 &

sleep 10

echo "Bootstrapping finished."

kill $TAIL_PID

exit 0
