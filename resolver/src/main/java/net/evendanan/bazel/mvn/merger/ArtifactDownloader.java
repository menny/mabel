package net.evendanan.bazel.mvn.merger;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import net.evendanan.bazel.mvn.api.Dependency;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ArtifactDownloader {

    private final ConnectionOpener mConnectionOpenner;
    private final File artifactsFolder;

    public ArtifactDownloader(File artifactsFolder) {
        this(URL::openStream, artifactsFolder);
    }

    @VisibleForTesting
    ArtifactDownloader(ConnectionOpener opener, File artifactsFolder) {
        mConnectionOpenner = opener;
        this.artifactsFolder = artifactsFolder;
    }

    public URI getLocalUriForDependency(Dependency dependency) throws IOException {
        final File localPath = new File(artifactsFolder, dependency.repositoryRuleName());
        //first, is the file exists locally already?
        if (localPath.exists() && localPath.isFile()) {
            return localPath.toURI();
        }

        //second, download to unique temp file
        final File tempDownloadFile = File.createTempFile("mabel_ArtifactDownloader", localPath.getName());
        try (final ReadableByteChannel readableByteChannel = Channels.newChannel(mConnectionOpenner.openInputStream(dependency.url().toURL()))) {
            try (final FileOutputStream tempOutput = new FileOutputStream(tempDownloadFile, false)) {
                System.out.print('⇵');
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
