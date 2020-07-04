package example.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties({"_version", "timestamp"})
public class JavaTime {
  private LocalDateTime localDateTime;
  private ZonedDateTime zonedDateTime;
  private OffsetDateTime offsetDateTime;
  private LocalDate localDate;
  private LocalTime localTime;
  private Duration duration;
  private Date date;
  private Timestamp timestamp;

  @JsonIgnore private Long _version;
}
