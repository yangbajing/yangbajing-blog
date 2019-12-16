title: 2019年12月深圳Scala Meetup预热：《Akka HTTP、gRPC与Typed Actor工程实践》
date: 2019-12-16 10:06:55
category: scala
tags:
  - akka
  - akka-http
  - akka-grpc
  - akka-typed
  - akka-cluster
  - akka-persistence
---

## 2019年12月深圳Scala Meetup

**1. 活动介绍**

好久不见，继Tubi TV赞助的两场北京Scala Meetup圆满落幕，深圳的Scala Meetup又要开幕啦！虽然连深圳都变冷了，但是Scalaer的热情丝毫不减，欢迎大家前来一起探讨Scala在生产环境中的实践和应用！

**2. 时间地点**

时间：2019年12月21日下午 13:00-18:00 （13:00开始签到）
地点：深圳市罗湖区春风路1005号联城联合大厦二楼 麦子艺术会议厅（地铁9号线文锦站B出口，或者2号线湖贝站A出口步行5分钟）

**3. 日程安排**

- 13:00-13:30：活动签到
- 13:30-13:40：主持人杜宇介绍本次主题和流程
- 13:40-14:40：《Akka HTTP、gRPC与Typed Actor工程实践》-- by 羊八井（杨景）
- 14:40-15:40：《Scala 的一些实践: Scala、Akka与Slick》-- by 刘涛
- 15:40-16:00：茶歇&互动交流
- 16:00-17:00：《Scalajs 与前端反应式编程》-- by 杜宇
- 17:00-18:00：《如何用Scala构建数据和通用服务—Scala生态系统和工程实践小结》-- by 凤凰木

**活动详细介绍和报名链接：**

- OSC活动：https://www.oschina.net/event/2313392
- 活动行：https://www.huodongxing.com/event/8521930173900

*感谢开源中国的赞助，交流会期间将抽奖送出**码云（Gitee）超大号鼠标垫**3份*

## 《Akka HTTP、gRPC与Typed Actor工程实践》

羊八井在本次活动讲述的主题为：**Akka HTTP、gRPC与Typed Actor工程实践**，主要通过一个示例来拉通介绍Akka的工程实践，示例为一个配置管理与服务发现应用（类似于阿里的Nacos）。这个实例除了标题提到的 Akka HTTP、Akka gRPC与最新的Akka Typed Actor，还有Akka Cluster与Akka Persistence的应用……也许主题名应该叫：《Akka HTTP、gRPC，Cluster，Persistence与Typed Actor工程实践》。

### 主要内容

1. **Typed Actor**：Typed Actor与经典Actor异同，生命周期、兼管、消息类型……
2. **Akka gRPC**：通过ScalaPB自动生成case class消息，Protobuf怎样声明`ActorRef[T]`类型的字段、区分声明gRPC服务使用消息与Actor使用消息，怎样运用gRPC的双向通信……
3. **Akka HTTP**：HTTP 2、怎样同时提供gRPC与REST服务……
4. **Akka Cluster**：怎样使用`ClusterSingleton`，怎样使用`DistributedPubSub`，怎样使用Protobuf作为集群数据序例化协议，怎样使用`ClusterSharding`将Actor自动分片到集群节点……
5. **Akka Persistence**：怎样应用`EventSourcedBehavior`实例Actor状态的自动持久化，使用JDBC或Cassandra做为底层持久化存储……

更多精彩内容请报名现场参与，到时大家一起讨论、交流……

### Code

内容比较多，在Meetup上很难一下子全部讲清，不过不用慌，羊八井提供了示例代码，你可以完整的运行、测试、阅读。代码仓库地址：

- Gitee：https://gitee.com/akka-fusion/fusion-discoveryx
- Github：https://github.com/akka-fusion/fusion-discoveryx
