package net.evendanan.bazel.mvn;

import com.google.devtools.bazel.workspace.maven.Rule;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class CompositeFormatterTest {

    @Test
    public void testCompositeFormatter() {
        final RuleFormatter formatter1 = Mockito.mock(RuleFormatter.class);
        Mockito.doReturn("first_format\n").when(formatter1).formatRule(Mockito.anyString(), Mockito.any());
        final RuleFormatter formatter2 = Mockito.mock(RuleFormatter.class);
        Mockito.doReturn("second_format\n").when(formatter2).formatRule(Mockito.anyString(), Mockito.any());

        final RuleFormatters.CompositeFormatter compositeFormatter = new RuleFormatters.CompositeFormatter(formatter1, formatter2);

        Rule rule = Mockito.mock(Rule.class);
        String indent = "   ";
        final String combinedFormat = compositeFormatter.formatRule(indent, rule);

        Mockito.verify(formatter1).formatRule(Mockito.same(indent), Mockito.same(rule));
        Mockito.verifyNoMoreInteractions(formatter1);
        Mockito.verify(formatter2).formatRule(Mockito.same(indent), Mockito.same(rule));
        Mockito.verifyNoMoreInteractions(formatter2);

        Assert.assertEquals("first_format\nsecond_format\n", combinedFormat);
    }
}