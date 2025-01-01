package net.evendanan.bazel.mvn.api;

import java.io.IOException;
import java.util.Collection;

public interface RuleWriter {

  void write(Collection<Target> dependencies) throws IOException;
}
