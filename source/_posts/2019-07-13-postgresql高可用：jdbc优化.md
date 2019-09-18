title: PostgreSQL高可用：JDBC优化
date: 2019-07-13 22:23:30
category:
tags:
---

## JDBC URL连接参数

- `targetServerType` = String: 只允许连接到具有所需状态的服务器，许可的值有any、master、secondary、preferSecondary。主/从区别目前是通过观察服务器是否允许写入来实现的。如果有可用的值，preferSecondary将尝试连接到secondary，否则将返回到允许连接的master。
- `loadBalanceHosts` = boolean: 默认（false）；若设为true则会从一组合适的候选主机中随机选择一个。
- `reWriteBatchedInserts` = boolean: 优化批量插入，从`insert into foo values(); insert into foo values()` 改进为 `insert into foo values (), ()`。可能会有2-3倍的性能提升。

## FusionJdbc配置示例

**连接主节点**

```HOCON
fusion.jdbc {
  default {
    poolName = "hongka_openapi"
    jdbcUrl = "jdbc:postgresql://10.0.32.37:5432,10.0.32.36:5432,10.0.32.35:5432/hongka_openapi?reWriteBatchedInserts=true&targetServerType=master"
    username = "hongka"
    password = "ZDexPJYTLi2Z"
    connectionTestQuery = "select 1;"
    maximumPoolSize = 4
    numThreads = 4
  }
}
```

**连接从节点**

```HOCON
fusion.jdbc {
  default {
    poolName = "hongka_openapi"
    jdbcUrl = "jdbc:postgresql://10.0.32.37:5432,10.0.32.36:5432,10.0.32.35:5432/hongka_openapi?reWriteBatchedInserts=true&targetServerType=preferSecondary&loadBalanceHosts=true"
    username = "hongka"
    password = "ZDexPJYTLi2Z"
    connectionTestQuery = "select 1;"
    maximumPoolSize = 4
    numThreads = 4
  }
}
```
