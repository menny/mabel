package net.evendanan.bazel.mvn.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

    public List<Dependency> deserialize(String jsonString) {
        return Arrays.asList(mGson.fromJson(jsonString, Dependency[].class));
    }

    public String serialize(Collection<Dependency> dependencies) {
        return mGson.toJson(dependencies.toArray(new Dependency[0]), Dependency[].class);
    }
}
