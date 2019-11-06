title: Akka Typed特性一览
date: 2019-11-06 21:54:36
categories:
  - scala
  - akka
tags:
  - akka-typed
  - akka-grpc
  - scala
  - akka
  - akka-2.6
---

## Hello Scala!

Akka Typed Actor从2.4开始直到2.5可以商用，进而Akka 2.6已经把Akka Typed Actor做为推荐的Actor使用模式。Typed Actor与原先的Untyped Actor最大的区别Actor有类型了，其签名也改成了`akka.actor.typed.ActorRef[T]`。通过一个简单的示例来看看在Akka Typed环境下怎样使用Actor。

```scala
  sealed trait Command
  final case class Hello(message: String, replyTo: ActorRef[Reply]) extends Command
  final case class Tell(message: String) extends Command

  sealed trait Reply
  final case class HelloReply(message: String) extends Reply

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case Hello(message, replyTo) =>
        replyTo ! HelloReply(s"$message, scala!")
        Behaviors.same
      case Tell(message) =>
        context.log.debug("收到消息：{}", message)
        Behaviors.same
    }
  }
```

Akka Typed不再需要通过类的形式来实现`Actor`接口定义，而是函数的形式来定义actor。可以看到，定义的actor类型为`Behavior[T]`（**形为**），通过`Behaviors.receiveMessage[T](T => Behavior[T]): Receive[T]`函数来处理接收到的消息，而`Receive`继承了`Behavior` trait。通过函数签名可以看到，每次接收到消息并对其处理完成后，都必需要返回一个新的形为。

`apply(): Behavior[Command]`函数签名里的范性参数类型`Command`限制了这个actor将只接收`Command`或`Command`子类型的消息，编译器将在编译期对传给actor的消息做类型检查，相对于从前的untyped actor可以向actor传入任何类型的消息，这可以限制的减少程序中的bug。特别是在程序规模很大，当你定义了成百上千个消息时。

也因为有类型的actor，在Akka Typed中没有了隐式发送的`sender: ActorRef`，必需在发送的消息里面包含回复字段，就如`Hello`消息定义里的`replyTo: ActorRef[Reply]`字段一样。actor在处理完`Hello`消息后可以通过它向发送者回复处理结果。

```scala
class HelloScalaSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "HelloScala" should {
    "tell" in {
      val actorRef = spawn(HelloScala(), "hello-on-tell")
      actorRef ! Tell("Hello")
    }

    "ask" in {
      val actorRef = spawn(HelloScala(), "hello-on-ask")
      val probe = createTestProbe[Reply]()
      actorRef ! Hello("hello", probe.ref)

      probe.expectMessageType[HelloReply] should be(HelloReply("hello, scala!"))
    }

    "ask pattern" in {
      import akka.actor.typed.scaladsl.AskPattern._
      val actorRef = spawn(HelloScala(), "hello-on-ask-pattern")
      val reply = actorRef
        .ask[Reply](replyTo => Hello("Hello", replyTo))
        .mapTo[HelloReply]
        .futureValue
      reply.message should be("Hello, scala!")
    }
  }
}
```

## 更复杂的一个示例

上一个示例简单的演示了Akka Typed Actor的功能和基本使用方式，接下来看一个更复杂的示例，将展示Akka Typed更多的特性及功能。

首先是消息定义：

```scala
  sealed trait Command
  trait ControlCommand extends Command { val clientId: String }
  final case class Connect(clientId: String, replyTo: ActorRef[Reply]) extends Command with ControlCommand
  final case class Disconnect(clientId: String, replyTo: ActorRef[Reply]) extends Command with ControlCommand
  final case class AskMessage(clientId: String, message: String, replyTo: ActorRef[Reply]) extends Command
  final case class ConnectCount(replyTo: ActorRef[Reply]) extends Command
  final case class PublishEvent(clientId: String, event: String, payload: String) extends Command

  sealed trait Reply { val status: Int }
  final case class Connected(status: Int, clientId: String) extends Reply
  final case class Disconnected(status: Int, clientId: String) extends Reply
  final case class MessageAsked(status: Int, clientId: String, reply: String) extends Reply
  final case class ConnectCounted(count: Int, status: Int = IntStatus.OK) extends Reply
  final case class ReplyError(status: Int, clientId: String) extends Reply
```

上面分别定义了actor可接收的请求消息：`Command`和返回结果消息：`Reply`。建议对于需要返回值的消息使用：`replyTo`来命名收受返回值的actor字段，这里也可以不定义`Reply` trait来做为统一的返回值类型，可以直接返回结果类型，如：`ActorRef[String`。

