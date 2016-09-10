# Lannister

[![Build Status](https://travis-ci.org/anyflow/lannister.svg?branch=master)](https://travis-ci.org/anyflow/lannister) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Coverage)

High performance MQTT broker w/ full specifications support, Clustering, WebSocket, SSL on Java - [Netty](https://github.com/netty/netty) & [Hazelcast](https://github.com/hazelcast/hazelcast) **under CONSTRUCTION**.

## Features
1. Full Protocol Specifications([MQTT Version 3.1](http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html), [MQTT Version 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)) Support includes
   * QoS 0,1,2 on Publish / Subscribe
   * Clean / Persistent Session
   * Retained / Will Message
2. 1000K connections per node
3. Clustering
   * Topic / Message / Message Status / Session redundant
   * No SPOF(peer-to-peer based : No master and slave)
   * Easy Scaling out   
4. WebSocket
5. SSL (TCP / WebSocket both)
6. $SYS
7. Plugin Support
   * Authenticator
   * Authorizer
   * ServiceChecker
   * ConnectEventListener
   * DisconnectEventListener
   * PublishEventListener
   * DeliveredEventListener
   * SubscribeEventListener
   * UnsubscribeEventListener
8. HTTP REST APIs for admin, client
9. Web admin Dashboard _(arranged for Lannister version 1.1)_
10. Data Persistence _(arranged for Lannister version 1.1)_

## Getting Started
Lannister is **under CONSTRUCTION**. But the most of features above are implemented and tested. You can run and test Lannister simply via,
```{r, engine='bash', count_lines}
# The below commands require Java 8 and Maven 3

# Clone lannister source
git clone https://github.com/anyflow/lannister.git
# Change directory to lannister
cd lannister

# Build all(interface, server, plugin-example) projects
mvn install

# Run lannister server directed by lannister.cfg/log4j.xml files in conf directory
mvn exec:java -pl server
```
## Version History
##### version 0.9.5.1 / Sep 11, 2016
- Externalize Hazelcast config - Add hazelcast.config.xml
- Add docker file and docker property - Run maven with profile (mvn -Plannister.docker)
- Add null checker in hazelcast serialization logics
- Fix bug - subscriptions remaining on disposing (clean) session
- Update Hazelcast version to 3.7.1
- Update Netty version to 4.1.5

##### version 0.9.5.0 / May 21, 2016
- Code Review, Test based on chapter 3.2 of Spec v3.1.1 Mandatory normative statements
- Add $SYS Required Topics

##### version 0.9.5-beta1 / May 16, 2016
- Code Review, Test based on chapter 3.1 of Spec v3.1.1 Mandatory normative statements
- Implement full features of Protocol Specification MQTT version 3.1.1
- Add clustering
- Add WebSocket _(default port: 2883)_
- Add SSL _(default port: 8883, default WebSocket SSL port: 9883)_
- Add plugin's : Framework & Default / Example Plugins
- Add admin HTTP REST APIs : listing topics, sessions

## Author
Park Hyunjeong / <anyflow@gmail.com>

## License
Lannister is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
