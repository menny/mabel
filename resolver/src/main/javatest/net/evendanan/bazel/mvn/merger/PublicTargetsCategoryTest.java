package net.evendanan.bazel.mvn.merger;

import net.evendanan.bazel.mvn.api.Target;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.Function;

public class PublicTargetsCategoryTest {

    @Test
    public void testCategoryAll() {
        Function<Target, Target> underTest =
                PublicTargetsCategory.create(PublicTargetsCategory.Type.all);

        Target target = new Target("a.b.c:d", "rule1", "name1").setPublicVisibility();

        Assert.assertSame(target, underTest.apply(target));
        Assert.assertTrue(target.isPublic());

        target = new Target("a.b.c:d", "rule1", "name1").setPrivateVisibility();

        Assert.assertSame(target, underTest.apply(target));
        Assert.assertFalse(target.isPublic());
    }
}
