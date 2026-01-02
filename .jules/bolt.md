## 2024-05-23 - Parallelizing Source Jar Location
**Learning:** `SourcesJarLocator` performs synchronous network I/O (HEAD requests) to check for source jars in a sequential stream. This is a significant bottleneck for builds with many dependencies.
**Action:** Switched to `parallelStream()` in `SourcesJarLocator` and replaced `HashMap` with `ConcurrentHashMap` in `GraphMemoizator` and `SourcesJarLocator` to ensure thread safety. This leverages Java's common fork-join pool to perform network checks concurrently.
