# The following dependencies were calculated from:
#
# generate_workspace --repository=https://maven.google.com/ --repository=https://jcenter.bintray.com/ --repository=https://repo1.maven.org/maven2/ --artifact=com.google.guava:guava:20.0 --artifact=com.beust:jcommander:1.72 --artifact=org.eclipse.aether:aether-api:1.1.0 --artifact=org.eclipse.aether:aether-spi:1.1.0 --artifact=org.eclipse.aether:aether-impl:1.1.0 --artifact=org.eclipse.aether:aether-util:1.1.0 --artifact=org.eclipse.aether:aether-transport-http:1.1.0 --artifact=org.eclipse.aether:aether-transport-classpath:1.1.0 --artifact=org.eclipse.aether:aether-transport-wagon:1.1.0 --artifact=org.eclipse.aether:aether-transport-file:1.1.0 --artifact=org.eclipse.aether:aether-connector-basic:1.1.0 --artifact=org.apache.maven:maven-aether-provider:3.2.3 --artifact=org.apache.maven:maven-model:3.2.3 --artifact=org.apache.maven:maven-model-builder:3.2.3 --artifact=org.apache.maven:maven-repository-metadata:3.2.3 --artifact=org.apache.maven:maven-artifact:3.5.0 --artifact=org.codehaus.plexus:plexus-interpolation:1.24 --artifact=org.codehaus.plexus:plexus-utils:3.0.24 --artifact=org.apache.httpcomponents:httpclient:4.5.3 --artifact=org.apache.commons:commons-lang3:jar:3.8.1 --artifact=com.google.code.findbugs:jsr305:3.0.2 --artifact=junit:junit:4.12 --artifact=org.mockito:mockito-core:2.23.4 --artifact=com.google.auto.value:auto-value:1.6.3 --rule_prefix=migration_tools --macro_prefix=migration_tools


# Loading a drop-in replacement for native.http_file
load('@bazel_tools//tools/build_defs/repo:http.bzl', 'http_file')

