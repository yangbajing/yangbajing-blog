title: 使用 Pulsar CDC 同步 PostgreSQL 数据造成 WAL 文件数量持续增长问题排查
date: 2021-09-22 19:50:31
category:
tags:
---

## 问题

监控发现 PostgreSQL 的 `pg_wal` 日志文件一直在持续增长，设置的 `max_wal_size = 2GB` 参数值未起作用。
```
-bash-4.2$ du -sh $PGDATA/pg_wal
61G    /data/pgsql/12/data/pg_wal
```


```postgresql
postgres=# select pg_walfile_name('0/14CB2278');
     pg_walfile_name      
--------------------------
 000000010000000000000014
(1 行记录)
```

进一步查询文件发现其创建时间为 9 月 1 号：
```
-bash-4.2$ ls -l $PGDATA/pg_wal/000000010000000000000014
-rw------- 1 postgres postgres 16777216 9月   1 12:31 000000010000000000000014
```

查询指定日志文件与单前最新日志文件的大小也于磁盘空间战胜一致
```postgresql
postgres=# select pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), '0/14CB2278'));
 pg_size_pretty 
----------------
 61 GB
(1 行记录)
```
