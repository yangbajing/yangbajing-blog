title: Linux与Shell
date: 2018-05-04 23:53:15
categories: unix/linux
tags:
- linux
- shell
- vim
- bash
---

## Linux，你必需知道的

**磁盘、进程、内存**

- 保存数据的磁盘
- 实际处理数据的进程
- 存储各种运行信息的内存

### 磁盘和文件

Linux系统没有盘符的概念，它和所有类UNIX系统一样，有一个树型结构的文件系统。

```
$ tree -d -L 1 /
/
├── bin -> usr/bin
├── boot
├── dev
├── etc
├── home
├── lib -> usr/lib
├── lib64 -> usr/lib64
├── media
├── mnt
├── opt
├── proc
├── root
├── run
├── sbin -> usr/sbin
├── srv
├── sys
├── tmp
├── usr
└── var
```

#### 磁盘

`df` 查看系统已挂载文件系统情况，df只会显示已挂载的分区。

```shell
$ df -h
文件系统        容量  已用  可用 已用% 挂载点
/dev/vda1        77G  1.8G   75G    3% /
devtmpfs        2.0G     0  2.0G    0% /dev
tmpfs           2.0G  4.0K  2.0G    1% /dev/shm
tmpfs           2.0G  8.4M  2.0G    1% /run
tmpfs           2.0G     0  2.0G    0% /sys/fs/cgroup
tmpfs           396M     0  396M    0% /run/user/1000
```

*vdaX 指文件系统分区，v在这里代表KVM使用的virto虚拟文件系统，X是分区号。相应的，sata, ssd等磁盘使用s开头，传统的HHD机械硬盘使用h开头。*

`fdisk` 查看磁盘硬件情况，fdisk 并不显示系统分区，显示电脑上的所有磁盘（包括未挂载磁盘）。

```shell
$ sudo fdisk -l
[sudo] hldev 的密码：

磁盘 /dev/vda：85.9 GB, 85899345920 字节，167772160 个扇区
Units = 扇区 of 1 * 512 = 512 bytes
扇区大小(逻辑/物理)：512 字节 / 512 字节
I/O 大小(最小/最佳)：512 字节 / 512 字节
磁盘标签类型：dos
磁盘标识符：0x00097eb4

   设备 Boot      Start         End      Blocks   Id  System
/dev/vda1   *        2048   161019903    80508928   83  Linux
/dev/vda2       161019904   167772159     3376128   82  Linux swap / Solaris
```

### 进程

***控制进程就等于控制Linux***

- `fork`：通过父进程创建一个子进程，子进程是父进程自身的一个副本。
- `exec`：舍弃进程原本携带信息，在进程执行时用新的程序代码替代调用进程的内容。

#### 后台执行

在执行命令的结尾加上 `&` ，可以使程序在后台运行。如：

```shell
java -jar app.jar &
```

使用 `nohup` 命令，可使程序输出不打印到时前端（终端），默认将打印到 `nohup.out` 文件。

```shell
nohup java -jar app.jar &
```

**Ctrl+Z**

使在前台运行的进程后台运行。

**`fg`**

将后台进程拉回前台运行。

#### 快速的数据处理管道

管道蕴含着Linux从Unix中继承的一个重要概念：

**程序应该只关注一个目标，并尽可能把它做好。程序应能够互相协同工作。让程序处理文本数据流，这是一个通用的接口**

- stdin：标准输入，文件描述符 **0**
- stdout：标准输出，文件描述符 **1**
- stderr: 标准错误输出，文件描述符 **2**

![Std in, out, err](/img/linux_shell/stdin_out_err.png)

```shell
$ cat
您好， <---- 从键盘输入
您好， <---- 在屏幕上输出相同内容
海数！ <---- 从键盘输入
海数！ <---- 在屏幕上输出相同内容
          <---- 按Ctrl+D结束
```

**管道**

![pipe](/img/linux_shell/pipe.png)

*管道中常用的快捷命令*

|  命令  |  说明  |
| ------ | ----- |
| cut    | 通过分隔符拆分后，显示指定的域 |
| grep   | 显示与模式相匹配的行 |
| head   | 显示文件的开始部分 |
| paste  | 通过指定的分隔符将两个文件的各行进行合并，或者通过指定的分隔符合并一个文件中的多行 |
| sort   | 对多行进行排序 |
| tr     | 替换、删除字符，压缩文字序列 |
| uniq   | 压缩连续的相同的行 |
| wc     | 显示文件字节数、字（word）数、行数 |

```shell
$ history | grep <...>   // 从历史记录查找使用过的命令
$ ps aux | grep <...>    // 查找匹配的进程
```

