package com.google.devtools.bazel.workspace.maven.adapter;

import com.google.devtools.bazel.workspace.maven.Rule;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import net.evendanan.bazel.mvn.api.Dependency;
import org.apache.maven.model.License;

public class RuleToDependency {

    public static Dependency from(Rule rule) {
        URI sources = uriWithClassifier(rule.getUrl(), "sources");
        return new Dependency(rule.groupId(), rule.artifactId(), rule.version(), rule.packaging(),
                rule.getDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getExportDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                rule.getRuntimeDeps().stream().map(RuleToDependency::from).collect(Collectors.toList()),
                URI.create(rule.getUrl()),
                sources,
                URI.create(""),
                rule.getLicenses().stream()
                        .map(License::getName)
                        .map(net.evendanan.bazel.mvn.api.Dependency.License::fromLicenseName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    private static URI uriWithClassifier(final String url, final String classifier) {
        final int extStartIndex = url.lastIndexOf(".");
        if (extStartIndex > 0) {
            try {
                URL classifiedUrl = new URL(String.format(Locale.US, "%s-%s%s", url.substring(0, extStartIndex), classifier, url.substring(extStartIndex)));
                HttpURLConnection con = (HttpURLConnection) classifiedUrl.openConnection();
                con.setRequestMethod("HEAD");
                final int responseCode = con.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    return classifiedUrl.toURI();
                } else {
                    return URI.create("");
                }
            } catch (Exception e) {
                return URI.create("");
            }
        } else {
            return URI.create("");
        }
    }
}
