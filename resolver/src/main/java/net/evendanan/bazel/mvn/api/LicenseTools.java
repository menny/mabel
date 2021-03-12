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
    private static final Pattern APACHE = Pattern.compile(".*Apache.*", CASE_INSENSITIVE);
    private static final Pattern APACHE_ASF_LICENSE = Pattern.compile(".*ASF.*License.*");
    private static final Pattern APACHE_ASF = Pattern.compile(".*ASF.*2.*");
    private static final Pattern MIT = Pattern.compile(".*MIT.*");
    private static final Pattern BSD = Pattern.compile(".*BSD.*");
    private static final Pattern FACEBOOK = Pattern.compile(".*Facebook.*License.*");
    private static final Pattern JSON = Pattern.compile(".*JSON.*License.*");
    private static final Pattern BOUNCY_CASTLE = Pattern.compile(".*Bouncy.*Castle.*");
    private static final Pattern CDDL = Pattern.compile(".*CDDL.*");
    private static final Pattern COMMON_PUBLIC =
            Pattern.compile(".*Common.+Public.+License.*", CASE_INSENSITIVE);
    private static final Pattern CDDL_FULL =
            Pattern.compile(
                    ".*COMMON.+DEVELOPMENT.+AND.+DISTRIBUTION.+LICENSE.*", CASE_INSENSITIVE);
    private static final Pattern GOOGLE_CLOUD =
            Pattern.compile("Google Cloud Software License", CASE_INSENSITIVE);
    private static final Pattern INDIANA_U =
            Pattern.compile(".*Indiana.+University.+License.*", CASE_INSENSITIVE);
    private static final Pattern ICU = Pattern.compile(".*ICU.+License.*");

    // reciprocal licenses
    private static final Pattern ECLIPSE =
            Pattern.compile(".*Eclipse\\s+Public\\s+License.*\\s+.*[12].*", CASE_INSENSITIVE);
    private static final Pattern EPL = Pattern.compile(".*EPL\\s+.*1.*");
    private static final Pattern MOZILLA_MPL = Pattern.compile(".*MPL.*1.1.*");
    private static final Pattern MOZILLA = Pattern.compile(".*Mozilla.*License.*", CASE_INSENSITIVE);

    // restricted licenses
    private static final Pattern GNU = Pattern.compile(".*GNU.*");
    private static final Pattern LGPL_GPL = Pattern.compile(".*GPL.*");

    // unencumbered licenses
    private static final Pattern CC0 = Pattern.compile(".*CC0.*");
    private static final Pattern PUBLIC_DOMAIN = Pattern.compile(".*Public.*Domain.*", CASE_INSENSITIVE);
    private static final Pattern ANDROID_SDK = Pattern.compile(".*Android.*License.*", CASE_INSENSITIVE);
    private static final Pattern NO_WARRANTY =
            Pattern.compile(".*provided.*without.*support.*or.*warranty.*", CASE_INSENSITIVE);

    // permissive
    private static final Pattern WTFPL = Pattern.compile(".*WTFPL.*");

    /**
     * Mapping between a license and its type. Data taken from
     * https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses Or from
     * the licenses themselves. Or from https://source.bazel.build/search?q=licenses%20f:BUILD
     */
    @Nullable
    public static License.Type fromLicenseName(final String licenseName) {
        if (Strings.isNullOrEmpty(licenseName)) return null;

        return ifAnyMatch(
                        License.Type.notice,
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
                                                License.Type.reciprocal,
                                                licenseName,
                                                ECLIPSE,
                                                EPL,
                                                MOZILLA_MPL,
                                                MOZILLA)
                                        .orElseGet(
                                                () ->
                                                        ifAnyMatch(
                                                                        License.Type.restricted,
                                                                        licenseName,
                                                                        GNU,
                                                                        LGPL_GPL)
                                                                .orElseGet(
                                                                        () ->
                                                                                ifAnyMatch(
                                                                                                License
                                                                                                        .Type.unencumbered,
                                                                                                licenseName,
                                                                                                CC0,
                                                                                                PUBLIC_DOMAIN,
                                                                                                ANDROID_SDK,
                                                                                                NO_WARRANTY)
                                                                                        .orElseGet(
                                                                                                () ->
                                                                                                        ifAnyMatch(
                                                                                                                        License
                                                                                                                                .Type.permissive,
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

    private static Optional<License.Type> ifAnyMatch(
            @Nonnull final License.Type license,
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
