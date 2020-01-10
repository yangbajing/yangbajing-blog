package blog.oauth2

import fusion.json.jackson.CborSerializable

/**
 * @param access_token 访问令牌
 * @param expires_in 令牌有效期（秒）
 * @param refresh_token 刷新令牌
 */
case class AccessToken(access_token: String, expires_in: Long, refresh_token: String) extends CborSerializable
