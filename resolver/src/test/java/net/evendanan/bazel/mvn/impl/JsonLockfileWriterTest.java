package net.evendanan.bazel.mvn.impl;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import net.evendanan.bazel.mvn.api.Target;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonLockfileWriterTest {

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testWritesRepositoryRules() throws Exception {
    File outputFile = tmp.newFile("lockfile.json");
    JsonLockfileWriter writer = new JsonLockfileWriter(outputFile);

    Target httpFile =
        new Target("net.evendanan:dep1:1.0.0", "http_file", "mvn__net_evendanan__dep1__1_0_0", "");
    httpFile.addList(
        "urls",
        Collections.singletonList(
            "https://repo.maven.apache.org/maven2/net/evendanan/dep1/1.0.0/dep1-1.0.0.jar"));
    httpFile.addString("sha256", "checksum123");

    writer.write(Collections.singletonList(httpFile));

    String jsonContent = Files.asCharSource(outputFile, Charsets.UTF_8).read();
    assertTrue(jsonContent.contains("\"net.evendanan:dep1:1.0.0\""));
    assertTrue(jsonContent.contains("\"repo_name\": \"mvn__net_evendanan__dep1__1_0_0\""));
    assertTrue(
        jsonContent.contains(
            "\"url\":"
                + " \"https://repo.maven.apache.org/maven2/net/evendanan/dep1/1.0.0/dep1-1.0.0.jar\""));
    assertTrue(jsonContent.contains("\"sha256\": \"checksum123\""));
  }

  @Test
  public void testWritesImportRules() throws Exception {
    File outputFile = tmp.newFile("lockfile.json");
    JsonLockfileWriter writer = new JsonLockfileWriter(outputFile);

    Target httpFile =
        new Target("net.evendanan:dep1:1.0.0", "http_file", "mvn__net_evendanan__dep1__1_0_0", "");
    httpFile.addList("urls", Collections.singletonList("http://example.com/dep1.jar"));

    Target jvmImport =
        new Target(
            "net.evendanan:dep1:1.0.0",
            "jvm_import",
            "mvn__net_evendanan__dep1__1_0_0_param", // Assuming import targets usually have a
            // suffix
            "");
    jvmImport.addList("deps", Collections.singletonList("mvn__net_evendanan__dep2__1_0_0"));
    jvmImport.addList("exports", Collections.singletonList("mvn__net_evendanan__dep3__1_0_0"));
    jvmImport.addList("runtime_deps", Collections.singletonList("mvn__net_evendanan__dep4__1_0_0"));

    writer.write(Arrays.asList(httpFile, jvmImport));

    String jsonContent = Files.asCharSource(outputFile, Charsets.UTF_8).read();

    assertTrue(jsonContent.contains("\"net.evendanan:dep1:1.0.0\""));

    // Check for dependencies presence
    assertTrue(jsonContent.contains("\"dependencies\": ["));
    assertTrue(jsonContent.contains("\"mvn__net_evendanan__dep2__1_0_0\""));

    // Check for exports presence
    assertTrue(jsonContent.contains("\"exports\": ["));
    assertTrue(jsonContent.contains("\"mvn__net_evendanan__dep3__1_0_0\""));

    // Check for runtime_deps presence
    assertTrue(jsonContent.contains("\"runtime_deps\": ["));
    assertTrue(jsonContent.contains("\"mvn__net_evendanan__dep4__1_0_0\""));
  }

  @Test
  public void testWritesSourceJars() throws Exception {
    File outputFile = tmp.newFile("lockfile.json");
    JsonLockfileWriter writer = new JsonLockfileWriter(outputFile);

    Target mainTarget =
        new Target("net.evendanan:dep1:1.0.0", "http_file", "mvn__net_evendanan__dep1__1_0_0", "");
    mainTarget.addList("urls", Collections.singletonList("https://example.com/lib.jar"));

    Target sourceTarget =
        new Target(
            "net.evendanan:dep1:1.0.0",
            "http_file",
            "mvn__net_evendanan__dep1__1_0_0__sources",
            "");
    sourceTarget.addList("urls", Collections.singletonList("https://example.com/lib-sources.jar"));
    sourceTarget.addString("sha256", "sourceChecksum");

    writer.write(Arrays.asList(mainTarget, sourceTarget));

    String jsonContent = Files.asCharSource(outputFile, Charsets.UTF_8).read();

    assertTrue(jsonContent.contains("\"sources\": {"));
    assertTrue(jsonContent.contains("\"url\": \"https://example.com/lib-sources.jar\""));
    assertTrue(jsonContent.contains("\"sha256\": \"sourceChecksum\""));
  }

  @Test
  public void testWritesAndroidAar() throws Exception {
    File outputFile = tmp.newFile("lockfile.json");
    JsonLockfileWriter writer = new JsonLockfileWriter(outputFile);

    Target httpFile =
        new Target(
            "net.evendanan:android-lib:1.0.0",
            "http_file",
            "mvn__net_evendanan__android_lib__1_0_0",
            "");
    httpFile.addList("urls", Collections.singletonList("http://example.com/lib.aar"));

    Target aarImport =
        new Target(
            "net.evendanan:android-lib:1.0.0",
            "aar_import",
            "mvn__net_evendanan__android_lib__1_0_0",
            "");

    writer.write(Arrays.asList(httpFile, aarImport));

    String jsonContent = Files.asCharSource(outputFile, Charsets.UTF_8).read();
    assertTrue(jsonContent.contains("\"target_type\": \"aar\""));
  }
}
