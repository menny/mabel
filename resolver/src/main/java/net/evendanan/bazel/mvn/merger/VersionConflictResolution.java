package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.GraphMerger;

public enum VersionConflictResolution {
    latest_version(new PinLatestVersionMerger()),
    breadth_first(new PinBreadthFirstVersionsMerger());

    private final GraphMerger mMerger;

    VersionConflictResolution(GraphMerger merger) {
        mMerger = merger;
    }

    public GraphMerger createMerger() {
        return mMerger;
    }
}
