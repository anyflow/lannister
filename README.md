# Lannister

[![Build Status](https://travis-ci.org/anyflow/lannister.svg?branch=master)](https://travis-ci.org/anyflow/lannister) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Coverage)

High performance MQTT broker w/ full spec,Clustering,WebSocket,SSL on Java - [Netty](https://github.com/netty/netty) & [Hazelcast](https://github.com/hazelcast/hazelcast)
**under CONSTRUCTION**.

## Features
1. Full Protocol Specifications([MQTT Version 3.1](http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html), [MQTT Version 3.11](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html)) Support includes
   * QoS 0,1,2 on Publish / Subscribe
   * Clean/Persistent Session
   * Retain / Will Message
2. 1000K connections per node
3. Clustering
   * Topic / Message / Message Status / Session redundant
   * Scalable
   * No SPOF(peer-to-peer based : No master and slave)
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
Lannister is **under CONSTRUCTION**. But the most of features above are implemented and tested. You can run and test Lannister via,
```
// The below commands requires Java 8 and Maven 3

git clone https://github.com/anyflow/lannister.git
mvn package exec:java
```
## Version History
##### [version 0.9.5-beta1](https://github.com/anyflow/lannister/commit/c47558dca141f5dd8162e512ec5079731814b6f4) / May 16, 2016
- Code Review, Test based on chapter 3.1 of Spec v3.1.1 Mandatory normative statements
- Implement full features of Protocol Specification MQTT version 3.1.1
- Add WebSocket _(default port: 2883)_
- Add SSL _(default port: 8883, default WebSocket SSL port: 9883)_
- Add Plugin - Framework & Default / Example Plugins
- Add admin HTTP REST APIs - listing topics, sessions

## Author
Park Hyunjeong / <anyflow@gmail.com>

## License
Lannister is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
