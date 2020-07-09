
load("@mabel//rules/maven_deps:mabel.bzl", "artifact")

def g_artifact(coordinate):
    return artifact(coordinate, repositories=['https://maven.google.com/'])
