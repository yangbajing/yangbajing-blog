title: Scala实战：使用Akka Stream优化异步代码
date: 2019-07-17 21:17:34
categories:
  - scala
  - scala实战
tags:
  - scala
  - akka
  - akka-stream
  - reactive
---

***Scala扫盲行动***

## 背景

今天同事在开发一个消息推送功能，业务还是比较简单的：

1. 通过地区查找这个区域内的所有组织
2. 通过组织ID获取每个组织的所有用户
3. 通过用户ID获得所有绑定了设备IMEI号
4. 通过IMEI号向设备上推送消息

### 0 业务代码

```scala
  implicit val system = ActorSystem()
  implicit val mat    = ActorMaterializer()
  import system.dispatcher

  def findOrgIdsByArea(area: String): Future[List[String]] = Future {
    (0 until Random.nextInt(50)).map(_.toString).toList
  }

  def findUserIdsByOrgId(orgId: String): Future[List[String]] = Future {
    (0 until Random.nextInt(50)).map(n => s"$orgId-$n").toList
  }

  def findImeisByUserIds(userIds: Iterable[String]): Future[List[String]] = Future {
    userIds.map(id => "imei-" + id).toList
  }
```

这段代码定义了3个函数，因为是演示，实现逻辑就非常简单。分别是：

1. 通过地区名查询地区内的所有组织ID
2. 通过组织ID获取组织内所有用户的ID
3. 通过用户ID列表查询绑定的设备IMEI

### 1 使用Future

```scala
  def firstOnFuture(): Future[Unit] = {
    findOrgIdsByArea("北京")
      .flatMap { orgIds =>
        val futures = orgIds.map(id => findUserIdsByOrgId(id))
        Future.sequence(futures)
      }
      .flatMap { orgUserIdList =>
        val futures = orgUserIdList.map(userIds => findImeisByUserIds(userIds))
        Future.sequence(futures)
      }
      .map { orgImeiList =>
        orgImeiList.foreach(imeis => batchSendMessage(imeis, "推送消息"))
      }
  }
```

第一版代码使用Scala Future来实现，它可以正确的实现业务功能，但代码看起来并不优雅。且它有一些问题：

1. 若所查询地区内用户非常多，会造成`orgImeiList`这个列表对象非常大，有可能会超过内存限制
2. 若每个组织内的用户很少，但组织很多。会造成`batchSendMessage`的批量发送优化失去效果，因为极端情况下有可能1000个组织的每个组织都只有一个用户

### 2 使用Akka Stream

```scala
  def secondOnAkkaStream(): Future[Done] = {
    Source
      .fromFuture(findOrgIdsByArea("北京"))                     // (1)
      .mapConcat(identity)                                      // (2)
      .mapAsync(4)(orgId => findUserIdsByOrgId(orgId))          // (3)
      .mapAsync(4)(userIds => findImeisByUserIds(userIds))
      .mapConcat(identity)
      .grouped(1000)                                            // (4)
      .runForeach(imeis => batchSendMessage(imeis, "推送消息")) // (5)
  }
```

第二版代码使用Akka Stream来优化之前的基于Future的异步代码。

- (1) `Source.fromFuture`将一个`Future[T]`类型转换成`Source[T, Any]`类型的Akka Stream流
- (2) `.mapContact(identity)`将一个`List[T]`类型的流拉平成`T`类型的流：`Source[T, Any]`。`identity`内置函数类似：`def identity(v: T): T = v`
- (3) `.mapAsync(N)(func: T => Future[R])`将一个返回Future结果的函数集成到Akka Stream流，`mapAsync(N)`这里的参数`N`用于设置使用几个线程来并发执行Akka Stream流中的元素
- (4) 将流中的元素按每1000个进行分组
- (5) `runForeach`运行Akka Stream流并按`grouped(1000)`生成的批次进行调用

可以看到，Akka Stream的代码解决了之前使用Scala Future的所有问题：代码更优雅、不会有内存泄露、有效的利用批量发送。

### 3 优化Akka Stream代码

```scala
  def secondOnAkkaStreamThrottle(): Future[Done] = {
    import scala.concurrent.duration._
    Source
      .fromFuture(findOrgIdsByArea("北京"))
      .mapConcat(identity)
      .mapAsync(4)(orgId => findUserIdsByOrgId(orgId))
      .mapAsync(4)(userIds => findImeisByUserIds(userIds))
      .mapConcat(identity)
      .grouped(1000)
      .throttle(5, 10.seconds)
      .runForeach(imeis => batchSendMessage(imeis, "推送消息"))
  }
```

这段代码在上一个使用Akka Stream的代码之上加上了流控的功能，限制每10秒内最多5次消息推送。

#### 4 集成消息系统 Kafka

```scala
  case class SendMessageByArea(area: String, content: String)

  def secondOnAkkaStreamKafka(): Future[Done] = {
    import scala.concurrent.duration._
    val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics("message"))
      .map(record => Jackson.convertValue[SendMessageByArea](record.value()))
      .flatMapConcat { req =>
        Source
          .fromFuture(findOrgIdsByArea(req.area))
          .mapConcat(identity)
          .mapAsync(4)(orgId => findUserIdsByOrgId(orgId))
          .mapAsync(4)(userIds => findImeisByUserIds(userIds))
          .mapConcat(identity)
          .grouped(1000)
          .throttle(5, 10.seconds)
          .map(imeis => batchSendMessage(imeis, req.content))
      }
      .runWith(Sink.ignore)
  }
```

So easy! 是的，这段代码就实现了从Kafka中获取消息、分组批量推送和并发次数流控的完整的一个消息系统的功能。

## 小结

以上都是些很实用的列子，但可以明显的体现出Akka Stream相对于默认的Scala Future的解决方案的优势。

