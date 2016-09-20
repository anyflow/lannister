#
# Copyright 2016 The Lannister Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/bin/sh

echo 'Boostrapping Lannister started'

cd "$(dirname "$0")"

echo 'current directory :'
pwd

echo 'Prepare log directory & file...'
LOG_DIRECTORY="${log.path}"
if [ -d "$LOG_DIRECTORY" ];
then
  echo $LOG_DIRECTORY' found'
else
  mkdir $LOG_DIRECTORY
  echo $LOG_DIRECTORY' created'
fi

LOG_FILE=$LOG_DIRECTORY"/output.log"
if [ -f "$LOG_FILE" ];
then
  echo $LOG_FILE' found'
else
  touch $LOG_FILE
  echo $LOG_FILE' created'
fi

tail -f -n0 $LOG_FILE &
TAIL_PID=$!

echo 'Execute Lannister execution file...'
EXECUTE_FILE='./../lib/${project.build.finalName}.jar'

nohup java -Dname=${project.build.finalName} ${project.build.startOption} -jar $EXECUTE_FILE >/dev/null 2>&1 &

sleep 10

echo "Bootstrapping finished."

kill -9 $TAIL_PID

exit 0
