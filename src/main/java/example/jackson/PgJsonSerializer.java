package example.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.r2dbc.postgresql.codec.Json;

import java.io.IOException;

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
