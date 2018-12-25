package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.io.IOException;
import java.util.Collection;

public interface RuleWriter {

    void write(Collection<Rule> rules) throws IOException;
}
