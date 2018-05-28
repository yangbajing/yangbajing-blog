#!/usr/bin/env bash

# $1, $2, $x, .... 代表输入的第一、第二、第三个命令行参数
echo $1 $2

# $@ 代表所有命令行参数
echo $@

if [ ! $JAVA_OPTS ]; then
  JAVA_OPTS=' -Xmx1G -Xms1G '
fi

# 执行Java程序
java $JAVA_OPTS -jar application.jar $@
