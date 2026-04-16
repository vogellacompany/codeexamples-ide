# Parallel Startup Configurator

Marks Eclipse platform bundles as safe for parallel activation during startup using the Equinox `Module.setParallelActivation(true)` API.

## Required Framework Configuration

Add to your `config.ini` or launch arguments:

```
equinox.start.level.thread.count=0
equinox.start.level.restrict.parallel=true
```

- `equinox.start.level.thread.count` — number of threads for bundle activation. `0` means auto-detect based on available processors. Default is `1` (sequential).
- `equinox.start.level.restrict.parallel` — when `true`, only bundles explicitly marked via `setParallelActivation(true)` are activated in parallel. When `false` (default), all bundles within a start level activate in parallel.

## How It Works

Equinox processes start levels sequentially (level 1 completes before level 2 begins). Within each start level, bundles are grouped:

1. Lazy parallel bundles (started concurrently)
2. Lazy sequential bundles (started one by one)
3. Eager parallel bundles (started concurrently)
4. Eager sequential bundles (started one by one)

Each group completes before the next begins. The parallel activation flag is persisted in the framework's module database, so it only needs to be set once.

## Limitations

This plug-in uses an OSGi DS immediate component. DS itself must be activated before this component runs, so the parallel flags are not effective on the first cold start. All subsequent launches benefit from the persisted settings.

For first-launch effectiveness, use a `BundleActivator` at start level 1 instead of DS.

## Bundle Safety

A bundle is safe for parallel activation if its activator is thread-safe and does not depend on side effects from other bundles' activators within the same start level. Standard OSGi service dependencies (via DS) are safe since service lookup is decoupled from activation order.
