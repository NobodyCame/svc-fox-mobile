# Fox Mobile

Fox Mobile is a client-side Fabric phone and calling mod for Minecraft 1.21.11. It provides its own interface, call controls, operator-backed features, and an optional compatibility bridge to clients running Simple Voice Call. This public repository contains the client mod source only; the companion operator backend is not published here.

## Identity and compatibility

- Fabric mod ID: `svc-fox-mobile`
- Display name: `Fox Mobile`
- Maintainer: `YukiiNoTenshi`
- Java: 21; minimum Fabric Loader: 0.18.5
- Resource namespace: `svc-fox-mobile` (matches the Fabric mod ID)
- Simple Voice Call integration ID: `simple-voice-call` (optional; used by the Legacy call engine)

On first launch, Fox Mobile copies old configuration, number cache, and user-data files to the new names only when the destination does not exist. Previous files remain as rollback copies. The Legacy call selection also accepts its previous saved value.

Phone numbers are hidden in Fox Mobile's nearby-player directory unless their owner enables public display. Players can add a number to a locally named contact while a call is active and edit the saved number later.

Fox Mobile code is copyrighted by YukiiNoTenshi; distribution terms are in [LICENSE](LICENSE). The optional Legacy bridge keeps the Simple Voice Call mod identifiers only where runtime compatibility requires them.

## Build

Use JDK 21 and the included Gradle wrapper:

```powershell
.\gradlew.bat build
```

The distributable JAR is written to `build/libs/svc-fox-mobile-<version>.jar`.

## Project notes

- [Changelog](docs/CHANGELOG_RU.md)
- [Module architecture](docs/MODULE_ARCHITECTURE.md)
- [Oracle server and backend connection guide](docs/ORACLE_SERVER_ARCHITECTURE_RU.md)
- [Call-control protocol](docs/CALL_CONTROL_PROTOCOL.md)
- The companion operator service is maintained separately and is not part of this repository.

Review the project and bundled-library terms in [LICENSE](LICENSE) before redistribution.
