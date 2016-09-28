# Lannister

[![Build Status](https://travis-ci.org/anyflow/lannister.svg?branch=master)](https://travis-ci.org/anyflow/lannister) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Coverage)

High performance MQTT broker w/ full specifications support, Clustering, WebSocket, SSL on Java - [Netty](https://github.com/netty/netty) & [Hazelcast](https://github.com/hazelcast/hazelcast) **under CONSTRUCTION**.

## Features
1. Full Protocol Specifications([MQTT Version 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html), [MQTT Version 3.1](http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html)) Support includes
   * QoS 0,1,2 on Publish / Subscribe
   * Clean / Persistent Session
   * Retained / Will Message
2. 1000K connections per node
3. Clustering
   * Topic / Message / Message Status / Session redundant
   * peer-to-peer based : No master and slave
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
10. Data Persistency _(arranged for Lannister version 1.1)_

## Getting Started
Lannister is **under CONSTRUCTION**. But the most of features above are implemented and tested. You can run and test Lannister simply via,

#### Executing pre-packaged version
1. Install java 8
2. Go to https://github.com/anyflow/lannister/tree/deploy-repo
3. Download a prefered compressed file(`zip` / `tar.gz` / `tar.bz2`)
4. Unpack downloaded file and move into the directory
5. Execute `./bin/startup.sh` (before execution, change mode may be required via `chmod 700 ./bin/startup.sh`)
6. For shutdown, execute `./bin/shutdown.sh` (Like `startup.sh`, `chmod` may be required before execution)

#### Source compiling version

```{r, engine='bash', count_lines}
# The below commands require Java 8 and Maven 3

# Clone lannister source
git clone https://github.com/anyflow/lannister.git

# Change directory to lannister
cd lannister

# Build all(interface, server, plugin-example) projects
mvn install

# Run lannister server directed by lannister.cfg/hazelcast.config.xml/log4j.xml files in conf directory
mvn exec:java -pl server
```

## Project site
For more information, visit http://anyflow.github.io/lannister/

## Version History
##### version 0.9.7 / Sep 28, 2016 KST
- **Pass specification tests**  
  * test suite : [Eclipse Paho Testing Utilities](https://github.com/eclipse/paho.mqtt.testing)
  * command : `run_test.py --testdir testsuites/basic`
  * passed testcase count : 321/321
- Fix failing to delete retain message
- Fix failing QoS adjustment in retain message publish
- Fix message reference counting error
- Refine `lannister.cfg` property names
- Optimize performance : Minimize serialization
- Update REST API address(default port : `8090`)
  * GET /topics => GET /api/topics
  * GET /session => GET /api/sessions
- Add Client ID generation REST API
  * POST /api/clients

##### version 0.9.6 / Sep 21, 2016
- **Open project site**(http://anyflow.github.io/lannister/)
- **Open deploy repository**(https://github.com/anyflow/lannister/tree/deploy-repo)
- **Pass specification tests**
  * test suite : [Eclipse Paho Testing Utilities](https://github.com/eclipse/paho.mqtt.testing)
  * command : `client_test.py -z -d -s -n`
  * passed testcase count : 15/15
- Handle overlapped topic filters
- Add nosys filter in topics REST API(`/topics?filter=nosys`)
- Add [MQTT-4.7.2-1] feature(no matching an invalid topic filter case)
- Add feature : Ban subscribing specific topic filters(`mqttserver.subscribe.banned_topicfilters` property in `lannister.cfg`) 
- Fix failing queued message delivery
- Fix disconnection on receiving retained message puback
- Fix disconnection on receiving message pubrec(on resending QoS2 message)

##### version 0.9.5.1 / Sep 11, 2016
- Externalize Hazelcast config - Add hazelcast.config.xml
- Add docker file and docker property - Run maven with profile (`mvn install -Pdocker`)
- Add null checker in hazelcast serialization logics
- Change WebSocket(ssl) ports to `9001`(`9002`)
- Update Hazelcast version to 3.7.1
- Update Netty version to 4.1.5
- Add PMD / Findbugs / Jacoco / surefire reports
- Start clustering TEST/FIX
  * Fix subscription failing on existing topic in clustered state
- Fix subscriptions remaining on disposing (clean) session
- Fix reconnect failing persisted session  
- Fix remaining no subscriber topic

##### version 0.9.5.0 / May 21, 2016
- **Add `$SYS` Required Topics**
- Code Review, Test based on chapter 3.2 of Spec v3.1.1 Mandatory normative statements

##### version 0.9.5-beta1 / May 16, 2016
- **Add clustering**
- **Add WebSocket (default port: `2883`)**
- **Add SSL (default port: `8883`, default WebSocket SSL port: `9883`)**
- **Add plugin's : Framework & Default / Example Plugins**
- **Add admin HTTP REST APIs : listing topics, sessions**
- Code Review, Test based on chapter 3.1 of Spec v3.1.1 Mandatory normative statements
- Implement full features of Protocol Specification MQTT version 3.1.1

## Author
Park Hyunjeong / <anyflow@gmail.com>

## License
Lannister is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).