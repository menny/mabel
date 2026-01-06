## 2024-05-23 - [Parallelize SourcesJarLocator]
**Learning:** `SourcesJarLocator` was sequentially checking for source JARs via HEAD requests. This I/O bound operation can be safely parallelized.
**Action:** Used `parallelStream()` in `SourcesJarLocator` and switched `mURLCache` and `GraphMemoizator` to `ConcurrentHashMap` with `computeIfAbsent` for thread safety.