# Repository rules macro to be run in the WORKSPACE file.
def generate_migration_tools_workspace_rules():
    # org.apache.httpcomponents:httpclient:jar:4.5.3
    http_file(
        name = "migration_tools___org_apache_httpcomponents__httpcore",
        urls = ["https://jcenter.bintray.com/org/apache/httpcomponents/httpcore/4.4.6/httpcore-4.4.6.jar"],
        downloaded_file_path = "httpcore-4.4.6.jar",
        )

    http_file(
        name = "migration_tools___org_eclipse_aether__aether_transport_wagon",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-transport-wagon/1.1.0/aether-transport-wagon-1.1.0.jar"],
        downloaded_file_path = "aether-transport-wagon-1.1.0.jar",
        )

    # org.eclipse.aether:aether-transport-http:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-classpath:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-impl:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-connector-basic:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-file:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-wagon:jar:1.1.0 got requested version
    # org.apache.maven:maven-aether-provider:jar:3.2.3 wanted version 0.9.0.M2
    http_file(
        name = "migration_tools___org_eclipse_aether__aether_util",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-util/1.1.0/aether-util-1.1.0.jar"],
        downloaded_file_path = "aether-util-1.1.0.jar",
        )

    http_file(
        name = "migration_tools___org_mockito__mockito_core",
        urls = ["https://jcenter.bintray.com/org/mockito/mockito-core/2.23.4/mockito-core-2.23.4.jar"],
        downloaded_file_path = "mockito-core-2.23.4.jar",
        )

    # com.kohlschutter.junixsocket:junixsocket-native-common:jar:2.0.4
    http_file(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket_common",
        urls = ["https://jcenter.bintray.com/com/kohlschutter/junixsocket/junixsocket-common/2.0.4/junixsocket-common-2.0.4.jar"],
        downloaded_file_path = "junixsocket-common-2.0.4.jar",
        )

    # org.apache.maven:maven-aether-provider:jar:3.2.3 got requested version
    # org.apache.maven:maven-model-builder:jar:3.2.3 got requested version
    http_file(
        name = "migration_tools___org_apache_maven__maven_model",
        urls = ["https://jcenter.bintray.com/org/apache/maven/maven-model/3.2.3/maven-model-3.2.3.jar"],
        downloaded_file_path = "maven-model-3.2.3.jar",
        )

    # org.apache.maven:maven-aether-provider:jar:3.2.3 got requested version
    http_file(
        name = "migration_tools___org_apache_maven__maven_model_builder",
        urls = ["https://jcenter.bintray.com/org/apache/maven/maven-model-builder/3.2.3/maven-model-builder-3.2.3.jar"],
        downloaded_file_path = "maven-model-builder-3.2.3.jar",
        )

    http_file(
        name = "migration_tools___org_eclipse_aether__aether_transport_http",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-transport-http/1.1.0/aether-transport-http-1.1.0.jar"],
        downloaded_file_path = "aether-transport-http-1.1.0.jar",
        )

    # org.apache.maven:maven-aether-provider:jar:3.2.3 got requested version
    http_file(
        name = "migration_tools___org_apache_maven__maven_repository_metadata",
        urls = ["https://jcenter.bintray.com/org/apache/maven/maven-repository-metadata/3.2.3/maven-repository-metadata-3.2.3.jar"],
        downloaded_file_path = "maven-repository-metadata-3.2.3.jar",
        )

    http_file(
        name = "migration_tools___org_apache_maven__maven_aether_provider",
        urls = ["https://jcenter.bintray.com/org/apache/maven/maven-aether-provider/3.2.3/maven-aether-provider-3.2.3.jar"],
        downloaded_file_path = "maven-aether-provider-3.2.3.jar",
        )

    # com.kohlschutter.junixsocket:junixsocket-native-common:jar:2.0.4
    http_file(
        name = "migration_tools___org_scijava__native_lib_loader",
        urls = ["https://jcenter.bintray.com/org/scijava/native-lib-loader/2.0.2/native-lib-loader-2.0.2.jar"],
        downloaded_file_path = "native-lib-loader-2.0.2.jar",
        )

    # org.eclipse.aether:aether-transport-http:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-spi:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-classpath:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-impl:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-util:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-connector-basic:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-file:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-wagon:jar:1.1.0 got requested version
    # org.apache.maven:maven-aether-provider:jar:3.2.3 wanted version 0.9.0.M2
    http_file(
        name = "migration_tools___org_eclipse_aether__aether_api",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-api/1.1.0/aether-api-1.1.0.jar"],
        downloaded_file_path = "aether-api-1.1.0.jar",
        )

    # junit:junit:jar:4.12
    http_file(
        name = "migration_tools___org_hamcrest__hamcrest_core",
        urls = ["https://jcenter.bintray.com/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"],
        downloaded_file_path = "hamcrest-core-1.3.jar",
        )

    http_file(
        name = "migration_tools___org_apache_maven__maven_artifact",
        urls = ["https://jcenter.bintray.com/org/apache/maven/maven-artifact/3.5.0/maven-artifact-3.5.0.jar"],
        downloaded_file_path = "maven-artifact-3.5.0.jar",
        )

    # org.apache.maven:maven-artifact:jar:3.5.0 wanted version 3.5
    http_file(
        name = "migration_tools___org_apache_commons__commons_lang3",
        urls = ["https://jcenter.bintray.com/org/apache/commons/commons-lang3/3.8.1/commons-lang3-3.8.1.jar"],
        downloaded_file_path = "commons-lang3-3.8.1.jar",
        )

    # com.google.auto.value:auto-value:jar:1.6.3
    http_file(
        name = "migration_tools___net_ltgt_gradle_incap__incap_processor",
        urls = ["https://jcenter.bintray.com/net/ltgt/gradle/incap/incap-processor/0.2/incap-processor-0.2.jar"],
        downloaded_file_path = "incap-processor-0.2.jar",
        )

    # log4j:log4j:bundle:1.2.17
    http_file(
        name = "migration_tools___org_apache_openejb__javaee_api",
        urls = ["https://jcenter.bintray.com/org/apache/openejb/javaee-api/5.0-2/javaee-api-5.0-2.jar"],
        downloaded_file_path = "javaee-api-5.0-2.jar",
        )

    # com.google.auto.value:auto-value:jar:1.6.3
    http_file(
        name = "migration_tools___com_google_auto_service__auto_service",
        urls = ["https://jcenter.bintray.com/com/google/auto/service/auto-service/1.0-rc4/auto-service-1.0-rc4.jar"],
        downloaded_file_path = "auto-service-1.0-rc4.jar",
        )

    http_file(
        name = "migration_tools___com_beust__jcommander",
        urls = ["https://jcenter.bintray.com/com/beust/jcommander/1.72/jcommander-1.72.jar"],
        downloaded_file_path = "jcommander-1.72.jar",
        )

    # org.apache.httpcomponents:httpclient:jar:4.5.3
    http_file(
        name = "migration_tools___commons_logging__commons_logging",
        urls = ["https://jcenter.bintray.com/commons-logging/commons-logging/1.2/commons-logging-1.2.jar"],
        downloaded_file_path = "commons-logging-1.2.jar",
        )

    # org.apache.maven:maven-model-builder:jar:3.2.3 wanted version 1.19
    http_file(
        name = "migration_tools___org_codehaus_plexus__plexus_interpolation",
        urls = ["https://jcenter.bintray.com/org/codehaus/plexus/plexus-interpolation/1.24/plexus-interpolation-1.24.jar"],
        downloaded_file_path = "plexus-interpolation-1.24.jar",
        )

    # com.kohlschutter.junixsocket:junixsocket-common:jar:2.0.4
    # com.kohlschutter.junixsocket:junixsocket-native-common:jar:2.0.4 got requested version
    http_file(
        name = "migration_tools___log4j__log4j",
        urls = ["https://jcenter.bintray.com/log4j/log4j/1.2.17/log4j-1.2.17.jar"],
        downloaded_file_path = "log4j-1.2.17.jar",
        )

    http_file(
        name = "migration_tools___org_eclipse_aether__aether_connector_basic",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-connector-basic/1.1.0/aether-connector-basic-1.1.0.jar"],
        downloaded_file_path = "aether-connector-basic-1.1.0.jar",
        )

    # org.eclipse.aether:aether-transport-http:jar:1.1.0
    http_file(
        name = "migration_tools___org_slf4j__jcl_over_slf4j",
        urls = ["https://jcenter.bintray.com/org/slf4j/jcl-over-slf4j/1.6.2/jcl-over-slf4j-1.6.2.jar"],
        downloaded_file_path = "jcl-over-slf4j-1.6.2.jar",
        )

    # net.ltgt.gradle.incap:incap-processor:jar:0.2
    http_file(
        name = "migration_tools___net_ltgt_gradle_incap__incap",
        urls = ["https://jcenter.bintray.com/net/ltgt/gradle/incap/incap/0.2/incap-0.2.jar"],
        downloaded_file_path = "incap-0.2.jar",
        )

    http_file(
        name = "migration_tools___com_google_auto_value__auto_value",
        urls = ["https://jcenter.bintray.com/com/google/auto/value/auto-value/1.6.3/auto-value-1.6.3.jar"],
        downloaded_file_path = "auto-value-1.6.3.jar",
        )

    # org.mockito:mockito-core:jar:2.23.4
    http_file(
        name = "migration_tools___net_bytebuddy__byte_buddy",
        urls = ["https://jcenter.bintray.com/net/bytebuddy/byte-buddy/1.9.3/byte-buddy-1.9.3.jar"],
        downloaded_file_path = "byte-buddy-1.9.3.jar",
        )

    # org.apache.maven:maven-aether-provider:jar:3.2.3 wanted version 0.9.0.M2
    http_file(
        name = "migration_tools___org_eclipse_aether__aether_impl",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-impl/1.1.0/aether-impl-1.1.0.jar"],
        downloaded_file_path = "aether-impl-1.1.0.jar",
        )

    # org.mockito:mockito-core:jar:2.23.4
    http_file(
        name = "migration_tools___org_objenesis__objenesis",
        urls = ["https://jcenter.bintray.com/org/objenesis/objenesis/2.6/objenesis-2.6.jar"],
        downloaded_file_path = "objenesis-2.6.jar",
        )

    http_file(
        name = "migration_tools___com_google_code_findbugs__jsr305",
        urls = ["https://jcenter.bintray.com/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar"],
        downloaded_file_path = "jsr305-3.0.2.jar",
        )

    # com.google.auto:auto-common:jar:0.8 wanted version 19.0
    # com.google.auto.service:auto-service:jar:1.0-rc4 wanted version 23.5-jre
    http_file(
        name = "migration_tools___com_google_guava__guava",
        urls = ["https://jcenter.bintray.com/com/google/guava/guava/20.0/guava-20.0.jar"],
        downloaded_file_path = "guava-20.0.jar",
        )

    # org.apache.maven:maven-model-builder:jar:3.2.3 wanted version 3.0.17
    # org.apache.maven.wagon:wagon-provider-api:jar:1.0 wanted version 1.4.2
    # org.apache.maven:maven-aether-provider:jar:3.2.3 wanted version 3.0.17
    # org.apache.maven:maven-model:jar:3.2.3 wanted version 3.0.17
    # org.apache.maven:maven-artifact:jar:3.5.0 got requested version
    # org.apache.maven:maven-repository-metadata:jar:3.2.3 wanted version 3.0.17
    http_file(
        name = "migration_tools___org_codehaus_plexus__plexus_utils",
        urls = ["https://jcenter.bintray.com/org/codehaus/plexus/plexus-utils/3.0.24/plexus-utils-3.0.24.jar"],
        downloaded_file_path = "plexus-utils-3.0.24.jar",
        )

    # org.slf4j:jcl-over-slf4j:jar:1.6.2
    http_file(
        name = "migration_tools___org_slf4j__slf4j_api",
        urls = ["https://jcenter.bintray.com/org/slf4j/slf4j-api/1.6.2/slf4j-api-1.6.2.jar"],
        downloaded_file_path = "slf4j-api-1.6.2.jar",
        )

    # org.eclipse.aether:aether-transport-wagon:jar:1.1.0
    http_file(
        name = "migration_tools___org_apache_maven_wagon__wagon_provider_api",
        urls = ["https://jcenter.bintray.com/org/apache/maven/wagon/wagon-provider-api/1.0/wagon-provider-api-1.0.jar"],
        downloaded_file_path = "wagon-provider-api-1.0.jar",
        )

    # net.bytebuddy:byte-buddy-agent:jar:1.9.3 got requested version
    # net.bytebuddy:byte-buddy:jar:1.9.3
    http_file(
        name = "migration_tools___com_google_code_findbugs__findbugs_annotations",
        urls = ["https://jcenter.bintray.com/com/google/code/findbugs/findbugs-annotations/3.0.1/findbugs-annotations-3.0.1.jar"],
        downloaded_file_path = "findbugs-annotations-3.0.1.jar",
        )

    # org.mockito:mockito-core:jar:2.23.4
    http_file(
        name = "migration_tools___net_bytebuddy__byte_buddy_agent",
        urls = ["https://jcenter.bintray.com/net/bytebuddy/byte-buddy-agent/1.9.3/byte-buddy-agent-1.9.3.jar"],
        downloaded_file_path = "byte-buddy-agent-1.9.3.jar",
        )

    # org.apache.maven:maven-model-builder:jar:3.2.3 got requested version
    # org.apache.maven:maven-aether-provider:jar:3.2.3
    http_file(
        name = "migration_tools___org_codehaus_plexus__plexus_component_annotations",
        urls = ["https://jcenter.bintray.com/org/codehaus/plexus/plexus-component-annotations/1.5.5/plexus-component-annotations-1.5.5.jar"],
        downloaded_file_path = "plexus-component-annotations-1.5.5.jar",
        )

    http_file(
        name = "migration_tools___org_eclipse_aether__aether_transport_classpath",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-transport-classpath/1.1.0/aether-transport-classpath-1.1.0.jar"],
        downloaded_file_path = "aether-transport-classpath-1.1.0.jar",
        )

    http_file(
        name = "migration_tools___org_eclipse_aether__aether_transport_file",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-transport-file/1.1.0/aether-transport-file-1.1.0.jar"],
        downloaded_file_path = "aether-transport-file-1.1.0.jar",
        )

    http_file(
        name = "migration_tools___junit__junit",
        urls = ["https://jcenter.bintray.com/junit/junit/4.12/junit-4.12.jar"],
        downloaded_file_path = "junit-4.12.jar",
        )

    # com.google.auto.service:auto-service:jar:1.0-rc4
    http_file(
        name = "migration_tools___com_google_auto__auto_common",
        urls = ["https://jcenter.bintray.com/com/google/auto/auto-common/0.8/auto-common-0.8.jar"],
        downloaded_file_path = "auto-common-0.8.jar",
        )

    # org.apache.httpcomponents:httpclient:jar:4.5.3
    http_file(
        name = "migration_tools___commons_codec__commons_codec",
        urls = ["https://jcenter.bintray.com/commons-codec/commons-codec/1.9/commons-codec-1.9.jar"],
        downloaded_file_path = "commons-codec-1.9.jar",
        )

    # net.bytebuddy:byte-buddy-agent:jar:1.9.3
    http_file(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket_native_common",
        urls = ["https://jcenter.bintray.com/com/kohlschutter/junixsocket/junixsocket-native-common/2.0.4/junixsocket-native-common-2.0.4.jar"],
        downloaded_file_path = "junixsocket-native-common-2.0.4.jar",
        )

    # org.eclipse.aether:aether-transport-http:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-classpath:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-impl:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-connector-basic:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-file:jar:1.1.0 got requested version
    # org.eclipse.aether:aether-transport-wagon:jar:1.1.0 got requested version
    # org.apache.maven:maven-aether-provider:jar:3.2.3 wanted version 0.9.0.M2
    http_file(
        name = "migration_tools___org_eclipse_aether__aether_spi",
        urls = ["https://jcenter.bintray.com/org/eclipse/aether/aether-spi/1.1.0/aether-spi-1.1.0.jar"],
        downloaded_file_path = "aether-spi-1.1.0.jar",
        )

    # org.eclipse.aether:aether-transport-http:jar:1.1.0 wanted version 4.3.5
    http_file(
        name = "migration_tools___org_apache_httpcomponents__httpclient",
        urls = ["https://jcenter.bintray.com/org/apache/httpcomponents/httpclient/4.5.3/httpclient-4.5.3.jar"],
        downloaded_file_path = "httpclient-4.5.3.jar",
        )


