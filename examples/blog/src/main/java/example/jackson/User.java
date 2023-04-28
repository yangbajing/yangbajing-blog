package example.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
public class User {
  private String id;

  //  @JsonSerialize(using = PgJsonSerializer.class)
  //  @JsonDeserialize(using = PgJsonDeserializer.class)
  private io.r2dbc.postgresql.codec.Json metadata;
}
