package net.evendanan.bazel.mvn.merger;

import com.google.common.base.Splitter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
  private static final Pattern SUB_VERSION_PATTERN = Pattern.compile("(\\d+)[-]*(.*)");
  private static final Splitter VERSION_SPLITTER = Splitter.on('.');

  @Override
  public int compare(String version1, String version2) {
    // a negative integer: first argument is less than
    // zero: equal to
    // a positive: first greater than the second

    // same string, same value.
    if (Objects.equals(version1, version2)) return 0;

    // empty value is less than anything
    if (version1 == null || version1.isEmpty()) return -1;
    if (version2 == null || version2.isEmpty()) return 1;

    final List<String> version1Split = VERSION_SPLITTER.splitToList(version1);
    final List<String> version2Split = VERSION_SPLITTER.splitToList(version2);

    final int minIndex = Math.min(version1Split.size(), version2Split.size());
    for (int splitIndex = 0; splitIndex < minIndex; splitIndex++) {
      final int subDiff =
          subVersionDiff(version1Split.get(splitIndex), version2Split.get(splitIndex));
      if (subDiff != 0) return subDiff;
    }

    // all minimum sub versions are equal. Longest version wins in this case.
    // 2.3 < 2.3.1
    return Integer.compare(version1Split.size(), version2Split.size());
  }

  private int subVersionDiff(String subVersion1, String subVersion2) {
    Matcher matcher1 = SUB_VERSION_PATTERN.matcher(subVersion1);
    Matcher matcher2 = SUB_VERSION_PATTERN.matcher(subVersion2);

    if (matcher1.find() && matcher2.find()) {
      final int number1 = Integer.parseInt(matcher1.group(1));
      final int number2 = Integer.parseInt(matcher2.group(1));
      if (number1 > number2) return 1;
      if (number1 < number2) return -1;

      String lexi1 = matcher1.group(2);
      String lexi2 = matcher2.group(2);
      if (lexi1.equals(lexi2)) return 0;
      // if either has no text part, then it's the final version
      if (lexi1.isEmpty()) return 1;
      if (lexi2.isEmpty()) return -1;

      // SNAPSHOT is really powerful.
      if (lexi1.equals("SNAPSHOT")) return 1;
      if (lexi2.equals("SNAPSHOT")) return -1;

      // just lexicographical diff.
      return Integer.compare(lexi1.compareTo(lexi2), 0);
    }

    return 0;
  }
}