# Transitive rules macro to be run in the BUILD.bazel file.
def generate_migration_tools_transitive_dependency_rules():
    native.java_import(
        name = "migration_tools___org_apache_httpcomponents__httpcore__4_4_6",
        jars = ["@migration_tools___org_apache_httpcomponents__httpcore//file"],
    )
    native.alias(
        name = "migration_tools___org_apache_httpcomponents__httpcore",
        actual = "migration_tools___org_apache_httpcomponents__httpcore__4_4_6",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-transport-wagon__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_transport_wagon//file"],
        deps = [
            ":migration_tools___org_apache_maven_wagon__wagon_provider_api",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_apache_maven_wagon__wagon_provider_api",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_transport_wagon",
        actual = "migration_tools___org_eclipse_aether__aether-transport-wagon__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-util__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_util//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_util",
        actual = "migration_tools___org_eclipse_aether__aether-util__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_mockito__mockito-core__2_23_4",
        jars = ["@migration_tools___org_mockito__mockito_core//file"],
        deps = [
            ":migration_tools___net_bytebuddy__byte_buddy",
            ":migration_tools___net_bytebuddy__byte_buddy_agent",
            ":migration_tools___org_objenesis__objenesis",
        ],
        exports = [
            ":migration_tools___net_bytebuddy__byte_buddy",
            ":migration_tools___net_bytebuddy__byte_buddy_agent",
            ":migration_tools___org_objenesis__objenesis",
        ],
    )
    native.alias(
        name = "migration_tools___org_mockito__mockito_core",
        actual = "migration_tools___org_mockito__mockito-core__2_23_4",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket-common__2_0_4",
        jars = ["@migration_tools___com_kohlschutter_junixsocket__junixsocket_common//file"],
        deps = [
            ":migration_tools___log4j__log4j",
        ],
        exports = [
            ":migration_tools___log4j__log4j",
        ],
    )
    native.alias(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket_common",
        actual = "migration_tools___com_kohlschutter_junixsocket__junixsocket-common__2_0_4",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven__maven-model__3_2_3",
        jars = ["@migration_tools___org_apache_maven__maven_model//file"],
        deps = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
        exports = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven__maven_model",
        actual = "migration_tools___org_apache_maven__maven-model__3_2_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven__maven-model-builder__3_2_3",
        jars = ["@migration_tools___org_apache_maven__maven_model_builder//file"],
        deps = [
            ":migration_tools___org_apache_maven__maven_model",
            ":migration_tools___org_codehaus_plexus__plexus_component_annotations",
            ":migration_tools___org_codehaus_plexus__plexus_interpolation",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
        exports = [
            ":migration_tools___org_apache_maven__maven_model",
            ":migration_tools___org_codehaus_plexus__plexus_component_annotations",
            ":migration_tools___org_codehaus_plexus__plexus_interpolation",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven__maven_model_builder",
        actual = "migration_tools___org_apache_maven__maven-model-builder__3_2_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-transport-http__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_transport_http//file"],
        deps = [
            ":migration_tools___org_apache_httpcomponents__httpclient",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
            ":migration_tools___org_slf4j__jcl_over_slf4j",
        ],
        exports = [
            ":migration_tools___org_apache_httpcomponents__httpclient",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
            ":migration_tools___org_slf4j__jcl_over_slf4j",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_transport_http",
        actual = "migration_tools___org_eclipse_aether__aether-transport-http__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven__maven-repository-metadata__3_2_3",
        jars = ["@migration_tools___org_apache_maven__maven_repository_metadata//file"],
        deps = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
        exports = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven__maven_repository_metadata",
        actual = "migration_tools___org_apache_maven__maven-repository-metadata__3_2_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven__maven-aether-provider__3_2_3",
        jars = ["@migration_tools___org_apache_maven__maven_aether_provider//file"],
        deps = [
            ":migration_tools___org_apache_maven__maven_model",
            ":migration_tools___org_apache_maven__maven_model_builder",
            ":migration_tools___org_apache_maven__maven_repository_metadata",
            ":migration_tools___org_codehaus_plexus__plexus_component_annotations",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_impl",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_apache_maven__maven_model",
            ":migration_tools___org_apache_maven__maven_model_builder",
            ":migration_tools___org_apache_maven__maven_repository_metadata",
            ":migration_tools___org_codehaus_plexus__plexus_component_annotations",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_impl",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven__maven_aether_provider",
        actual = "migration_tools___org_apache_maven__maven-aether-provider__3_2_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_scijava__native-lib-loader__2_0_2",
        jars = ["@migration_tools___org_scijava__native_lib_loader//file"],
    )
    native.alias(
        name = "migration_tools___org_scijava__native_lib_loader",
        actual = "migration_tools___org_scijava__native-lib-loader__2_0_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-api__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_api//file"],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_api",
        actual = "migration_tools___org_eclipse_aether__aether-api__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_hamcrest__hamcrest-core__1_3",
        jars = ["@migration_tools___org_hamcrest__hamcrest_core//file"],
    )
    native.alias(
        name = "migration_tools___org_hamcrest__hamcrest_core",
        actual = "migration_tools___org_hamcrest__hamcrest-core__1_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven__maven-artifact__3_5_0",
        jars = ["@migration_tools___org_apache_maven__maven_artifact//file"],
        deps = [
            ":migration_tools___org_apache_commons__commons_lang3",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
        exports = [
            ":migration_tools___org_apache_commons__commons_lang3",
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven__maven_artifact",
        actual = "migration_tools___org_apache_maven__maven-artifact__3_5_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_commons__commons-lang3__3_8_1",
        jars = ["@migration_tools___org_apache_commons__commons_lang3//file"],
    )
    native.alias(
        name = "migration_tools___org_apache_commons__commons_lang3",
        actual = "migration_tools___org_apache_commons__commons-lang3__3_8_1",
        visibility = ["//visibility:public"],
    )



    native.java_plugin(
        name = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2_0",
        processor_class = "net.ltgt.gradle.incap.processor.IncrementalAnnotationProcessorProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___net_ltgt_gradle_incap__incap",
        ],
        )
    native.java_plugin(
        name = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2_generate_api_0",
        processor_class = "net.ltgt.gradle.incap.processor.IncrementalAnnotationProcessorProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___net_ltgt_gradle_incap__incap",
        ],
        )
    native.java_plugin(
        name = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2",
        generates_api = 0,
        deps = [
            ":migration_tools___net_ltgt_gradle_incap__incap",
        ],
        plugins = [
            "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2_0",
        ],
        )
    native.java_plugin(
        name = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2_generate_api",
        generates_api = 1,
        deps = [
            ":migration_tools___net_ltgt_gradle_incap__incap",
        ],
        plugins = [
            "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2_generate_api_0",
        ],
        )
    native.alias(
        name = "migration_tools___net_ltgt_gradle_incap__incap_processor",
        actual = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2",
        visibility = ["//visibility:public"],
    )

    native.alias(
        name = "migration_tools___net_ltgt_gradle_incap__incap_processor_generate_api",
        actual = "migration_tools___net_ltgt_gradle_incap__incap-processor__0_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_openejb__javaee-api__5_0-2",
        jars = ["@migration_tools___org_apache_openejb__javaee_api//file"],
    )
    native.alias(
        name = "migration_tools___org_apache_openejb__javaee_api",
        actual = "migration_tools___org_apache_openejb__javaee-api__5_0-2",
        visibility = ["//visibility:public"],
    )



    native.java_plugin(
        name = "migration_tools___com_google_auto_service__auto-service__1_0-rc4_0",
        processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto__auto_common",
            ":migration_tools___com_google_guava__guava",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_service__auto-service__1_0-rc4_generate_api_0",
        processor_class = "com.google.auto.service.processor.AutoServiceProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto__auto_common",
            ":migration_tools___com_google_guava__guava",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_service__auto-service__1_0-rc4",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto__auto_common",
            ":migration_tools___com_google_guava__guava",
        ],
        plugins = [
            "migration_tools___com_google_auto_service__auto-service__1_0-rc4_0",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_service__auto-service__1_0-rc4_generate_api",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto__auto_common",
            ":migration_tools___com_google_guava__guava",
        ],
        plugins = [
            "migration_tools___com_google_auto_service__auto-service__1_0-rc4_generate_api_0",
        ],
        )
    native.alias(
        name = "migration_tools___com_google_auto_service__auto_service",
        actual = "migration_tools___com_google_auto_service__auto-service__1_0-rc4",
        visibility = ["//visibility:public"],
    )

    native.alias(
        name = "migration_tools___com_google_auto_service__auto_service_generate_api",
        actual = "migration_tools___com_google_auto_service__auto-service__1_0-rc4",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_beust__jcommander__1_72",
        jars = ["@migration_tools___com_beust__jcommander//file"],
    )
    native.alias(
        name = "migration_tools___com_beust__jcommander",
        actual = "migration_tools___com_beust__jcommander__1_72",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___commons-logging__commons-logging__1_2",
        jars = ["@migration_tools___commons_logging__commons_logging//file"],
    )
    native.alias(
        name = "migration_tools___commons_logging__commons_logging",
        actual = "migration_tools___commons-logging__commons-logging__1_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_codehaus_plexus__plexus-interpolation__1_24",
        jars = ["@migration_tools___org_codehaus_plexus__plexus_interpolation//file"],
    )
    native.alias(
        name = "migration_tools___org_codehaus_plexus__plexus_interpolation",
        actual = "migration_tools___org_codehaus_plexus__plexus-interpolation__1_24",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___log4j__log4j__1_2_17",
        jars = ["@migration_tools___log4j__log4j//file"],
        deps = [
            ":migration_tools___org_apache_openejb__javaee_api",
        ],
    )
    native.alias(
        name = "migration_tools___log4j__log4j",
        actual = "migration_tools___log4j__log4j__1_2_17",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-connector-basic__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_connector_basic//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_connector_basic",
        actual = "migration_tools___org_eclipse_aether__aether-connector-basic__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_slf4j__jcl-over-slf4j__1_6_2",
        jars = ["@migration_tools___org_slf4j__jcl_over_slf4j//file"],
        deps = [
            ":migration_tools___org_slf4j__slf4j_api",
        ],
        exports = [
            ":migration_tools___org_slf4j__slf4j_api",
        ],
    )
    native.alias(
        name = "migration_tools___org_slf4j__jcl_over_slf4j",
        actual = "migration_tools___org_slf4j__jcl-over-slf4j__1_6_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___net_ltgt_gradle_incap__incap__0_2",
        jars = ["@migration_tools___net_ltgt_gradle_incap__incap//file"],
    )
    native.alias(
        name = "migration_tools___net_ltgt_gradle_incap__incap",
        actual = "migration_tools___net_ltgt_gradle_incap__incap__0_2",
        visibility = ["//visibility:public"],
    )



    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_0",
        processor_class = "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_0",
        processor_class = "com.google.auto.value.extension.memoized.processor.MemoizedValidator",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_1",
        processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_1",
        processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_2",
        processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_2",
        processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_3",
        processor_class = "com.google.auto.value.processor.AutoValueBuilderProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_3",
        processor_class = "com.google.auto.value.processor.AutoValueBuilderProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_4",
        processor_class = "com.google.auto.value.processor.AutoValueProcessor",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_4",
        processor_class = "com.google.auto.value.processor.AutoValueProcessor",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3",
        generates_api = 0,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        plugins = [
            "migration_tools___com_google_auto_value__auto-value__1_6_3_0",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_1",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_2",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_3",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_4",
        ],
        )
    native.java_plugin(
        name = "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api",
        generates_api = 1,
        deps = [
            ":migration_tools___com_google_auto_service__auto_service",
            ":migration_tools___net_ltgt_gradle_incap__incap_processor",
        ],
        plugins = [
            "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_0",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_1",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_2",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_3",
            "migration_tools___com_google_auto_value__auto-value__1_6_3_generate_api_4",
        ],
        )
    native.alias(
        name = "migration_tools___com_google_auto_value__auto_value",
        actual = "migration_tools___com_google_auto_value__auto-value__1_6_3",
        visibility = ["//visibility:public"],
    )

    native.alias(
        name = "migration_tools___com_google_auto_value__auto_value_generate_api",
        actual = "migration_tools___com_google_auto_value__auto-value__1_6_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___net_bytebuddy__byte-buddy__1_9_3",
        jars = ["@migration_tools___net_bytebuddy__byte_buddy//file"],
        deps = [
            ":migration_tools___com_google_code_findbugs__findbugs_annotations",
        ],
    )
    native.alias(
        name = "migration_tools___net_bytebuddy__byte_buddy",
        actual = "migration_tools___net_bytebuddy__byte-buddy__1_9_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-impl__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_impl//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_impl",
        actual = "migration_tools___org_eclipse_aether__aether-impl__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_objenesis__objenesis__2_6",
        jars = ["@migration_tools___org_objenesis__objenesis//file"],
    )
    native.alias(
        name = "migration_tools___org_objenesis__objenesis",
        actual = "migration_tools___org_objenesis__objenesis__2_6",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_google_code_findbugs__jsr305__3_0_2",
        jars = ["@migration_tools___com_google_code_findbugs__jsr305//file"],
    )
    native.alias(
        name = "migration_tools___com_google_code_findbugs__jsr305",
        actual = "migration_tools___com_google_code_findbugs__jsr305__3_0_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_google_guava__guava__20_0",
        jars = ["@migration_tools___com_google_guava__guava//file"],
    )
    native.alias(
        name = "migration_tools___com_google_guava__guava",
        actual = "migration_tools___com_google_guava__guava__20_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_codehaus_plexus__plexus-utils__3_0_24",
        jars = ["@migration_tools___org_codehaus_plexus__plexus_utils//file"],
    )
    native.alias(
        name = "migration_tools___org_codehaus_plexus__plexus_utils",
        actual = "migration_tools___org_codehaus_plexus__plexus-utils__3_0_24",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_slf4j__slf4j-api__1_6_2",
        jars = ["@migration_tools___org_slf4j__slf4j_api//file"],
    )
    native.alias(
        name = "migration_tools___org_slf4j__slf4j_api",
        actual = "migration_tools___org_slf4j__slf4j-api__1_6_2",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_maven_wagon__wagon-provider-api__1_0",
        jars = ["@migration_tools___org_apache_maven_wagon__wagon_provider_api//file"],
        deps = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
        exports = [
            ":migration_tools___org_codehaus_plexus__plexus_utils",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_maven_wagon__wagon_provider_api",
        actual = "migration_tools___org_apache_maven_wagon__wagon-provider-api__1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_google_code_findbugs__findbugs-annotations__3_0_1",
        jars = ["@migration_tools___com_google_code_findbugs__findbugs_annotations//file"],
    )
    native.alias(
        name = "migration_tools___com_google_code_findbugs__findbugs_annotations",
        actual = "migration_tools___com_google_code_findbugs__findbugs-annotations__3_0_1",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___net_bytebuddy__byte-buddy-agent__1_9_3",
        jars = ["@migration_tools___net_bytebuddy__byte_buddy_agent//file"],
        deps = [
            ":migration_tools___com_google_code_findbugs__findbugs_annotations",
            ":migration_tools___com_kohlschutter_junixsocket__junixsocket_native_common",
        ],
    )
    native.alias(
        name = "migration_tools___net_bytebuddy__byte_buddy_agent",
        actual = "migration_tools___net_bytebuddy__byte-buddy-agent__1_9_3",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_codehaus_plexus__plexus-component-annotations__1_5_5",
        jars = ["@migration_tools___org_codehaus_plexus__plexus_component_annotations//file"],
    )
    native.alias(
        name = "migration_tools___org_codehaus_plexus__plexus_component_annotations",
        actual = "migration_tools___org_codehaus_plexus__plexus-component-annotations__1_5_5",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-transport-classpath__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_transport_classpath//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_transport_classpath",
        actual = "migration_tools___org_eclipse_aether__aether-transport-classpath__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-transport-file__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_transport_file//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
            ":migration_tools___org_eclipse_aether__aether_spi",
            ":migration_tools___org_eclipse_aether__aether_util",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_transport_file",
        actual = "migration_tools___org_eclipse_aether__aether-transport-file__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___junit__junit__4_12",
        jars = ["@migration_tools___junit__junit//file"],
        deps = [
            ":migration_tools___org_hamcrest__hamcrest_core",
        ],
        exports = [
            ":migration_tools___org_hamcrest__hamcrest_core",
        ],
    )
    native.alias(
        name = "migration_tools___junit__junit",
        actual = "migration_tools___junit__junit__4_12",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_google_auto__auto-common__0_8",
        jars = ["@migration_tools___com_google_auto__auto_common//file"],
        deps = [
            ":migration_tools___com_google_guava__guava",
        ],
        exports = [
            ":migration_tools___com_google_guava__guava",
        ],
    )
    native.alias(
        name = "migration_tools___com_google_auto__auto_common",
        actual = "migration_tools___com_google_auto__auto-common__0_8",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___commons-codec__commons-codec__1_9",
        jars = ["@migration_tools___commons_codec__commons_codec//file"],
    )
    native.alias(
        name = "migration_tools___commons_codec__commons_codec",
        actual = "migration_tools___commons-codec__commons-codec__1_9",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket-native-common__2_0_4",
        jars = ["@migration_tools___com_kohlschutter_junixsocket__junixsocket_native_common//file"],
        deps = [
            ":migration_tools___com_kohlschutter_junixsocket__junixsocket_common",
            ":migration_tools___log4j__log4j",
            ":migration_tools___org_scijava__native_lib_loader",
        ],
        exports = [
            ":migration_tools___com_kohlschutter_junixsocket__junixsocket_common",
            ":migration_tools___log4j__log4j",
            ":migration_tools___org_scijava__native_lib_loader",
        ],
    )
    native.alias(
        name = "migration_tools___com_kohlschutter_junixsocket__junixsocket_native_common",
        actual = "migration_tools___com_kohlschutter_junixsocket__junixsocket-native-common__2_0_4",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_eclipse_aether__aether-spi__1_1_0",
        jars = ["@migration_tools___org_eclipse_aether__aether_spi//file"],
        deps = [
            ":migration_tools___org_eclipse_aether__aether_api",
        ],
        exports = [
            ":migration_tools___org_eclipse_aether__aether_api",
        ],
    )
    native.alias(
        name = "migration_tools___org_eclipse_aether__aether_spi",
        actual = "migration_tools___org_eclipse_aether__aether-spi__1_1_0",
        visibility = ["//visibility:public"],
    )



    native.java_import(
        name = "migration_tools___org_apache_httpcomponents__httpclient__4_5_3",
        jars = ["@migration_tools___org_apache_httpcomponents__httpclient//file"],
        deps = [
            ":migration_tools___commons_codec__commons_codec",
            ":migration_tools___commons_logging__commons_logging",
            ":migration_tools___org_apache_httpcomponents__httpcore",
        ],
        exports = [
            ":migration_tools___commons_codec__commons_codec",
            ":migration_tools___commons_logging__commons_logging",
            ":migration_tools___org_apache_httpcomponents__httpcore",
        ],
    )
    native.alias(
        name = "migration_tools___org_apache_httpcomponents__httpclient",
        actual = "migration_tools___org_apache_httpcomponents__httpclient__4_5_3",
        visibility = ["//visibility:public"],
    )



