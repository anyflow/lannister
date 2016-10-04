<!--
  Copyright 2016 The Lannister Project

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 -->
## `[DRAFT]` Lannister

[![Build Status](https://travis-ci.org/anyflow/lannister.svg?branch=master)](https://travis-ci.org/anyflow/lannister) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Coverage)

Lannister is high performance MQTT broker equipped with full specifications support, Clustering, WebSocket, SSL written in Java with using [Netty](https://github.com/netty/netty) & [Hazelcast](https://github.com/hazelcast/hazelcast).

### Getting Started
For installing and starting up Lannister, move into [Getting Started](./gettingStarted.html).

### Features
##### Full Protocol Specifications([MQTT Version 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html), [MQTT Version 3.1](http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html)) support includes
* QoS 0,1,2 on Publish / Subscribe
* Clean / Persistent Session
* Retained / Will Message
* For more information, refer [Specification Conformance Test/Review Report](./specification.html).

##### Clustering
* Distribution Transparency / High Availability / Distributed Cache
* Easy, elastic Scaling out
* For more information, refer [Clustering](./clustering.html).

##### High performance
* 1M connections per node
* For more information, refer [Performance Test Report](./performance.html).

##### WebSocket

##### SSL (TCP / WebSocket channels both)

##### $SYS topics
* Static / Required Topics supported described in [SYS Topics](https://github.com/mqtt/mqtt.github.io/wiki/SYS-Topics)
* For more information, refer [$SYS topics](./dollarSys.html).

##### Plug-In Support
* Authenticator
* Authorizer
* ServiceChecker
* ConnectEventListener
* DisconnectEventListener
* PublishEventListener
* DeliveredEventListener
* SubscribeEventListener
* UnsubscribeEventListener
* For more information, refer [Plug-In](./plugin.html).

##### HTTP REST APIs for admin, client

##### Web administration Site

##### Features arranged for Lannister version 1.1

* Bridge
* Data Persistency
* Optional $SYS Topics
