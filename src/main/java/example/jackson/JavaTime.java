package example.jackson;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

@Data
@Accessors(chain = true)
public class JavaTime {
  private LocalDateTime localDateTime;
  private ZonedDateTime zonedDateTime;
  private OffsetDateTime offsetDateTime;
  private LocalDate localDate;
  private LocalTime localTime;
  private Duration duration;
  private Date date;
  private Timestamp timestamp;
}
