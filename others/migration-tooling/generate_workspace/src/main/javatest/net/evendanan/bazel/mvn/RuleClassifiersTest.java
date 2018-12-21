package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RuleClassifiersTest {

    @Test
    public void testAarClassifier() {
        final Rule aar = Mockito.mock(Rule.class);
        Mockito.doReturn("aar").when(aar).packaging();
        Assert.assertSame(RuleFormatters.AAR_IMPORT, RuleClassifiers.AAR_IMPORT.classifyRule(aar).orElse(null));

        Mockito.doReturn("jar").when(aar).packaging();
        Assert.assertSame(Optional.empty(), RuleClassifiers.AAR_IMPORT.classifyRule(aar));
    }

    @Test
    public void testJarInspector_java_plugin() throws Exception {
        final ClassLoader classLoader = RuleClassifiersTest.class.getClassLoader();
        final RuleFormatter processorFormatter = RuleClassifiers.performRemoteJarInspection(classLoader.getResourceAsStream("dataenum-processor-1.0.2.jar")).orElse(null);
        Assert.assertTrue(processorFormatter instanceof RuleFormatters.CompositeFormatter);

        Assert.assertSame(Optional.empty(), RuleClassifiers.performRemoteJarInspection(classLoader.getResourceAsStream("dataenum-1.0.2.jar")));
    }
}