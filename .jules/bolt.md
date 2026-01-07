## 2024-05-23 - [Parallel Stream Safety]
**Learning:** Switching to `parallelStream` requires careful audit of all shared mutable state, even in test helpers. I learned that `GraphMemoizator` was using a non-thread-safe `HashMap`, and `SourcesLocatorTest`'s `FakeOpener` also used `HashMap`.
**Action:** Always check thread-safety of underlying data structures when introducing parallel streams, including abstract classes and test doubles.
