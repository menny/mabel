## 2024-05-23 - Parallel Source Resolution
**Learning:** `SourcesJarLocator` was sequentially checking for source JARs using `HEAD` requests. This is a classic I/O bound bottleneck.
**Action:** Parallelized `fillSourcesAttribute` using `parallelStream()`. Crucially, this required upgrading `GraphMemoizator` and `SourcesJarLocator` caches to `ConcurrentHashMap` to ensure thread safety. `computeIfAbsent` was used to ensure atomicity.
