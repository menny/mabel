package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.devtools.bazel.workspace.maven.adapter.MigrationToolingGraphResolver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.evendanan.bazel.mvn.api.GraphResolver;
import net.evendanan.bazel.mvn.api.model.ExportsGenerationType;
import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.ResolutionOutput;
import net.evendanan.bazel.mvn.api.model.TargetType;
import net.evendanan.bazel.mvn.api.serialization.Serialization;

public class Resolver {

  private final GraphResolver resolver;

  private Resolver(boolean debugLogs, String jdkHome) {
    this.resolver = new MigrationToolingGraphResolver(debugLogs, jdkHome);
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    JCommander optionParser = JCommander.newBuilder().addObject(options).build();
    try {
      optionParser.parse(args);
    } catch (ParameterException e) {
      System.err.println("Unable to parse options: " + e.getLocalizedMessage());
      optionParser.usage();
      return;
    }
    if (Strings.isNullOrEmpty(options.artifact)) {
      System.err.println("Maven coordinate was not provided");
      optionParser.usage();
      return;
    }
    if (options.repositories.isEmpty()) {
      System.err.println("Repositories URLs were not provided!");
      optionParser.usage();
      return;
    }

    Resolver driver =
        new Resolver(
            options.debug_logs,
            new File(options.jdk_home).getAbsoluteFile().getCanonicalFile().toPath().toString());
    driver.writeResults(options, driver.generateFromArtifacts(options));
  }

  private Resolution generateFromArtifacts(Options options) {
    return resolver.resolve(options.artifact, options.repositories, options.blacklist);
  }

  private void writeResults(Options options, Resolution resolution) throws Exception {
    final File outputFile = new File(options.output_file);
    final File parentFolder = outputFile.getParentFile();
    if (!parentFolder.isDirectory() && !parentFolder.mkdirs()) {
      throw new IOException(
          "Failed to create folder for json file: " + parentFolder.getAbsolutePath());
    }

    try (final FileWriter writer = new FileWriter(outputFile, Charsets.UTF_8, false)) {
      new Serialization()
          .serialize(
              ResolutionOutput.create(
                  options.type, options.exportsGenerationType, options.test_only, resolution),
              writer);
    }
  }

  @Parameters(separators = "=")
  public static class Options {

    @Parameter(
        names = {"--artifact", "-a"},
        splitter = NoSplitter.class,
        description = "Maven artifact coordinate (e.g. groupId:artifactId:version).",
        required = true)
    String artifact;

    @Parameter(
        names = {"--type"},
        splitter = NoSplitter.class,
        description = "Type of artifact: inherit, jar, aar, naive, auto, processor.",
        required = true)
    TargetType type;

    @Parameter(
        names = {"--exports_generation"},
        splitter = NoSplitter.class,
        description = "Type of exports generation: inherit, all, requested_deps, none.",
        required = true)
    ExportsGenerationType exportsGenerationType;

    @Parameter(
        names = {"--test_only"},
        description = "Request test-only resolution",
        arity = 1)
    boolean test_only = false;

    @Parameter(
        names = {"--blacklist", "-b"},
        splitter = NoSplitter.class,
        description = "Blacklisted Maven artifact coordinates (e.g. groupId:artifactId:version).")
    List<String> blacklist = new ArrayList<>();

    @Parameter(
        names = {"--repository"},
        splitter = NoSplitter.class,
        description = "Maven repository url.",
        required = true)
    List<String> repositories = new ArrayList<>();

    @Parameter(
        names = {"--output_file"},
        description = "Path to output graph json file",
        required = true)
    String output_file = "";

    @Parameter(
        names = {"--jdk_home"},
        description = "Path to the local JDK",
        required = true)
    String jdk_home = "";

    @Parameter(
        names = {"--debug_logs"},
        description = "Will print out debug logs.",
        arity = 1)
    boolean debug_logs = false;
  }

  /**
   * Jcommander defaults to splitting each parameter by comma. For example,
   * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]', instead of
   * the intended 'group:artifact:[x1,x2]'
   *
   * <p>For more information: http://jcommander.org/#_splitting
   */
  public static class NoSplitter implements IParameterSplitter {

    @Override
    public List<String> split(String value) {
      return Collections.singletonList(value);
    }
  }
}
