title: Alpakka Kafka，反应式Kafka客户端
date: 2019-02-23 20:42:46
category:
  - scala
  - akka
tags:
  - akka
  - alpakka
  - kafka
  - akka-stream
  - alpakka-kafka
---

[Alpakka Kafka](https://doc.akka.io/docs/akka-stream-kafka/current/) 是一个要用于 Java 和 Scala 语言的开源的流感知和反应式集成数据线项目。它建立在 [Akka Stream](https://doc.akka.io/docs/akka/current/stream/index.html)之上，提供了 DSL 来支持反应式和流式编程，内置回压功能。Akka Streams 是 [Reactive Streams](https://www.reactive-streams.org/) 和JDK 9+ [java.util.concurrent.Flow](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Flow.html) 的兼容实现，可无缝地与其进行互操作。


要使用 Alpakka Kafka，需要在你的项目添加如下依赖：
```scala
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-RC2"
```

*当前支持 `kafka-clients` 2.1.x 和 `Akka Streams` 2.5.21。*

## 快速开始

对Akka Streams或Kafka不熟的，可先查阅两者的官方文档：

- [https://akka.io/](https://akka.io/)
- [https://kafka.apache.org/](https://kafka.apache.org/)

Alpakka Kafka 写的代码非常精致且简洁，也许你会一眼爱上它的美。

```scala
object KafkaGetting extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  import system.dispatcher
  val config = system.settings.config

  val producerSettings = 
    ProducerSettings(config.getConfig("akka.kafka.producer"),
      new StringSerializer, new StringSerializer)

  val consumerSettings = 
    ConsumerSettings(config.getConfig("akka.kafka.consumer"), 
      new StringDeserializer, new StringDeserializer)

  val producerQueue = Source
    .queue[String](128, OverflowStrategy.fail)
    .map(str => new ProducerRecord[String, String]("test", str))
    .toMat(Producer.plainSink(producerSettings))(Keep.left)
    .run()

  val consumerControl = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .map(record => record.value())
    .toMat(Sink.foreach(value => println(value)))(Keep.left)
    .run()

  Source(1 to 10)
    .map(_.toString)
    .throttle(1, 2.seconds)
    .runForeach(message => producerQueue.offer(message))
    .onComplete(tryValue => println(s"producer send over, return $tryValue"))

  println("Press 'enter' key exit.")
  StdIn.readLine()
  producerQueue.complete()
  consumerControl.shutdown()
  system.terminate()
  Await.result(system.whenTerminated, 10.seconds)
}
```

上面的代码实现了一个完整的Kafka生产者、消费者数据处理流程，整个处理都是异步、非阻塞的。没有显示线程创建、没有类似 `where(true)` 这样的消费处理循环……接下来，让我们分析下以上代码。

## 代码分析

### producerSettings

Alpakka Kafka 使用`ProducerSettings`来封装创建Kafka生产者时需要的参数，它使用了 [Typesafe Config](https://github.com/lightbend/config#using-hocon-the-json-superset) 通过可配置的方式来构建生产者。

producerSettings 使用 `"akka.kafka.producer"` 部分的参数来构造 Kafka 生产者，以下是一个示例的 Typesafe Config 配置：

```
akka.kafka.producer {
  # 同时可运行的send操作数量
  parallelism = 100

  # 调用 `KafkaProducer.close` 时等待关闭的时间
  close-timeout = 60s
  
  # 线程池
  use-dispatcher = "akka.kafka.default-dispatcher"

  # 定义 org.apache.kafka.clients.producer.ProducerConfig 属性需要的参数
  kafka-clients {
    # 使用英文逗号分隔多个Kafka服务地址
    bootstrap.servers = "localhost:9092"
  }
}
```

**consumerSettings**

consumerSettings 使用 `"akka.kafka.consumer"` 部分的参数来构造 Kafka 消费者，以下是一个示例的 Typesafe Config 配置：

```
akka.kafka.consumer {
  # 拉取数据间隔周期
  poll-interval = 50ms
  
  # 拉取数据超时时间
  poll-timeout = 30s

  # 调用 `KafkaConsumer.close` 时等待关闭的时间
  close-timeout = 20s
  
  # 线程池
  use-dispatcher = "akka.kafka.default-dispatcher"

  # 定义 org.apache.kafka.clients.producer.ProducerConfig 属性需要的参数
  kafka-clients {
    # 使用英文逗号分隔多个Kafka服务地址
    bootstrap.servers = "localhost:9092"

    # 自动commit消息
    enable.auto.commit = true

    # 消费者组ID
    group.id = "resource-dev"

    # 从最新的offset开始读取消息，否则从头开始读取
    auto.offset.reset = "earliest"
  }
}
```

**producerQueue**

使用Akka Streams构造一个生产者队列 `producerQueue`，再由 `Producer.plainSink` 来消费发送到 `producerQueue` 里的消息。需要注意的是构造 `Source.queue[String]` 时设置的 128 这个参数并不是 Kafka 的消息队列容量，而是 Akka Streams Source 构造出来的一个Queue。`Producer.plainSink` 是一个 **下游** ，它消费来自  `producerQueue` 这个上游的消息，再将数据发送到 Kafka 服务。

**consumerControl**

通过 `Consumer` 这个Akka Streams Source构造了一个Kafka消费者，并监听指定的 "test" 主题。`consumerControl` 流首先从收到的每个消息（`ConsumerRecord`）中取得 value，并发送到下游，下游通过 `Sink.foreach` 接收数据并打印到终端。

**Source(1 to 10)**

生成从1到10的字符串消息值，并每隔2秒通过 `producerQueue` 发送一个消息到Kafka。

## 小结

本文通过一个简单的示例展现怎样通过 Alpakka Kafka 来实现对 Kafka 的集成，完成的代码示例见：[https://github.com/ihongka/akka-fusion/blob/master/fusion-kafka/src/test/scala/fusion/kafka/getting/KafkaGetting.scala](https://github.com/ihongka/akka-fusion/blob/master/fusion-kafka/src/test/scala/fusion/kafka/getting/KafkaGetting.scala) 。

Kafka发展到现在，已不单单再是一个消息系统了，在MQ之外，它还提供了KSQL和Connector特性。应用基于 Kafka 可以有更多的设计和实现，而Akka Stream + Kafka是一个强大的组合，接下来我会写一系列文章介绍怎样使用 Alpakka Kafka 来基于 Kafka 进行应用和架构设计。

