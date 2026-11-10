# Bluetooth OpenLE-Cast

**OpenLE-Cast** — an open specification and Android app scaffold for broadcasting audio from one phone to many Bluetooth LE Audio (Auracast) receivers at once: silent-disco listening, shared movie audio, hearing-aid access.

Created by **Kenneth William Mayle** — https://5pm.io / https://kwm59.io

Contact: h@5pm.io

## License

Copyright (C) 2026 Kenneth William Mayle.

Licensed under the **GNU Affero General Public License v3.0 or later** (`AGPL-3.0-or-later`). See [`LICENSE`](LICENSE).

Under an additional term allowed by AGPL §7(b), anyone who copies, forks, or builds on this project must keep the [`NOTICE`](NOTICE) file and the credit line "OpenLE-Cast by Kenneth William Mayle (https://5pm.io / https://kwm59.io)". If you run a modified version as a network service, the AGPL requires you to offer your source code to its users.

## What's here

| Path | Contents |
|------|----------|
| [`docs/SPEC.md`](docs/SPEC.md) | Technical specification (use cases, hardware, LC3 codec, topology, broadcast parameters) |
| `app/build.gradle.kts` | Android build config (minSdk 33, targetSdk 35) |
| `app/src/main/java/io/kwm59/openlecast/` | Kotlin sources: permissions, Bluetooth helper, audio player, broadcast manager, QR code generator |

## Project status: early scaffold

This is a design draft plus partial code, not a working app yet.

- **Broadcasting is not implemented.** `AudioBroadcastManager.startBroadcast()` sets up broadcast metadata (ID, name, passkey) but does not start a real LE Audio broadcast stream. On stock Android, the broadcast-source API (`BluetoothLeBroadcast`) is a system API that needs privileged permissions, so a normal Play Store app can't start an Auracast broadcast itself. Realistic paths: hand off to the phone's built-in Auracast/"audio sharing" feature, target rooted/custom-ROM devices, or pair the app with an external Auracast transmitter.
- **Missing files:** `MainActivity.kt`, `AndroidManifest.xml`, `res/` layouts and values, root Gradle settings, and `proguard-rules.pro`.
- Some spec figures (range, power savings, device list) are rough targets and should be checked against current Bluetooth SIG and manufacturer data.

Contributions welcome under the same license.
