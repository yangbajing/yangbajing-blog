title: Cassandra备份、恢复
date: 2017-12-05 14:27:42
tags:
---

## 备份和数据恢复

### 关于镜像

Cassandra 通过直接保存所有在data目录中的磁盘数据文件（SSTable file）的镜像来备份数据。当系统还在线的时候，你可以保存所有
的keyspace数据或者单个keyspace数据，或者某一张表的数据。

使用并行的ssh工具，比如pssh，你可以给整个集群做镜像。这提供一种最终一致性备份。虽然没有一个节点可以在镜像制作过程中保证
他和备份节点数据的一致性，Cassandra内置一致性机制会使用一个恢复镜像恢复一致性。

当整个系统范围内的镜像都已经完成，你可以开启每个节点的增量备份，它将备份那些最后一次镜像后有改变的数据：每次SSTable刷新，
一个硬链接被复制到 data目录的/backups 子目录中（provided JNA is enabled）

如果允许JNA，镜像将只是建立一个硬链接。否则io将由于文件被从一个地方拷贝到另一处而增长，将明显降低效率。

## 创建镜像

**操作过程**

```
$ nodetool -h localhost -p 7199 snapshot <keyspace name>
```

*请使用实际的 keyspace 名替换 `<keyspace name>` 段*。

看下面实际操作：

```
$ /home/app/local/cassandra/bin/nodetool snapshot ig_crawler
Requested creating snapshot(s) for [ig_crawler] with snapshot name [1514377074990] and options {skipFlush=false}
Snapshot directory: 1514377074990
```

`1514377074990` 为生成的镜像快照名字，我们使用 `tree` 命令看看执行创建镜像后的 ig_crawler keyspace 目录结构：

```
[devops@dn126 ig_crawler]$ tree -d -L 4
.
├── c_gather_task_log-7ec1ecb062f411e78129670c2365db09
│   ├── backups
│   └── snapshots
│       └── 1514377074990
├── c_web_parsed-ad1019e0567711e7ba3fb1ef858b2357
│   ├── backups
│   └── snapshots
│       └── 1514377074990
└── c_web_raw-78427450564611e7ba3fb1ef858b2357
    ├── backups
    └── snapshots
        └── 1514377074990

12 directories
```

可以看到，在 ig_crawler keyspace中有3张表，`snapshot` 命令为每张表都生成了一个镜像，镜像名为：`1514377074990`。在这里，
我们将需要的 `<table name>-<uuid>/<snapshot name>` 目录下的数据都拷贝出来，复制到需要恢复的机器上。

## 恢复数据

1. 确保要恢复的数据表模式存在，Cassandra只能从存在的表模式中恢复镜像里的数据。如果表模式不存在，则必需首先创建它。
2. 如果可行，务必截断表。使用 `TRUNCATE TABLE <table name>` 将表中数据截断。我们现在做的都是全量备份，如果因为意外的删
    除数据造成 tombstone 的时间发生混乱，截断表可以确保不会有本应被删除的数据被恢复的情况发生。
3. 关闭所有节点 `nodetool stop`
4. 将生成的镜像文件，`<table name>-<uuid>/<snapshot name>/*` 目录下的所有数据拷贝到目标 Cassandra 的
   `<data directory>/<keyspace>/<table name>-<uuid>/` 目录中。*（注意：是将镜像目录的数据拷贝到目录keyspace的表目录里）*
5. 启动 Cassandra 并执行 `nodetool refresh` 命令恢复镜像内的数据
