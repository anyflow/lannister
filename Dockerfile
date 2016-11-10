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

# docker images
# docker ps -a
# docker rm 'container-name'
# docker rmi 'image-name'

# docker build -t lannister .
# docker run -it -p 11883:1883 -p 18883:8883 -p 19001:9001 -p 19002:9002 -p 18090:8090 -p 18493:8493 --name lannister lannister

FROM anyflow/javaserver
MAINTAINER Park Hyunjeong <anyflow@gmail.com>

RUN mkdir /opt/lannister/
RUN mkdir /opt/lannister/logs
RUN touch /opt/lannister/logs/application.log
RUN touch /opt/lannister/logs/output.log

ADD server/target/bin/*   /opt/lannister/bin/
ADD server/target/conf/*  /opt/lannister/conf/
ADD server/target/lib/*   /opt/lannister/lib/
ADD server/target/webapp/*   /opt/lannister/webapp/

RUN chmod 700 /opt/lannister/bin/startup.sh
RUN chmod 700 /opt/lannister/bin/shutdown.sh

EXPOSE 1883
EXPOSE 8883
EXPOSE 9001
EXPOSE 9002
EXPOSE 8090
EXPOSE 8493

CMD /opt/lannister/bin/startup.sh && /bin/bash