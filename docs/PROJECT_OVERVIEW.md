# Fox Mobile project overview

Fox Mobile is a Fabric phone and calling mod for Minecraft 1.21.11. This public
repository contains the client mod source. The companion operator service is
maintained separately and is not published here.

The Java client source lives in `src/main/java/`. Its resources, including the
mod metadata and the operator trust anchor used by the dedicated HTTP client,
are under `src/main/resources/`.

## Build

Use JDK 21 and the included Gradle wrapper:

```powershell
.\gradlew.bat build
```

The distributable JAR is written to `build/libs/`.

## Scope and licensing

This repository contains reviewed source and user-facing technical documents.
It intentionally excludes the operator backend, local deployment material,
credentials, databases, build outputs, and development snapshots. See [LICENSE](../LICENSE) and
[NOTICE.md](../NOTICE.md) for distribution and third-party notices.
