title: 怎样在 Akka Persistence 中实现分页查询
date: 2020-01-09 20:01:38
category:
  - scala
  - akka
tags:
  - akka-persistence
  - akka
  - scala
  - pageable
---

在 Akka Persistence 中，数据都缓存在服务内存（状态），后端存储的都是一些持久化的事件日志，没法使用类似 SQL 一样的 DSL 来进行分页查询。利用 Akka Streams 和 Actor 我们可以通过编码的方式来实现分页查询的效果，而且这个分页查询还是分步式、并行的……

## EventSourcedBehavior

Akka Persistence的`EventSourcedBehavior`里实现了**CQRS**模型，通过`commandHandler`与`eventHandler`解耦了命令处理与事件处理。`commandHandler`处理传入的命令并返回一个事件，并可选择将这个事件持久化；若事件需要持久化，则事件将被传给`eventHandler`处理，`eventHandler`处理完事件后将返回一个“新的”状态（也可以不更新，直接返回原状态）。

```scala
def apply[Command, Event, State](
      persistenceId: PersistenceId,
      emptyState: State,
      commandHandler: (State, Command) => Effect[Event, State],
      eventHandler: (State, Event) => State): EventSourcedBehavior[Command, Event, State]
```

这里我们定义两个 `EventSourcedBehavior`：

- `ConfigManager`：拥有所有配置ID列表，并作为 State 保存在 EventSourcedBehavior
- `ConfigEntity`: 拥有每个配置数据，并作为 State 保存在 EventSourcedBehavior


