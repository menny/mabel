package net.evendanan.bazel.mvn;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IParameterSplitter;

import net.evendanan.bazel.mvn.api.model.ExportsGenerationType;
import net.evendanan.bazel.mvn.api.model.TargetType;
import net.evendanan.bazel.mvn.merger.PublicTargetsCategory;
import net.evendanan.bazel.mvn.merger.VersionConflictResolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Parameters(separators = "=")
public class CommandLineOptions {
    @Parameter(
            names = {"--graph_file", "-a"},
            splitter = NoSplitter.class,
            description = "JSON files representing dependency-graph.",
            required = true)
    List<String> artifacts = new ArrayList<>();

    @Parameter(
            names = {"--output_macro_file_path"},
            description = "Path to output macros bzl file",
            required = true)
    String output_macro_file = "";

    @Parameter(
            names = {"--repository_rule_name"},
            description = "The name of the repository",
            required = true)
    String repository_rule_name = "";

    @Parameter(
            names = {"--output_target_build_files_base_path"},
            description = "Base path to output alias targets BUILD.bazel files")
    String output_target_build_files_base_path = "";

    @Parameter(
            names = {"--package_path"},
            description = "Package path for for transitive rules.")
    String package_path = "";

    @Parameter(
            names = {"--rule_prefix"},
            description = "Prefix to add to all rules.")
    String rule_prefix = "";

    @Parameter(
            names = {"--create_deps_sub_folders"},
            description = "Generate sub-folders matching dependencies tree.",
            arity = 1)
    boolean create_deps_sub_folders = true;

    @Parameter(
            names = {"--fetch_srcjar"},
            description = "Will also try to locate srcjar for the dependency.",
            arity = 1)
    boolean fetch_srcjar = false;

    @Parameter(
            names = {"--calculate_sha"},
            description = "Will also calculate SHA256 for the dependency.",
            arity = 1)
    boolean calculate_sha = true;

    @Parameter(
            names = {"--debug_logs"},
            description = "Will print out debug logs.",
            arity = 1)
    boolean debug_logs = false;

    @Parameter(
            names = {"--output_pretty_dep_graph_filename"},
            description = "If set, will output the dependency graph to this file.")
    String output_pretty_dep_graph_filename = "";

    @Parameter(
            names = {"--artifacts_path"},
            description = "Where to store downloaded artifacts.",
            required = true)
    String artifacts_path;

    @Parameter(
            names = {"--keep_output_folder"},
            description =
                    "Do not delete the output-folder prior to writing generated files. This helps if you store other files at the same folder.",
            arity = 1)
    boolean keep_output_folder = false;

    @Parameter(
            names = {"--public_targets_category"},
            description =
                    "Set public visibility of resolved targets. Default is 'all'. Can be: 'requested_deps', 'exports', 'recursive_exports', 'all'.",
            required = true)
    PublicTargetsCategory.Type public_targets_category = PublicTargetsCategory.Type.all;

    @Parameter(
            names = {"--version_conflict_resolver"},
            description =
                    "Which version-conflict resolution strategy to use. Default is 'latest_version'. Can be: 'latest_version', 'breadth_first', 'depth_first'.",
            required = true)
    VersionConflictResolution version_conflict_resolver = VersionConflictResolution.latest_version;

    @Parameter(
            names = {"--type"},
            splitter = NoSplitter.class,
            description = "Type of artifact: inherit, jar, aar, naive, auto, processor.",
            required = true)
    TargetType type;


    @Parameter(
            names = {"--exports_generation"},
            splitter = NoSplitter.class,
            description = "Where to generate exports: all, requested_deps, none. Note: Can not be inherit for this call.",
            required = true)
    ExportsGenerationType exports_generation;

    /**
     * Jcommander defaults to splitting each parameter by comma. For example,
     * --a=group:artifact:[x1,x2] is parsed as two items 'group:artifact:[x1' and 'x2]', instead of
     * the intended 'group:artifact:[x1,x2]'
     *
     * <p>For more information: http://jcommander.org/#_splitting
     */
    static class NoSplitter implements IParameterSplitter {

        @Override
        public List<String> split(String value) {
            return Collections.singletonList(value);
        }
    }
}
