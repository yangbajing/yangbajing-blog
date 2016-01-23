title: Cassandra日常
date: 2016-01-11 15:52:55
categories: work
tags:
- cassandra
- devops
---

## 线上情况

**重启Cassandra进程:**

```
sudo -u cassandra /opt/cassandra/bin/nodetool flush && sudo -u cassandra kill -9 `
```

