FROM anyflow/javaserver
MAINTAINER Park Hyunjeong <anyflow@gmail.com>

RUN mkdir /opt/lannister/
RUN mkdir /opt/lannister/logs
RUN touch /opt/lannister/logs/application.log
RUN touch /opt/lannister/logs/output.log

ADD server/target/*.jar   /opt/lannister/
ADD server/target/bin/*   /opt/lannister/bin/
ADD server/target/conf/*  /opt/lannister/conf/
ADD server/target/lib/*   /opt/lannister/lib/


RUN chmod 700 /opt/lannister/bin/startup.sh
RUN chmod 700 /opt/lannister/bin/shutdown.sh

RUN ln -s /opt/lannister/bin/startup.sh startup
RUN ln -s /opt/lannister/bin/shutdown.sh shutdown
RUN ln -s /opt/lannister/logs/application.log application.log
RUN ln -s /opt/lannister/logs/output.log output.log

EXPOSE 1883
EXPOSE 8883
EXPOSE 9001
EXPOSE 9002