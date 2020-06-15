package net.evendanan.bazel.mvn.api;

import com.google.common.base.Strings;
import net.evendanan.bazel.mvn.api.model.License;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Types of licenses. Taken from https://docs.bazel.build/versions/master/be/functions.html#licenses
 */
public final class LicenseTools {
    // notice licenses
    private static Pattern APACHE = Pattern.compile(".*Apache.*", CASE_INSENSITIVE);
    private static Pattern APACHE_ASF_LICENSE = Pattern.compile(".*ASF.*License.*");
    private static Pattern APACHE_ASF = Pattern.compile(".*ASF.*2.*");
    private static Pattern MIT = Pattern.compile(".*MIT.*");
    private static Pattern BSD = Pattern.compile(".*BSD.*");
    private static Pattern FACEBOOK = Pattern.compile(".*Facebook.*License.*");
    private static Pattern JSON = Pattern.compile(".*JSON.*License.*");
    private static Pattern BOUNCY_CASTLE = Pattern.compile(".*Bouncy.*Castle.*");
    private static Pattern CDDL = Pattern.compile(".*CDDL.*");
    private static Pattern COMMON_PUBLIC =
            Pattern.compile(".*Common.+Public.+License.*", CASE_INSENSITIVE);
    private static Pattern CDDL_FULL =
            Pattern.compile(
                    ".*COMMON.+DEVELOPMENT.+AND.+DISTRIBUTION.+LICENSE.*", CASE_INSENSITIVE);
    private static Pattern GOOGLE_CLOUD =
            Pattern.compile("Google Cloud Software License", CASE_INSENSITIVE);
    private static Pattern INDIANA_U =
            Pattern.compile(".*Indiana.+University.+License.*", CASE_INSENSITIVE);
    private static Pattern ICU = Pattern.compile(".*ICU.+License.*");

    // reciprocal licenses
    private static Pattern ECLIPSE =
            Pattern.compile(".*Eclipse\\s+Public\\s+License.*\\s+.*[12].*", CASE_INSENSITIVE);
    private static Pattern EPL = Pattern.compile(".*EPL\\s+.*1.*");
    private static Pattern MOZILLA_MPL = Pattern.compile(".*MPL.*1.1.*");
    private static Pattern MOZILLA = Pattern.compile(".*Mozilla.*License.*", CASE_INSENSITIVE);

    // restricted licenses
    private static Pattern GNU = Pattern.compile(".*GNU.*");
    private static Pattern LGPL_GPL = Pattern.compile(".*GPL.*");

    // unencumbered licenses
    private static Pattern CC0 = Pattern.compile(".*CC0.*");
    private static Pattern PUBLIC_DOMAIN = Pattern.compile(".*Public.*Domain.*", CASE_INSENSITIVE);
    private static Pattern ANDROID_SDK = Pattern.compile(".*Android.*License.*", CASE_INSENSITIVE);
    private static Pattern NO_WARRANTY =
            Pattern.compile(".*provided.*without.*support.*or.*warranty.*", CASE_INSENSITIVE);

    // permissive
    private static Pattern WTFPL = Pattern.compile(".*WTFPL.*");

    /**
     * Mapping between a license and its type. Data taken from
     * https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses Or from
     * the licenses themselves. Or from https://source.bazel.build/search?q=licenses%20f:BUILD
     */
    @Nullable
    public static License fromLicenseName(final String licenseName) {
        if (Strings.isNullOrEmpty(licenseName)) return null;

        return ifAnyMatch(
                        License.notice,
                        licenseName,
                        APACHE,
                        APACHE_ASF,
                        APACHE_ASF_LICENSE,
                        MIT,
                        BSD,
                        FACEBOOK,
                        JSON,
                        BOUNCY_CASTLE,
                        COMMON_PUBLIC,
                        CDDL,
                        CDDL_FULL,
                        GOOGLE_CLOUD,
                        INDIANA_U,
                        ICU)
                .orElseGet(
                        () ->
                                ifAnyMatch(
                                                License.reciprocal,
                                                licenseName,
                                                ECLIPSE,
                                                EPL,
                                                MOZILLA_MPL,
                                                MOZILLA)
                                        .orElseGet(
                                                () ->
                                                        ifAnyMatch(
                                                                        License.restricted,
                                                                        licenseName,
                                                                        GNU,
                                                                        LGPL_GPL)
                                                                .orElseGet(
                                                                        () ->
                                                                                ifAnyMatch(
                                                                                                License
                                                                                                        .unencumbered,
                                                                                                licenseName,
                                                                                                CC0,
                                                                                                PUBLIC_DOMAIN,
                                                                                                ANDROID_SDK,
                                                                                                NO_WARRANTY)
                                                                                        .orElseGet(
                                                                                                () ->
                                                                                                        ifAnyMatch(
                                                                                                                        License
                                                                                                                                .permissive,
                                                                                                                        licenseName,
                                                                                                                        WTFPL)
                                                                                                                .orElseGet(
                                                                                                                        () -> {
                                                                                                                            System
                                                                                                                                    .out
                                                                                                                                    .println(
                                                                                                                                            String
                                                                                                                                                    .format(
                                                                                                                                                            Locale
                                                                                                                                                                    .ROOT,
                                                                                                                                                            "License with name '%s' is unrecognized.",
                                                                                                                                                            licenseName));
                                                                                                                            return null;
                                                                                                                        })))));
    }

    private static Optional<License> ifAnyMatch(
            @Nonnull final License license,
            @Nonnull final String text,
            @Nonnull Pattern... patterns) {
        for (final Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return Optional.of(license);
            }
        }

        return Optional.empty();
    }
}