**过滤并保存系统运行的所有进程号**

```shell
$ ps -ef > /tmp/tmp0
$ cat /tmp/tmp0 | tr -s " " | cut -d " " -f 2 | grep -v "PID" > /tmp/tmp1
```

*cut 命令把 -d 选项指定的文字作为文字分隔符,仅抽取 -f 选项定位的数据(域)。这个例子中,使用空格作为分隔符。但是，由于连续的空格会被当作多个分隔符,因此要事先通过 tr 命令将连续空格转换成一个空格。*

### 内存

**物理地址空间和逻辑地址空间**

物理内存分为Linux内核自身使用的区域和用户进程使用的区域。内核使用低端内存，高端内存被分配给了用户进程。低端内存中的空闲区域也将被分配给用户进程使用。

![Memory](/img/linux_shell/memory.png)

进程在访问物理内存时并不直接指定物理地址，而是指定逻辑地址。内存的内核数据区域中预先设置逻辑地址和物理地址的对应“页表”，
然后 CPU 上搭载的 MMU(Memory Management Unit)硬件会参照该页表,自动实现对映射后物理地址上的数据的访问。

为每个进程提供独立的内存空间，等于实现了进程之间的安全保护。

通过 `/proc/meminfo` 系统运行内存信息文件查看内存状态

```shell
$ cat /proc/meminfo
MemTotal:        4046524 kB
MemFree:         3717696 kB
MemAvailable:    3673100 kB
Buffers:            2076 kB
Cached:           182044 kB
SwapCached:            0 kB
Active:            89820 kB  <---- Active(anon) + Active(file)
Inactive:         153048 kB  <---- Inactive(anon) + Inactive(file)
Active(anon):      59104 kB
Inactive(anon):    46128 kB
Active(file):      30716 kB
Inactive(file):   106920 kB
......
```

通过 `free` 命令查看内存状态

```shell
$ free
              total        used        free      shared  buff/cache   available
Mem:        4046524      115940     3717364       46484      213220     3672772
Swap:       3376124           0     3376124
```
## Linux 发行版

Linux本身指 GNU/Linux 内核，但只有内核一般用户是没法使用的。所以世面上就出现了很多 Linux 发行版，它打包并整理了一系列开箱即用的工具。

### CentOS

CentOS(Community Enterprise Operating System，中文意思是:社区企业操作系统)是Linux发行版之一，它是来自于Red Hat Enterprise Linux
依照开放源代码规定释出的源代码所编译而成。由于出自同样的源代码，因此有些要求高度稳定性的服务器以CentOS替代商业版的Red Hat Enterprise Linux使用。
两者的不同，在于CentOS并不包含封闭源代码软件。

公司的服务器（测试、生产）环境使用的CentOS 7。

### Ubuntu

Ubuntu（友帮拓、优般图、乌班图）是一个以桌面应用为主的开源GNU/Linux操作系统，Ubuntu 是基于Debian GNU/Linux，由全球化的专业开发团队（Canonical Ltd）打造的。
其名称来自非洲南部祖鲁语或豪萨语的“ubuntu”一词 [2]  ，类似儒家“仁爱”的思想，意思是“人性”、“我的存在是因为大家的存在”，是非洲传统的一种价值观。

公司部分开发人员的开发电脑安装了 Ubuntu 16.04 操作系统。

### 虚拟化

Linux拥有强大的虚拟化能力。常见的有：

- KVM：完全虚拟化，OpenStack默认使用它做为系统虚拟化工具
- Xen：完全虚拟化，类似KVM的另一套虚拟化
- Cgroup：半虚拟化，Docker/k8s等容器虚拟化的核心

![Email Network virt](/img/linux_shell/email-network-virt.png)

一个邮件发送系统的虚拟网络构成。

## 开始使用Linux

### 终端

终端会话是用户与shell环境打交道的地方，如果你使用的是基于图形用户界面的系统，这指的就是终端窗口。如果没有图形用户界面(生产服务器或SSH会话),那么登录后你看到的就是shell提示符。

CentOS 7的终端提示如下：

```
[hldev@centos7-001 opt]$ cd /usr/local/share/
[hldev@centos7-001 share]$ pwd
/usr/local/share
```

**$** 表示普通用户，**#** 表示管理员用户root。root是Linux系统中权限最高的用户。 

### 环境变量

环境变量通常保存了可执行文件、库文件等的搜索路径列表。例如 `$PATH` 和 `$LD_LIBRARY_PATH`：

```
PATH=/usr/local/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/home/hldev/.local/bin:/home/hldev/bin
```

