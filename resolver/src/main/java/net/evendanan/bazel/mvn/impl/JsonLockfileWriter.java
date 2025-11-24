package net.evendanan.bazel.mvn.impl;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.evendanan.bazel.mvn.api.RuleWriter;
import net.evendanan.bazel.mvn.api.Target;

/**
 * Writes a JSON lockfile containing all resolved Maven dependencies with their metadata. This
 * lockfile is used by the bzlmod module extension to create repository rules without needing to
 * re-resolve dependencies from Maven repositories.
 *
 * <p>The writer processes two types of targets: 1. Repository rules (http_file) - these contain
 * URLs and SHA256 hashes 2. Import rules (jvm_import, aar_import, etc.) - these contain dependency
 * metadata
 *
 * <p>The lockfile format is designed to be minimal and contain only the information needed by the
 * bzlmod extension to recreate the repository rules and build targets.
 */
public class JsonLockfileWriter implements RuleWriter {

  private final File outputFile;
  private final Gson gson;

  public JsonLockfileWriter(File outputFile) {
    this.outputFile = outputFile;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  @Override
  public void write(Collection<Target> targets) throws IOException {
    // We need to process both repository rules (http_file) and import rules (jvm_import, etc)
    // and combine their information into a single lockfile entry per artifact

    Map<String, ArtifactInfo> artifactMap = new HashMap<>();

    for (Target target : targets) {
      String mavenCoordinate = target.getMavenCoordinates();

      ArtifactInfo info = artifactMap.computeIfAbsent(mavenCoordinate, k -> new ArtifactInfo());

      if ("http_file".equals(target.getRuleName())) {
        // This is a repository rule containing URL and SHA256
        processRepositoryRule(target, info);
      } else if (target.getRuleName().equals("jvm_import")
          || target.getRuleName().equals("aar_import")
          || target.getRuleName().equals("java_plugin")
          || target.getRuleName().equals("native.alias")) {
        // This is an import rule containing dependency information
        processImportRule(target, info);
      }
    }

    JsonObject root = new JsonObject();
    root.addProperty("version", "1.0");

    JsonObject artifacts = new JsonObject();
    for (Map.Entry<String, ArtifactInfo> entry : artifactMap.entrySet()) {
      String mavenCoordinate = entry.getKey();
      ArtifactInfo info = entry.getValue();
      if (info.hasMainArtifact()) {
        artifacts.add(mavenCoordinate, info.toJson());
      }
    }

    root.add("artifacts", artifacts);

    try (OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8)) {
      gson.toJson(root, writer);
    }
  }

  private void processRepositoryRule(Target target, ArtifactInfo info) {
    // Extract information from http_file repository rule
    // Target name ending with __sources indicates source JAR
    if (target.getTargetName().endsWith("__sources")) {
      info.sourcesRepoName = target.getTargetName();
      // We'll need to extract URL and SHA256 from the target's attributes
      // For now, store the target for later processing
      info.sourcesTarget = target;
    } else {
      info.repoName = target.getTargetName();
      info.mainTarget = target;
    }
  }

  private void processImportRule(Target target, ArtifactInfo info) {
    // Extract information from jvm_import or other import rules
    info.importTarget = target;
  }

  /** Helper class to accumulate information about a single artifact from multiple targets. */
  private static class ArtifactInfo {
    String repoName;
    Target mainTarget; // http_file target for main artifact
    String sourcesRepoName;
    Target sourcesTarget; // http_file target for sources
    Target importTarget; // jvm_import or similar target

    boolean hasMainArtifact() {
      return mainTarget != null;
    }

    JsonObject toJson() {
      JsonObject artifact = new JsonObject();

      // repo_name from the repository rule
      if (repoName != null) {
        artifact.addProperty("repo_name", repoName);
      }

      // Extract URL and SHA256 from the main http_file target
      if (mainTarget != null) {
        // URLs are stored as a list, but we expect exactly one
        java.util.List<String> urls = mainTarget.getListAttribute("urls");
        if (!urls.isEmpty()) {
          artifact.addProperty("url", urls.get(0));
        }

        String sha256 = mainTarget.getStringAttribute("sha256");
        if (sha256 != null && !sha256.isEmpty()) {
          artifact.addProperty("sha256", sha256);
        }
      }

      // Extract sources information if available
      if (sourcesTarget != null) {
        JsonObject sources = new JsonObject();
        java.util.List<String> urls = sourcesTarget.getListAttribute("urls");
        if (!urls.isEmpty()) {
          sources.addProperty("url", urls.get(0));
        }

        String sha256 = sourcesTarget.getStringAttribute("sha256");
        if (sha256 != null && !sha256.isEmpty()) {
          sources.addProperty("sha256", sha256);
        }

        if (sources.has("url")) {
          artifact.add("sources", sources);
        }
      }

      // Extract dependency information from the import target
      if (importTarget != null) {
        // Dependencies
        JsonArray dependencies = new JsonArray();
        for (String dep : importTarget.getListAttribute("deps")) {
          dependencies.add(dep);
        }
        artifact.add("dependencies", dependencies);

        // Exports
        JsonArray exports = new JsonArray();
        for (String export : importTarget.getListAttribute("exports")) {
          exports.add(export);
        }
        artifact.add("exports", exports);

        // Runtime dependencies
        JsonArray runtimeDeps = new JsonArray();
        for (String runtimeDep : importTarget.getListAttribute("runtime_deps")) {
          runtimeDeps.add(runtimeDep);
        }
        artifact.add("runtime_deps", runtimeDeps);

        // Test only flag
        Boolean testOnly = importTarget.getBooleanAttribute("testonly");
        artifact.addProperty("test_only", testOnly != null ? testOnly : false);

        // Target type from rule name
        String targetType = "jar"; // default
        if (importTarget.getRuleName().equals("aar_import")) {
          targetType = "aar";
        } else if (importTarget.getRuleName().equals("java_plugin")) {
          targetType = "processor";
        }
        artifact.addProperty("target_type", targetType);

        // Licenses - extract from tags attribute
        JsonArray licenses = new JsonArray();
        java.util.List<String> tags = importTarget.getListAttribute("tags");
        java.util.List<String> licensesList = importTarget.getListAttribute("licenses");

        // For now, just store license names (URLs would need to come from POM metadata)
        for (String license : licensesList) {
          JsonObject licenseObj = new JsonObject();
          licenseObj.addProperty("name", license);
          licenseObj.addProperty("url", "");
          licenses.add(licenseObj);
        }
        artifact.add("licenses", licenses);
      } else {
        // No import target, provide defaults
        artifact.add("dependencies", new JsonArray());
        artifact.add("exports", new JsonArray());
        artifact.add("runtime_deps", new JsonArray());
        artifact.addProperty("test_only", false);
        artifact.addProperty("target_type", "jar");
        artifact.add("licenses", new JsonArray());
      }

      return artifact;
    }
  }
}
