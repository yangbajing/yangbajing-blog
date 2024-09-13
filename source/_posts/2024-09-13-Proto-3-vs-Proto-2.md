title: proto 3 VS proto 2
date: 2024-09-13 11:26:17
category: essay
tags: [protobuf, "proto 3", "proto 2", grpc]
---

proto 3 是 proto 2 的简化版，当前两个版本均处于活跃状态。

## 常见问题

proto 3 和 proto 2 是导线（**wire**）兼容的：这意味着若在 proto 2 和 proto 3 中构造具有相同的二进制表示，那么它们可以跨版本引用符号，并生成配合良好的代码。

## 区别

### 存在性（`optional`）

- proto 2: 原生支持 `optional`。新应用中可以使用封装（wrapper）类型来表示可能缺失的值。
- proto 3: 最初不支持 `optional`，所有字段都是可选的且具有默认值。但自 2020 年起 proto 3 支持具有 `has_xxx()` 方法的 `optional` 字段。如果你的协议要求了解字段是否存在，则需要使用 `optional`。

### 默认值（`default`）

- proto 2: 字段可以有默认值，且可自定义，也可以没有。如：`optional int32 age = 1 [default = 18]`
- proto 3: 不允许自定义默认值。在 proto 3 中，所有字段在使用时的默认值都是一致的“零值”，比如：0、false、空字符串等。而数据传输时将

### 必填字段（`required`）

proto 3 删除了对 `required` 字段的支持。

### repeated 字段的 `packed` 编码

在 proto 3 中，repeated 字段默认使用 `packed` 编码，以节省空间。而 proto 2 中需要明确使用 `[packed=true]` 来指定使用 `packed` 编码。

### 枚举默认值

- proto 3: `enum` 需要一个值为 `0` 的项作为默认值。
- proto 2: 将 `enum` 声明中的第一个语法条目作为默认值，否则将不予指定。

### 未识别的枚举

在使用封闭枚举的语言中（如 Java）：

- 所有 proto 3 枚举都会生成一个 UNRECOGNIZED 条目，以容纳未知的枚举值。proto3 设置器禁止 UNRECOGNIZED 值，因此，如果枚举字段值为 UNRECOGNIZED 时，从一个 proto 到另一个 proto 的枚举字段的简单复制将崩溃。
- proto 2 枚举从不表示未知的枚举值，而是将它们放在未知字段集中。proto2 枚举可能会产生令人困惑的行为（例如，当遇到未知值时，重复字段会报告不正确的计数，并在重新序列化时重新排序）

### 枚举交叉引用

- proto 2 报文可以引用 proto 3 枚举或报文。
- 由于语义不同，proto3 报文不能引用 proto 2 枚举。

### Extension（扩展）/ Any

proto 3 删除了对 extension 的支持；取而代之的是使用 `Any` 字段来表示非类型字段。扩展机制与普通字段声明线是兼容的，而 `Any` 则不兼容，因此随着模式的演进，字段不能更改为 `Any` ，而在 proto 2 中可以更改为扩展。

Any 使用基于字符串的 `type_url` 作为键，而扩展名使用的是 `varint` 编码的字段编号，因此在 `Any` 在传输过程中会更加冗长。

急切（eagerly）或惰性（lazily）解析：

- Extension（除 `MessageSet` 之外）会被快速解析（如果您提供了自定义扩展注册表，有时会有选择性地进行解析）
- `Any` 总是惰性解析。对于某些应用程序（例如，Android 应用程序可能更喜欢在 UI 线程之外解析消息）来说，性能配置文件中的这种差异可能非常重要。

### 字符串字段验证

- proto 2 无法验证入站/出站字节是否确实为 UTF-8 编码。
- proto 3 在解析过程中和面向字节的设置器中验证所有字符串字段均已正确 UTF-8 编码。

这种验证方式意味着在 proto 3 中解析字符串字段需要更多 CPU 资源，而且在传递结构不当的字符串字段时可能会出现解析失败。反过来说，急切验证可以确保快速发现问题，并从源头加以解决。

### JSON 消息

proto 3 为所有特性定义了完善的 JSON 规范，而对于各种 proto 2 特性（如扩展）则没有规范。因此，proto2 特性的行为取决于具体实现。

### 其它

- proto 3 在 C++ 枚举中添加了 int min/max 哨兵，防止使用 -Werror,-Wswitch.
- 在 proto 3 中，optional 字段不能更改为 repeated 字段，因为这会导致旧信息被宣布无效。
- 重命名或更改 `Any` proto 中使用的任何 proto 包都是不安全的。Extension `的解析是数字式的，就像字段编号一样。Any` proto 的解析是字符串式的，就像存根方法一样。

## 总结

新项目中推荐使用 proto 3，它更简单、更强大，并且保持了一定程度的与 proto 2 兼容。

### 参考

- proto 3 语言指南: https://protobuf.dev/programming-guides/proto3
- proto 2 语言指南: https://protobuf.dev/programming-guides/proto2/
- proto3 VS proto2: https://www.hackingnote.com/en/versus/proto2-vs-proto3/index.html
