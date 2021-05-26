"""A simple example for how to replace the provided artifact macro"""

load("@mabel//rules/maven_deps:mabel.bzl", "artifact")

def g_artifact(coordinate, type = "auto"):
    return artifact(coordinate, repositories = ["https://maven.google.com/"], type = type)
