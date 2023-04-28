package blog.oauth2

import java.util.UUID

import helloscala.common.util.StringUtils

import scala.concurrent.duration.Duration

object OAuthUtils {
  type DueEpochMillis = Long

  // 过期时间比实际时间多5秒，保证客户端在过期时间点时刷新时新、旧 access_token 在一定时间内都有效
  val DEVIATION = 5000L

  def expiresInToEpochMillis(d: Duration): DueEpochMillis = System.currentTimeMillis() + d.toMillis + DEVIATION

  def generateToken(): String = {
    val iter = Iterator.continually(StringUtils.randomString(2))
    UUID.randomUUID().toString.split('-').zip(iter).map { case (x, y) => x + y }.mkString
  }

  def generateToken(userId: String): String = {
    userId + "-" + UUID.randomUUID().toString.replaceAll("-", "")
  }
}
