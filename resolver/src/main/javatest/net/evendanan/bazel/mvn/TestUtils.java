package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mockito.Mockito;

public class TestUtils {


    public static Rule createMockRule(String mavenDep, String url, List<String> depsLabels, List<String> exportsLabels, List<String> runtimeLabels) {
        final Rule rule = Mockito.mock(Rule.class);
        final String[] depsPart = mavenDep.split(":", -1);

        final String normalized = mavenDep.replaceAll(":", "__").replaceAll("-", "_");
        Mockito.doReturn("mvn__" + normalized).when(rule).mavenGeneratedName();
        Mockito.doReturn("safe_mvn__" + depsPart[0].replace('.', '_') + "__" + depsPart[1]).when(rule).safeRuleFriendlyName();
        Mockito.doReturn(url).when(rule).getUrl();
        Mockito.doReturn(depsPart[0]).when(rule).groupId();
        Mockito.doReturn(depsPart[1]).when(rule).artifactId();

        Mockito.doReturn(generateMockedDeps(depsLabels)).when(rule).getDeps();
        Mockito.doReturn(generateMockedDeps(exportsLabels)).when(rule).getExportDeps();
        Mockito.doReturn(generateMockedDeps(runtimeLabels)).when(rule).getRuntimeDeps();

        return rule;
    }


    private static Set<Rule> generateMockedDeps(final List<String> depsLabels) {
        Set<Rule> deps = new HashSet<>(depsLabels.size());
        depsLabels.forEach(dep -> {
            final Rule depRule = Mockito.mock(Rule.class);
            Mockito.doReturn("safe_mvn__" + dep).when(depRule).safeRuleFriendlyName();
            deps.add(depRule);
        });
        return deps;
    }
}
