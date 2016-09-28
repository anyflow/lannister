# Version History
## version 0.9.7.1 / Sep 28, 2016
- Move deploy-repo to https://goo.gl/GJ1piF

## version 0.9.7 / Sep 28, 2016
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

## version 0.9.6 / Sep 21, 2016
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

## version 0.9.5.1 / Sep 11, 2016
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

## version 0.9.5.0 / May 21, 2016
- **Add `$SYS` Required Topics**
- Code Review, Test based on chapter 3.2 of Spec v3.1.1 Mandatory normative statements

## version 0.9.5-beta1 / May 16, 2016
- **Add clustering**
- **Add WebSocket (default port: `2883`)**
- **Add SSL (default port: `8883`, default WebSocket SSL port: `9883`)**
- **Add plugin's : Framework & Default / Example Plugins**
- **Add admin HTTP REST APIs : listing topics, sessions**
- Code Review, Test based on chapter 3.1 of Spec v3.1.1 Mandatory normative statements
- Implement full features of Protocol Specification MQTT version 3.1.1