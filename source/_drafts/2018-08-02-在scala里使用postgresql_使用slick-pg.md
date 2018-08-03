title: 在Scala里使用PostgreSQL 使用slick-pg
date: 2018-08-02 00:30:48
category: scala
tags:
  - jdbc
  - postgres
  - scala
  - slick
  - slick-pg
---

Scala里访问关系性数据库还是延用的JDBC技术，本文将介绍在Scala里怎样使用Slick来访问PostgreSQL数据库。同时，利用Scala丰富的语言特性来简化我们的数据库开发工作。

[Slick]()

*本文示例代码：[https://github.com/yangbajing/scala-applications/tree/master/jdbc-slick](https://github.com/yangbajing/scala-applications/tree/master/jdbc-slick)*。

## 安装数据库

为了统一数据库环境，这里使用Docker来安装数据库。Dockerfile脚本如下：

```
FROM postgres:10

RUN localedef -i zh_CN -c -f UTF-8 -A /usr/share/locale/locale.alias zh_CN.UTF-8

```

