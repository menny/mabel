"""A simple example for how to replace the provided artifact macro"""

load("@mabel//rules:mabel.bzl", "artifact")

def g_artifact(coordinate):
    return artifact(coordinate = coordinate, repositories = ["https://maven.google.com/"], type = "naive")
