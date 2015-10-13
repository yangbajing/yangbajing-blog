title: Play2 + ReactiveMongo 实现一个活动报名应用
date: 2015-07-29 20:17:26
categories: playframework
tags:
- playframework
- reactivemongo
- 活动报名
---
- Play 2: `https://playframework.com`
- ReactiveMongo: `http://reactivemongo.org`

***代码在：*** http://git.oschina.net/socialcredits/social-credits-activity-service

<script src="https://git.oschina.net/socialcredits/social-credits-activity-service/widget_preview"></script>

<style>
.pro_name a{color: #4183c4;}
.osc_git_title{background-color: #d8e5f1;}
.osc_git_box{background-color: #fafafa;}
.osc_git_box{border-color: #ddd;}
.osc_git_info{color: #666;}
.osc_git_main a{color: #4183c4;}
</style>

公司要做一些活动，需要一个线上的活动报名应用。想着前几天刚好看了下 `ReactiveMongo` ，觉得这个小应用正好练练手。

这个活动应用的功能非常简单：用户在线填写表单，提交表单，后台存储数据并向指定的专员发送邮箱通知。

## Play 项目

整个项目目录结构如下：

``` scala
├── app
│   ├── controllers
│   │   └── inapi
│   ├── utils
│   └── views
│       └── activity
├── conf
├── data
│   └── src
│       └── main
├── platform
│   └── src
│       └── main
├── project
├── static
│   └── src
│       └── site
└── util
    └── src
        ├── main
```

`app`、`conf`都是 `Play` 的标准目录，分别放置代码文件和项目配置文件。`app.views` 包下的是Play的模板页面文件。

`static` 是用于放置前端源文件的，包括：`js`、`sass`等，使用`gulp`编译，并输入到 `public` 目录。

`platform` 目录放置一些业务代码，比如：Service。

`data` 目录是数据相关类的存放地，包括`model`、`domain`和数据库访问代码，一此数据类相关的隐式转换代码
也放置在此。

`util` 就是工具库了，包括常量定义、配置文件读取、枚举等。

## ReactiveMongo

### connection mongo collection

使用 `ReactiveMongo` 连接数据库需要先创建一个 `MongoDrvier` ，并调用 `driver.connection` 方法创建连接，进而通过 `conn.db` 方法获取一个数据库访问。

[*MyDriver.scala*](http://git.oschina.net/socialcredits/social-credits-activity-service/blob/master/data/src/main/scala/cn/socialcredits/activity/data/driver/MyDriver.scala)

``` scala
class MyDriver private() {
  val driver = new MongoDriver()

  def connection = driver.connection(List(Settings.mongo.host))

  private def db(implicit ex: ExecutionContext) = connection.db(Settings.mongo.dbName)

  def collActivity(implicit ex: ExecutionContext) = db.collection[BSONCollection]("activity")

  def collActivityRegistration(implicit ex: ExecutionContext) = db.collection[BSONCollection]("activityRegistration")
}
```

### case class 与 BSON的转换。

使用 `Macros.handler` 是最简单的实现 `case class` 与 `BSON` 转换的方法，它用到了 scala macro。代码如
：`implicit val __activityHandler = Macros.handler[Activity]`

[*BSONImplicits*](http://git.oschina.net/socialcredits/social-credits-activity-service/blob/master/data/src/main/scala/cn/socialcredits/activity/data/implicits/BSONImplicits.scala)

``` scala
implicit object LocalDateTimeHandler extends BSONHandler[BSONDateTime, LocalDateTime] {
  override def read(bson: BSONDateTime): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(bson.value), ZoneOffset.ofHours(8))

  override def write(t: LocalDateTime): BSONDateTime =
    BSONDateTime(t.toInstant(ZoneOffset.ofHours(8)).toEpochMilli)
}

implicit val __activityHandler = Macros.handler[Activity]
```

### 数据库访问

查找一个Activity使用 `find()` 方法获取一个访问数据库游标，再在游标上调用 `.one[Activity]` 方法即可获
取一个 `Activity` 对象，以 `Option[Activity]`

[*ActivityRepo*](http://git.oschina.net/socialcredits/social-credits-activity-service/blob/master/data/src/main/scala/cn/socialcredits/activity/data/repo/ActivityRepo.scala)

``` scala
def findOneById(id: BSONObjectID): Future[Option[Activity]] = {
  activityColl.find(BSONDocument("_id" -> id)).one[Activity]
}
```

## 发送邮件

邮箱发送使用了 `commons-email` ，发送邮件的代码非常简单。

[*EmailService*](http://git.oschina.net/socialcredits/social-credits-activity-service/blob/master/platform/src/main/scala/cn/socialcredits/activity/business/EmailService.scala)。

``` scala
@Singleton
class EmailService {
  private val emailSenderActor = Akka.system.actorOf(Props[EmailServiceActor], "email-sender")

  def sendEmail(id: String, subject: String, content: String): Unit = {
    emailSenderActor ! SendEmail(id, subject, content)
  }
}

class EmailServiceActor extends Actor with StrictLogging {
  override def receive: Receive = {
    case SendEmail(id, subject, content) =>
      val email = new SimpleEmail()
      email.setHostName(Settings.email.hostName)
      email.setSmtpPort(Settings.email.portSsl)
      email.setSSLOnConnect(true)
      email.setAuthenticator(new DefaultAuthenticator(Settings.email.username, Settings.email.password))
      email.setFrom(Settings.email.from)
      email.setSubject(subject)
      email.setMsg(content)
      email.addTo(Settings.email.to: _*)
      logger.info(
        s"""id: $id
            |from: ${Settings.email.from}
            |to: ${Settings.email.to}
            |$subject
            |$content""".stripMargin)
      val result = email.send()
      logger.info(
        s"""id: $id
            |[result] $result""".stripMargin)
  }
}
```

程序中使用了一个 `Actor` 来对邮件发送动作做队列化处理，感觉有点小题大作。不过 `Actor` 默认邮箱是`FIFO`的，这个特性很适合发送邮件的队列操作。

