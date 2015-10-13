title: REST服务下设计AccessToken
date: 2015-07-12 10:25:41
updated: 2015-07-13 17:23:43
tags:
-  rest
- playframework
- redis
- accesstoken
---
- **REST:** https://zh.wikipedia.org/zh/REST
- **Play2:** http://playframework.com/
- **Redis:** http://redis.io/
- **Access Token:** https://en.wikipedia.org/wiki/Access_token

最近要设计一套`API`以提供给接入商使用（以下简称`corp`），正好可使用`Play2`对`REST`天然良好的支持。但是在`AccessToken`的设计时费了下精力。参考了一些网上的设计，大同小异。最后参考了微信公众号的`AccesToken`设计方案，方案见：`http://mp.weixin.qq.com/wiki/11/0e4b294685f817b95cbed85ba5e82b8f.html`。

## 我的方案

- 客户端调用`/api/token`接口，传入`client_id`, `client_secret`请求参数以生成`Access Token`，调用成功将返回JSON，包含两个参数：`access_token`，`expires_in`。
- 每次HTTP请求，客户端都应在请求参数上附加`access_token`参数
- 在`Play Action`中对`access_token`进行校验

因为使用`Play2`开发`REST`服务，对于`Access Token`的校验自然就想到了使用自定义`Action`来实现。在自定义Action中，对每次请求参数中的`access_token`将进行有效性校验，校验失败会返回错误。自定义`Action`代码如下：

``` scala
case class ClientTokenRequest[A](clientToken: ClientAccessTokenInfo,
    request: Request[A]) extends WrappedRequest[A](request)

object ClientAction
  extends ActionBuilder[ClientTokenRequest]
  with ActionTransformer[Request, ClientTokenRequest] {

  override protected def transform[A](request: Request[A]): Future[ClientTokenRequest[A]] = {
    request.getQueryString("access_token") match {
      case Some(at) =>
        ApiClientTokenService().getTokenInfoByAccessToken(at) match {
          case Some(token) => Future.successful(new ClientTokenRequest(token, request))
          case None => throw FjUnauthorizedException("access token invalid")
        }
      case None =>
        throw FjUnauthorizedException("access token not exists")
    }
  }
}
```

在`ApiClientTOkenService().getTokenInfoByAccessToken()`方法中，根据在url上的`access_token`参数在`redis`中查找相应键值。代码示例如下：

``` scala
redisClients.withClient { client =>
    client.get(Commons.API_CLIENT_TOKEN_REDIS_KEY_PREFIX + accessToken).flatMap(s => s.split('\n') match {
      case Array(corpId, clientId) =>
        Some(ClientAccessTokenInfo(accessToken, corpId, clientId))
      case _ =>
        None
    })
}
```

对于`AccessToken`超期的设计，可以使用`redis`提供的`EXPIRE`功能（http://redis.io/commands/expire）。使用`scala-redis`库，它封装好了在scala下访问redis的各种API。在`set`方法中可以设置键的超时值。

``` scala
def createClientTokenInfo(corp: Corporation) = {
  val token = ApiClientTokenService.generateToken(corp)
  redisClients.withClient { client =>
    client.set(
      Commons.API_CLIENT_TOKEN_REDIS_KEY_PREFIX + token.accessToken,
      corp.id + '\n' + corp.client_id,
      false,
      Seconds(Settings.server.clientTokenTimeout.getSeconds + 10))
  }
  token
}
```

使用`redis`来保存`Access Token`有诸多好处：

- 简单、快捷，不需要自己设计超期、并发等功能。简化代码
- 无状态，便于系统横向扩展

