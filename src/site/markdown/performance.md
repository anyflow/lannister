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
## `[DRAFT]` [DRAFT] Performance Test

* linear performance drop due to the nature of distributed cache manner.

* CPU usage
* memory usage
* Latency
* concurrent connections
* message per second

#### Test conditions

1. pub/sub 1:1 테스트(publisher와 subscriber가 다름)
2. publish interval : 5 minutes
3. keep alive interval : 30 minutes
4. QoS level : 1 (pub / sub 공통)
5. payload size : 256 byte

|no|session count(try)|session count(established)|cpu usage|memory usage|pub-sub rate (msg/sec)|
|---|---|---|---|---|---|
||||||||

#### server spec.
<!-- r3.2xlarge : 8 core / 64G -->

#### server OS
Ubuntu 14.0.3LTS

#### OS kernel tuning History
* `net.core.somaxconn = 65535` : socket 연결 대기 최대 개수(default : 128)
* `fs.nr_open = 1048576` : 단일 프로세스가 운용 가능한 최대 파일 개수
* `fs.file-max = 1048576` : 시스템 전체에서 운용 가능한 최대 파일 개수
* `fs.file-max >= fs.nr_open >= ulimit -n`

#### Lannister version
0.9.8-release
