package com.google.devtools.bazel.workspace.maven;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.building.UrlModelSource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UrlModelSource2 extends UrlModelSource implements ModelSource2 {

    public UrlModelSource2(URL pomUrl) {
        super(pomUrl);
    }

    @Override
    public ModelSource2 getRelatedSource(String relPath) {
        return null;
    }

    @Override
    public URI getLocationURI() {
        try {
            return getUrl().toURI();
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }
}
