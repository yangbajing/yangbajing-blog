title: CRUDer 的自我修养：PostgreSQL
category: java
tags:
  - postgresql
  - jdbc
  - jackson
  - enum
---


## 数组

其实 JDBC 标准本身是支持数组的，而且大部分数据库也支持数组类型，但日常开发中这个特性使用得还是比较少。在业务建模中应用数组是很常见的需求，比如一篇文章的 TAG 标签列表、组织的管理员（用户ID）列表等很多对于事实表需要存储少量列表数据的情况。而对于列表数据很多的情况使用数组存储就不大合适，继续使用一张关系表是个有效的解决方案，比如组织内的成员列表，因为一个组织内的成员可能很多，可能成千上万；这么大的一个数组是不必要的，而且通常业务上也不会一次性对这么大的一个数组进行查询或处理。

在了解了数组类型的适用范围后，我们来看看 PG 里具体怎样定义和使用数组。PG 的数组类型是一个完备的类型，支持几乎所有的类型的数组，如：`int[]`、`text[]`、`numeric[]`、`timestamp[]` 等。可以看到，要声明一个列为数组类型，只需要在普通的类型后面加个中括号 `[]` 即可。而在定义数组时，通常有两种写法：

*通过 `::type[]` 从字符串强制转换*，字符串应该是一个用大括号包裹起来的数组：

```sql
insert into t_test(topic, tags, user_ids, create_time)
values ('topic-001', '{"PostgreSQL","JDBC","Mybatis","R2DBC"}'::text[], '{1,2,3}'::int[], now());
```

*通过中括号并由 `array` 修饰*：

```sql
insert into t_test(topic, tags, user_ids, create_time)
values ('topic-002', array ['Java','Scala','Javascript','SQL'], array [2,3,4], now());
```
