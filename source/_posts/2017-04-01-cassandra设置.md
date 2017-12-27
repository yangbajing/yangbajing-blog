title: Cassandra设置
date: 2017-04-01 12:29:41
categories: 
- bigdata
- cassandra
tags:
- cassandra
---

## 操作系统

**修改操作系统的TCP keepalive**

```
sudo /sbin/sysctl -w net.ipv4.tcp_keepalive_time=60 net.ipv4.tcp_keepalive_intvl=60 net.ipv4.tcp_keepalive_probes=5
```

## 集群机制

**一致性哈希**

- Gossip协议：用于在环内节点之间传播Cassandra状态信息
- Snitch：支持多个数据中心
- 复制策略：数据的冗余生策略

**commit log**

- 进行写操作时，先把数据定入commit log
- 只有数据被写入commit log时，才算写入成功
- 当发生掉电、实例崩溃等问题时，可以使用commit log进行恢复

**memtable**

- 数据成功写入commit log后，就开始写入内存中的memtable
- memtable中的数据达到一定阈值后，开始把数据写入硬盘中的SSTable，然后在内存中重新建立一个memtable接收下一批数据
- 上述过程是非阻塞的
- 查询时优先查询memtable

**副本因子----控制数据的冗余份数**

```sql
CREATE KEYSPACE Excelsior WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };
CREATE KEYSPACE Excalibur WITH REPLICATION = {'class' : 'NetworkTopologyStrategy', 'dc1' : 3, 'dc2' : 2};
```

## 可调节的一致性

### 写操作的Consistency Level

**ANY**

- 任意一个节点写操作已经成功。如果所有的replica节点都挂了，写操作还是可以在记录一个hinted handoff事件之后，返回成功。如果所有的replica节点都挂了，写入的数据，在挂掉的replica节点恢复之前，读不到。
- 最小的延时等待，并且确保写请求不会失败。相对于其他级别提供最低的一致性和最高的可用性。

**ALL**

- 写操作必须将指定行的数据写到所有replica节点的commit log和memtable。
- 相对于其他级别提供最高的一致性和最低的可用性。

**EACH\_QUORUM**

- 写操作必须将指定行的数据写到每个数据中心的quorum数量的replica节点的commit log和memtable。
- 用于多数据中心集群严格的保证相同级别的一致性。例如，如果你希望，当一个数据中心挂掉了，或者不能满足quorum数量的replica节点写操作成功时，写请求返回失败。

**LOCAL\_ONE**

- 任何一个本地数据中心内的replica节点写操作成功。
- 对于多数据中心的情况，往往期望至少一个replica节点写成功，但是，又不希望有任何跨数据中心的通信。LOCAL\_ONE正好能满足这样的需求。

**LOCAL\_QUORUM**

- 本地数据中心内quorum数量的replica节点写操作成功。避免跨数据中心的通信。
- 不能和SimpleStrategy一起使用。用于保证本地数据中心的数据一致性。

**LOCAL\_SERIAL**

- 本地数据中心内quorum数量的replica节点有条件地（conditionally）写成功。
- 用于轻量级事务（lightweight transaction）下实现linearizable consistency，避免发生无条件的（unconditional）更新。。

**ONE**

- 任意一个replica节点写操作已经成功。
- 满足大多数用户的需求。一般离coordinator节点具体最近的replica节点优先执行。

（即使指定了consistency level ON或LOCAL\_QUORUM，写操作还是会被发送给所有的replica节点，包括其他数据中心的里replica节点。consistency level只是决定了，通知客户端请求成功之前，需要确保写操作成功的replica节点的数量。）

### 读操作的Consistency Level

**ALL**

- 向所有replica节点查询数据，返回所有的replica返回的数据中，timestamp最新的数据。如果某个replica节点没有响应，读操作会失败。
- 相对于其他级别，提供最高的一致性和最低的可用性。

**EACH\_QUORUM**

- 向每个数据中心内quorum数量的replica节点查询数据，返回时间戳最新的数据。
- 同LOCAL\_QUORUM。

**LOCAL\_SERIAL**

- 同SERIAL，但是只限制为本地数据中心。
- 同SERIAL。

**LOCAL\_QUORUM**

- 向每个数据中心内quorum数量的replica节点查询数据，返回时间戳最新的数据。避免跨数据中心的通信。
- 使用SimpleStrategy时会失败。

**LOCAL\_ONE**

- 返回本地数据中心内离coordinator节点最近的replica节点的数据。
- 同写操作Consistency level中该级别的用法。

**ONE**

- 返回由snitch决定的最近的replica返回的结果。默认情况下，后台会触发read repair确保其他replica的数据一致。
- 提供最高级别的可用性，但是返回的结果不一定最新。

**QUORUM**

- 读取所有数据中心中quorum数量的节点的结果，返回合并后timestamp最新的结果。
- 保证很强的一致性，虽然有可能读取失败。

**SERIAL**

- 允许读取当前的（包括uncommitted的）数据，如果读的过程中发现uncommitted的事务，则commit它。
- 轻量级事务。

**TWO**

- 返回两个最近的replica的最新数据。
- 和ONE类似。

**THREE**

- 返回三个最近的replica的最新数据。
- 和TWO类似。

### 关于QUORUM级别

QUORUM级别确保数据写到指定quorum数量的节点。一个quorum的值由下面的公式四舍五入计算而得：

```
(sum_of_replication_factors / 2) + 1
```

`sum_of_replication_factors` 指每个数据中心的所有 replication\_factor 设置的总和。

例如，如果某个单数据中心的replication factor是3，quorum值为2-表示集群可以最多容忍1个节点down。如果replication factor是6，quorum值为4-表示集群可以最多容忍2个节点down。如果是双数据中心，每个数据中心的replication factor是3，quorum值为4-表示集群可以最多容忍2个节点down。如果是5数据中心，每个数据中心的replication factor of 3，quorum值为8 。

如果想确保读写一致性可以使用下面的公式：

```
(nodes_written + nodes_read) > replication_factor
```

例如，如果应用程序使用QUORUM级别来读和写，replication factor 值为3，那么，该设置能够确保2个节点一定会被写入和读取。读节点数加上写写点数（4）个节点比replication factor （3）大，这样就能确保一致性。

## 应用开发

### Java批量查询、写入配置

在使用 `BatchStatement` 进行插入操作时会发现，当数据量稍大以后数据库中并没有加入新的数据。这是因为Cassandra默认对批量操作的数据大小限制得比较低。我们将其修改即可。

```shell
# Log WARN on any batch size exceeding this value. 5kb per batch by default.
# Caution should be taken on increasing the size of this threshold as it can lead to node instability.
batch_size_warn_threshold_in_kb: 1000

# Fail any batch exceeding this value. 50kb (10x warn threshold) by default.
batch_size_fail_threshold_in_kb: 2000
```
