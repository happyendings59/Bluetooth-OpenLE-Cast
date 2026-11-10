# Broadcast Paths

OpenLE-Cast — Copyright (C) 2026 Kenneth William Mayle — https://5pm.io / https://kwm59.io
Licensed under the GNU AGPL v3 or later.

---

## Why there are three paths

On stock Android, the API that starts an LE Audio broadcast (`android.bluetooth.BluetoothLeBroadcast`) is a **system API**. It needs the `BLUETOOTH_PRIVILEGED` permission, which only apps built into the phone's firmware get. A normal app from the Play Store cannot start an Auracast broadcast on its own.

OpenLE-Cast works around this in three ways. The app lets the user pick one under **"How to broadcast"**.

| | A. Phone's built-in sharing | B. External transmitter | C. Direct (privileged) |
|---|---|---|---|
| Works on stock phones | Yes, on supported models | Yes, any Android phone | No |
| Extra hardware | None | Auracast transmitter | None |
| Who does the broadcasting | The phone's OS | The transmitter | OpenLE-Cast |
| App controls name/passkey | No (OS sets them) | Via transmitter's own tool | Yes |
| Status | Implemented | Implemented | Experimental, untested |

---

## A. Hand off to the phone's built-in audio sharing

Recent phones ship Auracast "Audio sharing" in their Bluetooth settings. Google lists Pixel 8 and newer, Samsung Galaxy S23/S24/S25 and Z Fold5–7, and several recent Xiaomi and POCO models. Both the phone and the headphones must support LE Audio.

**How the app does it:**

1. The user taps **Start sharing**. The app opens the phone's Bluetooth settings (there's no public shortcut straight to the sharing page).
2. The user turns on Audio sharing / Auracast there. The OS shows its own QR code for friends to scan.
3. Back in the app, the user presses **Play**. Media audio now goes to the OS broadcast.
4. The app watches the phone's audio outputs and shows "Phone is broadcasting" when an output of type `AudioDeviceInfo.TYPE_BLE_BROADCAST` appears.

**Limits:** the OS chooses the stream name, passkey and QR code. Some phones only allow sharing while LE Audio earbuds are connected.

---

## B. Play into an external Auracast transmitter

A small Auracast transmitter takes ordinary audio in and broadcasts it as Auracast. This works with any phone, including ones without LE Audio.

Examples:
- **Avantree C82 Aura** — USB Auracast transmitter, about $60. Range up to roughly 30 m, latency around 50–80 ms, and supports an encrypted mode with a passcode you set. Check its compatibility list for your phone.
- **Sennheiser BTD 700** — USB-C / USB-A dongle with Auracast broadcast.
- Any transmitter with a 3.5 mm input or a Bluetooth (A2DP) input that re-broadcasts as Auracast.

**How the app does it:**

1. The user plugs in or connects the transmitter.
2. The app detects a USB, wired, or Bluetooth A2DP output and shows its name.
3. The user presses **Play**. Audio goes to the transmitter, which broadcasts it.
4. The app's QR code shows the stream name and passkey so friends know which stream to pick.

**Limits:** the stream name and passkey are set in the transmitter's own setup tool, not by the app.

---

## C. Direct broadcast (privileged install only)

`PrivilegedBroadcaster.kt` calls `BluetoothLeBroadcast` directly through reflection: it starts a broadcast with the user's stream name and optional passkey, and stops it later.

It only works when the app holds `BLUETOOTH_PRIVILEGED`, for example:
- built into a custom ROM (LineageOS, GrapheneOS fork, etc.) as a privileged app, or
- on a rooted phone, installed into `/system/priv-app/` with a matching `privapp-permissions` XML file.

On a normal phone this option is greyed out and none of that code runs.

**Status:** written against the AOSP Android 13–15 method names but **not tested on hardware**. Method signatures can change between Android versions.

**About the QR code:** the official Auracast QR format includes the phone's Bluetooth address, which apps can't read. The app's QR code is a readable label (stream name and passkey), not a one-tap join code.

---

## Sources

- Google — [Share your world of sound with LE Audio on Android](https://blog.google/products/android/le-audio-auracast-support)
- Avantree — [C82 Aura USB Auracast transmitter](https://avantree.com/products/c82-aura-usb-auracast-audio-adapter)
- Sennheiser — [BTD 700](https://ae.sennheiser-hearing.com/products/btd-700)
- Android Developers — `AudioDeviceInfo.TYPE_BLE_BROADCAST`, `BluetoothAdapter.isLeAudioBroadcastSourceSupported()`
