package net.evendanan.bazel.mvn.api;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class Target {

  private static final String VISIBILITY_ATTR = "visibility";
  private static final ListValue PUBLIC_VISIBILITY =
      new ListValue(Collections.singleton("//visibility:public"));
  private static final ListValue PRIVATE_VISIBILITY =
      new ListValue(Collections.singleton("//visibility:private"));

  private static final String EXTRA_INDENT = "    ";

  private final String mavenCoordinates;
  private final List<String> comments;
  private final String ruleName;
  private final String targetName;
  private final String nameSpacedTargetName;
  private final Map<String, AttributeValue> attributes = new LinkedHashMap<>();

  public Target(
      final String maven,
      final String rule,
      final String targetName,
      final String nameSpacedTargetName) {
    this.mavenCoordinates = maven;
    this.ruleName = rule;
    this.targetName = targetName;
    this.nameSpacedTargetName = nameSpacedTargetName;
    comments = new ArrayList<>();
  }

  public Target(final String maven, final String rule, final String targetName) {
    this(maven, rule, targetName, targetName);
  }

  public String getMavenCoordinates() {
    return mavenCoordinates;
  }

  public Collection<String> getComments() {
    return Collections.unmodifiableCollection(comments);
  }

  public void addComment(String comment) {
    comments.add(comment);
  }

  public String getRuleName() {
    return ruleName;
  }

  public String getTargetName() {
    return targetName;
  }

  public String getNameSpacedTargetName() {
    return nameSpacedTargetName;
  }

  public Map<String, AttributeValue> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  public String getStringAttribute(String name) {
    AttributeValue value = attributes.get(name);
    if (value instanceof StringValue) {
      return ((StringValue) value).value;
    }
    return null;
  }

  public List<String> getListAttribute(String name) {
    AttributeValue value = attributes.get(name);
    if (value instanceof ListValue) {
      return new ArrayList<>(((ListValue) value).value);
    }
    return Collections.emptyList();
  }

  public Boolean getBooleanAttribute(String name) {
    AttributeValue value = attributes.get(name);
    if (value instanceof BooleanValue) {
      return ((BooleanValue) value).value;
    }
    return null;
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

  public Target addVariable(String name, String varName) {
    attributes.put(name, new VarValue(varName));
    return this;
  }

  public Target addList(String name, Collection<String> value) {
    attributes.put(name, new ListValue(value));
    return this;
  }

  private void outputTarget(String indent, StringBuilder builder) {
    builder.append(indent).append(ruleName).append("(");
    builder.append(System.lineSeparator());
    builder
        .append(indent)
        .append(EXTRA_INDENT)
        .append("name")
        .append(" = \"")
        .append(targetName)
        .append("\",");

    List<Map.Entry<String, Target.AttributeValue>> attributesToWrite =
        new ArrayList<>(attributes.size() + 1);
    attributesToWrite.addAll(attributes.entrySet());
    attributesToWrite.sort(Comparator.comparing(Map.Entry::getKey));
    attributes.forEach(
        (key, value) -> {
          builder.append(System.lineSeparator());
          builder.append(indent).append(EXTRA_INDENT).append(key).append(" = ");
          addAttribute(indent + EXTRA_INDENT, builder, value.outputValue());
          builder.append(',');
        });

    builder.append(System.lineSeparator()).append(indent).append(')');
  }

  public String outputString(String indent) {
    StringBuilder builder = new StringBuilder();
    outputTarget(indent, builder);
    return builder.toString();
  }

  private void addAttribute(
      final String indent, final StringBuilder builder, final Collection<String> values) {
    int lineIndex = 0;
    for (final String value : values) {
      if (lineIndex > 0) {
        builder.append(System.lineSeparator()).append(indent);
        if (lineIndex != (values.size() - 1)) {
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

  public Target setPrivateVisibility() {
    attributes.put(VISIBILITY_ATTR, PRIVATE_VISIBILITY);
    return this;
  }

  public boolean isPublic() {
    return attributes.getOrDefault(VISIBILITY_ATTR, new ListValue(Collections.emptyList()))
        == PUBLIC_VISIBILITY;
  }

  interface AttributeValue {

    Collection<String> outputValue();
  }

  static class BooleanValue implements AttributeValue {

    final boolean value;

    BooleanValue(final boolean value) {
      this.value = value;
    }

    @Override
    public Collection<String> outputValue() {
      return Collections.singletonList(value ? "True" : "False");
    }
  }

  static class IntValue implements AttributeValue {

    final int value;

    IntValue(final int value) {
      this.value = value;
    }

    @Override
    public Collection<String> outputValue() {
      return Collections.singletonList(Integer.toString(value));
    }
  }

  static class StringValue implements AttributeValue {

    final String value;

    StringValue(final String value) {
      this.value = value;
    }

    @Override
    public Collection<String> outputValue() {
      return Collections.singletonList(String.format(Locale.US, "\"%s\"", value));
    }
  }

  static class VarValue implements AttributeValue {

    final String varName;

    VarValue(final String varName) {
      this.varName = varName;
    }

    @Override
    public Collection<String> outputValue() {
      return Collections.singletonList(varName);
    }
  }

  static class ListValue implements AttributeValue {

    final Collection<String> value;

    ListValue(final Collection<String> value) {
      this.value = ImmutableList.copyOf(value);
    }

    @Override
    public Collection<String> outputValue() {
      if (value.isEmpty()) {
        return Collections.singletonList("[]");
      }
      if (value.size() == 1) {
        return Collections.singletonList(
            String.format(Locale.US, "[\"%s\"]", value.iterator().next()));
      }

      final List<String> stringList =
          value.stream()
              .sorted()
              .map(str -> String.format(Locale.US, "\"%s\",", str))
              .collect(Collectors.toList());
      stringList.add(0, "[");
      stringList.add("]");
      return stringList;
    }
  }
}
