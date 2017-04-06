title: 分布式爬虫 1：使用play-ws爬取新闻
date: 2017-03-07 22:27:59
category: scala
tags:
- play-ws
- akka
- scala
---
这系列文章将使用Scala语言开发一个分布式爬虫系统，将主要基于 Akka 技术栈进行开发。既然是分布式爬虫，我们就先从单机异步爬虫开始。我们将使用到 [play-ws](https://github.com/playframework/play-ws) 和 [jsoup](https://jsoup.org/) 两个库来进行异步HTTP请求和Html网页解析。

首先我们来看 play-ws ，它是 [Playframework](http://playframework.com/) 里面的 WS 组件的可独立使用版本。从 play 2.6 版本开始，play官方开始了更模块化的组织方式，将之前很多好用、易用的功能独立出来，如：`play-json`、`play-ws` 等。