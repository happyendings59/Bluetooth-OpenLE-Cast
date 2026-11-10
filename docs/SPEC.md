# OpenLE-Cast — Bluetooth LE Audio Broadcast Specification

By Kenneth William Mayle — https://5pm.io / https://kwm59.io  
Contact: h@5pm.io
Original draft dated 1-27-2026. Revised 11-9-2026 (corrected figures, see `docs/BROADCAST-PATHS.md`).
Licensed under the GNU AGPL v3 or later (see `LICENSE` and `NOTICE`).

---

## 1. Overview & Use Cases

**LE Audio Auracast Broadcasting** allows ONE Android device to broadcast high-quality audio to **unlimited receiver devices** (within range), all playing in sync.

**Use cases:**

- Broadcast music to multiple headphones/earbuds at once (friends in café)
- Create "silent disco" group listening sessions
- Personal audio sharing (movie/podcast to multiple people)
- Hearing aid compatibility for accessibility
- Low power consumption vs. Classic Bluetooth A2DP

**Key advantage over Classic Bluetooth:**

- Classic A2DP: 1 phone → 2 devices max (dual audio)
- LE Audio Auracast: 1 phone → unlimited devices (Auracast capable)

---

## 2. Hardware & Software Requirements

**TRANSMITTER (Your Phone):**

- Android 13+ (minimum API 33, recommend API 35+)
- Bluetooth 5.2+ chipset with LE Audio broadcast source support
- Phones with built-in Auracast audio sharing (2026): Pixel 8 and newer, Samsung Galaxy S23/S24/S25 and Z Fold5–7, recent Xiaomi and POCO models
- Any other Android phone can broadcast through an external Auracast transmitter (see `docs/BROADCAST-PATHS.md`)
- Sufficient RAM: 6GB+ recommended
- Storage: 50MB+ free

**RECEIVERS (Friends' Devices):**

- Any device with Bluetooth LE Audio Auracast support:
  - LE Audio earbuds and headphones that support Auracast receive
  - Auracast-capable hearing aids and cochlear-implant processors
  - Phones or USB-C receivers that can join a broadcast
  - **Note:** LE Audio support does not always mean Auracast receive support. Check the maker's spec sheet before relying on a model.

**Network:**

- Range: typically 10–30 m indoors from a phone; up to roughly 50–100 m line of sight with a stronger dedicated transmitter
- Requires no Wi-Fi or cellular
- 2.4 GHz unlicensed band (same as Classic Bluetooth)

---

## 3. Audio Codec & Quality

**LC3 Codec (Low Complexity Communications Codec):**

- Bitrate: fixed per stream; common Auracast configurations run about 32–96 kbps per channel (broadcast cannot adapt per listener)
- Sample rates: 8, 16, 24, 32, 48 kHz
- Codec delay: about 7.5–12.5 ms (LC3 frame plus look-ahead)
- Power: lower than Classic Bluetooth A2DP at similar quality; actual savings vary by device

**Broadcast Audio Format:**

- **Mono or Stereo:** App can send either
- **Frame duration:** 7.5 or 10 ms (standard)
- **Number of streams:** 1 primary + optional secondary
- **Broadcast Code (optional):** 4–16 byte code; only listeners who enter it can decrypt the audio

---

## 4. Connection Topology

```
┌─────────────────┐
│  Your Android   │
│    Phone        │  (Broadcast Source / BAP Broadcast Source)
│  (Transmitter)  │
└────────┬────────┘
         │
         │  LE Audio Broadcast Stream(s)
         │
   ┌─────┼─────┬──────────┬──────────┐
   │     │     │          │          │
   ▼     ▼     ▼          ▼          ▼
Friend1 Friend2 Friend3  Friend4  Friend5+
Earbud  Phone  Earbuds  Earbuds   (Any Auracast device)
(RX)    (RX)   (RX)     (RX)      (RX)
```

**Connection flow:**

1. **Your phone:** Initiates broadcast → selects audio codec (LC3) → starts sending BIS (Broadcast Isochronous Stream)
2. **Friends' devices:** Scan for nearby broadcasts → display QR code or "Connect" prompt → join broadcast (no pairing needed)
3. **Sync:** Every receiver plays each audio frame after the same presentation delay, so all listeners hear it at the same moment

---

## 5. Android Framework Integration (API Level 33+)

**Key Classes:**

| Class | Purpose |
|-------|---------|
| `BluetoothAdapter` | Get profile proxy for LE Audio |
| `BluetoothLeAudio` | LE Audio unicast profile (earbuds connected to the phone) |
| `BluetoothLeBroadcast` | Broadcast source control — **system API**, privileged apps only |
| `AudioManager` | Audio routing and device detection |
| `MediaSession` | Media player integration (pause/play) |
| `AudioDeviceInfo.TYPE_BLE_BROADCAST` | Detects when the phone's own Auracast sharing is the active output |

**Permissions Required:**

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

**System Flags (AOSP / custom ROMs only — not settable by apps on stock phones):**

```
bluetooth.profile.bap.broadcast.source.enabled = true
bluetooth.profile.bap.broadcast.assist.enabled = true
```

---

## 6. Broadcast Parameters

| Parameter | Value | Notes |
|-----------|-------|-------|
| **Advertising Interval** | 20–100 ms | Scan frequency |
| **Broadcast Duration** | Infinite | Until stopped |
| **QR Code Data** | Broadcast ID + passkey | Optional encryption |
| **Max Receivers** | No protocol limit | Listeners only receive; limited by range and interference |
| **Codec Frame Size** | 60–120 bytes | LC3 compressed audio |
| **Latency (E2E)** | About 30–100 ms | Fine for music; may be noticeable when syncing with video on another screen |
