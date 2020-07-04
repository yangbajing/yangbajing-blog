package fusion.json.jackson

import com.fasterxml.jackson.databind.ObjectMapper

class JacksonDemo {
  val objectMapper = new ObjectMapper().findAndRegisterModules()

}
