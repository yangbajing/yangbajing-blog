title: AkkaStream基础
tags:
---

## 核心概念

Akka Stream是一个使用有界缓冲区域来处理和转换一个序列里每个元素的库。

**Stream（流）**

涉及移动和转换数据的过程。

**Element（元素）**

无素是流的处理单元。操作所有从上游到下游的转换和传输元素。缓冲区大小。

**Back-pressure（背压）**

流量控制的一种手段，是数据消费者向生产者通报当前处理能力的一种方法，可以有效的减慢上流生产者的生产速度来匹配下游消费都的劣绅速度。在 Akka Stream 中背压通常非阻塞或异步实现的。

**Non-Blocking（非阻塞）**

非阻塞意为着某个操作将不妨碍调用线程的执行，即使这个操作需要很长时间才能完成。

**Graph（图）**

流处理的拓扑描述，定义流运行时每个元素的流动路径。

**Processing Stage（处理阶段）**

构建图形的所有构建块的通用名称。一个处理阶段的例子如：`map()`，`filter()`，可以自定义图的处理阶段 `GraphStage`，比如连接 `Merge` 或 `Broadcast`。Akka Stream 内建的处理阶段见：[stages overview](https://doc.akka.io/docs/akka/current/stream/stages-overview.html)。

## 定义和运行流

Akka Stream 使用以下抽象来表现线性的处理管道：

**Source（源）**

只有输出的处理阶段，每当下流处理阶段已经准备好处理数据时源就向下发射数据元素。

**Sink（槽）**

只有输入的处理阶段，在此请求和接受所有数据元素。槽可以减缓（控制）上游生产元素的速度。

**Flow（流程）**

同时具有输入和输出的处理阶段，通过转换流径它的数据元素来连接上游到下游。

**RunnableGraph（可运行图）**

在流程（一个或多个Flow）两边附加上源（Source）和槽（Sink），等待调用 `run()` 方法执行。

TODO

```scala
val source = Source(1 to 10)
val sink = Sink.fold[Int, Int](0)(_ + _)

// 连接 Source 到 Sink，获得一个 RunnableGraph
val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

// 从 FoldSink 实现流程并获得结果（结果是一个 Future）
val sum: Future[Int] = runnable.run()
```

TODO

```scala
val source = Source(1 to 10)
val sink = Sink.fold[Int, Int](0)(_ + _)

// 实现流程并获得槽的值
val sum: Future[Int] = source.runWith(sink)
```

***注意***

*默认情况下Akka Stream只支持下个下游阶段处理元素。若需要支持多个下游进行处理需要明确的标识 `fan-out`（支持多个下游处理阶段）。此外，Akka Stream还投笔提供了处理多播的功能。通过命名的 `fan-out` 元素来支持广播（通过信号把元素发给所有下游处理阶段）或负载平衡（通过信号把元素发给下游处理阶段的其中一个）*


