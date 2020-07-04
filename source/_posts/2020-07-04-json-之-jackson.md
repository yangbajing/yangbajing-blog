title: JSON 之 Jackson
date: 2020-07-04 12:59:19
category:
  - java
tags:
  - json
  - jackson
  - java
  - scala
  - jackson-module-scala
  - spring
  - spring-boot
---

**Jackson** 是 Java 生态下的一款 JSON （返）序列化工具，具有高效、强大、安全（*没有 **Fastjson** 那么多的安全漏洞*）等特性。同时应用广泛，Spring Boot/Cloud、Akka、Spark 等众多框架都将其作为默认 JSON 处理工具。

## 依赖

要使用 Jackson，需要在项目中添加如下依赖（注：使用 Spring Boot 时不需要手动添加，Spring 框架已经默认包含）：

**Maven**
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.11.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
    <version>2.11.1</version>
</dependency>
```

**Sbt**
```sbt
libraryDependencies ++= Seq(
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.11.1",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.11.1"
)
```

- jsr310：Java 8 新加日期、时间类型支持（`java.time.*`）支持
- jdk8：Java 8 新加数据类型支持（`Optional`等）

## 简明使用

### 获取 Jackson

Jackson 在使用之前需要实例化一个 `ObjectMapper` 对像（它不直接提供全局的默认静态方法）。通常我们会将 `objectMapper` 定义成一个静态成员，或通过 DI 框架注入使用。

**Java**
```java
public static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
```

**Spring**
```java
@Autowired
private ObjectMapper objectMapper;
```

*注：Spring 默认不会自动加载 classpath 路径的所有 Jackson Module。需要在 `objectMapper` 上调用 `registerModule` 方法手动注册。*

**Scala**
```scala
val objectMapper = new ObjectMapper().findAndRegisterModules()
```

### 建议的 Jackson 配置

**编码配置**

```java
    ObjectMapper objectMapper = new ObjectMapper()
            // 自动加载 classpath 中所有 Jackson Module
            .findAndRegisterModules()
            // 时区序列化为 +08:00 形式
            .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false)
            // 日期、时间序列化为字符串
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // 持续时间序列化为字符串
            .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            // 当出现 Java 类中未知的属性时不报错，而是忽略此 JSON 字段
            .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
            // 枚举类型调用 `toString` 方法进行序列化
            .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
            // 设置 java.util.Date 类型序列化格式
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
            // 设置 Jackson 使用的时区
            .setTimeZone(SimpleTimeZone.getTimeZone("GMT+8"));
```

### 创建 JSON 对像

Jackson 的 `ArrayNode` 和 `ObjectNode` 对象均不能直接创建，需要通过 `objectMapper` 来创建。同时，两个 **Node** 对像都 `JsonNode` 的子类。

```
ArrayNode jsonArray = objectMapper.createArrayNode();
jsonArray.add("Jackson").add("JSON");
ObjectNode jsonObject = objectMapper.createObjectNode()
        .put("title", "Json 之 Jackson")
        .put("readCount", 1024);
```

### 反序列化

```java
String jsonText = ....;

// json text -> Jackson json node
JsonNode javaTimeNode = objectMapper.readTree(jsonText);

// json text -> java class
JavaTime javaTime1 = objectMapper.readValue(jsonText, JavaTime.class);

// Jackson json node -> java class
JavaTime javaTime2 = objectMapper.treeToValue(javaTimeNode, JavaTime.class);
```

### 序列化

```java
ZonedDateTime zdt = ZonedDateTime.parse("2020-07-02T14:31:28.822+08:00[Asia/Shanghai]");
JavaTime javaTime = new JavaTime()
                .setLocalDateTime(zdt.toLocalDateTime())
                .setZonedDateTime(zdt)
                .setOffsetDateTime(zdt.toOffsetDateTime())
                .setLocalDate(zdt.toLocalDate())
                .setLocalTime(zdt.toLocalTime())
                .setDuration(Duration.parse("P1DT1H1M1.1S"))
                .setDate(Date.from(zdt.toInstant()))
                .setTimestamp(Timestamp.from(zdt.toInstant()));
out.println(objectMapper.writeValueAsString(javaTime));
out.println(objectMapper.writeValueAsString(jsonObject));
out.println(objectMapper.writeValueAsString(jsonArray));
```

输出：
```
{"localDateTime":"2020-07-02T14:31:28.822","zonedDateTime":"2020-07-02T14:31:28.822+08:00","offsetDateTime":"2020-07-02T14:31:28.822+08:00","localDate":"2020-07-02","localTime":"14:31:28.822","duration":"PT25H1M1.1S","date":"2020-07-02 14:31:28","timestamp":"2020-07-02 14:31:28"}
{"title":"Json 之 Jackson","readCount":1024}
["Jackson","JSON"]
```

**Java 类转换成 Jackson JasonNode**
```java
JsonNode jsonNode = objectMapper.valueToTree(javaTime);
```

**美化输出**
```java
objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(javaTime)
```

```
{
  "localDateTime" : "2020-07-02T14:31:28.822",
  "zonedDateTime" : "2020-07-02T14:31:28.822+08:00",
  "offsetDateTime" : "2020-07-02T14:31:28.822+08:00",
  "localDate" : "2020-07-02",
  "localTime" : "14:31:28.822",
  "duration" : "PT25H1M1.1S",
  "date" : "2020-07-02 14:31:28",
  "timestamp" : "2020-07-02 14:31:28"
}
```

### 序列化时忽略某些字段

**@JsonIgnore**

```java
@JsonIgnore
private Long _version;
```

`@JsonIgnore` 注解也可以添加在 **getter** 函数上。

**JsonIgnoreProperties**

```java
@JsonIgnoreProperties({"_version", "timestamp"})
public class JavaTime {
}
```

### 自定义序列化、反序列化器

对于某些自定义类型或 Jackson 不支持的类型，可以实现自己的序列化、反序列化器。

**POJO**

在类型上使用 `@JsonSerialize` 和 `@JsonDeserialize` 注解来分别指定序列化和反序列化器。

```java
@Data
public class User {
    private String id;

