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
# Lannister

[![Build Status](https://travis-ci.org/anyflow/lannister.svg?branch=master)](https://travis-ci.org/anyflow/lannister/branches) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/8d72feca76504d89a9846beecbbbc34b)](https://www.codacy.com/app/anyflow/lannister?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=anyflow/lannister&amp;utm_campaign=Badge_Coverage)

**Lannister** is a lightweight MQTT broker equipped with full specifications support, Clustering, WebSocket, SSL written in Java.

> MQTT is a machine-to-machine (M2M)/"Internet of Things" connectivity protocol. It was designed as an extremely lightweight publish/subscribe messaging transport. It is useful for connections with remote locations where a small code footprint is required and/or network bandwidth is at a premium. For example, it has been used in sensors communicating to a broker via satellite link, over occasional dial-up connections with healthcare providers, and in a range of home automation and small device scenarios. It is also ideal for mobile applications because of its small size, low power usage, minimised data packets, and efficient distribution of information to one or many receivers - [http://mqtt.org](http://mqtt.org/)

### Features
#### Full Protocol Specifications([MQTT Version 3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html), [MQTT Version 3.1](http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/mqtt-v3r1.html)) support includes
* QoS 0,1,2 on Publish / Subscribe
* Clean / Persistent Session
* Retained / Will Message
* For more information, refer [Specification Conformance Test/Review Report](./specification.html).

#### Clustering
* Distribution Transparency / High Availability / Distributed Cache
* Easy, elastic Scaling out
* For more information, refer [Clustering](./clustering.html).

#### Plug-In Support
* Adapter/Framework for customizing broker logics against MQTT events
* For more information, refer [Plug-In](./plugin.html).

#### WebSocket
* Built-in WebSocket support on the same JVM of regular TCP channel
* For more information, refer [WebSocket settings](./configuration.html#websocket).

#### SSL (TCP / WebSocket channels both)
* Built-in SSL support for both TCP(`mqtts://`) and WebSocket(`wss://`) channels
* For more information, refer [SSL settings](./configuration.html#ssl).

#### $SYS topics
* Static / Required Topics supported described in [SYS Topics](https://github.com/mqtt/mqtt.github.io/wiki/SYS-Topics)
* For more information, refer [$SYS topics](./dollarSys.html).

#### High performance
* 100K connections (in non-clustering Mode)
* For more information, refer [Performance Test Report](./performance.html).

#### Features arranged for Lannister version 1.1 _(Under development)_
* Web administration Site
* extended HTTP REST APIs for admin, client
* Bridge
* Data Persistency
* Optional $SYS Topics

### Getting Started
For installing and starting up Lannister, move into [Getting Started](./gettingStarted.html).
