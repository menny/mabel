package net.evendanan.bazel.mvn;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class Target {

    private final String type;
    private final String name;
    private final Map<String, AttributeValue> attributes = new LinkedHashMap<>();

    public Target(final String type, final String name) {
        this.type = type;
        this.name = name;
    }

    public Target addBoolean(String name, boolean value) {
        attributes.put(name, new BooleanValue(value));
        return this;
    }

    public Target addInt(String name, int value) {
        attributes.put(name, new IntValue(value));
        return this;
    }

    public Target addString(String name, String value) {
        attributes.put(name, new StringValue(value));
        return this;
    }

    public Target addList(String name, Collection<String> value) {
        attributes.put(name, new ListValue(value));
        return this;
    }

    public void outputTarget(String indent, StringBuilder builder) {
        builder.append(indent).append(type).append("(name = '").append(name).append("',");

        attributes.forEach((key, value) -> {
            builder.append('\n');
            builder.append(indent).append(RuleFormatters.RULE_INDENT).append(key).append(" = ");

            addAttribute(indent + RuleFormatters.RULE_INDENT, builder, value.outputValue());
            builder.append(',');
        });

        builder.append('\n').append(indent).append(')').append('\n');
    }

    private void addAttribute(final String indent, final StringBuilder builder, final Collection<String> values) {
        int lineIndex = 0;
        for (final String value : values) {
            if (lineIndex > 0) {
                builder.append('\n').append(indent);
                if (lineIndex != (values.size() - 1)) {
                    builder.append(RuleFormatters.RULE_INDENT);
                }
            }
            builder.append(value);

            lineIndex++;
        }
    }

    public Target setPublicVisibility() {
        return addList("visibility", Collections.singleton("//visibility:public"));
    }

    private interface AttributeValue {

        Collection<String> outputValue();
    }

    private static class BooleanValue implements AttributeValue {

        private final boolean value;

        private BooleanValue(final boolean value) {this.value = value;}

        @Override
        public Collection<String> outputValue() {
            return Collections.singletonList(value ? "True" : "False");
        }
    }

    private static class IntValue implements AttributeValue {

        private final int value;

        private IntValue(final int value) {this.value = value;}

        @Override
        public Collection<String> outputValue() {
            return Collections.singletonList(Integer.toString(value));
        }
    }

    private static class StringValue implements AttributeValue {

        private final String value;

        private StringValue(final String value) {this.value = value;}

        @Override
        public Collection<String> outputValue() {
            return Collections.singletonList(String.format(Locale.US, "'%s'", value));
        }
    }

    private static class ListValue implements AttributeValue {

        private final Collection<String> value;

        private ListValue(final Collection<String> value) {
            this.value = ImmutableList.copyOf(value);
        }

        @Override
        public Collection<String> outputValue() {
            if (value.isEmpty()) {
                return Collections.singletonList("[]");
            }
            if (value.size() == 1) {
                return Collections.singletonList(String.format(Locale.US, "['%s']", value.iterator().next()));
            }

            final List<String> stringList = value.stream()
                .sorted()
                .map(str -> String.format(Locale.US, "'%s',", str))
                .collect(Collectors.toList());
            stringList.add(0, "[");
            stringList.add("]");
            return stringList;
        }
    }
}
