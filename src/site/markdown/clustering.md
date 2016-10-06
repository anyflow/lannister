## `[DRAFT]` Clustering

Lannister's clustering features mainly rely on Hazelcast and follows its guidance strictly, hence the documentation focused on general clustering characteristics of Lannister and configuration used. For detailed information and fine tuning, refer [Hazelcast Reference Manual](http://docs.hazelcast.org/docs/3.7/manual/html-single/index.html).

### Characteristics
* **Distribution Transparency** : Clustered data are fully accessible on any node in the same cluster without knowing cluster information include location.
* **High Availability** : Backups are distributed on every update of Topic / Session / Message / Message Status data in a cluster.
* **Data Partitioning** : Clustered data is stored in Distributed Cache, thus it minimize redundancy overhead and makes scaling out easy(compared to data replication manner).
* **Single process(JVM) per node** : Logics and (clustered) data in a Lannister node are co-located in a single process(JVM), which removes additional dependency and configuration(Hazelcast Embedded Mode).
* **Dynamic Joining** : Lannister nodes can be joined/unjoined dynamically, which makes scaling elastic. It also supports auto-scaling of AWS explicitly.

![characteristics diagram](images/clustering_architecture.svg)

The diagram depicts characteristics described in the above section. 3 Lannister nodes works in a cluster and a MQTT client access one of the nodes. The client accesses all data in the cluster but it doesn't care where the data is in thanks to logical view. Behind the scene, the node access data to local or remote master and update backup at each access.

Every nodes works in its own single process(JVM) and has no dependency on other process.

(Dynamic Joining is omitted in the diagram).

### Configuration
```
<group>
  <name>LANNISTER</name>
  <password>LANNISTER_HAZELCAST_GROUP_PASSWORD</password>
</group>

<properties>
  <property name="hazelcast.logging.type">slf4j</property>
  <property name="hazelcast.shutdownhook.enabled">false</property>
  <property name="hazelcast.max.no.heartbeat.seconds">15</property>
  <property name="hazelcast.restart.on.max.idle">true</property>
  <property name="hazelcast.client.heartbeat.interval">5000</property>
  <property name="hazelcast.client.heartbeat.timeout">30000</property>
  <property name="hazelcast.client.max.no.heartbeat.seconds">15</property>
</properties>

<network>
  <port auto-increment="true" port-count="${hazelcast.port-count}">${hazelcast.port}</port>
  <join>
    <multicast enabled="${hazelcast.multicast.enabled}">
      <multicast-group>${hazelcast.multicast-group}</multicast-group>
      <multicast-port>${hazelcast.multicast-port}</multicast-port>
    </multicast>
    <tcp-ip enabled="${hazelcast.tcp-ip.enabled}">
      <member>${hazelcast.member1}</member>
      <member>${hazelcast.member2}</member>
    </tcp-ip>
  </join>
</network>
```
For more information, refer [Understanding Configuration](http://docs.hazelcast.org/docs/3.7/manual/html-single/index.html#understanding-configuration)
