package net.evendanan.bazel.mvn.api.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;

import java.io.Reader;

public class Serialization {

    private final Gson mGson;

    public Serialization() {
        mGson =
                new GsonBuilder()
                        .registerTypeAdapter(Resolution.class, new Adapters.ResolutionSerializer())
                        .registerTypeAdapter(
                                Resolution.class, new Adapters.ResolutionDeserializer())
                        .registerTypeAdapter(Dependency.class, new Adapters.DependencySerializer())
                        .registerTypeAdapter(
                                Dependency.class, new Adapters.DependencyDeserializer())
                        .registerTypeAdapter(
                                MavenCoordinate.class, new Adapters.MavenCoordinateSerializer())
                        .registerTypeAdapter(
                                MavenCoordinate.class, new Adapters.MavenCoordinateDeserializer())
                        .registerTypeAdapter(License.class, new Adapters.LicenseSerializer())
                        .registerTypeAdapter(License.class, new Adapters.LicenseDeserializer())
                        .create();
    }

    public Resolution deserialize(Reader jsonInput) {
        return mGson.fromJson(jsonInput, Resolution.class);
    }

    public void serialize(Resolution resolution, Appendable writer) {
        mGson.toJson(resolution, Resolution.class, writer);
    }
}
