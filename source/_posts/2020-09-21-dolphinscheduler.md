title: DolphinScheduler
date: 2020-09-21 16:17:04
category: work
tags:
  - scheduler
---

## 系统配置

操作系统环境

```
systemctl stop firewalld
systemctl disable firewalld

# Install softwares
yum -y install epel-release
yum -y install java-11-openjdk-devel tree htop vim sshpass wget curl

# Add user
useradd dolphinscheduler
echo dolphinscheduler | passwd --stdin dolphinscheduler
echo 'dolphinscheduler  ALL=(ALL)  NOPASSWD: NOPASSWD: ALL' >> /etc/sudoers
sed -i 's/Defaults    requirett/#Defaults    requirett/g' /etc/sudoers

# Create directory
mkdir /opt/dolphinscheduler
chown -R dolphinscheduler:dolphinscheduler /opt/dolphinscheduler

```

## 免密登录

**`su dolphinscheduler`**

```
ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

for ip in ds2 ds3;     #请将此处ds2 ds3替换为自己要部署的机器的hostname
do
    ssh-copy-id  $ip   #该操作执行过程中需要手动输入dolphinscheduler用户的密码
done
```

## 安装 PostgreSQL

```
sudo yum -y install https://yum.postgresql.org/12/redhat/rhel-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
sudo yum -y install postgresql12 postgresql12-server
sudo /usr/pgsql-12/bin/postgresql-12-setup initdb
sudo systemctl enable postgresql-12
sudo systemctl start postgresql-12
```

```
sudo -u postgres psql -c "create user dolphinscheduler nosuperuser replication encrypted password 'dolphinscheduler';"
sudo -u postgres psql -c "create database dolphinscheduler owner=dolphinscheduler;"
```
