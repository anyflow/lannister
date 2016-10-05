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
## `[DRAFT]` Package/Project structure

Lannister package and project structure are described.

### Package structure
```
root              # package root
 |-- bin          # startup / shutdown script files
 |-- config       # main / clustering / logging configuration files               
 |-- lib          # lannister and its dependency jar files
 |-- plugin       # plugin-example and 3rd party plugin files
 |-- webapp       # lannister admin web resource files
 |-- logs         # runtime log files

```

### Project structure
Lannister has 3 sub-projects - `interface`, `server`, `plugin-example`.

* [`interface`](./lannister-interface/index.html) : Source codes common to `server` and `plugin-example`
* [`server`](./lannister-server/index.html) : Lannister server source codes
* [`plugin-example`](./lannister-plugin-example/index.html) : Lannister plugin example source codes

Lannister follows Maven standard directory layout. The diagram below shows main directories of the project. For more information about it refer [Introduction to the Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html).

```
root                   # project root
 |-- src
 |    |-- site         # root project documentation & config/resource files
 |-- target               
 |-- interface         # interface project root
 |    |-- src
 |    |    |-- main    # interface project source code & config/resource files
 |    |-- target
 |-- server            # server project root
 |    |-- src
 |    |    |-- main    # interface project source code & config/resource files
 |    |-- target       #

```
