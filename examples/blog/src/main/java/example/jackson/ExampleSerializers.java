package example.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;
import io.r2dbc.postgresql.codec.Json;

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
