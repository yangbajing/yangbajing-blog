title: Akka Typed 常用交互模式
date: 2019-11-08 10:29:25
categories:
  - scala
  - akka
tags:
  - akka
  - akka-typed
  - scala
  - design-pattern
---

本文将探讨Akka Typed下actor的常用交互模式，相对经典的untyped actor，typed actor在交互与使用方式上有着显著的区别。对Akka Typed还不太了解的读者可以先参阅我的上一篇文章：[《Akka Typed新特性一览](https://www.yangbajing.me/2019/11/06/akka-typed%E6%96%B0%E7%89%B9%E6%80%A7%E4%B8%80%E8%A7%88/)。

*本文大量参译了Akka官方文档《Interation Patterns》一文（原文链接：[https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html](https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html)）。在巨人的基础之上加入了作者自身的理解和解读，同时作者还添加了一些认为有用的交互模式。希望能给读者带来 1+1>2 的感受。*

- 发送并忘记 (Fire and Forget)
- 请求/响应 (Request-Response)
- 适配响应、消息适配器 (Adapted Response)
- 两个actor之间的请求/响应 (Request-Response with ask between two actors)
- 来自actor外部的请求/响应 (Request-Response with ask from outside an Actor)
- `Future`结果发送给（actor）自己 (Send Future result to self)
- 每会话一个子actor (Per session child Actor)
- 通用响应聚合器 (General purpose response aggregator)
- 延迟尾部截断 (Latency tail chopping)
- 消息（定时）调度给（actor）自己 (Scheduling messages to self)
- 回复到分片actor (Responding to a sharded actor)

## 发送并忘记 (Fire and Forget)

使用`tell`或`!`函数向actor发送消息，并且在消息内没有可回复的actor引用字段（如：`replyTo: ActoRef[T]`）既是典型的发送并忘记模式。

#### 适用范围

1. 消息是否被处理不重要
2. 不需要确保消息被成功交付或处理
3. 吞吐量高（若发送确认回复至少需要创建两位的消息数量）

#### 问题

1. 若消息流入速度高于actor的处理能力，则很可能会将接收者的消息邮箱填满，并有可能导致JVM崩溃
2. 如果消息丢失将无法知道

## 请求/响应 (Request-Response)

actor之间的许多交互需要从接收方返回一个或多个响应消息，这可以是查询的结果、请求或处理的确认ACK或请求订阅的事件等……在Akka Typed，响应的接收者（发起请求的actor，请求方）必须被编码为消息本身的一个字段，这样接收者才能使用此字段来发送（`tell`或`!`）响应给请求方。

```scala
case class Request(query: String, replyTo: ActorRef[Response])
case class Response(result: String)

// send request message
recipient ! CookieFabric.Request("give me cookies", context.self)

  // receive & response
  Behaviors.receiveMessage[Request] {
    case Request(query, replyTo) =>
      // ... process query ...
      replyTo ! Response(s"Here are the cookies for [$query]!")
      Behaviors.same
  }
```

#### 适用范围

1. 订阅actor并希望收到被订阅actor响应的许多消息

#### 问题

1. 响应消息也许不匹配请求actor的类型限制，（参阅：**适配响应** 获取解决方案）
2. 很难检测到请求是否送达或已被处理
3. 当请求actor发起多次请求时，不能区分多个响应消息对应到哪个请求上（可在消息内加上请求id或引入新的独立接收者可解决此问题）

## 适配响应、消息适配器 (Adapted Response)

通常情况下，发送actor不支持也不意思支持接收actor的响应消息类型（不然就会退化成大部分actor都继承同一个trait，这样就失去了 **Typed** 的意义！）。这种情况下，我们需要提供正确类型的`ActorRef[T]`，并将响应消息调整为发送actor可以处理的类型。

```scala
  val backend: ActorRef[Backend.Command] = _

  trait Command
  final private case class WrappedBackendResponse(response: Backend.Response) extends Command

  Behaviors.setup[Command] { context =>
    val backendAdapter: ActorRef[Backend.Response] = 
      context.messageAdapter(resp => WrappedBackendResponse(resp))

    backend ! Backend.Register(backendAdapter)

    Behaviors.receiveMessage[Command] {
      case WrappendBackendResponse(resp) =>
        resp match {
          case Backend.Registered(...) =>
            // process response message
          case _ =>
        }
        Behaviors.same
    }
  }
```

应该为不同的消息类型注册独立的消息适配器，同一个消息类型多次注册的消息适配器只有最后一个生效。

如果响应的消息类与给定消息适配器匹配或是其消息适配器消息类型的子类型，则使用它。若有多个消息适配器符合条件，则将选用最后注册的那个。

消息适配器（context.messageAdapter返回的ActorRef[T]）的生命周期同`context`所在actor。建议在`Behaviors.step`或`AbstractBehavior`构造函数中注册适配器，但也可以在稍后注册它们。

适配器函数在actor中运行，可安全的访问其（actor）内部状态。但注意不能抛出异常，否则actor将被停止！

#### 适用范围

1. 在不同的actor消息协议间进行转换
2. 订阅响应消息的actor，并将期响应转换成发送actor可接收的类型

#### 问题

1. 难以检测消息是否送达或已被处理
2. 每个响应消息只能进行一次自适应，如果注册了新的适配器则旧的将被替换。如果不同的目标actor使用相同的响应类型，则它们自动选择哪个适配器更合适。这需要在消息中编码某种相关性来解决
3. 除非协议已经包含提供上下文的方法，例如在响应中返回发送的请求ID。否则交互就不能绑定到某个上下文中。

## 两个actor之间的请求/响应 (Request-Response with ask between two actors)

当请求与响应之间存在1:1映射时，可以通过调用`ActorContext`上的`ask`函数来于另一个actor进行交互。

1. 构造一个传出消息，它使用`context.ask[Response]`提供的`ActorRef[Response]`作为接收响应的actor放入消息中
2. 将成功/失败（Try[V]）转换为发送者actor可接收的消息类型

```scala
  val backend: ActorRef[Backend.Command] = _

  trait Command
  final private case class WrappedQueryResponse(
    reqId: String, 
    response: Try[Backend.Response], 
    replyTo: ActorRef[Response]) extends Command

  Behaviors.setup[Command] { context =>
    implicit val timeout: Timeout = 3.seconds

    Behaviors.receiveMessage[Command] {
      case Query(reqId, name, replyTo) =>
        context.ask(backend, ref => Backend.Query(name, ref)) { value =>
          WrappedBackendResponse(reqId, value, replyTo)
        }
        Behaviors.same
      case WrappendQueryResponse(reqId, value, replyTo) =>
        replyTo ! value
          .map(resp => Queried(200, reqId, Some(resp))
          .getOrElse(Queried(500, reqId))
        Behaviors.same
    }
  }
```

`context.ask`的响应转换函数参数在接收actor中运行，可以安全的访问actor内部状态，但抛出异常的话actor将会被停止。

#### 适用范围

1. 单个查询响应的转换
2. 发送actor需要在继续之前知道消息已被处理（通过`context.ask(..., ...)(mapResponse)`的`mapResponse`函数）
3. 如果请求超时，允许actor重新发送消息（通过`mapResponse`函数处理`Failure[TimeoutException]`）
4. 跟踪未完成的请求
5. 保存上下文。发送者actor接收的请求有上下文信息，如：请求ID `reqId`，而后端协议不支持这个参数时。

#### 问题

1. 一个`ask`只能有一个响应（因为`ask`会创建一个临时actor，这个actor在收到响应后就会结束自己）
2. 当请求超时时，接收actor（发回响应的那个）并不知道且仍可能将请求处理关完成，甚至若接收actor很忙的话会在请求超时发生以后再处理它
3. Finding a good value for the timeout, especially when ask triggers chained asks in the receiving actor. You want a short timeout to be responsive and answer back to the requester, but at the same time you do not want to have many false positives

## 来自actor外部的请求/响应 (Request-Response with ask from outside an Actor)

通过`ask`的另一个版本（由`AskPattern._`隐式导入）可以在actor外部（`actorRef.ask`）实现请求/响应式交互。`ask`调用将返回`Future[T]`，若在指定超时内没有响应，则以`Failure[TimeoutException]`作为结果。

```scala
import akka.actor.typed.scaladsl.AskPattern._
implicit val timeout: Timeout = 3.seconds

val result: Future[Frontend.QueryResponse] = 
  actorRef.ask(ref => Frontend.Query(reqId, name, ref)).mapTo[Frontend.QueryResponse]
```

#### 适用范围

1. 从actor系统外部访问时，如Akka HTTP请求访问actor获取响应值

#### 问题

1. 在返回的`Future`回调内很可能意外的捕获了外部状态，因为这些回调将在于`ask`运行时不同的线程上执行
2. 一个`ask`只能有一个响应（`ask`将生成临时actor）
3. 当请求超时时，接收actor并不知道且仍将继续处理请求直至完成，甚至可以超时发生后才开始处理它

## `Future`结果发送给（actor）自己 (Send Future result to self)

当在actor内部执行异步操作（返回一个`Future`时）需要小心处理，因为actor与那个异步操作不在同一个线程。`ActorContext`提供了`pipeToSelf`方法来将`Future`的结果安全传给actor自己。

```scala
case UserList(...., replyTo) =>
  val usersF = userRepository.list(....)
  context.pipeToSelf(usersF) { 
    case Success(users) => WrappedUserList(Right(users), replyTo)
    case Failure(_: TimeoutException) => WrappedUserList(Left(503), replyTo)
    case Failure(_: TimeoutException) => WrappedUserList(Left(500), replyTo)
  }
  Behaviors.same
case WrappedUserList(value, replyTo) =>
  val resp = value match {
    case Right(users) => UserListed(200, users)
    case Left(status) => UserListed(status, Nil)
  }
  replyTo ! resp
  Behaviors.same
```

#### 适用范围

1. 调用返回`Future`的外部服务时
2. 当`Future`完成，actor需要继续处理时
3. 保留原始请求的上下文，并在`Future`完成时使用它。如：`replyTo: ActorRef[_]`

#### 问题

1. 为`Future`结果添加过多的包装消息

## 每会话一个子actor (Per session child Actor)

在某些情况下，对请求的完整响应只能在从其他actor收集多个响应后再创建并发送回请求方。对于这种交互，最好将工作委托给每 **session** 子actor，还可以包含任意逻辑来实现重试、超时失败、尾部截断、进度检查等。

请注意，这基本上就是`ask`的实现方式，如果只需要一个带超时的响应，那么使用`ask`更好。

子actor是用它需要做工作的上下文创建的，包括它可以响应的`ActorRef[_]`。当完整的结果出现时，子actor会用结果进行响应并停止自身。

由于session actor的协议不是公共API，而是父actor的实现细节，因此使用显式协议并调整session actor与之交互的actor的消息可能并不总是有意义，可以让session actor接收任何消息（`Any`）。

```scala
case class Keys()
case class Wallet()

object Home {
  sealed trait Command
  case class LeaveHome(who: String, replyTo: ActorRef[ReadyToLeaveHome]) extends Command
  case class ReadyToLeaveHome(who: String, keys: Keys, wallet: Wallet)

  def apply(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      Behaviors.receiveMessage[Command] {
        case LeaveHome(who, replyTo) =>
          context.spawn(prepareToLeaveHome(who, replyTo, keyCabinet, drawer), s"leaving-$who")
          Behaviors.same
      }
    }
  }

  // Per session actor behavior
  def prepareToLeaveHome(
      whoIsLeaving: String,
      replyTo: ActorRef[ReadyToLeaveHome],
      keyCabinet: ActorRef[KeyCabinet.GetKeys],
      drawer: ActorRef[Drawer.GetWallet]): Behavior[NotUsed] = {
    Behaviors.setup[AnyRef] { context =>
      var wallet: Option[Wallet] = None
      var keys: Option[Keys] = None
      keyCabinet ! KeyCabinet.GetKeys(whoIsLeaving, context.self.narrow[Keys])
      drawer ! Drawer.GetWallet(whoIsLeaving, context.self.narrow[Wallet])

      def nextBehavior(): Behavior[AnyRef] = (keys, wallet) match {
        case (Some(w), Some(k)) =>
          replyTo ! ReadyToLeaveHome(whoIsLeaving, w, k)
          Behaviors.stopped
        case _ => Behaviors.same
      }

      Behaviors.receiveMessage {
        case w: Wallet =>
          wallet = Some(w)
          nextBehavior()
        case k: Keys =>
          keys = Some(k)
          nextBehavior()
        case _ =>
          Behaviors.unhandled
      }
    }
    .narrow[NotUsed] // 标记此actor行为不需要接受任务请求消息
  }
}
```

不需要关心actor协议，因为除了对查询的响应之外，没有任何地方会向`prepareToLeaveHome`发送消息。但在交互时会将消息限制为有限的类型。

#### 适用范围

1. 在结果生成前，一个请求会导致与其它actor（或与多个外部服务进行交互）
2. 需要处理请求确认并保证消息至少一次传递时

#### 问题

1. 子actor生命周期必须要小心管理才能避免造成资源泄漏，很容易出现未能停止子actor的情况
2. 增加了复杂性，每个子actor都可与它的父actor并发执行

## 通用响应聚合器 (General purpose response aggregator)

类似上一个 **每会话一个子actor** 模式。这种模式有很多变体，这里简单介绍其中的几种形态。

#### 适用范围

#### 问题

- 延迟尾部截断 (Latency tail chopping)
- 消息（定时）调度给（actor）自己 (Scheduling messages to self)
- 回复到分片actor (Responding to a sharded actor)