设置环境变量可以在命令行直接使用 `export` 命令：`export JAVA_HOME="/opt/local/jdk1.8`。但这样设置在系统重启后就无效了，我们可以把它写到配置文件里面。全局配置：**/etc/profile**，用户配置：**~/.bash\_prfile**。若环境变量写在配置文件内，在重启系统前默认是不会生效的，需要手动使其生效。

```
. ~/.bash_profile
```

### 安装软件

在Linux下一般有两种方式来安装软件：

1. 使用包管理器自动安装
2. 使用源码编译安装

##### RPM

CentOS 7 使用yum包管理器安装RPM格式的软件。常用命令有：

1. 列出已安装包：`rpm -l`
2. 用yum安装软件：`sudo yum install <software name>`
3. 用yum更新软件：
    - `sudo yum update <software name>`
    - `sudo yum update`（更新系统）
4. 用yum卸载软件：`sudo yum erase <software name>`

##### yum

## 工具和命令

### 命令

##### ls：查看文件

`ls` 列出目录内容。

```shell
$ ls
pgdg-centos96-9.6-3.noarch.rpm  project

$ ls -l
总用量 8
-rw-rw-r-- 1 hldev hldev 4824 9月  19 2017 pgdg-centos96-9.6-3.noarch.rpm
drwxrwxr-x 2 hldev hldev    6 2月  12 13:04 project

$ ls -alhF
 总用量 32K
 drwx------. 3 hldev hldev  173 2月  12 13:04 ./
 drwxr-xr-x. 3 root  root    19 9月  19 2017 ../
 -rw-------. 1 hldev hldev 2.9K 5月   2 10:56 .bash_history
 -rw-r--r--. 1 hldev hldev   18 8月   3 2017 .bash_logout
 -rw-r--r--. 1 hldev hldev  193 8月   3 2017 .bash_profile
 -rw-r--r--. 1 hldev hldev  231 8月   3 2017 .bashrc
 -rw-------  1 hldev hldev   46 9月  19 2017 .lesshst
 -rw-rw-r--  1 hldev hldev 4.8K 9月  19 2017 pgdg-centos96-9.6-3.noarch.rpm
 drwxrwxr-x  2 hldev hldev    6 2月  12 13:04 project/
 -rw-------  1 hldev hldev  460 9月  20 2017 .psql_history
```

- -a 列出所有文件
- -l 列出文件明细
- -h 使用 human 友好的方式显示文件内容大小
- -F 将指示符添加到显示的项后面。如：* 可执行，/ 目录，@ 符号连接，= UNIX套接字，

##### tar（解）压缩

将多个文件压缩成一个单一文件，或相反。Linux tar 命令支持多种压缩格式。常用的有：gzip和bzip2

```
tar czf dist.tar.gz dist
tar cjf dist.tar.bz2 dist
tar xcf dist.tar.gz
tar xjf dist.tar.bz2 -C /opt/haishu/var/www/auth-boot
```

- -c 压缩文件，
- -z 使用 gzip 进行（解）压缩
- -j 使用 bzip2 进行（解）压缩
- -x 解压文件
- -f 指定压缩生成/解压文件

##### cURL：数据传输命令行工具

**cURL** 是传输数据到服务器或从服务器获取数据的工具，支持多种协议。如：DICT, FILE, FTP, FTPS, GOPHER, HTTP, HTTPS, IMAP, IMAPS, LDAP, LDAPS, POP3, POP3S, RTMP, RTSP, SCP, SFTP, SMB, SMBS, SMTP, SMTPS, TELNET, TFTP。

**上传文件**

- `-XPOST`：使用HTTP POST方法
- `-F \'filename=@app.jar\'`：上传文件字段设置为filename，引用本地目录的 app.jar 文件

```
curl -XPOST -F 'filename=@app.jar'
```

**常用选项**

- `--progress`：显示进度条
- `--silent`：不显示进度信息
- `-O`：设置远程文件下载到本地系统的目标文件名
- `-C -`：支持断点续传，`-C`后面的`-`选项记cURL推断出正确的续传位置，也可以指定明确的字节偏移。

##### wget: 命令行HTTP客户端

**下载文件**

`-c`: 断点下载文件

```
wget -c https://mirrors.aliyun.com/centos/7.4.1708/isos/x86_64/CentOS-7-x86_64-DVD-1708.iso
```

**镜像网站**

```
wget -m -k -e robots=off https://www.hualongdata.com/
```

- `-e robots=off`：让wget耍流氓无视robots.txt协议

##### ps：查找进程

显示当前进程（所有或指定）的快照。

显示系统所有进程

```
ps -ef
```

