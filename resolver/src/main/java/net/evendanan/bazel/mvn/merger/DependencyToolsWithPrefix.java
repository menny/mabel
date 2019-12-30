package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.MavenCoordinate;

public class DependencyToolsWithPrefix extends DependencyTools {
    private final String mPrefix;

    public DependencyToolsWithPrefix(String prefix) {
        mPrefix = prefix;
    }

    @Override
    public String repositoryRuleName(MavenCoordinate dependency) {
        return mPrefix + super.repositoryRuleName(dependency);
    }

    @Override
    public String targetName(MavenCoordinate dependency) {
        return mPrefix + super.targetName(dependency);
    }
}
