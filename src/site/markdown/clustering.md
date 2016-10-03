## Clustering

Lannister's clustering features mainly rely on [Hazelcast](https://github.com/hazelcast/hazelcast), hence the documentation briefly depicts features, runtime architecture and configuration related with clustering.
In the chapter, general characteristics and configuration of Lannister cluster are explained.

### Characteristics
* **Distribution Transparency** : Clustered data are fully accessible on any node in the same cluster without knowing cluster information include location.
* **High Availability** : Backups are distributed on every update of Topic / Session / Message / Message Status data in a cluster.
* **Data Partitioning** : Clustered data is stored in Distributed Cache, thus it minimize redundancy overhead and makes scaling out easy(compared to data replication manner).
* **Single process(JVM) per node** : Logics and (clustered) data in a Lannister node are co-located in a single process(JVM), which removes additional dependency and configuration(Hazelcast Embedded Mode).
* **Dynamic Joining** : Lannister nodes can be joined/unjoined dynamically, which makes scaling elastic. It also supports auto-scaling of AWS explicitly.

### Runtime Architecture
![runtime architecture](images/clustering_architecture.svg)

The diagram depicts characteristics described in the above section. 3 Lannister nodes works in a cluster and a MQTT client access one of the nodes. The client accesses all data in the cluster but it doesn't care where the data is in thanks to logical view. Behind the scene, the node access data to local or remote master and update backup at each access.

Every nodes works in its own single process(JVM) and has no dependency on other process.

(Dynamic Joining is not depicted in the diagram).

### Configuration
For more information, refer [Understanding Configuration](http://docs.hazelcast.org/docs/3.7/manual/html-single/index.html#understanding-configuration)
