package net.evendanan.bazel.mvn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.base.Strings;
import com.google.devtools.bazel.workspace.maven.adapter.MigrationToolingGraphResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.GraphResolver;

public class Resolver {

    private final GraphResolver resolver;

    private Resolver() {
        this.resolver = new MigrationToolingGraphResolver();
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
            optionParser.usage();
            return;
        }
        if (options.repositories.isEmpty()) {
            optionParser.usage();
            return;
        }

        Resolver driver = new Resolver();
        Collection<Dependency> dependencies = driver.generateFromArtifacts(options);
        driver.writeResults(options, dependencies);
    }

    private Collection<Dependency> generateFromArtifacts(Options options) {
        return resolver.resolve(options.artifact, options.repositories, options.blacklist);
    }

    private void writeResults(Options options, Collection<Dependency> resolvedDependencies) throws Exception {
        //outputFile
    }

    @Parameters(separators = "=")
    public static class Options {

        @Parameter(
                names = {"--artifact", "-a"},
                splitter = NoSplitter.class,
                description = "Maven artifact coordinate (e.g. groupId:artifactId:version).",
                required = true
        ) String artifact;

        @Parameter(
                names = {"--blacklist", "-b"},
                splitter = NoSplitter.class,
                description = "Blacklisted Maven artifact coordinates (e.g. groupId:artifactId:version)."
        ) List<String> blacklist = new ArrayList<>();

        @Parameter(
                names = {"--repository"},
                splitter = NoSplitter.class,
                description = "Maven repository url.",
                required = true
        ) List<String> repositories = new ArrayList<>();

        @Parameter(
                names = {"--output_file"},
                description = "Path to output graph json file",
                required = true
        ) String output_file = "";
    }

    /**
     * Jcommander defaults to splitting each parameter by comma. For example,
     * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]',
     * instead of the intended 'group:artifact:[x1,x2]'
     *
     * For more information: http://jcommander.org/#_splitting
     */
    public static class NoSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }
}
