title: PostgreSQL安装配置
date: 2016-07-26 10:29:01
category:
- bigdata
- postgresql
tags:
- postgresql
- 高可用
- 负载均衡
- 复制
---

## 安装

本文使用[http://www.enterprisedb.com/products-services-training/pgdownload](http://www.enterprisedb.com/products-services-training/pgdownload)提供的PostgreSQL二进制版本。

```
wget -c http://get.enterprisedb.com/postgresql/postgresql-9.5.3-2-linux-x64.run
chmod +x postgresql-9.5.3-2-linux-x64.run
```

使用如下命令安装（使用无人值守的方式安装，可以通过`--help`查看可选配置选项）：

```
sudo ./postgresql-9.5.3-2-linux-x64.run --mode unattended --superpassword yangbajing --locale zh_CN.utf8 --serverport 5432
sudo ln -sf /opt/PostgreSQL/9.5/pg_env.sh /etc/profile.d/
```

**没有重启数据库的话，可能需要单独执行`/opt/PostgreSQL/9.5/pg_env.sh`脚本来设置相关环境变量，这样你才能正常使用PostgreSQL数据库。**

## 关闭数据库

通过向**postgres**进程发送不同的信号来控制关闭的类型。

- SIGTERM: 这是智能关闭模式。在接收SIGTERM后， 服务器将不允许新连接，但是会让现有的会话正常结束它们的工作。仅当所有的会话终止后它才关闭。 如果服务器处在线备份模式，它将等待直到在线备份模式不再被激活。当在线备份模式被激活时， 仍然允许新的连接，但是只能是超级用户的连接（这一例外允许超级用户连接来终止在线备份模式）。 如果服务器在恢复时请求智能关闭，恢复和流复制只有在所有正常会话都终止后才停止。
- SIGINT: 这是快速关闭模式。服务器不再允许新的连接，并向所有现有服务器进程发送SIGTERM，让它们中断当前事务并立刻退出。然后服务器等待所有服务器进程退出并最终关闭。 如果服务处于在线备份模式，备份模式将被终止并致使备份无用。
- SIGQUIT: 这是立即关闭模式。服务器将给所有子进程发送 SIGQUIT并且等待它们终止。那些没有在 5 秒内终止的子进程将被主postgres进程发送 SIGKILL，这样那些进程会终止而不做进一 步等待。这将导致在下一次启动时（通过重放 WAL 日志）恢复。只在紧急 时才推荐这种方式。



