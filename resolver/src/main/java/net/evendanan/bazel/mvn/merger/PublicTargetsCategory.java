package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Target;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PublicTargetsCategory {

    public static Function<Target, Target> create(
            Type type,
            Set<MavenCoordinate> rootDependencies,
            Collection<Dependency> resolvedDependencies) {
        switch (type) {
            case all:
                return Function.identity();
            case requested_deps:
                return new RequestedDeps(rootDependencies);
            default:
                throw new UnsupportedOperationException("type " + type + " was not implemented");
        }
    }

    public enum Type {
        all,
        requested_deps
    }

    private static class RequestedDeps implements Function<Target, Target> {

        private final Set<String> mRootMvn;

        RequestedDeps(Collection<MavenCoordinate> rootDependencies) {
            mRootMvn =
                    rootDependencies.stream()
                            .map(MavenCoordinate::toMavenString)
                            .collect(Collectors.toSet());
        }

        @Override
        public Target apply(Target target) {
            if (target.isPublic() && mRootMvn.contains(target.getMavenCoordinates())) {
                target.setPublicVisibility();
            } else {
                target.setPrivateVisibility();
            }
            return target;
        }
    }
}
