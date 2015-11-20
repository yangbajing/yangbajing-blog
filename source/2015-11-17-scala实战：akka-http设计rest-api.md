title: Scala实战：Akka Http设计REST API
date: 2015-11-17 00:04:39
categories: scala
tags:
- scala
- akka
- mongodb
- mong-scala-driver
- akka-http
---

**使用`Akka HTTP`设计`REST API`**

本文简单介绍了`Akka HTTP`的基本概念和使用方式，并讲述了一个基于`Token`校验的公共API的设计方式与实现。

源码：[https://github.com/yangbajing/scala-applications/tree/master/akka-rest-api](https://github.com/yangbajing/scala-applications/tree/master/akka-rest-api)

## Akka HTTP

Akka HTTP基于actor和Akka stream实现了一个完整的HTTP服务端和客户端。它不是一个框架，而是提供了一个更通用和构建HTTP服务的工具包。

Akka HTTP遵循开放的设计和提供几种不同级别的API“做同样的事”。你可以选择最适合你的应用程序的抽象层。这提供了更多的灵活性，但可能需要你写更多的应用程序代码。

Akka HTTP的结构分为几个模块：

**akka-http-core**

一个完整的、低层次的，服务端和客户端HTTP实现（包括WebSocket）。

**akka-http**

更高层次的抽象，如(un)marshalling（解析与序列化），(de)compression以及用于定义HTTP API的强大DSL。

**akka-http-testkit**

难正服务端实现的测试工具和实用工具集。

**akka-http-spary-json** 

预定义的胶水代码，用于（反）序列化JSON。

**akka-http-xml**

预定义的胶水代码，用于（反）序列化XML。

## Akka HTTP Routing，优雅的定义API


## Marshall和Unmarshall，自定义JSON解析


## 异常处理


# Filter


# Token校难的设计


# 附：使用`mongo-scala-driver`暴露Mongodb API


