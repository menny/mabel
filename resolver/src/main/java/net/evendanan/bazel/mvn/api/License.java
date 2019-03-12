package net.evendanan.bazel.mvn.api;

import com.google.common.base.Strings;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Types of licenses. Taken from https://docs.bazel.build/versions/master/be/functions.html#licenses
 */
public enum License {
    /**
     * Requires mandatory source distribution.
     */
    restricted,

    /**
     * Allows usage of software freely in unmodified form. Any modifications must be made freely available.
     */
    reciprocal,

    /**
     * Original or modified third-party software may be shipped without danger nor encumbering other sources. All of the licenses in this category do, however, have an "original Copyright notice" or "advertising clause", wherein any external distributions must include the notice or clause specified in the license.
     */
    notice,

    /**
     * Code that is under a license but does not require a notice.
     */
    permissive,

    /**
     * Public domain, free for any use.
     */
    unencumbered;

    //notice licenses
    private static Pattern APACHE = Pattern.compile(".*Apache.*\\s+.*2.*");
    private static Pattern MIT = Pattern.compile(".*MIT.*");
    private static Pattern BSD = Pattern.compile(".*BSD.*");

    //reciprocal licenses
    private static Pattern ECLIPSE = Pattern.compile(".*Eclipse\\s+Public\\s+License.*\\s+.*1.*");
    private static Pattern GNU = Pattern.compile(".*GNU.*");

    //unencumbered licenses
    private static Pattern CC0 = Pattern.compile(".*CC0.*");

    /**
     * Mapping between a license and its type.
     * Data taken from https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licenses
     */
    @Nullable
    public static License fromLicenseName(final String licenseName) {
        if (Strings.isNullOrEmpty(licenseName)) return null;

        return ifAnyMatch(notice, licenseName, APACHE, MIT, BSD)
                .orElseGet(() -> ifAnyMatch(reciprocal, licenseName, ECLIPSE, GNU)
                        .orElseGet(() -> ifAnyMatch(unencumbered, licenseName, CC0)
                                .orElseGet(() -> {
                                    System.out.println(String.format(Locale.ROOT, "License with name '%s' is unrecognized.", licenseName));
                                    return null;
                                })));
    }

    private static Optional<License> ifAnyMatch(@Nonnull final License license, @Nonnull final String text, @Nonnull Pattern... patterns) {
        for (final Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return Optional.of(license);
            }
        }

        return Optional.empty();
    }
}
