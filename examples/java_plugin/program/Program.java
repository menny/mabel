package examples.java_plugin.program;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Program {
  static Program create(String output) {
    return new examples.java_plugin.program.AutoValue_Program(output);
  }

  abstract String output();
}
