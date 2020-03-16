title: 译：Akka 2.6.4 Released
date: 2020-03-16 11:07:42
category:
  - scala
  - akka
tags:
  - scala
  - akka
  - at-least-once-delivery
---

***原文地址：*** <a href="https://akka.io/blog/news/2020/03/13/akka-2.6.4-released" target="_blank">https://akka.io/blog/news/2020/03/13/akka-2.6.4-released</a>

亲爱的 hakkers 们，

We are excited to announce a new patch release of Akka 2.6. In addition to bug fixes and improvements it includes 3 bigger new features related to Akka Cluster. Notable changes relative to 2.6.3 include:

我们很激动的公布 Akka 2.6 的新的补丁版本发布。（该版本）添加了 bug 修复和有关 Akka Cluster 的3个比较大的新特性。它相对 2.6.3（版本）的显著变化包括：

- 可靠交付（at-least-once-delivery），见以下说明：[#20984](https://github.com/akka/akka/issues/20984)
- 分片守护进程，见以下说明：[#28710](https://github.com/akka/akka/issues/28710)
- 分布式发布/订阅，见以下说明：[#26338](https://github.com/akka/akka/issues/26338)
- 对来自 `messageAdapter`、`pipeToSelf` 和 `ask` 的异常使用监督（机制），[#28592](https://github.com/akka/akka/issues/28592)
- `ActorRef` 能静默的忽略所有消息，[#25306](https://github.com/akka/akka/issues/25306)
- 在 ByteString 中支持 base64，并提高了 `ByteString.decodeString` 的性能，[#28697](https://github.com/akka/akka/issues/28697)
- 提高了 `ExplicitlyTriggeredScheduler` 的取消效果，感谢 [@bszwej](https://github.com/bszwej)，[#28604](https://github.com/akka/akka/issues/28604)
- 支持 Streams 里的日志标记（log marker），感谢 [@duxet](https://github.com/duxet)，[#28450](https://github.com/akka/akka/issues/28450)
- InmemJournal 处理标记事件，感谢 [@odd](https://github.com/odd)，[#28552](https://github.com/akka/akka/issues/28552)
- 修复 Java 中 lease（用于集群节点间的分布式协调，见：Akka Coordination）的加载实现，[#28685](https://github.com/akka/akka/issues/28685)
- 改善了分片查询，[#27406](https://github.com/akka/akka/issues/27406)
- 改善 `PostStop` 监护人行为，[#28557](https://github.com/akka/akka/issues/28557)
- 更新 Jackson 到 2.10.3 版本，[#28678](https://github.com/akka/akka/pull/28678)
- 更新 Aeron 到 1.26.0 版本，[#28690](https://github.com/akka/akka/pull/28690) 

A total of 84 issues were closed since 2.6.3. The complete list can be found on the [2.6.4 milestone](https://github.com/akka/akka/milestone/162?closed=1) on github.

从 2.6.3 版本开始关闭的所有问题，完整列表能在 Github 的 [2.6.4 milestone](https://github.com/akka/akka/milestone/162?closed=1) 页面找到。

## Reliable delivery
## 可靠交付

Reliable delivery is useful for interactions between actors that require at-least or effectively once processing. Lost messages are detected, resent and deduplicated as needed. In addition, it also includes flow control for
the sending of messages to avoid that a fast producer overwhelms a slower consumer or sends messages at a higher rate than what can be transferred over the network.

可靠交付对于 actor 之间要求至少或确定一次处理交互很有用。检测到丢失消息，根据需要重发和删除重复。另外，也包含消息发送的流量控制，以避免快速生产者压倒慢速消费者或者（避免）以比网络可以处理更高的速率发送消息。

There are 3 supported patterns for point-to-point, work pulling and Cluster Sharding.

有3种被支持的模式：点对点（point-to-point）、工作拉取（work pulling）和集群分片（Cluster Sharding）。

Even if the primary purpose is to use this in an Akka Cluster it works with exactly the same APIs for a local `ActorSystem`. The flow control and work pulling mechanism can be equally important for local communication between actors.

即使主要是在 Akka Cluster 中使用，它也与本地 `ActorSystem` 的 API 完全相同。流量控制和工作拉取机制对于 actor 的本地通信可能同样重要。

You find more information and code examples in the [documentation for reliable delivery](https://doc.akka.io/docs/akka/current/typed/reliable-delivery.html).

你可在 [可靠交付 文档](https://doc.akka.io/docs/akka/current/typed/reliable-delivery.html) 找到更多信息和代码示例。

Since this is a new feature it's marked as ["May Change"](https://doc.akka.io/docs/akka/current/common/may-change.html). It needs feedback from real usage before finalizing the API, and your help with trying it out is very welcome. It is also not recommended to use this module in production just yet.

这个新特性被标记为 ["可能改变"](https://doc.akka.io/docs/akka/current/common/may-change.html)。在 API 被完成之前，它需要实际使用的反馈，非常欢迎您尝试它并帮助（我们）。也建议你不要在生产中使用此模块。

## Sharded daemon processes
## 分片守护进程

Sharded daemon processes is an internal feature from [Lagom](https://www.lagomframework.com) that has been backported to Akka. It allows a number of processing actors to be kept alive and balanced across the cluster. The main envisioned use case is workers that consume tagged Akka persistence events and update read side projections in CQRS applications.

分片守护进程是一个 [Lagom](https://www.lagomframework.com) 内部特性，被反向移植到 Akka。它允许大量（正在进行）处理的 actor 在整个集群保持活跃和平衡。设想的主要用例是多个 worker（分割数据处理）消费被标记的 Akka 持久化事件和 CQRS 应用程序的读端投影工作。

You find more information and a code example in the [documentation for sharded daemon processes](https://doc.akka.io/docs/akka/current/typed/cluster-sharded-daemon-process.html).

你可以在 [分片守护进程 文档](https://doc.akka.io/docs/akka/current/typed/cluster-sharded-daemon-process.html) 找到更多信息和代码示例。

Since this is a new feature it's marked as ["May Change"](https://doc.akka.io/docs/akka/current/common/may-change.html). We are however not expecting any major changes as the API surface is very small.

这个新特性被标记为 ["可能改变"](https://doc.akka.io/docs/akka/current/common/may-change.html)。由于 API 界面很小，我们预计不会有重要改变。

## Distributed publish subscribe
## 分页式发布/订阅

Distributed pub sub for Akka Typed allows for defining topics which actors on any node in the cluster can subscribe to. When a message is published to the topic the message is delivered to all known subscribers. This cluster tool is implemented on top of the typed system receptionist rather than the pre existing classic distributed publish subscribe tool.

Akka Typed 的分布式发布/订阅允许定义主题，集群任意节点上的 actor 都可以订阅。当一个消息被发布到主题，消息将被送达到所有已知的订阅者。该集群工具在类型化系统的接待员（receptionist）上实现，而不是基于经典的分布式发布/订阅工具实现。

The distributed publish subscribe topics also work in a non-cluster setting where all publishers and subscribers are local and can therefore be used as an alternative to the `ActorSystem` event bus.

分布式发布/订阅主题也可以在非集群化设置里工作，所有发布者和订阅者都在本地，即可使用 `ActorSystem` 的事件总线替代（集群方式实现）。

You find more information and a code example in the [documentation for distributed pub sub](https://doc.akka.io/docs/akka/current/typed/distributed-pub-sub.html).

你可以在 [分布式订阅/发布](https://doc.akka.io/docs/akka/current/typed/distributed-pub-sub.html) 找到更多信息和代码示例。

## 功劳

此次发布获得了24位提交者的帮助 - 非常感谢！

```
commits  added  removed
     25   4224      397 Johan Andr?n
     17  18929       73 Patrik Nordwall
     14    470       65 Christopher Batey
     10    273      125 Arnout Engelen
      5    417       28 Renato Cavalcanti
      4    144       42 Enno
      3    170       12 Ignasi Marimon-Clos
      3     46       19 Johannes Rudolph
      2   3458     1448 Helena Edelson
      2    999        2 eyal farago
      2    123        5 Razvan Vacaru
      2      6        1 Arnaud Burlet
      1    127       10 Bart?omiej Szwej
      1    115        6 Viktor Klang (?)
      1     88        0 mghildiy
      1     72        0 Evgeny Sidorov
      1      9        6 Odd M?ller
      1      6        1 Yury Gribkov
      1      3        1 Jacek Ewertowski
      1      0        3 yiksanchan
      1      2        1 Bartosz Firyn
      1      1        1 Nicolas Deverge
      1      1        1 Cl?ment Grimal
      1      1        1 Oliver Wickham
```

祝 hakking!

– Akka 团队