显示系统所有终端所有用户进程

```
ps -aux
```

##### tail: 监控文件

实时监控 `application.log` 日志文件最新1024行内容：

```
tail -f -n 1024 application.log
```

##### top：查看系统运行状态

实时显示Linux系统运行状况

```
$ top
top - 15:47:49 up  1:03,  1 user,  load average: 0.29, 0.27, 0.42
Tasks: 315 total,   2 running, 245 sleeping,   0 stopped,   0 zombie
%Cpu(s):  0.5 us,  0.3 sy,  0.0 ni, 99.1 id,  0.0 wa,  0.1 hi,  0.0 si,  0.0 st
KiB Mem : 32445240 total, 26918760 free,  3062800 used,  2463680 buff/cache
KiB Swap: 16290812 total, 16290812 free,        0 used. 28910020 avail Mem 

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND         
 1560 yangjing  20   0  983496 121052  75104 S   1.7  0.4   1:29.94 Xorg
 1809 yangjing  20   0 4449852 426112 108836 R   1.7  1.3   5:11.92 gnome-shell
 2710 yangjing  20   0 1200352 116584  82764 S   1.0  0.4   0:14.09 tilix
 6391 yangjing  20   0 7548768 360604  77256 S   0.7  1.1   0:07.54 chrome
 6708 yangjing  20   0  162236   4452   3708 R   0.7  0.0   0:00.15 top
```

- 第一行：当前时间，系统运行时长，登录用户数，平均系统负载：1分钟、5分钟、15分钟
- 第二行：任务（进程）总数，运行数，休眠数，已停止数，僵尸进程数
- 第三行：CPU运行百分比。us: 用户空间占，用户进程空间内改变过优先级的进程占，空闲CPU占，等待输入输出的CPU，硬中断（Hardware IRQ）占，软中断（Software Interrupts）占
- 第四行：物理内存总数（KB），空闲内存，已使用内存，缓存的内存
- 第五行：交换空间总数，空闲交换内存，已使用，可使用

- PID: 进程号
- USER: 运行进程的用户
- PR: 进程优先级
- NI: nice值。负值表示高优先级，正值表示低优先级
- VIRT: 进程使用的虚拟内存总量，单位kb。VIRT=SWAP+RES
- RES: 进程使用的、未被换出的物理内存大小，单位kb。RES=CODE+DATA
- SHR: 共享内存大小，单位kb
- S: 进程状态。D=不可中断的睡眠状态 R=运行 S=睡眠 T=跟踪/停止 Z=僵尸进程
- %CPU: 上次更新到现在的CPU时间占用百分比
- %MEM: 进程使用的物理内存百分比
- TIME+: 进程使用的CPU时间总计，单位1/100秒
- COMMAND: 进程名称（命令名/命令行）

##### 用户管理

**group**

创建用户组：`groupadd -g 1100 devops`。创建一个组：devops，并指定组GID为1100

**user**

创建用户：`useradd -u 1100 -g 1100 -G wheel -k -m -d /home/devops devops`。

- `-u`: 指定用户UID
- `-g`: 指定组GID，需要存在
- `-G`: 指定附加组，需要存在
- `-k`: 骨架目录中的文件和目录将被拷贝到用户主目录
- `-m`: 若 `-d` 指定目录不存在则创建
- `-d`: 指定用户主目录

##### ssh

**ssh**

OpenSSH客户端

```
$ ssh -p 22222 hldev@centos7-001
```

**sftp**

基于SSH协议的FTP客户端，可以通过SSH登录服务器并使用FTP协议上传、下载文件

```
$ sftp hldev@centos7-001
hldev@centos7-001's password: 
Connected to centos7-001.
sftp> ls
README.md                         pgdg-centos96-9.6-3.noarch.rpm    project                           
sftp> get pgdg-centos96-9.6-3.noarch.rpm 
Fetching /home/hldev/pgdg-centos96-9.6-3.noarch.rpm to pgdg-centos96-9.6-3.noarch.rpm
/home/hldev/pgdg-centos96-9.6-3.noarch.rpm                                                            100% 4824    47.0KB/s   00:00
sftp> !ls
assets	pgdg-centos96-9.6-3.noarch.rpm	README.html  README.md
sftp> put README.html
Uploading README.html to /home/hldev/README.html
README.html                                                                                           100%   10KB  25.2MB/s   00:00 
sftp> ls
README.html                       README.md                         pgdg-centos96-9.6-3.noarch.rpm    project
```

- put: 上传本地文件到服务器
- get: 从服务器下载文件
- !command: 命令前添加!，命令将在本地执行。

