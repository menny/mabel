package net.evendanan.bazel.mvn.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;
import net.evendanan.bazel.mvn.api.License;

class Adapters {
    private static final Type DEPENDENCIES_TYPE = new TypeToken<Collection<Dependency>>() {
    }.getType();
    private static final Type LICENSES_TYPE = new TypeToken<Collection<License>>() {
    }.getType();

    private static String getMavenKey(String group, String artifact, String version) {
        return String.format(Locale.ROOT, "%s:%s:%s", group, artifact, version);
    }

    static class DependencyDeserializer implements JsonDeserializer<Dependency> {
        private final Map<String, Dependency> cache = new HashMap<>();

        @Override
        public Dependency deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            final String groupId = jsonObject.get("groupId").getAsString();
            final String artifactId = jsonObject.get("artifactId").getAsString();
            final String version = jsonObject.get("version").getAsString();

            final String key = getMavenKey(groupId, artifactId, version);

            if (cache.containsKey(key)) {
                return cache.get(key);
            } else {
                final Dependency dependency = new Dependency(
                        groupId, artifactId, version, jsonObject.get("packaging").getAsString(),
                        context.deserialize(jsonObject.get("dependencies"), DEPENDENCIES_TYPE),
                        context.deserialize(jsonObject.get("exports"), DEPENDENCIES_TYPE),
                        context.deserialize(jsonObject.get("runtimeDependencies"), DEPENDENCIES_TYPE),
                        URI.create(jsonObject.get("url").getAsString()), URI.create(jsonObject.get("sourcesUrl").getAsString()), URI.create(jsonObject.get("javadocUrl").getAsString()),
                        context.deserialize(jsonObject.get("licenses"), LICENSES_TYPE));

                cache.put(key, dependency);

                return dependency;
            }
        }
    }

    static class DependencySerializer implements JsonSerializer<Dependency> {

        @Override
        public JsonElement serialize(final Dependency dependency, final Type type, final JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("groupId", dependency.groupId());
            jsonObject.addProperty("artifactId", dependency.artifactId());
            jsonObject.addProperty("version", dependency.version());
            jsonObject.addProperty("packaging", dependency.packaging());

            jsonObject.add("dependencies", context.serialize(dependency.dependencies(), DEPENDENCIES_TYPE));
            jsonObject.add("exports", context.serialize(dependency.exports(), DEPENDENCIES_TYPE));
            jsonObject.add("runtimeDependencies", context.serialize(dependency.runtimeDependencies(), DEPENDENCIES_TYPE));

            jsonObject.addProperty("url", dependency.url().toASCIIString());
            jsonObject.addProperty("sourcesUrl", dependency.sourcesUrl().toASCIIString());
            jsonObject.addProperty("javadocUrl", dependency.javadocUrl().toASCIIString());
            jsonObject.addProperty("javadocUrl", dependency.javadocUrl().toASCIIString());

            jsonObject.add("licenses", context.serialize(dependency.licenses(), LICENSES_TYPE));

            return jsonObject;
        }
    }

    static class LicenseDeserializer implements JsonDeserializer<License> {
        @Override
        public License deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            return License.valueOf(License.class, jsonElement.getAsString());
        }
    }

    static class LicenseSerializer implements JsonSerializer<License> {

        @Override
        public JsonElement serialize(final License license, final Type type, final JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(license.name());
        }
    }
}
