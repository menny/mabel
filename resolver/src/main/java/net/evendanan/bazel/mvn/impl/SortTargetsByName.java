package net.evendanan.bazel.mvn.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import net.evendanan.bazel.mvn.api.Target;

public class SortTargetsByName {

    private static final Comparator<? super Target> TARGET_COMPARATOR = (Comparator<Target>) (o1, o2) -> {
        final int mavenDiff = o1.getMavenCoordinates().compareTo(o2.getMavenCoordinates());

        if (mavenDiff!=0) return mavenDiff;

        return o1.getTargetName().compareTo(o2.getTargetName());
    };

    public static Collection<Target> sort(Collection<Target> targets) {
        ArrayList<Target> sorted = new ArrayList<>(targets);
        sorted.sort(TARGET_COMPARATOR);

        return sorted;
    }
}