`put`, `get` 命令还有些参数可以设置，常用如下：

- -P: 同步完整的文件权限的访问时间
- -r: 递归拷贝目录内的所有文件

**scp**

基于SSH协议的文件拷贝工具，可以将本地文件拷贝到远程服务器。

```
$ scp README.md hldev@centos7-001:/home/hldev/
hldev@centos7-001's password: 
README.md
```

## VIM，编辑器之神

*CentOS系统默认只安装了VI，要使用VIM需要安装：`sudo yum -y install vim`*

VIM全称是：Vi IMproved。改进的Vi，程序员的文本编辑器。VIM被称为编辑器之神。使用 VIM 非常简单，在终端输入 `vim` 即可。VIM支持全键盘操作，
不需要使用鼠标即可操作，可显著提供工作效率。

同时也可以指定要打开的文件。打开 vim 时可以使用参数，比如：`-R` 只读模式。在使用VIM查看比较大的日志文件等时候可以防止误操作修改文件。

VIM运行时有三种模式：

- Normal模式：在Normal模式下，用户可以移动光标、操纵文字、输入各种控制操作
- Edit模式：顾名思义，编辑模式下可以输入内容。
- Command模式（指令模式）：在Normal模式下，按 `shift+;`（输入英文冒号）则可进入指令模式。当编辑器下部出现闪烁的英文冒号时既进入指令模式。

使用VIM打开本文 `vim README.md`，默认处于 Normal 模式。

![VIM open file](/img/linux_shell/vim-open-file.png)

### VIM 基本使用

这时，可以使用键盘上的 H、J、K、L 4个键移动光标，分别为：向左、上、下、右移动。当光标移动到期望的位置后可以有多种方法进入Edit（编辑）模式。
`i`：在光标指定字符前进入编辑模式，`a`：在光标指定字符后进入编辑模式，`s`：替换光标指定字符并进入编辑模式。

退出Edit模式有两种方法：

1. 按 `Esc` 键退出Edit模式并进入Normal模式
2. 按 `ctrl+[` 组合键

### VIM技巧

##### 保存和退出（Command模式）

输入 `w` 将保存当前编辑内容，若是打开的新文件可以在 `w` 后输入需要保存的文件名。

输入 `q` 将退出VIM，退出前必需保存已编辑内容。若想放弃当前编辑内容并退出，输入 `q!` 。

`w`, `q` 可以组合输入。比如保存并退出：`wq`。

##### 快速移动（Normal模式）

**翻页**

`ctrl-f` 向前翻一页，`ctrl-b` 向后翻一页（向前翻页时VIM将保留之前页的最后两行）。

**快速定位到文本开头或结尾**

连按两次 `g` 键将光标移动到整个文本开头，`shift+g`将移动光标到整个文本结尾。

##### 复制、粘贴行（Normal模式）

连续按两次 `y` 键复制单行，在需要粘贴的地方使用 `p` 键进行粘贴（将粘贴到光标所在行之下）。

**复制多行**

按 `shift+v` 组合键可以高亮一行，这时可以使用 J、K 进行上下移动，选中需要的多行后再按 `y` 键即可复制多行。

##### 撤销、恢复编辑（Normal模式）

在Normal模式下按 `u` 键将撤销最后一次编辑（每次保存文件算一次编辑）。按 `ctrl+r` 将恢复撤销。撤销和恢复可进行多次。

##### 自动补全（Edit模式）

在需要实例的文字后按 `ctrl+c ctrl+p` 会弹出自动补全选择框，这里可以使用 `ctrl+n` 或 `ctrl+p` 进行向下、向下移动，选中需要实例的单词后按 `Enter` 键即可。

##### 将本地路径录入文本（Edit模式）

使用 `ctrl+c ctrl+f` 将弹出本地路径选择框（相对当前编辑文本路径），移动光标选中需要的路径，按 `Enter` 键后录入到文本中。

### VIM配置

可以自定义VIM配置，VIM的配置文件为：`.vimrc`，一般在用户主目录下。编辑配置文件：

```
$ vim ~/.vimrc
```

输入以下内容打开高亮和自动缩进。

```
syntax on
set ai
```

## Shell

Shell是用户使用Linux的桥梁，它既是一种命令语言，又是一种程序。在Linux下，一般默认使用的是 [Bash](https://en.wikipedia.org/wiki/Bash_(Unix_shell))。以下内容非特殊说明，默认使用的Bash。

### Bash脚本

可以将一系列命令放到一个脚本文件中自动执行，一般这类脚本文件都以 `.sh` 结尾。

```
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
```

上面是一个普通的shell脚本，使用Bash执行。


