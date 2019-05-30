title: 转发：一次操作系统报错OutOfMemory Error的处理记录
date: 2019-05-30 10:34:52
category: work
tags:
  - outofmemory
  - vm.overcommit_memory
---

- 原文地址：https://www.jianshu.com/p/3f8692eb3660
- 原文作者：1nfinity


在启动公司内嵌的tomcat容器时出现报错, 如下：

```
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (malloc) failed to allocate 160088 bytes for AllocateHeap
# An error report file with more information is saved as:
# /users/xxx/hs_err_pidxxxx.log
```

然后查看/users/xxx/hs_err_pidxxxx.log内容：

```
#
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 357564416 bytes for committing reserved memory.
# Possible reasons:
#   The system is out of physical RAM or swap space
#   The process is running with CompressedOops enabled, and the Java Heap may be blocking the growth of the native heap
# Possible solutions:
#   Reduce memory load on the system
#   Increase physical memory or swap space
#   Check if swap backing store is full
#   Decrease Java heap size (-Xmx/-Xms)
#   Decrease number of Java threads
#   Decrease Java thread stack sizes (-Xss)
#   Set larger code cache with -XX:ReservedCodeCacheSize=
#   JVM is running with Unscaled Compressed Oops mode in which the Java heap is
#     placed in the first 4GB address space. The Java Heap base address is the
#     maximum limit for the native heap growth. Please use -XX:HeapBaseMinAddress
#     to set the Java Heap base and to place the Java Heap above 4GB virtual address.
# This output file may be truncated or incomplete.
#
#  Out of Memory Error (os_linux.cpp:2749), pid=4252, tid=0x00007f3f38bb5700
#
# JRE version:  (8.0_201-b09) (build )
# Java VM: Java HotSpot(TM) 64-Bit Server VM (25.201-b09 mixed mode linux-amd64 compressed oops)
# Core dump written. Default location: /users/ems/core or core.4252 (max size 521000 kB). To ensure a full core dump, try "ulimit -c unlimited" before starting Java again
#
```

翻译过来就是本地内存分配失败, 可能的原因有两种

1. 系统物理内存或虚拟内存不足
2. 程序在压缩指针模式下运行, Java堆会阻塞本地堆的增长

然后使用`free -m`命令查询, 发现内存足够:

那么尝试按第二个问题进行解决, 可能的方案有两种:

1. 禁止使用压缩指针模式
    - 方法: 在`catalina.sh`中`JAVA_OPTS`的值后面添加`-XX:-UseCompressedOops`, 再重启tomcat
2. 将Java堆的起始地址设置成使Java堆大小+起始地址大于4G,
    - 原因: 请参考 https://blogs.oracle.com/poonam/running-on-a-64bit-platform-and-still-running-out-of-memory,
    - 方法: 在这里我将起始地址简单直接的设为4G即4294967296

在尝试过这两种方法后发现依然报同样的错误
这时我在想会不会是堆内存过大, 导致系统无法分配内存, 于是进行尝试: 把堆内存减少一半, 看看效果.

1. 方法: 在`catalina.sh`中`JAVA_OPTS`的值中把原来的`-Xms1024m -Xmx2048m`改为`-Xms512m -Xmx1024m`, 再重启tomcat

结果JVM启动成功, 问题解决。

后续思考: 为什么在可用内存充足的情况下系统无法分配给JVM更多内存? 一直没有想到完美的解释, 如果有明白的兄弟可以指教一下.
尝试对后续思考进行解答: 原因应该还是内存不足, 可能操作系统会预留一些内存, 而我的机器上默认的启动参数是`-Xms1024m -Xmx2048m`, 
可能已经超过了系统允许分配的最高值, 因此无法分配内存. 当我使用`java -Xms10m -Xmx20m`可以启动成功, `java -Xms500m -Xmx2000m会失败, 
因此, 应该还是内存不足的问题对后续思考的最终解答及该问题的完美解决方案:

这个问题是由于`/proc/meminfo`下的`vm.overcommit_memory`被设置成不允许`overcommit`造成的

首先了解一下`overcommit`的意思: 用户进程申请的是虚拟地址, 而这个虚拟地址是不允许任意申请的, 因为虚拟内存需要物理内存做支撑, 如果分配太多虚拟内存, 会对性能参数影响. `overcommit`就是对虚拟内存的过量分配`vm.overcommit_memory`的用处: 控制过量分配的策略. 这个参数一共有3个可选值:


1. `0`: Heuristic overcommit handling. 就是由操作系统自己决定过量分配策略
2. `1`: Always overcommit. 一直允许过量分配
3. `2`: Don't overcommit. 不允许过量分配

在这个案例里面, 使用`sysctl vm.overcommit_memory`来查看, 发现`vm.overcommit_memory = 2`, 即采用的是不允许过量分配的设置. 而在错误日志中也证明了这一点：

```
CommitLimit:    15951192 kB
Committed_AS:   15837036 kB
```

解决方案是`sudo sysctl vm.overcommit_memory=0`, 即`vm.overcommit_memory = 0`, 允许系统自己决定过量分配策略（推荐编辑`/etc/sysctl.conf`文件使永久生效）。



