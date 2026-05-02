package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.RuleClassifier;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;

public class RuleClassifiers {

  public static TargetsBuilder priorityRuleClassifier(
      Collection<RuleClassifier> classifiers,
      TargetsBuilder defaultFormatter,
      final Dependency dependency) {
    return classifiers.stream()
        .map(classifier -> classifier.classifyRule(dependency))
        .filter(l -> !l.isEmpty())
        .findFirst()
        .<TargetsBuilder>map(TargetsBuilders.CompositeBuilder::new)
        .orElse(defaultFormatter);
  }

  private static class PackagingClassifier implements RuleClassifier {

    private final String packaging;
    private final TargetsBuilder targetsBuilder;

    private PackagingClassifier(final String packaging, TargetsBuilder targetsBuilder) {
      this.packaging = packaging;
      this.targetsBuilder = targetsBuilder;
    }

    @Override
    public List<TargetsBuilder> classifyRule(final Dependency dependency) {
      if (packaging.equals(dependency.mavenCoordinate().packaging())) {
        return Collections.singletonList(targetsBuilder);
      } else {
        return Collections.emptyList();
      }
    }
  }

  public static class AarClassifier extends PackagingClassifier {
    public AarClassifier() {
      super("aar", TargetsBuilders.AAR_IMPORT_WITHOUT_EXPORTS);
    }
  }

  public static class PomClassifier extends PackagingClassifier {
    public PomClassifier() {
      super("pom", TargetsBuilders.POM_IMPORT);
    }
  }

  public static class JarInspector {

    private final Function<Dependency, URI> downloader;

    public JarInspector(Function<Dependency, URI> downloader) {
      this.downloader = downloader;
    }

    private static List<TargetsBuilder> performRemoteJarInspection(InputStream inputStream)
        throws IOException {
      final List<TargetsBuilder> detectedModules = new ArrayList<>();
      try (JarInputStream zipInputStream = new JarInputStream(inputStream, false)) {
        JarEntry jarEntry = zipInputStream.getNextJarEntry();
        while (jarEntry != null) {
          final String jarEntryName = jarEntry.getName();
          if (jarEntryName.equalsIgnoreCase(
              "META-INF/services/javax.annotation.processing.Processor")) {
            List<String> lines =
                CharStreams.readLines(new InputStreamReader(zipInputStream, Charsets.UTF_8));

            parseServicesProcessorFileContent(lines).ifPresent(detectedModules::add);
            break;
          }
          zipInputStream.closeEntry();
          jarEntry = zipInputStream.getNextJarEntry();
        }
      }

      return detectedModules;
    }

    private static Optional<TargetsBuilder> parseServicesProcessorFileContent(
        List<String> processorContent) {
      if (processorContent != null && processorContent.size() > 0) {
        final List<String> processors =
            processorContent.stream()
                .filter(s -> s != null && s.length() > 0)
                .filter(s -> !s.startsWith("#"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (processors.size() > 0) {
          return Optional.of(new TargetsBuilders.JavaPluginFormatter(processors));
        }
      }
      return Optional.empty();
    }

    public List<TargetsBuilder> findAllPossibleBuilders(final Dependency dependency) {
      try (InputStream networkInputStream = downloader.apply(dependency).toURL().openStream()) {
        return performRemoteJarInspection(networkInputStream);
      } catch (IOException e) {
        e.printStackTrace();
        return Collections.emptyList();
      }
    }
  }

  public static class JarClassifier implements RuleClassifier {
    private final Function<Dependency, List<TargetsBuilder>> mJarInspector;

    public JarClassifier(Function<Dependency, List<TargetsBuilder>> jarInspector) {
      mJarInspector = jarInspector;
    }

    @Override
    public List<TargetsBuilder> classifyRule(Dependency dependency) {
      // TODO: in the future, we should use android import
      // final boolean isAndroid = dependency.url().endsWith(".aar");

      return mJarInspector.apply(dependency);
    }
  }
}
