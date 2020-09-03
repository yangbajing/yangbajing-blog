package example.jackson;

import com.fasterxml.jackson.core.Version;

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
