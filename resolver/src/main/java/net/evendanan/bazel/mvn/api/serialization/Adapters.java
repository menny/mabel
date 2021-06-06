package net.evendanan.bazel.mvn.api.serialization;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import net.evendanan.bazel.mvn.api.model.Dependency;
import net.evendanan.bazel.mvn.api.model.ExportsGenerationType;
import net.evendanan.bazel.mvn.api.model.License;
import net.evendanan.bazel.mvn.api.model.MavenCoordinate;
import net.evendanan.bazel.mvn.api.model.Resolution;
import net.evendanan.bazel.mvn.api.model.ResolutionOutput;
import net.evendanan.bazel.mvn.api.model.TargetType;

import java.lang.reflect.Type;
import java.util.Collection;

class Adapters {
    private static final Type DEPENDENCIES_LIST_TYPE =
            new TypeToken<Collection<Dependency>>() {
            }.getType();
    private static final Type MAVEN_LIST_TYPE =
            new TypeToken<Collection<MavenCoordinate>>() {
            }.getType();
    private static final Type LICENSES_LIST_TYPE =
            new TypeToken<Collection<License>>() {
            }.getType();

    static class ResolutionOutputDeserializer implements JsonDeserializer<ResolutionOutput> {

        @Override
        public ResolutionOutput deserialize(
                final JsonElement jsonElement,
                final Type type,
                final JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            return ResolutionOutput.create(
                    context.deserialize(jsonObject.get("t"), TargetType.class),
                    context.deserialize(jsonObject.get("e"), ExportsGenerationType.class),
                    context.deserialize(jsonObject.get("to"), boolean.class),
                    context.deserialize(jsonObject.get("r"), Resolution.class));
        }
    }

    static class ResolutionOutputSerializer implements JsonSerializer<ResolutionOutput> {

        @Override
        public JsonElement serialize(
                final ResolutionOutput resolution,
                final Type type,
                final JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.add(
                    "t", context.serialize(resolution.targetType(), TargetType.class));
            jsonObject.add(
                    "e", context.serialize(resolution.exportsGenerationType(), ExportsGenerationType.class));
            jsonObject.add(
                    "to", context.serialize(resolution.testOnly(), boolean.class));
            jsonObject.add(
                    "r",
                    context.serialize(
                            resolution.resolution(), Resolution.class));

            return jsonObject;
        }
    }

    static class ResolutionDeserializer implements JsonDeserializer<Resolution> {

        @Override
        public Resolution deserialize(
                final JsonElement jsonElement,
                final Type type,
                final JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            return Resolution.create(
                    context.deserialize(jsonObject.get("c"), MavenCoordinate.class),
                    context.deserialize(jsonObject.get("d"), DEPENDENCIES_LIST_TYPE));
        }
    }

    static class ResolutionSerializer implements JsonSerializer<Resolution> {

        @Override
        public JsonElement serialize(
                final Resolution resolution,
                final Type type,
                final JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.add(
                    "c", context.serialize(resolution.rootDependency(), MavenCoordinate.class));
            jsonObject.add(
                    "d",
                    context.serialize(
                            resolution.allResolvedDependencies(), DEPENDENCIES_LIST_TYPE));

            return jsonObject;
        }
    }

    static class DependencyDeserializer implements JsonDeserializer<Dependency> {

        @Override
        public Dependency deserialize(
                final JsonElement jsonElement,
                final Type type,
                final JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            return Dependency.builder()
                    .url(jsonObject.get("u").getAsString())
                    .sourcesUrl(jsonObject.get("sU").getAsString())
                    .javadocUrl(jsonObject.get("jU").getAsString())
                    .mavenCoordinate(
                            context.deserialize(jsonObject.get("c"), MavenCoordinate.class))
                    .dependencies(context.deserialize(jsonObject.get("d"), MAVEN_LIST_TYPE))
                    .exports(context.deserialize(jsonObject.get("e"), MAVEN_LIST_TYPE))
                    .runtimeDependencies(context.deserialize(jsonObject.get("r"), MAVEN_LIST_TYPE))
                    .licenses(context.deserialize(jsonObject.get("l"), LICENSES_LIST_TYPE))
                    .build();
        }
    }

    static class DependencySerializer implements JsonSerializer<Dependency> {

        @Override
        public JsonElement serialize(
                final Dependency dependency,
                final Type type,
                final JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("u", dependency.url());
            jsonObject.addProperty("sU", dependency.sourcesUrl());
            jsonObject.addProperty("jU", dependency.javadocUrl());

            jsonObject.add(
                    "c", context.serialize(dependency.mavenCoordinate(), MavenCoordinate.class));

            jsonObject.add("d", context.serialize(dependency.dependencies(), MAVEN_LIST_TYPE));
            jsonObject.add("e", context.serialize(dependency.exports(), MAVEN_LIST_TYPE));
            jsonObject.add(
                    "r", context.serialize(dependency.runtimeDependencies(), MAVEN_LIST_TYPE));

            jsonObject.add("l", context.serialize(dependency.licenses(), LICENSES_LIST_TYPE));

            return jsonObject;
        }
    }

    static class MavenCoordinateDeserializer implements JsonDeserializer<MavenCoordinate> {
        @Override
        public MavenCoordinate deserialize(
                final JsonElement jsonElement,
                final Type type,
                final JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            final String groupId = jsonObject.get("g").getAsString();
            final String artifactId = jsonObject.get("a").getAsString();
            final String version = jsonObject.get("v").getAsString();
            final String packaging = jsonObject.get("p").getAsString();

            return MavenCoordinate.create(groupId, artifactId, version, packaging);
        }
    }

    static class MavenCoordinateSerializer implements JsonSerializer<MavenCoordinate> {

        @Override
        public JsonElement serialize(
                final MavenCoordinate mavenCoordinate,
                final Type type,
                final JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("g", mavenCoordinate.groupId());
            jsonObject.addProperty("a", mavenCoordinate.artifactId());
            jsonObject.addProperty("v", mavenCoordinate.version());
            jsonObject.addProperty("p", mavenCoordinate.packaging());

            return jsonObject;
        }
    }

    static class LicenseDeserializer implements JsonDeserializer<License> {
        @Override
        public License deserialize(
                final JsonElement jsonElement,
                final Type type,
                final JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            final String name = jsonObject.get("n").getAsString();
            final String url = jsonObject.get("u").getAsString();

            return License.create(name, url);
        }
    }

    static class LicenseSerializer implements JsonSerializer<License> {

        @Override
        public JsonElement serialize(
                final License license,
                final Type type,
                final JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("n", license.name());
            jsonObject.addProperty("u", license.url());

            return jsonObject;
        }
    }
}
