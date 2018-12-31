package net.evendanan.bazel.mvn.api;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class Target {

    private static final String VISIBILITY_ATTR = "visibility";
    private static final ListValue PUBLIC_VISIBILITY = new ListValue(Collections.singleton("//visibility:public"));

    private static final String EXTRA_INDENT = "    ";

    private final String mavenCoordinates;
    private final String ruleName;
    private final String targetName;
    private final Map<String, AttributeValue> attributes = new LinkedHashMap<>();

    public Target(final String maven, final String rule, final String targetName) {
        this.mavenCoordinates = maven;
        this.ruleName = rule;
        this.targetName = targetName;
    }

    public String getMavenCoordinates() {
        return mavenCoordinates;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getTargetName() {
        return targetName;
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
        builder.append(indent).append(ruleName).append("(name = '").append(targetName).append("',");

        attributes.forEach((key, value) -> {
            builder.append(System.lineSeparator());
            builder.append(indent).append(EXTRA_INDENT).append(key).append(" = ");
            addAttribute(indent + EXTRA_INDENT, builder, value.outputValue());
            builder.append(',');
        });

        builder.append(System.lineSeparator())
                .append(indent).append(')')
                .append(System.lineSeparator());
    }

    public String outputString(String indent) {
        StringBuilder builder = new StringBuilder();
        outputTarget(indent, builder);
        return builder.toString();
    }

    private void addAttribute(final String indent, final StringBuilder builder, final Collection<String> values) {
        int lineIndex = 0;
        for (final String value : values) {
            if (lineIndex > 0) {
                builder.append(System.lineSeparator()).append(indent);
                if (lineIndex!=(values.size() - 1)) {
                    builder.append(EXTRA_INDENT);
                }
            }
            builder.append(value);

            lineIndex++;
        }
    }

    public Target setPublicVisibility() {
        attributes.put(VISIBILITY_ATTR, PUBLIC_VISIBILITY);
        return this;
    }

    public boolean isPublic() {
        return attributes.getOrDefault(VISIBILITY_ATTR, new ListValue(Collections.emptyList()))==PUBLIC_VISIBILITY;
    }

    private interface AttributeValue {

        Collection<String> outputValue();
    }

    private static class BooleanValue implements AttributeValue {

        private final boolean value;

        private BooleanValue(final boolean value) {
            this.value = value;
        }

        @Override
        public Collection<String> outputValue() {
            return Collections.singletonList(value ? "True":"False");
        }
    }

    private static class IntValue implements AttributeValue {

        private final int value;

        private IntValue(final int value) {
            this.value = value;
        }

        @Override
        public Collection<String> outputValue() {
            return Collections.singletonList(Integer.toString(value));
        }
    }

    private static class StringValue implements AttributeValue {

        private final String value;

        private StringValue(final String value) {
            this.value = value;
        }

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
            if (value.size()==1) {
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
