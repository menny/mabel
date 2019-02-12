package net.evendanan.bazel.mvn.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.evendanan.bazel.mvn.api.Dependency;

class Adapters {

    private static <T> Collection<T> asList(JsonDeserializationContext context, final JsonArray jsonArray, final Class<T> tClass) {
        ArrayList<T> list = new ArrayList<>();
        for (int elementIndex = 0; elementIndex < jsonArray.size(); elementIndex++) {
            list.add(context.deserialize(jsonArray.get(elementIndex), tClass));
        }

        return list;
    }

    private static <T> JsonArray fromList(final JsonSerializationContext context, final Collection<T> elements, final Class<T> tClass) {
        JsonArray array = new JsonArray(elements.size());
        for (final T element : elements) {
            array.add(context.serialize(element, tClass));
        }

        return array;
    }

    static class DependencyDeserializer implements JsonDeserializer<Dependency> {
        private final Map<String, Dependency> cache = new HashMap<>();
        @Override
        public Dependency deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonObject = jsonElement.getAsJsonObject();

            final String groupId = jsonObject.get("groupId").getAsString();
            final String artifactId = jsonObject.get("artifactId").getAsString();
            final String version = jsonObject.get("version").getAsString();

            final String maven = mavenKey(groupId, artifactId, version);
            if (cache.containsKey(maven)) {
                return cache.get(maven);
            } else {
                final Dependency dependency = new Dependency(
                        groupId, artifactId, version, jsonObject.get("packaging").getAsString(),
                        asList(context, jsonObject.getAsJsonArray("dependencies"), Dependency.class), asList(context, jsonObject.getAsJsonArray("exports"), Dependency.class), asList(context, jsonObject.getAsJsonArray("runtimeDependencies"), Dependency.class),
                        URI.create(jsonObject.get("url").getAsString()), URI.create(jsonObject.get("sourcesUrl").getAsString()), URI.create(jsonObject.get("javadocUrl").getAsString()),
                        asList(context, jsonObject.getAsJsonArray("licenses"), Dependency.License.class));
                cache.put(maven, dependency);
                return dependency;
            }
        }

        private String mavenKey(final String groupId, final String artifactId, final String version) {
            return String.format(Locale.ROOT, "%s:%s:%s", groupId, artifactId, version);
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

            jsonObject.add("dependencies", fromList(context, dependency.dependencies(), Dependency.class));
            jsonObject.add("exports", fromList(context, dependency.exports(), Dependency.class));
            jsonObject.add("runtimeDependencies", fromList(context, dependency.runtimeDependencies(), Dependency.class));

            jsonObject.addProperty("url", dependency.url().toASCIIString());
            jsonObject.addProperty("sourcesUrl", dependency.sourcesUrl().toASCIIString());
            jsonObject.addProperty("javadocUrl", dependency.javadocUrl().toASCIIString());
            jsonObject.addProperty("javadocUrl", dependency.javadocUrl().toASCIIString());

            jsonObject.add("licenses", fromList(context, dependency.licenses(), Dependency.License.class));

            return jsonObject;
        }
    }

    static class LicenseDeserializer implements JsonDeserializer<Dependency.License> {
        @Override
        public Dependency.License deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {
            return Dependency.License.valueOf(Dependency.License.class, jsonElement.getAsString());
        }
    }

    static class LicenseSerializer implements JsonSerializer<Dependency.License> {

        @Override
        public JsonElement serialize(final Dependency.License license, final Type type, final JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(license.name());
        }
    }
}
