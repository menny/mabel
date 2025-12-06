package net.evendanan.bazel.mvn.impl;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.TargetsBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.TargetType;

public class TargetsBuilderForType {
  private final Function<MavenCoordinate, TargetType> targetTypeProvider;
  private final Function<Dependency, URI> downloader;

  public TargetsBuilderForType(
      Function<MavenCoordinate, TargetType> targetTypeProvider,
      Function<Dependency, URI> downloader) {
    this.targetTypeProvider = targetTypeProvider;
    this.downloader = downloader;
  }

  public TargetsBuilder generateBuilder(Dependency dependency) {
    final TargetType type = targetTypeProvider.apply(dependency.mavenCoordinate());
    if (type == null)
      throw new IllegalArgumentException(
          "Dependency: "
              + dependency.mavenCoordinate()
              + ": Unable to figure out builder for type NULL. This may be caused of unknown root"
              + " dependency.");
    switch (type) {
      case pom:
        return TargetsBuilders.POM_IMPORT;
      case jar:
        return TargetsBuilders.JAVA_IMPORT;
      case aar:
        return TargetsBuilders.AAR_IMPORT_WITHOUT_EXPORTS;
      case naive:
        return new NaiveBuilder();
      case processor:
        return new ProcessorBuilder(downloader);
      case auto:
        return new AutoBuilder(downloader);
      default:
        throw new IllegalArgumentException(
            "Dependency: "
                + dependency.mavenCoordinate()
                + ": Unable to figure out builder for type "
                + type.toString());
    }
  }

  @VisibleForTesting
  static class ProcessorBuilder implements TargetsBuilder {

    private final RuleClassifiers.JarInspector jarInspector;

    public ProcessorBuilder(Function<Dependency, URI> downloader) {
      jarInspector = new RuleClassifiers.JarInspector(downloader);
    }

    @Override
    public List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools) {
      return new TargetsBuilders.CompositeBuilder(jarInspector.findAllPossibleBuilders(dependency))
          .buildTargets(dependency, dependencyTools);
    }
  }

  @VisibleForTesting
  static class NaiveBuilder implements TargetsBuilder {

    @Override
    public List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools) {
      return RuleClassifiers.priorityRuleClassifier(
              Arrays.asList(
                  new RuleClassifiers.PomClassifier(), new RuleClassifiers.AarClassifier()),
              TargetsBuilders.JAVA_IMPORT,
              dependency)
          .buildTargets(dependency, dependencyTools);
    }
  }

  @VisibleForTesting
  static class AutoBuilder implements TargetsBuilder {

    private final RuleClassifiers.JarInspector jarInspector;

    public AutoBuilder(Function<Dependency, URI> downloader) {
      jarInspector = new RuleClassifiers.JarInspector(downloader);
    }

    @Override
    public List<Target> buildTargets(Dependency dependency, DependencyTools dependencyTools) {
      return RuleClassifiers.priorityRuleClassifier(
              Arrays.asList(
                  new RuleClassifiers.PomClassifier(),
                  new RuleClassifiers.JarClassifier(jarInspector::findAllPossibleBuilders),
                  new RuleClassifiers.AarClassifier()),
              TargetsBuilders.JAVA_IMPORT,
              dependency)
          .buildTargets(dependency, dependencyTools);
    }
  }
}
