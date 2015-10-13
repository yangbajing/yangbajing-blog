title: 解决Eclipse Access restriction：问题
date: 2010-10-29 18:50:18
categories: java
tags:
- java
- eclipse
---
今天在Linux下用eclipse 3.5开发Jpcap相关的程序，先试试官方的UdpSend.java能否跑起来。結果eclipse给出了
如下错误提示：

```
    Access restriction: The type JpcapCaptor is not accessible due to restriction on required library /media/sda7/opt/jdk1.6.0_16/jre/lib/
 ext/jpcap.jar
```

其实要解决它也很容易，在Window - Java - Compiler - Errors/Warnings界面的Deprecated and restricted API下。把Forbidden reference (access rules): 的规则由默认的Error改为Warning即可。
