title: 使用WebFlux与Reactor
date: 2019-05-07 21:48:18
category:
  - work
  - java
tags:
  - webflux
  - reactor
  - spring
  - functional
  - java
---

## 注意

### empty会造成之后的所有转换操作不执行

当`Mono<T>`的计算结果为empty时，在它之后添加的多个转换操作都不会被触发执行。有一些方法可以解决这个问题：

1. 使用`Optional<T>`来包裹可能为空的数据类型。

```
Mono<Optional<User>> findById(String userId);
```

2. 使用`.switchIfEmpty`或`.defaultIfEmpty`来将empty转换成其它有值的结果。

```
Mono<User> findById(String userId);

findById(userId)
    .switchIfEmpty(Mono.
```

