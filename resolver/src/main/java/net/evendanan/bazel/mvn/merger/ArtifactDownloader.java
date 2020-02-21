package net.evendanan.bazel.mvn.merger;

import com.google.common.annotations.VisibleForTesting;
import net.evendanan.bazel.mvn.api.DependencyTools;
import net.evendanan.bazel.mvn.api.model.Dependency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ArtifactDownloader {

    private final ConnectionOpener mConnectionOpener;
    private final File mArtifactsFolder;
    private final DependencyTools mDependencyTools;

    public ArtifactDownloader(File artifactsFolder, DependencyTools dependencyTools) {
        this(URL::openStream, artifactsFolder, dependencyTools);
    }

    @VisibleForTesting
    ArtifactDownloader(ConnectionOpener opener, File artifactsFolder, DependencyTools dependencyTools) {
        mConnectionOpener = opener;
        mArtifactsFolder = artifactsFolder;
        mDependencyTools = dependencyTools;
    }

    public URI getLocalUriForDependency(Dependency dependency) throws IOException {
        final File localPath = new File(mArtifactsFolder, mDependencyTools.repositoryRuleName(dependency));
        //first, is the file exists locally already?
        if (localPath.exists() && localPath.isFile()) {
            return localPath.toURI();
        }

        //second, download to unique temp file
        final File tempDownloadFile = File.createTempFile("mabel_ArtifactDownloader", localPath.getName());
        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(mConnectionOpener.openInputStream(new URL(dependency.url())))) {
            try (final FileOutputStream tempOutput = new FileOutputStream(tempDownloadFile, false)) {
                tempOutput.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        }

        //third, rename the temp file to localPath
        Files.move(tempDownloadFile.toPath(), localPath.toPath(), REPLACE_EXISTING);

        //done
        return localPath.toURI();
    }

    interface ConnectionOpener {
        InputStream openInputStream(URL url) throws IOException;
    }
}
