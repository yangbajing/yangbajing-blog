title: 使用Lightbend Config
date: 2019-08-04 20:40:30
category: work
tags:
  - config
  - 配置
---

Lightbend config 提供了一种叫 HOCON 的标记语言来进行配置管理，它是JVM上的配置管理工具，可用来替代Java自带的Properties；而相对于使用JSON来管理配置，它具有注释、变量、可组合等更丰富的特性。本文分享些日常开发工作中使用Lightbend config的经验和技巧。

*Lightbend config以前叫Typesafe config，Github地址为：[https://github.com/lightbend/config](https://github.com/lightbend/config) 。*

## HOCON 特性

1. Comments, with # or //
2. Allow omitting the {} around a root object
3. Allow = as a synonym for :
4. Allow omitting the = or : before a { so foo { a : 42 }
5. Allow omitting commas as long as there's a newline
6. Allow trailing commas after last element in objects and arrays
7. Allow unquoted strings for keys and values
8. Unquoted keys can use dot-notation for nested objects, foo.bar=42 means foo { bar : 42 }
9. Duplicate keys are allowed; later values override earlier, except for object-valued keys where the two objects are merged recursively
10. include feature merges root object in another file into current object, so foo { include "bar.json" } merges keys in bar.json into the object foo
11. include with no file extension includes any of .conf, .json, .properties
12. you can include files, URLs, or classpath resources; use include url("http://example.com") or file() or classpath() syntax to force the type, or use just include "whatever" to have the library do what you probably mean (Note: url()/file()/classpath() syntax is not supported in Play/Akka 2.0, only in later releases.)
13. substitutions foo : ${a.b} sets key foo to the same value as the b field in the a object
14. substitutions concatenate into unquoted strings, foo : the quick ${colors.fox} jumped
15. substitutions fall back to environment variables if they don't resolve in the config itself, so ${HOME} would work as you expect. Also, most configs have system properties merged in so you could use ${user.home}.
16. substitutions normally cause an error if unresolved, but there is a syntax ${?a.b} to permit them to be missing.
17. += syntax to append elements to arrays, path += "/bin"
18. multi-line strings with triple quotes as in Python or Scala

## 起步

Ligthbend config非常简单，使用之前需要引入相关依赖：

```scala
libraryDependencies += "com.typesafe" % "config" % "1.3.4"
```

或

```xml
<dependency>
    <groupId>com.typesafe</groupId>
    <artifactId>config</artifactId>
    <version>1.3.4</version>
</dependency>
```

## 技巧

### 从URL地址或注册中心获取配置

使用Java命令行配置`-Dconfig.url`就可以从网络地址上直接获得配置。比如通过如下从Nacos中获取配置可如
