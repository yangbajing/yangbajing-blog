title: 译：Akka gRPC 0.8.0 Released
date: 2020-03-13 15:46:51
category:
  - scala
  - akka
tags:
  - scala
  - akka
  - akka-grpc
  - grpc
---

亲爱的 hakker 们！

我们高兴的公布 [Akka gRPC](https://doc.akka.io/docs/akka-grpc/) 0.8.0 版本！[gRPC](https://grpc.io/) 是请求/响应和流式处理（非持久化）场景的传输机制。参见 [Why gRPC？](https://doc.akka.io/docs/akka-grpc/current/whygrpc.html) （获得）何时使用 gRPC 作为传输机制的更多信息。这个版本引入了许多令人兴奋的新特性，并使我们更接近 1.0.0 （版本），我们预计在数据周内发布。

主要的改进包括：

- 基本支持 [gRPC Server Reflection](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md) [#380](https://github.com/akka/akka-grpc/issues/380)，允许交互式/动态客户端发现（gRPC）服务和方法，基于 [Cloudstate](https://github.com/cloudstateio/cloudstate/blob/master/proxy/core/src/main/scala/io/cloudstate/proxy/Reflection.scala) 的实现。
- 支持 [gRPC-Web](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md) [#695](https://github.com/akka/akka-grpc/issues/695)，生成 gRPC 可用于浏览器 JavaScript 客户端，由 [@timw](https://github.com/timw) 贡献。
- 支持自定义尾随响应头 [#838](https://github.com/akka/akka-grpc/pull/838)，例如：可用于实现 [rich error response](https://grpc.io/docs/guides/error/#richer-error-model) ，由 [@drmontgomery](https://github.com/drmontgomery) 贡献。
- 改善了在多个后端实例上通过 [Akka Discovery](https://doc.akka.io/docs/akka/current/discovery/) [#81](https://github.com/akka/akka-grpc/pull/811) 的客户端负载均衡发现（机制）。

有关所有变更的描述，请见 [release overview](https://github.com/akka/akka-grpc/releases/tag/v0.8.0)。

Credits

这个版本包含9位提交者的提交——非常感谢大家！

```
commits  added  removed
     44   3136      984 Arnout Engelen
      1   1180      353 Tim Whittington
      1    842      214 David Montgomery
      1     53       43 tayvs
      8    156       45 Enno
      2     16        8 Renato Cavalcanti
      2      3       10 Ignasi Marimon-Clos
      1     13        5 lukasito
      1      2        0 Parth
```

祝愉快！

—— Akka 团队

