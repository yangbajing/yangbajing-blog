title: Linux下安装Postgresql-9.0.x提示：Cannot read termcap databass
date: 2011-10-12 00:10:18
categories: unix/linux
tags:
- linux
- postgresql
---
使用在postgresql.org下载的x86_64二进制版的postgres 9.0.3安装包，解压到了/opt/pgsql。使用initdb命令初始化数据库后向往常一样使用psql命令登陆数据库，提示找不到termcap等一些动态库，把/opt/pgsql/lib目录加入LD_LIBRARY_PATH环境变量就好了。

再次使用psql登陆数据库，却提示如下错误：

    [yangjing@yangxunjing ~]$ /opt/Netposa/usr/pgsql/9.0/bin/psql -p 5433 -U yangjing -d netposa  
    psql (9.0.3)  
    Type "help" for help.  
    Cannot read termcap database;  
    using dumb terminal settings.  
    Aborted  

`google`查找后说是缺少`termcap`库，但是在`/opt/pgsql/lib`目录下是有这个库的：`libtermcap.so.2`，我给它做了个软链接`libtermcap.so`后再次运行`psql`命令错误依旧。后来安装了系统自带的`compat-libtermcap-2.0.8-49.el6.x86_64`软件包后就可以正常运行`psql`命令登陆数据库了。发现`termcap`包在`/etc`目录下生成了一个`termcap`数据库文件。我把`/etc/termcap`文件备份后删除`compat-libtermcap`软件包，再把`termcap`文件拷贝回`/etc`目录再次运行`psql`命令也能正常登陆`postgresql`数据库。看来我只需要把`termcap`文件留个备份就好了，以后再次使用官方的二进包安装时将其放到`/etc`目录就行了。

（注：使用rpm包安装的不需要termcap数据文件，看了下psql的库依赖都没有使用到libtermcap.so。不知道官方的二进制包为什么需要这个库。现在大部份软件都是使用的ncurses了。）

