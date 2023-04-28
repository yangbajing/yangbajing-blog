package example.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.Deserializers;

import java.util.Optional;

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
