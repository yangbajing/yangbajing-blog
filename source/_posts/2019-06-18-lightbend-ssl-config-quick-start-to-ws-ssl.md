title: WS SSL快速起步 - lightbend SSL Config
date: 2019-06-18 11:36:10
category:
  - scala
  - akka
tags:
  - akka
  - ssl-config
---

*原文：[Quick Start to WS SSL](https://lightbend.github.io/ssl-config/WSQuickStart.html)*

本文适用于需要通过 HTTPS 连接到远程 Web 服务而不想阅读整个手册的用户。如果需要设置 Web 服务或配置客户端身份认证，请阅读 [Generating X.509 Certificates](https://lightbend.github.io/ssl-config/CertificateGeneration.html) 。

## 通过 HTTPS 连接到远程服务器

如果远程服务器正在使用一个由已知证书颁发机构签发的证书，那么WS应该在不进行任何额外配置的情况下即可正常工作。这里就结束了！

如果Web服务未使用众所周知的证书颁发机构，则它正在使用私有CA或自签名证书。您可以通过使用curl轻松确定这一点：

```
curl https://financialcryptography.com # uses cacert.org as a CA
```

如果收到以下错误，则必须获得CA的证书并将其添加到信任存储。

```
curl: (60) SSL certificate problem: Invalid certificate chain
More details here: http://curl.haxx.se/docs/sslcerts.html

curl performs SSL certificate verification by default, using a "bundle"
 of Certificate Authority (CA) public keys (CA certs). If the default
 bundle file isn't adequate, you can specify an alternate file
 using the --cacert option.
If this HTTPS server uses a certificate signed by a CA represented in
 the bundle, the certificate verification probably failed due to a
 problem with the certificate (it might be expired, or the name might
 not match the domain name in the URL).
If you'd like to turn off curl's verification of the certificate, use
 the -k (or --insecure) option.
```

## 获取根CA证书

理想情况下，这应该在外部完成：Web服务的所有者应该以一种不可伪造的方式，最好是亲自向您提供根CA证书。

在没有通信的情况下（不建议这样做），您有时可以使用JDK 1.8中的`keytool`直接从证书链获取根CA证书：

```
keytool -printcert -sslserver playframework.com
```

返回的 #2 部分即是根证书：

```
Certificate #2
====================================
Owner: CN=GlobalSign Root CA, OU=Root CA, O=GlobalSign nv-sa, C=BE
Issuer: CN=GlobalSign Root CA, OU=Root CA, O=GlobalSign nv-sa, C=BE
```

要获得可导出格式的证书链，请使用-rfc选项：

```
keytool -printcert -sslserver playframework.com -rfc
```

将返回一系列PEM格式的证书：

```
-----BEGIN CERTIFICATE-----
...
-----END CERTIFICATE-----
```

可以复制粘贴到文件中。链中最后一个证书将是根CA证书。

***注意***
 
*并非所有网站都包含根CA证书。您应该使用`keytool`或证书解码器对证书进行解码，以确保您拥有正确的证书。*

## 将trust manager（信任管理器）指向PEM文件

将以下内容添加到`application.conf`中，具体指定**pem**格式：

```
ssl-config {
  trustManager = {
    stores = [
      { type = "PEM", path = "/path/to/cert/globalsign.crt" }
    ]
  }
}
```

这将告诉信任管理器忽略证书的默认的**cacerts**存储，并且只使用您的自定义CA证书。

之后，将配置WS，您可以测试您的连接是否可以使用：

```
WS.url("https://example.com").get()
```

您可以在 [示例配置](https://lightbend.github.io/ssl-config/ExampleSSLConfig.html) 页面上看到更多示例。
