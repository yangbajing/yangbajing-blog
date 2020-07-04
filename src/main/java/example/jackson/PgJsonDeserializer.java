package example.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.r2dbc.postgresql.codec.Json;

import java.io.IOException;

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
