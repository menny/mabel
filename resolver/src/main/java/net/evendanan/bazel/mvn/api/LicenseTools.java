package net.evendanan.bazel.mvn.api;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.evendanan.bazel.mvn.api.model.License;
import org.apache.commons.lang3.StringUtils;

/**
 * Types of licenses. Taken from https://docs.bazel.build/versions/master/be/functions.html#licenses
 */
public final class LicenseTools {
  private static final List<LicenseClassType> msKnownLicenses =
      Arrays.asList(
          // notice licenses
          type(Pattern.compile(".*Apache.*", CASE_INSENSITIVE), "Apache", License.Class.notice),
          type(
              Pattern.compile(".*ASF.*License.*", CASE_INSENSITIVE),
              "Apache",
              License.Class.notice),
          type(Pattern.compile(".*ASF.*2.*", CASE_INSENSITIVE), "Apache", License.Class.notice),
          type(Pattern.compile(".*MIT.*"), "MIT", License.Class.notice),
          type(Pattern.compile(".*BSD.*"), "BSD", License.Class.notice),
          type(
              Pattern.compile(".*Facebook.*License.*", CASE_INSENSITIVE),
              "Facebook",
              License.Class.notice),
          type(
              Pattern.compile(".*JSON.*License.*", CASE_INSENSITIVE), "JSON", License.Class.notice),
          type(
              Pattern.compile(".*Bouncy.*Castle.*", CASE_INSENSITIVE),
              "Bouncy Castle",
              License.Class.notice),
          type(Pattern.compile(".*CDDL.*"), "CDDL", License.Class.notice),
          type(
              Pattern.compile(
                  ".*COMMON.+DEVELOPMENT.+AND.+DISTRIBUTION.+LICENSE.*", CASE_INSENSITIVE),
              "CDDL",
              License.Class.notice),
          type(
              Pattern.compile(".*Common.+Public.+License.*", CASE_INSENSITIVE),
              "Common-Public",
              License.Class.notice),
          type(
              Pattern.compile("Google Cloud Software License", CASE_INSENSITIVE),
              "Google Cloud",
              License.Class.notice),
          type(
              Pattern.compile(".*Indiana.+University.+License.*", CASE_INSENSITIVE),
              "Indiana University",
              License.Class.notice),
          type(Pattern.compile(".*ICU.+License.*", CASE_INSENSITIVE), "ICU", License.Class.notice),
          // reciprocal licenses
          type(
              Pattern.compile(".*Eclipse\\s+Public\\s+License.*\\s+.*[12].*", CASE_INSENSITIVE),
              "Eclipse",
              License.Class.reciprocal),
          type(Pattern.compile(".*EPL\\s+.*1.*"), "Eclipse", License.Class.reciprocal),
          type(
              Pattern.compile(".*Mozilla.*License.*", CASE_INSENSITIVE),
              "Mozilla",
              License.Class.reciprocal),
          type(Pattern.compile(".*MPL.*1.1.*"), "Mozilla", License.Class.reciprocal),

          // restricted licenses
          type(Pattern.compile(".*GNU.*"), "GPL", License.Class.restricted),
          type(Pattern.compile(".*GPL.*"), "GPL", License.Class.restricted),

          // unencumbered licenses
          type(Pattern.compile(".*CC0.*"), "CC0", License.Class.unencumbered),
          type(
              Pattern.compile(".*Public.*Domain.*", CASE_INSENSITIVE),
              "Public-Domain",
              License.Class.unencumbered),
          type(
              Pattern.compile(".*Android.*License.*", CASE_INSENSITIVE),
              "AOSP",
              License.Class.unencumbered),
          type(
              Pattern.compile(".*provided.*without.*support.*or.*warranty.*", CASE_INSENSITIVE),
              "NO-WARRANTY",
              License.Class.unencumbered),

          // permissive
          type(Pattern.compile(".*WTFPL.*"), "WTFPL", License.Class.permissive));

  private static LicenseClassType type(
      Pattern licenseNameRegex, String licenseType, License.Class licenseClass) {
    return new LicenseClassType(licenseNameRegex, licenseType, licenseClass);
  }

  @Nonnull
  public static String typeFromLicenseName(@Nullable final String licenseName) {
    final String unknownType = "UNKNOWN";
    if (StringUtils.isBlank(licenseName)) return unknownType;

    return msKnownLicenses.stream()
        .filter(l -> l.licenseNameRegex.matcher(licenseName).find())
        .findFirst()
        .map(l -> l.licenseType)
        .orElse(unknownType);
  }

  /**
   * Mapping between a license and its class. Data taken from
   * https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses Or from the
   * licenses themselves. Or from https://source.bazel.build/search?q=licenses%20f:BUILD
   */
  @Nullable
  public static License.Class classFromLicenseName(@Nullable final String licenseName) {
    if (StringUtils.isBlank(licenseName)) return null;
    return msKnownLicenses.stream()
        .filter(l -> l.licenseNameRegex.matcher(licenseName).find())
        .findFirst()
        .map(l -> l.licenseClass)
        .orElse(null);
  }

  private static class LicenseClassType {
    public final License.Class licenseClass;
    public final String licenseType;
    public final Pattern licenseNameRegex;

    private LicenseClassType(
        Pattern licenseNameRegex, String licenseType, License.Class licenseClass) {
      this.licenseNameRegex = licenseNameRegex;
      this.licenseType = licenseType;
      this.licenseClass = licenseClass;
    }
  }
}
