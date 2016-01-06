title: Cassandra设置
date: 2015-12-07 11:29:41
categories: bigdata
tags:
- cassandra
---

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

`CREATE KEYSPACE Excelsior WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };`

`CREATE KEYSPACE "Excalibur" WITH REPLICATION = {'class' : 'NetworkTopologyStrategy', 'dc1' : 3, 'dc2' : 2};`

