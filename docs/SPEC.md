# OpenLE-Cast — Bluetooth LE Audio Broadcast Specification

By Kenneth William Mayle — https://5pm.io / https://kwm59.io  
Contact: h@5pm.io
Original draft dated 1-27-2026.
Licensed under the GNU AGPL v3 or later (see `LICENSE` and `NOTICE`).

---

## 1. Overview & Use Cases

**LE Audio Auracast Broadcasting** allows ONE Android device to broadcast high-quality audio to **unlimited receiver devices** (within range), all perfectly synchronized.

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
- Bluetooth 5.2+ chipset
- Compatible phones (2026): Pixel 8/9+, Samsung Galaxy S23/24/25, Xiaomi devices
- Sufficient RAM: 6GB+ recommended
- Storage: 50MB+ free

**RECEIVERS (Friends' Devices):**

- Any device with Bluetooth LE Audio Auracast support:
  - Pixel 8/9 phones
  - Samsung Galaxy S23+ phones
  - Sony WF-1000-5/WF-C700N earbuds
  - Compatible hearing aids
  - **Note:** Not every earbud has Auracast yet; must verify compatibility

**Network:**

- Bluetooth LE range: ~120–240 meters (open space)
- Requires no Wi-Fi or cellular
- 2.4 GHz unlicensed band (same as Classic Bluetooth)

---

## 3. Audio Codec & Quality

**LC3 Codec (Low Complexity Communications Codec):**

- Bitrate: 32–320 kbps (scales with network conditions)
- Sample rates: 8, 16, 24, 32, 48 kHz
- Latency: 5–20 ms (negligible for casual listening)
- Power: ~70% lower than Classic Bluetooth

**Broadcast Audio Format:**

- **Mono or Stereo:** App can send either
- **Frame duration:** 7.5 or 10 ms (standard)
- **Number of streams:** 1 primary + optional secondary
- **Passkey (optional):** Only invited receivers decode audio

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
3. **Sync:** All receivers stay synchronized to ~10µs precision

---

## 5. Android Framework Integration (API Level 33+)

**Key Classes:**

| Class | Purpose |
|-------|---------|
| `BluetoothAdapter` | Get profile proxy for LE Audio |
| `BluetoothLeAudio` | Broadcast source control |
| `AudioManager` | Audio routing and device detection |
| `MediaSession` | Media player integration (pause/play) |
| `BluetoothLeAudio.Callback` | Connection state callbacks |

**Permissions Required:**

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

**System Flags (Required to enable):**

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
| **Max Receivers** | Unlimited (theoretical) | Limited by range/RF interference |
| **Codec Frame Size** | 60–120 bytes | LC3 compressed audio |
| **Latency (E2E)** | 20–50 ms | User imperceptible |
