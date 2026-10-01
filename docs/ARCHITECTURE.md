# WIN ZONE Companion App — Architecture Overview

## 1. System Responsibilities

The WIN ZONE Companion App is an Android-native telemetry and OCR capture client:
- **Zero Financial Execution:** The client never touches wallets, balances, or stakes. The only RPC trigger is `start_match` when both sides agree on kickoff.
- **State Reporting:** Captures the display via `MediaProjection`, processes frames locally with bundled ML Kit OCR v2, and streams state to Supabase every 2 seconds.
- **Mandatory Notification:** Capture is bounded to a persistent foreground service with an un-swipeable notification (`CATEGORY_SERVICE`).

---

## 2. Component Diagram

```
+-------------------------------------------------------------+
|                         Compose UI                          |
|  LoginScreen | HomeScreen | JoinScreen | PermissionScreen   |
|               CaptureScreen | SettingsScreen                |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                     CaptureController                       |
|          Shared In-Memory State & Observer Buses            |
+-------------------------------------------------------------+
                               |
                               v
+-------------------------------------------------------------+
|                       CaptureService                        |
|       (Foreground Service: mediaProjection / WakeLock)      |
+-------------------------------------------------------------+
         |                                          |
         v                                          v
+----------------------+                 +--------------------+
| MediaProjectionSession |                |    FrameSampler    |
| (VirtualDisplay /    |                 |   (2s Cadence)     |
|  ImageReader)        |                 +--------------------+
+----------------------+                            |
                                                    v
                                         +--------------------+
                                         |   FrameAnalyzer    |
                                         | (Downscale + OCR)  |
                                         +--------------------+
                                                    |
                                                    v
                                         +--------------------+
                                         |   StateReporter    |
                                         | (Rate Limit/Dedup) |
                                         +--------------------+
                                           /                \
                                          v                  v
                           +----------------------+   +-------------------+
                           |   SupabaseRpcClient  |   |  OfflineQueueDao  |
                           |   (Ktor CIO Engine)  |   |  (Room Database)  |
                           +----------------------+   +-------------------+
```

---

## 3. Data Flow

1. **Screen Acquisition:** `ImageReader` captures `PixelFormat.RGBA_8888` buffer.
2. **Analysis:** `FrameAnalyzer` downscales to max 1280px on the longest edge, runs ML Kit Latin Text Recognition, maps bounding boxes into `GameProfile` regions, and extracts score, clock, and team labels.
3. **Classification:** Deterministic keyword and state heuristics classify the layout into `in_play`, `full_time`, `opponent_disconnected`, `opponent_conceded`, or `unknown`.
4. **Telemetry Streaming:** `StateReporter` suppresses duplicate states, enforces rate limits (min 1.5s between network attempts), and retries with exponential backoff.
5. **Agreement Evaluation:** `AgreementGate` monitors local and remote states; when both agree on kickoff or full-time conditions, atomic triggers fire `start_match` or `app_finalize_result`.
