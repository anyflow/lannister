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
## $SYS topics

$SYS Topics are not a part of [MQTT 3.1.1 specification](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718108), but mentioned as non normative comments like the below.

>**Non normative comment**

>* $SYS/ has been widely adopted as a prefix to topics that contain Server-specific information or control APIs
>* Applications cannot use a topic with a leading $ character for their own purposes

>**Non normative comment**

>* A subscription to “#” will not receive any messages published to a topic beginning with a $
>* A subscription to “+/monitor/Clients” will not receive any messages published to “$SYS/monitor/Clients”
>* A subscription to “$SYS/#” will receive messages published to topics beginning with “$SYS/”
>* A subscription to “$SYS/monitor/+” will receive messages published to “$SYS/monitor/Clients”
>* For a Client to receive messages from topics that begin with $SYS/ and from topics that don’t begin with a $, it has to subscribe to both “#” and “$SYS/#”

That means there's no commonly-defined/accepted list of the topics. Lannister adopted concept and definitions described in [SYS Topics](https://github.com/mqtt/mqtt.github.io/wiki/SYS-Topics) of mqtt.github.io whose content is quite acceptable in the situation.

Topics in the below are full list of 'Required Topics' defined in the site, supported by Lannister(Optional Topics is arranged for version 1.1)

### Required Topics
|topic name|description|
|---|---|
|`$SYS/broker/load/bytes/received`|The total number of bytes received since the broker started|
|`$SYS/broker/load/bytes/sent`|The total number of bytes sent since the broker started|
|`$SYS/broker/clients/connected`|The number of currently connected clients|
|`$SYS/broker/clients/disconnected`|The total number of persistent clients (with clean session disabled) that are registered at the broker but are currently disconnected|
|`$SYS/broker/clients/maximum`|The maximum number of active clients that have been connected to the broker. This is only calculated when the $SYS topic tree is updated, so short lived client connections may not be counted|
|`$SYS/broker/clients/total`|The total number of connected and disconnected clients with a persistent session currently connected and registered on the broker|
|`$SYS/broker/messages/received`|The total number of messages of any type received since the broker started|
|`$SYS/broker/messages/sent`|The total number of messages of any type sent since the broker started|
|`$SYS/broker/messages/publish/dropped`|The total number of publish messages that have been dropped due to inflight/queuing limits|
|`$SYS/broker/messages/publish/received`|The total number of PUBLISH messages received since the broker started|
|`$SYS/broker/messages/publish/sent`|The total number of PUBLISH messages sent since the broker started|
|`$SYS/broker/messages/retained/count`|The total number of retained messages active on the broker|
|`$SYS/broker/subscriptions/count`|The total number of subscriptions active on the broker|
|`$SYS/broker/time`|The current time on the server|
|`$SYS/broker/uptime`|The amount of time in seconds the broker has been online|
|`$SYS/broker/version`|The version of the broker. Static|