    @JsonSerialize(using = PgJsonSerializer.class)
    @JsonDeserialize(using = PgJsonDeserializer.class)
    private io.r2dbc.postgresql.codec.Json metadata;
}
```

先将 R2DBC PostgreSQL 的 JSON 转化为 JsonNode 对象，再调用 `gen.writeTree` 对其进行序列化。这样才能保证序列化出来的字段值是一个 JSON 对象或 JSON 数组。

```java
import io.r2dbc.postgresql.codec.Json;

public class PgJsonSerializer extends StdSerializer<Json> {
    public PgJsonSerializer() {
        super(Json.class);
    }

    @Override
    public void serialize(Json value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        JsonParser parser = gen.getCodec().getFactory().createParser(value.asArray());
        JsonNode node = gen.getCodec().readTree(parser);
        gen.writeTree(node);
    }
}
```

通过 `ObjectCodec#readTree` 将 `JsonParse` 读取为一个 `TreeNode` 对像，再将其序列化为 **JSON 格式**（JSON 字符串的字符数组形式）后传给 `Json.of` 函数。

```java
import io.r2dbc.postgresql.codec.Json;

public class PgJsonDeserializer extends StdDeserializer<Json> {
    public PgJsonDeserializer() {
        super(Json.class);
    }

    @Override
    public Json deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        TreeNode node = p.getCodec().readTree(p);
        ObjectMapper objectMapper = (ObjectMapper) p.getCodec();
        byte[] value = objectMapper.writeValueAsBytes(node);
        return Json.of(value);
    }
}
```

### Jackson Module

当自定义（反）序列化变多时，在每个类上通过注解手工指定（反）序列化器就变得很繁琐。我们可以通过定义一个 Jackson Module 并使用 `.findAndRegisterModules()` 通过 Java 的 Service 机制自动注册到 `ObjectMapper`。

**1.** 定义 **Serializers** 和 **Deserializers**

```java
public class ExampleSerializers extends Serializers.Base implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  @Override
  public JsonSerializer<?> findSerializer(
      SerializationConfig config, JavaType type, BeanDescription beanDesc) {
    final Class<?> raw = type.getRawClass();
    if (Json.class.isAssignableFrom(raw)) {
      return new PgJsonSerializer();
    }
    return super.findSerializer(config, type, beanDesc);
  }
}

public class ExampleDeserializers extends Deserializers.Base implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  @Override
  public JsonDeserializer<?> findBeanDeserializer(
      JavaType type, DeserializationConfig config, BeanDescription beanDesc)
      throws JsonMappingException {
    if (type.hasRawClass(Optional.class)) {
      return new PgJsonDeserializer();
    }

    return super.findBeanDeserializer(type, config, beanDesc);
  }
}
```

**2.** 实现 **Module**

```java
public class ExampleModule extends com.fasterxml.jackson.databind.Module {
    @Override
    public void setupModule(SetupContext context) {
        context.addSerializers(new ExampleSerializers());
        context.addDeserializers(new ExampleDeserializers());
    }

    @Override
    public String getModuleName() {
        return "ExampleModule";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }
}
```

**3.** 定义 `META-INF.services` 文件（可选）

通过 Java ServiceLoader 机制，Jackson 可以自动注册配置的 **Module**。在 `com.fasterxml.jackson.databind.Module` 配置文件里指定需要自动注册 Module 的全路径，多个 Module 可以写在多行。注意：**services** 配置文件必需为 `com.fasterxml.jackson.databind.Module`。

```
src/main/resources/
├── META-INF
│   └── services
│       └── com.fasterxml.jackson.databind.Module
```

若没有配置 **services** 文件，则在调用 `objectMapper.findAndRegisterModules()` 时不能自动加载，需要通过 `objectMapper.registerModule` 方法手动注册，如：

```java
objectMapper.registerModule(new ExampleModule());
```

## Spring

**Spring Boot 配置文件**

```yml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    locale: zh_CN
    serialization:
      WRITE_DATES_WITH_ZONE_ID: false
      WRITE_DURATIONS_AS_TIMESTAMPS: false
      WRITE_DATES_AS_TIMESTAMPS: false
      FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS: false
      WRITE_ENUMS_USING_TO_STRING: true
```

**WebFlux 加载 jackson-module-scala**

添加依赖：
```xml
<dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-scala_2.12</artifactId>
    <version>2.11.1</version>
</dependency>
```

在 `configureHttpMessageCodecs` 中配置 `JsonDecoder` 和 `JsonEncoder`。

```java
@EnableWebFlux
@Configuration
public class CoreWebConfiguration implements WebFluxConfigurer {
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        ServerCodecConfigurer.ServerDefaultCodecs defaultCodecs = configurer.defaultCodecs();
        defaultCodecs.enableLoggingRequestDetails(true);
        objectMapper.registerModule(new DefaultScalaModule());
        defaultCodecs.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON));
        defaultCodecs.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON));
    }
}
```
