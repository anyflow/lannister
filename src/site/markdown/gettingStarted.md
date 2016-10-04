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
## `[DRAFT]` Getting Started

#### Prerequisite
* Java 8 or later
* Maven 3 (case for starting from source)

#### Starting from package
1. Go to https://goo.gl/GJ1piF
2. Download a compressed file preferred (`zip` / `tar.gz` / `tar.bz2`)
3. Unpack downloaded file and move into the directory
4. Execute `./bin/startup.sh`
5. For shutdown, execute `./bin/shutdown.sh`

#### Starting from source

```{r, engine='bash', count_lines}

# Download lannister source
git clone https://github.com/anyflow/lannister.git

# Change directory to lannister
cd lannister

# Build(The command builds all sub-projects(interface, server, plugin-example simultaneously)
mvn install

# Run lannister server
mvn exec:java -pl server
```