这里将定义两个actor，一个做为父actor，一个做为子actor。父actor为：`ComplexActor`，管理连接客户端和转发消息到子actor，每次有新的客户端连接上来时做以客户端`clientId`做为名字创建一个子actor；子actor：`ComplexClient`，保持客户端连接会话，处理消息……

**ComplexActor**
```scala
final class ComplexActor private(context: ActorContext[ComplexActor.Command]) {
  import ComplexActor._
  private var connects = Map.empty[String, ActorRef[Command]]

  def receive(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case cmd @ Connect(clientId, replyTo) =>
          if (connects.contains(clientId)) {
            replyTo ! Connected(IntStatus.CONFLICT, clientId)
          } else {
            val child = context.spawn(
              Behaviors
                .supervise(ComplexClient(clientId))
                .onFailure(SupervisorStrategy.restart),
              clientId)
            context.watch(child)
            connects = connects.updated(clientId, child)
            child ! cmd
          }
          Behaviors.same
        ....
        case _ =>
          Behaviors.same
      }
      .receiveSignal {
        case (_, Terminated(child)) =>
          val clientId = child.path.name
          connects -= clientId
          context.unwatch(child)
          Behaviors.same
      }
}
```

`ComplexActor`在收到`Connect`消息后将首先判断请求客户端ID（`clientId`）是否已经连接，若重复连接将直接返回409错误（`Connected(IntStatus.CONFLICT, _)`）。若是一个新连接将调用`context.spawn`函数在创建一个字actor：`ComplexClient`。`spawn`函数签名如下：

```scala
def spawn[U](behavior: Behavior[U], name: String, props: Props = Props.empty): ActorRef[U]
```

`behavior`是要创建的actor，`name`为子actor的名字，需要保证在同一级内唯一（兄弟之间），`props`可对actor作一些自定义，如：线程执行器（`Dispatcher`）、邮箱等。

`receiveSignal`用于接收系统控制信号消息，经典actor的`preRestart`和`postStop`回调函数（将分别做为`PreRestart`和`PostStop`信号），以及`Terminated`消息都将做为信号发送到这里。

**ComplexClient**
```scala
final class ComplexClient private (
    clientId: String,
    context: ActorContext[ComplexActor.Command]) {
  import ComplexActor._

  def active(): Behavior[Command] = Behaviors.receiveMessagePartial {
    ....
    case SessionTimeout =>
      context.log.warn("Inactive timeout, stop!")
      Behaviors.stopped
  }

  def init(): Behavior[Command] = Behaviors.receiveMessage {
    case Connect(`clientId`, replyTo) =>
      replyTo ! Connected(IntStatus.OK, clientId)
      context.setReceiveTimeout(120.seconds, SessionTimeout)
      active()
    case other =>
      context.log.warn("Receive invalid command: {}", other)
      Behaviors.same
  }
```

`ComplexClient`定义了两个形为函数，`init()`和`active`。当客户端连接成功以后会返回`active()`函数作为actor新的形为来接收之后的消息。这种返回一个新的`Behavior`函数的形式替代了经典actor里的`become`、`unbecome`函数，它更直观，甚至还可以使用这种方式来实现**状态机**。

`context.setReceiveTimeout(120.seconds, SessionTimeout)`用来设置两次消息接收之间的超时时间，这里设备为120秒。可以通过方式来实现服务端会话（session）超时判断，当session超时时返回`Behaviors.stopped`消息来停止actor（自己）。这里需要注意的是`context.stop`只能用来停止直接子actor，停止actor自身返回`stopped`形为即可，这与经典actor有着明显的区别。

## 查找actor

Akka Typed取消了`actorSelection`函数，不再允许通过actor path路径来查找ActorRef。取而代之的是使用`Receptionist`机制来注册服务（actor实例）。也就是说，在Akka Typed中，actor默认情况下是不能查找的，只能通过引用（`ActorRef[T]`）来使用，要么actor之间具有父子关系，要么通过消息传递`ActorRef[T]`……

```scala
object ComplexActor {
  val serviceKey = ServiceKey[Command]("complex")

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(serviceKey, context.self)
    new ComplexActor(context).receive()
  }
}
```



# 小结

**完整示例代码**

- [https://github.com/yangbajing/scala-web-development/blob/master/book/src/test/scala/book/typed/HelloScala.scala](https://github.com/yangbajing/scala-web-development/blob/master/book/src/test/scala/book/typed/HelloScala.scala)
- [https://github.com/yangbajing/scala-web-development/blob/master/book/src/test/scala/book/typed/ComplexActor.scala](https://github.com/yangbajing/scala-web-development/blob/master/book/src/test/scala/book/typed/ComplexActor.scala)
