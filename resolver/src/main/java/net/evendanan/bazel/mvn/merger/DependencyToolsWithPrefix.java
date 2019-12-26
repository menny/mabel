package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.DependencyTools;

public class DependencyToolsWithPrefix extends DependencyTools {
    private final String mPrefix;

    public DependencyToolsWithPrefix(String prefix) {
        mPrefix = prefix;
    }

    @Override
    public String repositoryRuleName(Dependency dependency) {
        return mPrefix + super.repositoryRuleName(dependency);
    }

    @Override
    public String targetName(Dependency dependency) {
        return mPrefix + super.targetName(dependency);
    }
}
