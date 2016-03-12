title: Elasticsearch集群设置
date: 2016-03-08 10:25:24
categories:
- bigdata
- elasticsearch
tags:
- elasticsearch
- cluster
---

Elasticsearch是一个优秀的全文检索和分析引擎，由Shay Banon发起的一个开源搜索服务器项目，2010年2月发布。具有分布式性质和实时功能。

本文使用最新版 [Elasticsearch 2.2.0](https://download.elasticsearch.org/elasticsearch/release/org/elasticsearch/distribution/tar/elasticsearch/2.2.0/elasticsearch-2.2.0.tar.gz)，推荐使用 Java 8 update 20或更新版。

## 配置

Elasticsearch使用很方便，默认开箱即用。不过做为一个集群，还是需要稍做一些配置。整个配置都位于 config 目录，可以看到两个文件：elasticsearch.yml和logging.yml，分别是Elasticsearch服务配置文件和日志配置文件。

elasticsearch.yml设置服务的默认配置值，但因为设置可在运行时更改，所以这里的值可能并不是服务运行中实际的设置参数。但有两个值在运行时是不能更改的，分别：cluster.name和node.name。

- cluster.name: 保存集群的名字，不同的集群用名字来区分，设置成相同名字的各个节点将开成一个集群。
- node.name: 节点实例的名字，可以不用设置，服务启动时将自动选择一个唯一的名字。不过需要注意的是，每次服务启动时选择的，所以在每次重启后名字可能都不一样。若需要在API中提及具体实例名或者用监控工具查看具体节点时，自定义一个名字还是很有帮助的。

除了集群相关配置，我们还应该修改 path.* 配置项：

- path.data: 持久化数据存储位置
- path.logs: 日志存储位置

## 系统需求

Elasticsearch在建立索引，尤其是在有很多分片和副本的情况下将会创建很多文件。需要修改系统对打开文件描述符的限制，推荐设置为大于32000个。在Linux系统上，一般在 /etc/security/limits.conf 目录修改。

```
root soft nofile 65535
root hard nofile 65535
* soft nofile 65535
* hard nofile 65535
```



