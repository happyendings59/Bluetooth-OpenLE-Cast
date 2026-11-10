# Bluetooth OpenLE-Cast

**OpenLE-Cast** — an open specification and Android app scaffold for broadcasting audio from one phone to many Bluetooth LE Audio (Auracast) receivers at once: silent-disco listening, shared movie audio, hearing-aid access.

Created by **Kenneth William Mayle** — https://5pm.io / https://kwm59.io

Contact: h@5pm.io

## License

Copyright (C) 2026 Kenneth William Mayle.

Licensed under the **GNU Affero General Public License v3.0 or later** (`AGPL-3.0-or-later`). See [`LICENSE`](LICENSE).

Under an additional term allowed by AGPL §7(b), anyone who copies, forks, or builds on this project must keep the [`NOTICE`](NOTICE) file and the credit line "OpenLE-Cast by Kenneth William Mayle (https://5pm.io / https://kwm59.io)". If you run a modified version as a network service, the AGPL requires you to offer your source code to its users.

## How it broadcasts

Stock Android only lets built-in system apps start an Auracast broadcast directly, so OpenLE-Cast offers three ways to do it. Details in [`docs/BROADCAST-PATHS.md`](docs/BROADCAST-PATHS.md).

1. **Phone's built-in audio sharing** — on phones that have it (Pixel 8+, Galaxy S23+, recent Xiaomi/POCO), the app opens the sharing settings, plays your audio, and detects when the broadcast is live.
2. **External Auracast transmitter** — works with any Android phone. Plug in a USB-C, 3.5 mm or Bluetooth-input Auracast transmitter and the app plays into it.
3. **Direct broadcast** — the app starts the broadcast itself with your stream name and passkey. Only works on rooted phones or custom ROMs where the app is installed as a privileged system app. *Experimental, untested.*

## What's here

| Path | Contents |
|------|----------|
| [`docs/SPEC.md`](docs/SPEC.md) | Technical specification (use cases, hardware, LC3 codec, topology, broadcast parameters) |
| [`docs/BROADCAST-PATHS.md`](docs/BROADCAST-PATHS.md) | The three broadcast paths, compatible phones and transmitters |
| `app/src/main/java/io/kwm59/openlecast/` | Kotlin sources: main screen, broadcast manager, privileged broadcaster, permissions, Bluetooth helper, audio player, QR code generator |
| `app/src/main/res/`, `AndroidManifest.xml` | Layout, strings, theme, icon, permissions |
| `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts` | Gradle build (AGP 8.5, Kotlin 1.9, minSdk 33, targetSdk 35) |

## Building

Open the project folder in Android Studio (Ladybug or newer) and let it sync; it will create the Gradle wrapper. Run on an Android 13+ phone.

## Project status

- **Not yet compiled or tested on a device.** The code was written against the public Android 13–15 APIs, but no full Android build has been run yet. Expect small fixes on first build.
- Paths 1 and 2 use only public APIs. Path 3 uses reflection on a system API and needs real-device testing on a custom ROM.
- The in-app QR code shows the stream name and passkey as text. It is not the official one-tap Auracast join code, which needs the phone's Bluetooth address that apps can't read.

## Roadmap

- **Build and test on real hardware** for paths 1 and 2, and fix whatever the first Android Studio build turns up.
- **Direct USB dongle broadcasting (no root).** Android apps can talk to USB devices directly, so OpenLE-Cast could drive a low-cost Bluetooth USB dongle itself, the way Google's Bumble stack does on a computer. That would let any Android phone broadcast with the app fully controlling the stream name, passkey and official Auracast QR join code, with no root and no dedicated transmitter. Help wanted.
- **Official Auracast QR codes** once the app controls the broadcast (path 3 or the USB dongle path).

## Related projects

OpenLE-Cast builds on ideas already explored elsewhere. Worth a look:

- [Bumble Auracast app](https://google.github.io/bumble/apps_and_tools/auracast.html): Google's Python Bluetooth stack, broadcasts Auracast from a computer with a USB dongle
- [Zephyr RTOS broadcast audio samples](https://kitemetric.com/blogs/mastering-auracast-broadcasting-multiple-audio-subgroups-with-zephyr): embedded broadcast source and sink
- [BlueZ Auracast broadcasting](https://www.linuxtoday.com/blog/bluez-powered-auracast-broadcasting-on-genio-700/): Linux broadcasting work by Collabora
- [sendspin-auracast](https://pypi.org/project/sendspin-auracast/): Python Auracast streamer
- [Web-based broadcast assistant](https://dev.to/denladeside/a-web-based-broadcast-assistant-cld): browser tool for joining Auracast streams
- Android's built-in audio sharing on recent Pixel, Samsung and Xiaomi phones, and USB transmitters such as the Avantree C82 Aura and Sennheiser BTD 700

Contributions welcome under the same license.
