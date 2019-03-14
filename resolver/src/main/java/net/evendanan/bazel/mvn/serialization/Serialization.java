package net.evendanan.bazel.mvn.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.util.zip.ZipOutputStream;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.License;

public class Serialization {

    private static Gson create() {
        return new GsonBuilder()
                .registerTypeAdapter(Dependency.class, new Adapters.DependencySerializer())
                .registerTypeAdapter(Dependency.class, new Adapters.DependencyDeserializer())
                .registerTypeAdapter(License.class, new Adapters.LicenseSerializer())
                .registerTypeAdapter(License.class, new Adapters.LicenseDeserializer())
                .create();
    }

    public Dependency deserialize(Reader jsonInput) {
        return create().fromJson(jsonInput, Dependency.class);
    }

    public void serialize(Dependency dependency, Appendable writer) {
        create().toJson(dependency, Dependency.class, writer);
    }
}
