package net.evendanan.bazel.mvn.api.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.ResolutionOutput;

public class Serialization {

  private final Gson mGson;

  public Serialization() {
    mGson =
        new GsonBuilder()
            .registerTypeAdapter(ResolutionOutput.class, new Adapters.ResolutionOutputSerializer())
            .registerTypeAdapter(
                ResolutionOutput.class, new Adapters.ResolutionOutputDeserializer())
            .registerTypeAdapter(Resolution.class, new Adapters.ResolutionSerializer())
            .registerTypeAdapter(Resolution.class, new Adapters.ResolutionDeserializer())
            .registerTypeAdapter(Dependency.class, new Adapters.DependencySerializer())
            .registerTypeAdapter(Dependency.class, new Adapters.DependencyDeserializer())
            .registerTypeAdapter(MavenCoordinate.class, new Adapters.MavenCoordinateSerializer())
            .registerTypeAdapter(MavenCoordinate.class, new Adapters.MavenCoordinateDeserializer())
            .registerTypeAdapter(License.class, new Adapters.LicenseSerializer())
            .registerTypeAdapter(License.class, new Adapters.LicenseDeserializer())
            .create();
  }

  public ResolutionOutput deserialize(Reader jsonInput) {
    return mGson.fromJson(jsonInput, ResolutionOutput.class);
  }

  public void serialize(ResolutionOutput resolution, Appendable writer) {
    mGson.toJson(resolution, ResolutionOutput.class, writer);
  }
}
