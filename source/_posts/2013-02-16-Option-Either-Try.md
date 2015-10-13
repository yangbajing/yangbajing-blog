title: Option，Either和Try
date: 2013-02-16 00:49:00
categories: scala
tags:
- scala
- option
- either
- try
---

***scala数据交互***

本文介绍在Scala 2.10中怎样使用一种函数式的方式来处理数据交互，包括入参及返回值。

- Option: 解决null（空指针）问题
- Either: 解决返回值不确定（返回两个值的其中一个）问题
- Try: 解决函数可能会抛出异常问题


## Option

API：[http://www.scala-lang.org/api/2.10.0/index.html#scala.Option](http://www.scala-lang.org/api/2.10.0/index.html#scala.Option)

在Java中，当一个操作返回无效值或数据不存在时通常情况下返回返回一个 `null` 。比如 `servlet` 中
`request.getParameter("id")` ，或id不存在将返回 `null` 。`Map<String, String>` 也是，
`data.get("dd")` 当Key：dd不存在时也将返回 `null` 。一句话，在Java的世界，你需要随时小心
`NullPointerException` ！

现在，我们再来看看Scala中的解决方案：Option。Option实际上有3个类型：Option、Some和None，
Some和None都是Option的子类型，Some和None。Option表示可选的值，它的返回类型是 `scala.Some`
或 `scala.None` 。Some代表返回有效数据，None代表返回空值。最常用的使用方式是把scala.Option
当作集合或单子（monad）使用，可以调用它的map、flatMap、filter或foreach方法。这里我们来看看一
些例子：

    scala> val name: Option[String] = Some(" name  ")
    name: Option[String] = Some( name  )
    
    scala> val upper = name map { _.trim } filter { _.length != 0 } map { _.toUpperCase }
    upper: Option[String] = Some(NAME)
    
    scala> println(upper getOrElse "-")
    NAME
    
    scala> upper.get
    res1: String = NAME

从这个简单的例子可以知道，Option作为变量的类型，而它实际拥有的类型为Some或None。在这里把
`Some(" name  ")` 换成 `None` 再试试：

    scala> val name: Option[String] = None
    name: Option[String] = None
    
    scala> val upper = name map { _.trim } filter { _.length != 0 } map { _.toUpperCase }
    upper: Option[String] = None
    
    scala> println(upper getOrElse "-")
    -
    
    scala> upper.get
    java.util.NoSuchElementException: None.get
    	at scala.None$.get(Option.scala:313)
    	at scala.None$.get(Option.scala:311)

我们可以使用 `getOrElse` 方法来获取实际的数据，这个方法包含一个参数，当Option为None时将返回传
入的参数值。而对于Some，可以直接使用 `get` 方法获取实际的数据值; None是不可以的。

这里，我们在为Option赋值时显示的使用了Some或None类型，当使用Some时需求我们保证数据有效（不可为
null）。其实我们可以使用Option对象来进行赋值：

    scala> val x: Option[String] = Option("name")
    x: Option[String] = Some(name)
    
    scala> val y: Option[String] = Option(null)
    y: Option[String] = None
    
    scala> val x = Option("name")
    x: Option[String] = Some(name)
    
    scala> val a = Option(null)
    a: Option[Null] = None

使用Option的妙处在，使用Servlet时可以如下：

    val username = Option(request getParameter "username")
    val password = Option(request getParameter "password")
    
    val login_? = 
      for (
        un <- username;
        pwd <- password;
        isLogin <- login(un, pwd)) yield isLogin  // login方法登陆验证成功返回true
    
    login_? match {
      case Some(_) =>
        // 登陆成功
        // redirect to ...
      case None =>
        // 登陆失败
        // 返回错误消息
    }

这里，`Option.apply` 方法在 `request.getParameter` 返回null时将为 `username` 赋值为 
None。因为Option实现了 `map, filter, flatMap, foeach及toList` 方法，我们可以在for静达式
中使用它。

## Either

API：[http://www.scala-lang.org/api/2.10.0/index.html#scala.util.Either](http://www.scala-lang.org/api/2.10.0/index.html#scala.util.Either)

程序设计中经常会有这样的需求，一个函数（或方法）在传入不同参数时会返回不同的值。返回值是两个不相关
的类型，分别为： `Left` 和 `Right` 。惯例中我们一般认为 `Left` 包含错误或无效值， `Right` 
包含正确或有效值（这个和从小尝到的左、右派分子定义相反啊！）

    def readfile(): Either[IOException, String] = try {
      Right("羊八井好帅 ^_^！")
    } catch {
      case e: IOException => Left(e)
    }
    
    println(readfile match {
      case Right(msg) => msg
      case Left(e) => e.getMessage
    })

除了使用match case方式来获取数据，我们还可以分别使用 `.right.get` 和 `.left.get` 方法，当
然你需要使用 `.isRight` 或 `.isLeft` 先判断一下。Left或Right类型也有 `filter, flatMap,
foreach, get, getOrElse, map` 方法，它们还有 `toOption, toSeq` 方法，分别返回一个 `Option` 
或 `Seq` 。


## Try

API：[http://www.scala-lang.org/api/2.10.0/index.html#scala.util.Try](http://www.scala-lang.org/api/2.10.0/index.html#scala.util.Try)

在刚才的 `readfile` 方法里，我们显示使用了 try catch 语法。Scala 2.10提供了 `Try` 来更优
雅的实现这一功能。对于有可能抛出异常的操作。我们可以使用Try来包裹它。

    scala> val z: Try[Int] = Try{ 27 }
    z: scala.util.Try[Int] = Success(27)
    
    scala> val y: Try[Int] = Try{ throw new NullPointerException("null ...") }
    y: scala.util.Try[Int] = Failure(java.lang.NullPointerException: null ...)
    
    scala> val x: Try[Int] = Success{ 27 }
    x: scala.util.Try[Int] = Success(27)





