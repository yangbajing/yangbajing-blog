title: Akka Typed 常用设计模式
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

- Fire and Forget
- Request-Response
- Adapted Response
- Request-Response with ask between two actors
- Request-Response with ask from outside an Actor
- Send Future result to self
- Per session child Actor
- General purpose response aggregator
- Latency tail chopping
- Scheduling messages to self
- Responding to a sharded actor
