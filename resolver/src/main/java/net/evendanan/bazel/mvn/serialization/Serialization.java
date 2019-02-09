package net.evendanan.bazel.mvn.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import net.evendanan.bazel.mvn.api.Dependency;

public class Serialization {

    private final Gson mGson;

    public Serialization() {
        mGson = new GsonBuilder()
                .registerTypeAdapter(Dependency.class, new Adapters.DependencySerializer())
                .registerTypeAdapter(Dependency.class, new Adapters.DependencyDeserializer())
                .registerTypeAdapter(Dependency.License.class, new Adapters.LicenseSerializer())
                .registerTypeAdapter(Dependency.License.class, new Adapters.LicenseDeserializer())
                .create();

    }

    public Dependency deserialize(Reader jsonInput) {
        return mGson.fromJson(jsonInput, Dependency.class);
    }

    public String serialize(Dependency dependency) {
        return mGson.toJson(dependency, Dependency.class);
    }
}
