# WIN ZONE — Android Companion App
## End-to-End Build Specification (Phase D)

**Document status:** Final building plan. Supersedes all prior drafts.
**Audience:** Automated coding agent (Google Antigravity) + human reviewer.
**Deliverable:** Signed release APK, sideloadable, hosted on WIN ZONE website.
**App package name:** `com.winzone.companion`
**App display name:** `WIN ZONE`
**Version:** `1.0.0` (versionCode 1)

---

## 0. Read this first (agent instructions)

You are building a **screen-capture + OCR + telemetry client**. You are **not** building a game, a wallet, or a result authority. Every money decision happens on the Supabase backend. The app's job is to:

1. Log the user in with Supabase Auth.
2. Find any open match for that user.
3. Capture the phone screen via `MediaProjection`.
4. Run on-device OCR (ML Kit Text Recognition v2) on captured frames.
5. Classify the frame into a layout enum.
6. Extract fields (teams, scores, penalties, clock).
7. Report state to Supabase every 2 seconds via `submit_screen_state`.
8. When both players' reported states agree, call `start_match` (kickoff).
9. When both players' reported states agree at `full_time`, call `app_finalize_result`.
10. Never move money. Never enter results manually. Never hide the foreground notification.

Every decision in this document is final unless a section explicitly says "configurable."

---

## 1. Product summary

### 1.1 One-paragraph description
WIN ZONE Companion is a sideloaded Android app that watches a user's phone screen during a matched 1v1 mobile game, extracts the match state via on-device OCR, and streams that state to the WIN ZONE Supabase backend every two seconds. The backend uses the agreed state from both players to trigger kickoff (`start_match`) and full-time settlement (`app_finalize_result`). The app never touches money, never records results by hand, and never runs without a visible foreground notification.

### 1.2 Non-goals (explicitly out of scope)
- No in-app wallet, balance, deposit, withdrawal, or stake UI.
- No manual result entry by the user.
- No matchmaking logic (backend matches players; app only joins).
- No chat, no social features.
- No analytics beyond what the backend already stores.
- No ads.
- No Play Store submission in v1.0.0 (sideload only; Play Store is a later phase).

### 1.3 Hard rules (must never be violated)
- **R1.** Money moves only at `start_match` (kickoff). The app never calls any money-moving RPC other than `start_match`.
- **R2.** No winner is declared before kickoff. The app must not call `app_finalize_result` unless the local state has already passed through `in_play` for this match.
- **R3.** Result is decided at `full_time`, `opponent_disconnected`, or `opponent_conceded`.
- **R4.** Draw = equal regulation score **AND** equal penalty score at `full_time`. Backend refunds both stakes. The app does not compute refunds; it only reports the state that lets the backend decide.
- **R5.** No manual result entry. The app detects and reports.
- **R6.** The app never initiates money movement. Read + report only (except `start_match`, which is a state-agreement trigger, not a money choice).
- **R7.** The foreground notification must be visible the entire time capture is running. The app must not offer any way to hide it.
- **R8.** The app does not run capture without the notification. If the OS kills the notification, capture stops.

---

## 2. Tech stack (locked)

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 1.9.24 |
| Build system | Gradle (Kotlin DSL) | 8.7 |
| Android Gradle Plugin | AGP | 8.4.0 |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 34 (Android 14) | — |
| Compile SDK | 34 | — |
| UI | Jetpack Compose (Material 3) | BOM 2024.06.00 |
| Navigation | `androidx.navigation:navigation-compose` | 2.7.7 |
| DI | Hilt | 2.51.1 |
| Async | Kotlin Coroutines + Flow | 1.8.1 |
| Networking | Supabase Kotlin SDK (`io.github.jan-tennert.supabase`) | 2.6.0 |
| HTTP engine | Ktor CIO | 2.3.11 |
| Serialization | `kotlinx.serialization` | 1.6.3 |
| OCR | ML Kit Text Recognition v2 (on-device, Latin) | `com.google.mlkit:text-recognition:16.0.0` |
| Image handling | `androidx.camera` not used; raw `ImageReader` + `Bitmap` | — |
| Persistence | `androidx.datastore:datastore-preferences` + `EncryptedSharedPreferences` for tokens | 1.1.1 |
| Logging | `timber` | 5.0.1 |
| Testing | JUnit5, MockK, Turbine, Compose UI Test | — |
| Crash reporting | Firebase Crashlytics (optional; can be omitted for sideload) | — |

**Dependency note:** ML Kit Text Recognition v2 is bundled (not the Play Services variant) so the app works fully offline for OCR. APK size will be ~15 MB larger; acceptable.

---

## 3. Supabase backend contract (assumed — verify with backend before coding)

Base URL: `https://jfniylmbogodgozcczln.supabase.co`
Publishable key: `sb_publishable_ZyP4LV6xjYu8T4tyPAgWFw_IQEPiVui` (safe to ship if RLS is enforced)

### 3.1 Auth
Standard Supabase Auth email/password. Same credentials as the website.
- `POST /auth/v1/token?grant_type=password` — login
- `POST /auth/v1/token?grant_type=refresh_token` — refresh
- Session must be refreshed in the foreground service, not only in the Activity.

### 3.2 RPCs (all already built and tested on backend)

#### `app_join_match(match_id uuid) -> match_snapshot`
Returns a single-row snapshot. Expected fields (verify against backend):

```json
{
  "match_id": "uuid",
  "status": "matched | in_progress",
  "player_one_id": "uuid",
  "player_two_id": "uuid",
  "my_side": "a | b",              // server-assigned: which side THIS user is
  "team_a_label": "string|null",   // server-provided, may be null
  "team_b_label": "string|null",
  "stake": "numeric|null",
  "created_at": "timestamptz",
  "kickoff_at": "timestamptz|null"
}
```

`my_side` is the authoritative source for the **team_a/team_b ↔ player_one/player_two** mapping. The app never decides its own side.

#### `submit_screen_state(...) -> void`
Parameters (exact names):

```
p_match_id        uuid
p_layout          text      -- enum: in_play | full_time | opponent_disconnected | opponent_conceded | unknown
p_team_a          text|null
p_team_b          text|null
p_score_a         int|null
p_score_b         int|null
p_pen_a           int|null   -- null when no shootout
p_pen_b           int|null
p_clock_seconds   int|null   -- null if unreadable
p_raw_json        jsonb      -- see §9.4
```

Called every 2 seconds while capture is active. Backend is idempotent.

#### `start_match(match_id uuid) -> void`
Called by the app **only** when local state and the opponent's reported state both satisfy the agreement rule (§11.1). Backend is the actual money mover; the app is just the trigger. Backend must be idempotent.

#### `app_finalize_result(match_id uuid) -> void`
Called by the app **only** when local state and the opponent's reported state both satisfy the full-time agreement rule (§11.2). Backend decides winner/draw/refund. App is just the trigger. Idempotent.

### 3.3 Match discovery query
The app queries `matches` filtered by:
- `player_one_id = auth.uid()` **OR** `player_two_id = auth.uid()`
- `status IN ('matched', 'in_progress')`

Use the Supabase Kotlin SDK's PostgREST client with `.or { filter(...) }` and `.inList("status", listOf("matched", "in_progress"))`. RLS must allow the user to see their own rows.

### 3.4 Backend invariants the app relies on (must be true)
- `submit_screen_state` rejects writes from users not in the match.
- `submit_screen_state` rejects writes when match status is not `matched`/`in_progress`.
- `start_match` rejects if match is not in `matched`.
- `app_finalize_result` rejects if match is not in `in_progress`.
- Both RPCs are idempotent (safe to retry).
- The backend stores the last state per player and evaluates agreement server-side too (defense in depth). The app's local agreement check is only to decide *when* to call the RPC — the backend remains authoritative.

---

## 4. Project structure (exact)

```
winzone-companion/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/winzone/companion/
│       │   │   ├── WinZoneApp.kt                      // @HiltAndroidApp
│       │   │   ├── MainActivity.kt                    // single-activity Compose host
│       │   │   ├── di/
│       │   │   │   ├── SupabaseModule.kt
│       │   │   │   ├── OcrModule.kt
│       │   │   │   ├── CaptureModule.kt
│       │   │   │   └── AppModule.kt
│       │   │   ├── data/
│       │   │   │   ├── auth/
│       │   │   │   │   ├── AuthRepository.kt
│       │   │   │   │   └── SessionStore.kt
│       │   │   │   ├── match/
│       │   │   │   │   ├── MatchRepository.kt
│       │   │   │   │   └── MatchSnapshot.kt
│       │   │   │   ├── state/
│       │   │   │   │   ├── ScreenStateRepository.kt
│       │   │   │   │   ├── ScreenStatePayload.kt
│       │   │   │   │   └── RawJson.kt
│       │   │   │   └── remote/
│       │   │   │       ├── SupabaseRpcClient.kt
│       │   │   │       └── RetryPolicy.kt
│       │   │   ├── capture/
│       │   │   │   ├── CaptureService.kt              // foreground service
│       │   │   │   ├── MediaProjectionSession.kt
│       │   │   │   ├── FrameAnalyzer.kt
│       │   │   │   ├── FrameSampler.kt
│       │   │   │   └── NotificationFactory.kt
│       │   │   ├── ocr/
│       │   │   │   ├── OcrEngine.kt                   // ML Kit wrapper
│       │   │   │   ├── LayoutClassifier.kt
│       │   │   │   ├── FieldExtractor.kt
│       │   │   │   ├── GameProfile.kt
│       │   │   │   └── profiles/
│       │   │   │       └── DefaultGameProfile.kt
│       │   │   ├── agreement/
│       │   │   │   ├── AgreementGate.kt
│       │   │   │   └── AgreementRules.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/ (Color.kt, Type.kt, Theme.kt)
│       │   │   │   ├── nav/AppNav.kt
│       │   │   │   ├── login/LoginScreen.kt + LoginViewModel.kt
│       │   │   │   ├── home/HomeScreen.kt + HomeViewModel.kt
│       │   │   │   ├── join/JoinScreen.kt + JoinViewModel.kt
│       │   │   │   ├── permission/PermissionScreen.kt
│       │   │   │   ├── capture/CaptureScreen.kt + CaptureViewModel.kt
│       │   │   │   ├── fallback/TeamNameFallbackDialog.kt
│       │   │   │   └── settings/SettingsScreen.kt
│       │   │   └── util/
│       │   │       ├── Constants.kt
│       │   │       ├── Logging.kt
│       │   │       ├── Time.kt
│       │   │       └── BitmapUtils.kt
│       │   └── res/
│       │       ├── values/strings.xml
│       │       ├── values/themes.xml
│       │       ├── drawable/ic_capture_notification.xml
│       │       └── xml/network_security_config.xml
│       └── test/kotlin/com/winzone/companion/         // unit tests
│       └── androidTest/kotlin/com/winzone/companion/  // instrumented tests
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── keystore/                                          // NOT committed
│   └── winzone-release.jks
└── docs/
    └── BUILD.md
```

---

## 5. AndroidManifest.xml (exact contents)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Foreground service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Keep CPU awake during capture -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Optional: to prompt for battery optimization exemption -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:name=".WinZoneApp"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:networkSecurityConfig="@xml/network_security_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.WinZone"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".capture.CaptureService"
            android:exported="false"
            android:foregroundServiceType="mediaProjection"
            android:stopWithTask="false" />

        <!-- FileProvider for sharing logs if needed -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

**Notes:**
- `android:stopWithTask="false"` ensures the service survives task removal. Android will still show the "app is running in background" prompt on some OEMs — that's expected and acceptable.
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` is required on API 34+. Without it, `startForeground` throws `SecurityException`.
- `POST_NOTIFICATIONS` is runtime-requested on API 33+.
- `network_security_config.xml` pins `jfniylmbogodgozcczln.supabase.co` to TLS 1.2+ and disallows cleartext.

---

## 6. Permissions flow (exact ordering)

This ordering is critical on Android 14+. Wrong order → `SecurityException`.

### 6.1 First launch
1. User logs in (Supabase Auth).
2. App queries for open matches. If none → `HomeScreen` shows "No open match."
3. If a match exists → user taps **Join match** → `app_join_match(match_id)` → navigate to `PermissionScreen`.

### 6.2 Permission screen (sequential, each explained to the user)
1. **Notifications** (API 33+): rationale → `ActivityResultContracts.RequestPermission()` for `POST_NOTIFICATIONS`. If denied, block capture (R7/R8). Show "Capture needs the notification to run. Enable it in Settings."
2. **Battery optimization exemption** (optional, recommended): rationale → `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent. If denied, warn but allow proceeding.
3. **MediaProjection consent**: `MediaProjectionManager.createScreenCaptureIntent()` → `startActivityForResult`. This **must** be launched from the Activity, **after** the user has acknowledged the notification permission, and **before** the service starts.
4. On consent result `RESULT_OK`, store the `resultCode` and `Intent` data, then start `CaptureService` with those extras.

### 6.3 Re-consent
`MediaProjection` consent is per-session on Android 14+. Every time the app wants to capture, it must:
- Start the foreground service **first** (with type `mediaProjection` and a placeholder notification).
- Then call `MediaProjectionManager.getMediaProjection(resultCode, data)` inside the service.
- Actually, the correct 14+ sequence is: request consent in Activity → pass result to service → service calls `startForeground` with `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` → service calls `getMediaProjection(...)`. The service must call `startForeground` **before** `getMediaProjection`.

**Locked sequence:**
1. Activity: `startActivityForResult(createScreenCaptureIntent())`.
2. On `RESULT_OK`: `ContextCompat.startForegroundService(context, Intent(context, CaptureService::class.java).apply { putExtra(EXTRA_RESULT_CODE, resultCode); putExtra(EXTRA_RESULT_DATA, data) })`.
3. Service `onStartCommand`: build notification → `startForeground(NOTIF_ID, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)` → then `mediaProjectionManager.getMediaProjection(resultCode, data)` → create `VirtualDisplay` → `ImageReader` → start sampling.

### 6.4 If user denies MediaProjection
Show a blocking screen: "Capture cannot start without screen permission. Return to WIN ZONE website." Provide a button to open the website.

---

## 7. Authentication

### 7.1 Login screen
- Email + password fields.
- "Log in" button.
- Error text area.
- Link: "Open winzone website" (opens browser).

### 7.2 AuthRepository
```kotlin
interface AuthRepository {
    val session: Flow<Session?>          // null = logged out
    suspend fun signIn(email: String, password: String): Result<Session>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshIfNeeded(): Result<Unit>
}
```

- Uses Supabase Kotlin SDK's `GoTrue` plugin.
- Session persisted in `EncryptedSharedPreferences` (AES-256-GCM, master key in Android Keystore).
- On app start: read session; if expired, refresh; if refresh fails, sign out and route to Login.

### 7.3 Background refresh
`CaptureService` owns a coroutine that calls `authRepository.refreshIfNeeded()` every 10 minutes while capture runs. If refresh fails 3 times in a row, the service stops capture, shows a notification "Session expired — open WIN ZONE," and does not retry further until the user reopens the app.

---

## 8. Match discovery and join

### 8.1 MatchRepository
```kotlin
interface MatchRepository {
    suspend fun findOpenMatch(userId: String): MatchSnapshot?
    suspend fun joinMatch(matchId: String): Result<MatchSnapshot>
}
```

### 8.2 findOpenMatch
PostgREST query:
```
GET /rest/v1/matches
  ?or=(player_one_id.eq.{uid},player_two_id.eq.{uid})
  &status=in.(matched,in_progress)
  &order=created_at.desc
  &limit=1
```
Deserialize into `MatchSnapshot`. Return null if empty.

### 8.3 joinMatch
RPC call `app_join_match(match_id = matchId)`. Returns `MatchSnapshot` including `my_side`. Cache `my_side` in memory for the session — it's the authoritative team mapping.

---

## 9. Capture pipeline

### 9.1 CaptureService lifecycle

**States:** `IDLE → STARTING → RUNNING → STOPPING → IDLE`

**onStartCommand:**
1. Extract `EXTRA_RESULT_CODE` and `EXTRA_RESULT_DATA` (MediaProjection consent).
2. Build persistent notification via `NotificationFactory.buildCaptureNotification()`.
3. `startForeground(NOTIF_ID, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)`.
4. Acquire `PARTIAL_WAKE_LOCK` (tag `winzone:capture`, timeout 4 hours; renew in loop).
5. Call `MediaProjectionSession.start(resultCode, data)`.
6. Launch `FrameSampler` coroutine (2s cadence).
7. Launch `StateReporter` coroutine.
8. Launch `SessionRefresher` coroutine (10 min cadence).
9. Return `START_STICKY`? **No.** Return `START_NOT_STICKY`. Reason: if the OS kills the service, we do not want it to restart without a fresh MediaProjection consent (which it can't get in the background). Instead, the app shows a notification "Capture stopped — reopen WIN ZONE."

**onDestroy:**
- Cancel all coroutines.
- Release `VirtualDisplay`, `ImageReader`, `MediaProjection`.
- Release wake lock.
- Do **not** cancel the notification manually — Android removes it when the service is destroyed. But if the user swipes it, the service must stop (Android does this automatically on some versions; we also listen for `NotificationListenerService`? No — simpler: rely on `stopWithTask=false` and foreground service semantics).

**Notification content (locked):**
- Title: `WIN ZONE — capture active`
- Text: `Match {shortId} — reporting every 2s`
- Icon: `ic_capture_notification` (a shield-eye glyph)
- Ongoing: `true`
- Category: `CATEGORY_SERVICE`
- Actions: `Stop capture` (opens app, does not directly stop — user must confirm in-app)
- Channel: `winzone_capture` (importance LOW, no sound, no vibration)

### 9.2 MediaProjectionSession

```kotlin
class MediaProjectionSession(private val context: Context) {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    fun start(resultCode: Int, data: Intent) {
        val mpm = context.getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, data)
        projection!!.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { /* signal service to stop */ }
        }, Handler(Looper.getMainLooper()))

        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection!!.createVirtualDisplay(
            "winzone-capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
    }

    fun acquireLatestBitmap(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        return try {
            image.toBitmap()
        } finally {
            image.close()
        }
    }
}
```

**Frame conversion:** `ImageReader` yields `Image` with `RGBA_8888`. Use `image.planes[0].buffer` + `rowStride`/`pixelStride` to build a `Bitmap`. Copy into a reusable `Bitmap` to avoid GC churn (see `BitmapUtils.kt`).

**Downscaling:** Before OCR, downscale to `max(width, 1280)` on the long edge to keep ML Kit fast. Keep the full-resolution bitmap only if needed for a specific field; default profile uses the downscaled one.

### 9.3 FrameSampler
- Coroutine loop: `while (isActive) { sample(); delay(2000) }`.
- Each sample:
  1. `val bmp = session.acquireLatestBitmap() ?: return`.
  2. `val result = frameAnalyzer.analyze(bmp)`.
  3. `stateReporter.submit(result)`.
  4. `bmp.recycle()` if not reused.
- Skips a sample if the previous one is still in flight (`AtomicBoolean`).

### 9.4 FrameAnalyzer output

```kotlin
data class FrameAnalysis(
    val layout: Layout,                 // enum
    val teamA: String?,
    val teamB: String?,
    val scoreA: Int?,
    val scoreB: Int?,
    val penA: Int?,
    val penB: Int?,
    val clockSeconds: Int?,
    val ocrConfidence: Float,           // 0f..1f, min over extracted fields
    val layoutConfidence: Float,        // 0f..1f
    val capturedAtMs: Long
)
```

`raw_json` payload (locked schema):
```json
{
  "ocr_text": "full concatenated text, newline-separated",
  "ocr_blocks": [
    { "text": "...", "left": 0, "top": 0, "right": 0, "bottom": 0, "conf": 0.0 }
  ],
  "layout_confidence": 0.0,
  "ocr_confidence": 0.0,
  "frame_ts_ms": 0,
  "frame_width": 0,
  "frame_height": 0,
  "profile_id": "default-v1",
  "app_version": "1.0.0"
}
```

`ocr_blocks` is truncated to the 40 largest blocks to keep payload under ~10 KB.

### 9.5 StateReporter

```kotlin
class StateReporter(
    private val rpc: SupabaseRpcClient,
    private val agreementGate: AgreementGate,
    private val matchId: String,
    private val mySide: Side
) {
    private val mutex = Mutex()
    private var lastSent: FrameAnalysis? = null

    suspend fun submit(analysis: FrameAnalysis) = mutex.withLock {
        // skip if identical to last sent (layout + scores + clock bucket)
        if (isDuplicate(analysis, lastSent)) return
        val payload = analysis.toPayload(matchId, mySide)
        val result = retryWithBackoff { rpc.submitScreenState(payload) }
        if (result.isSuccess) lastSent = analysis
        agreementGate.onLocalState(payload, result.isSuccess)
    }
}
```

**Duplicate suppression:** If layout is identical, scores identical, and `clock_seconds` is within ±1s of the last sent value, skip the network call. This keeps traffic sane during static screens.

**Backoff:** See §10.

---

## 10. Error handling and retry

### 10.1 RetryPolicy
```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 6,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 30000,
    factor: Double = 2.0,
    jitterMs: Long = 250,
    block: suspend () -> T
): Result<T>
```

Delays: 1s, 2s, 4s, 8s, 16s, 30s (capped), each ±0–250ms jitter.

**Retry on:**
- `IOException`, `SocketTimeoutException`, `UnknownHostException`
- HTTP 5xx
- HTTP 429 (respect `Retry-After` if present)

**Do not retry on:**
- HTTP 401 → trigger `authRepository.refreshIfNeeded()`, then retry once. If still 401, stop capture and notify.
- HTTP 403 → log, drop the frame, keep going (likely RLS; alert via notification).
- HTTP 400 → log, drop the frame (bad payload; a bug).

### 10.2 Network loss during capture
- `submit_screen_state` failures queue in memory (max 30 frames ≈ 1 minute). On reconnect, flush in order, then resume live.
- If the queue exceeds 30, drop oldest (we care about current state, not history).
- If `start_match` or `app_finalize_result` fails after max attempts, surface a high-priority notification: "Action required — open WIN ZONE." The app retries these two RPCs every 30s until success or the match ends.

### 10.3 MediaProjection revoked mid-capture
`MediaProjection.Callback.onStop()` → signal service → stop capture → show notification "Screen capture stopped by system. Reopen WIN ZONE to resume." Do not attempt to restart without user action.

### 10.4 App force-stopped by user
- Android kills the service, notification disappears.
- No heartbeat is sent. The backend will see stale state for this player.
- The opponent's phone will (via OCR) eventually see the game's "opponent disconnected" screen and report `opponent_disconnected`. The backend then finalizes per rules.
- The force-stopped user's app does nothing until reopened.

### 10.5 OCR low confidence at kickoff
- Threshold: `ocrConfidence < 0.60` **AND** layout == `in_play` **AND** `teamA`/`teamB` empty.
- Trigger `TeamNameFallbackDialog`: modal Compose dialog with two text fields.
- While dialog is open, `FrameSampler` pauses (flag in shared `CaptureController`).
- On submit, the typed names are cached in `CaptureController` and merged into every subsequent `FrameAnalysis` until OCR produces non-empty names with confidence ≥ 0.75.
- On cancel, app keeps trying OCR; dialog reappears after 10s if still failing.

---

## 11. Agreement logic

### 11.1 Kickoff agreement (`start_match`)

**Local condition** (must all be true):
- `layout == in_play`
- `scoreA != null && scoreB != null`
- `teamA` non-empty && `teamB` non-empty
- `clockSeconds != null && clockSeconds <= 120` (within first 2 minutes)
- `ocrConfidence >= 0.70`

**Remote condition** (must all be true):
- Backend reports opponent's latest state with `layout == in_play`
- Opponent's `teamA`/`teamB` non-empty
- Opponent's `scoreA == 0 && scoreB == 0`
- Opponent's `clockSeconds <= 120`

**Action:** call `start_match(matchId)` once. Guard with an `AtomicBoolean` per match so it never fires twice. After success, set `kickoffConfirmed = true` locally; `app_finalize_result` requires this flag (R2).

**How does the app see the opponent's state?** Two options — pick one and document it:
- **(A) Server push via Realtime:** subscribe to `matches` row updates for this match. The backend writes `p1_last_state`, `p2_last_state` JSONB columns. The app reads the opponent's column from the realtime payload.
- **(B) Polling:** every 2s, the same call cycle also does a `GET /rest/v1/matches?id=eq.{matchId}&select=p1_last_state,p2_last_state`.

**Locked choice: (A) Supabase Realtime.** Subscribe on `CaptureService` start; unsubscribe on stop. Fall back to (B) if the realtime channel drops for >10s. This must be confirmed with the backend team — the columns must exist and be updated by `submit_screen_state`.

If the backend does not yet expose opponent state to the client, the app cannot do agreement locally and must rely on the backend to call `start_match` server-side. **In that case, the app's `start_match`/`app_finalize_result` calls are removed and the backend does everything.** Confirm with backend before coding §11.

### 11.2 Full-time agreement (`app_finalize_result`)

**Local condition:**
- `kickoffConfirmed == true`
- `layout in { full_time, opponent_disconnected, opponent_conceded }`
- `scoreA != null && scoreB != null`
- For `full_time`: `clockSeconds != null && clockSeconds >= 0` (game clock stopped; many games show `90:00` or `FT`)
- `ocrConfidence >= 0.65`

**Remote condition:**
- Opponent's latest state has the same terminal layout **or** a compatible one:
  - Both `full_time` → agree
  - One `full_time`, other `opponent_disconnected`/`opponent_conceded` → agree (the disconnected player sees their own screen differently)
- Scores match on both sides (if both `full_time`)

**Action:** call `app_finalize_result(matchId)` once. Guard with `AtomicBoolean`.

### 11.3 AgreementRules (unit-testable, pure functions)

```kotlin
object AgreementRules {
    fun canStartMatch(local: FrameAnalysis, remote: RemoteState, mySide: Side): Boolean
    fun canFinalize(local: FrameAnalysis, remote: RemoteState, mySide: Side, kickoffConfirmed: Boolean): Boolean
    fun sidesMatch(local: FrameAnalysis, remote: RemoteState, mySide: Side): Boolean
}
```

`mySide` is used to swap `score_a`/`score_b` when comparing to the opponent's view. Both sides report from their own perspective; agreement is checked on the **canonical** (a/b) view.

---

## 12. OCR pipeline

### 12.1 OcrEngine
```kotlin
interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock>
)

data class OcrBlock(
    val text: String,
    val left: Int, val top: Int, val right: Int, val bottom: Int,
    val confidence: Float
)
```

- Wraps ML Kit `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`.
- Runs on `Dispatchers.Default`.
- Reuses the recognizer instance; closes on service destroy.
- Times out after 3 seconds per frame (returns empty result).

### 12.2 GameProfile
```kotlin
data class GameProfile(
    val id: String,
    val displayName: String,
    val regions: Regions,
    val layoutMarkers: LayoutMarkers,
    val clockFormat: ClockFormat,
    val scoreRegex: Regex,
    val teamNameRegex: Regex?   // null = use whole-line heuristics
)

data class Regions(
    val teamA: RectF,        // normalized 0f..1f of frame
    val teamB: RectF,
    val scoreA: RectF,
    val scoreB: RectF,
    val penA: RectF?,
    val penB: RectF?,
    val clock: RectF?
)

data class LayoutMarkers(
    val fullTimeKeywords: List<String>,          // e.g. ["FULL TIME", "FT", "MATCH ENDED"]
    val disconnectedKeywords: List<String>,      // e.g. ["OPPONENT DISCONNECTED", "OPPONENT LEFT"]
    val concededKeywords: List<String>,          // e.g. ["OPPONENT CONCEDED", "SURRENDERED"]
    val inPlayIndicators: List<String>           // e.g. ["PAUSE"] (pause button visible = in play)
)

enum class ClockFormat { MM_SS, MM_SS_APOSTROPHE, SECONDS_ONLY }
```

### 12.3 DefaultGameProfile
The brief does not name the target game. **Locked default:** ship with a `DefaultGameProfile` whose regions are tuned for the most common landscape 1v1 layout:

- Team A name: top-left, `RectF(0.02f, 0.03f, 0.30f, 0.12f)`
- Team B name: top-right, `RectF(0.70f, 0.03f, 0.98f, 0.12f)`
- Score A: `RectF(0.30f, 0.03f, 0.45f, 0.12f)`
- Score B: `RectF(0.55f, 0.03f, 0.70f, 0.12f)`
- Clock: `RectF(0.45f, 0.03f, 0.55f, 0.12f)`
- Penalties: null (no shootout in default profile)

`GameProfile` is **configurable at runtime via a `profiles.json` bundled in assets** so a new game can be added without an app update (drop a JSON, rebuild, or in a future version, fetch remotely). For v1.0.0, the default profile is the only one, and it is documented as a placeholder that the human owner will tune once the real game is chosen.

### 12.4 LayoutClassifier

**Algorithm (locked, deterministic, no ML model):**
1. Run OCR → `OcrResult`.
2. Check `fullTimeKeywords` (case-insensitive, whole-word) against `fullText`. If match ≥ 1 and confidence ≥ 0.7 → `full_time`.
3. Check `disconnectedKeywords`. If match → `opponent_disconnected`.
4. Check `concededKeywords`. If match → `opponent_conceded`.
5. Check `inPlayIndicators` (e.g. a pause button label). If match → `in_play`.
6. Fallback: if `clockSeconds` parsed successfully AND `scoreA`/`scoreB` parsed → `in_play`.
7. Else → `unknown`.

`layoutConfidence` = average confidence of the blocks that matched the winning rule; `0.5` for the fallback rule; `0.0` for `unknown`.

### 12.5 FieldExtractor

For each region in the active `GameProfile`:
1. Crop the downscaled bitmap to the region.
2. Optionally upscale 2× if region height < 32 px.
3. Run OCR on the crop (or reuse blocks that fall entirely inside the region — cheaper).
4. Parse:
   - **Score:** first integer in the crop matching `^\d{1,2}$`.
   - **Clock:** match `ClockFormat`; convert to seconds.
   - **Team name:** strip whitespace, remove control chars, collapse spaces, cap at 32 chars. If `teamNameRegex` is set, apply it; else take the longest alphabetic run.
   - **Penalties:** only if region non-null; same parse as score.
5. Confidence per field = OCR block confidence, or 0f if missing.

**Cropping cost:** re-running OCR on 6 crops is ~3× slower than one full-frame OCR. **Locked choice:** run OCR once on the full downscaled frame, then map each OCR block's bounding box into each region and pick the blocks that fall inside. This is faster and gives the same result for axis-aligned regions. Only fall back to per-crop OCR if a region yields zero blocks.

### 12.6 Clock handling
- If `clockSeconds` is parsed: use it.
- If clock region is null or unreadable: use **wall-clock since kickoff**: `clockSeconds = (nowMs - kickoffAtMs) / 1000`. `kickoffAtMs` is set when `start_match` succeeds.
- If neither: send `null`.

---

## 13. UI screens (Compose, Material 3)

### 13.1 Navigation graph
```
Login → Home → Join → Permissions → Capture → (Fallback dialog overlay)
                  ↘ Settings
```

### 13.2 LoginScreen
- Logo, email field, password field (with show/hide), "Log in" button.
- Loading state on button.
- Error text below.
- Footer link: "Open winzone website."

### 13.3 HomeScreen
- Greeting: "Signed in as {email}."
- If open match: card with match ID (short), stake, opponent ID (short), "Join match" button.
- If no open match: "No open match. Waiting for the website to match you." + "Refresh" button.
- Pull-to-refresh triggers `findOpenMatch` again.
- Sign-out in top app bar.

### 13.4 JoinScreen
- Shows the `MatchSnapshot` returned by `app_join_match`.
- Shows "You are player {one|two} — team {A|B}." (from `my_side`).
- Button: "Continue to permissions."

### 13.5 PermissionScreen
- Three rows: Notifications, Battery optimization (optional), Screen capture.
- Each row: status icon, label, "Grant" button.
- Bottom button "Start capture" enabled only when notifications + MediaProjection granted.
- On start → launch `createScreenCaptureIntent()` → on `RESULT_OK` → start service → navigate to Capture.

### 13.6 CaptureScreen
- Big status: "Capturing — reporting every 2s."
- Last reported state card: layout, scoreA–scoreB, clock, last sent time.
- Agreement status: "Waiting for opponent…" / "Kickoff triggered" / "Full-time agreement reached."
- "Stop capture" button (with confirmation dialog).
- Does **not** hide the notification.

### 13.7 TeamNameFallbackDialog
- Modal, non-dismissible by tap-outside (must choose Cancel or Submit).
- Two text fields: Team A name, Team B name.
- Note: "OCR couldn't read the team names. Type them so we can track the match."
- Submit → cache in `CaptureController` → resume sampler.
- Cancel → dismiss, re-prompt after 10s.

### 13.8 SettingsScreen
- Version, build number, profile ID.
- "Sign out."
- "Open logs" (shows last 500 lines from in-memory ring buffer; shareable).
- Toggle: "Verbose logging" (debug only, hidden in release unless long-pressed on version).

---

## 14. DI wiring (Hilt)

### 14.1 SupabaseModule
```kotlin
@Module @InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}
```

BuildConfig fields come from `local.properties` (not committed):
```
SUPABASE_URL=https://jfniylmbogodgozcczln.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_ZyP4LV6xjYu8T4tyPAgWFw_IQEPiVui
```

### 14.2 OcrModule
Provides a singleton `TextRecognizer` and `OcrEngine`.

### 14.3 CaptureModule
Provides `MediaProjectionSession`, `FrameAnalyzer`, `StateReporter` (scoped to service, not singleton — use `@ServiceScoped` custom scope or construct manually in the service).

---

## 15. Build configuration

### 15.1 `app/build.gradle.kts` (key excerpts)
```kotlin
android {
    namespace = "com.winzone.companion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.winzone.companion"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../keystore/winzone-release.jks")
            storePassword = System.getenv("WINZONE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("WINZONE_KEY_ALIAS")
            keyPassword = System.getenv("WINZONE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.6.0")
    implementation("io.ktor:ktor-client-cio:2.3.11")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

### 15.2 ProGuard rules (`proguard-rules.pro`)
```
# Supabase / Ktor / kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.winzone.companion.**$$serializer { *; }
-keepclassmembers class com.winzone.companion.** { *** Companion; }
-keepclasseswithmembers class com.winzone.companion.** { kotlinx.serialization.KSerializer serializer(...); }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# OkHttp (transitive)
-dontwarn okhttp3.**
-dontwarn okio.**

# Timber
-dontwarn org.jetbrains.annotations.**
```

---

## 16. Testing plan

### 16.1 Unit tests (`src/test/`)
- `AgreementRulesTest` — all combinations of local/remote states for kickoff and finalize.
- `LayoutClassifierTest` — golden OCR text dumps → expected layout.
- `FieldExtractorTest` — golden OCR blocks + profile → expected fields.
- `RetryPolicyTest` — verify backoff sequence, retry/no-retry HTTP codes.
- `ClockParserTest` — MM:SS, MM'SS, seconds-only.
- `PayloadSerializationTest` — `FrameAnalysis` → `raw_json` round-trip.

### 16.2 Instrumented tests (`src/androidTest/`)
- `CaptureServiceTest` — start with a fake MediaProjection intent, verify `startForeground` called with correct type, verify notification is ongoing.
- `MediaProjectionSessionTest` — verify `ImageReader` produces a bitmap (requires a real device or a Robolectric shadow).
- `LoginFlowTest` — Compose UI test: enter credentials, assert navigation to Home on success.

### 16.3 Manual test matrix (must pass before release)
| Device | Android | Test |
|---|---|---|
| Pixel 6 | 14 | Full flow, kickoff, full-time, draw, opponent disconnect |
| Samsung A54 | 14 | Same |
| Xiaomi Redmi Note 12 | 13 | OEM killer behavior, service survives 30 min screen-off |
| OnePlus Nord | 12 | Same |
| Pixel 4a | 11 | Backward compat |
| Emulator API 26 | 8.0 | Min SDK sanity |

### 16.4 Golden-file fixtures
Store anonymized screenshots + expected OCR output in `src/test/resources/fixtures/`. Each fixture is `{name}.png` + `{name}.expected.json`. Tests iterate the folder. This is how `GameProfile` tuning is validated.

---

## 17. Release process

### 17.1 Keystore
- Generate `winzone-release.jks` once, **on the project owner's machine**:
  ```
  keytool -genkeypair -v -keystore winzone-release.jks -alias winzone -keyalg RSA -keysize 4096 -validity 10000
  ```
- Store passwords in environment variables (`WINZONE_KEYSTORE_PASSWORD`, `WINZONE_KEY_ALIAS`, `WINZONE_KEY_PASSWORD`). Never commit them.
- Back up the keystore + passwords in two separate secure locations. Losing it means no updates ever.
- **Play App Signing note:** if Play Store later, either enroll in Play App Signing from the first upload (recommended — then this keystore becomes the "upload key") or keep this as the signing key and accept that losing it kills the app. **Locked choice: enroll in Play App Signing when the Play Store phase starts; keep this keystore as the upload key.**

### 17.2 Build command
```
./gradlew :app:assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`.

### 17.3 Verification before publishing
1. `apksigner verify --print-certs app-release.apk` — confirm signature.
2. Install on a clean device (no debug build present).
3. Run the full flow once.
4. Confirm the foreground notification appears within 5s of starting capture and never disappears while capture runs.
5. Confirm the app **cannot** start capture if notifications are denied (test by revoking).
6. Confirm no wallet/money UI exists anywhere.

### 17.4 Distribution
- Upload `app-release.apk` to the WIN ZONE website at a stable URL, e.g. `https://winzone.example/app/winzone-1.0.0.apk`.
- Provide a `.sha256` checksum alongside.
- Share link via WhatsApp: `https://winzone.example/app`.
- No in-app update mechanism in v1.0.0. (A `version` RPC + update check is a v1.1 feature; note it in `docs/BUILD.md`.)

---

## 18. Logging and diagnostics

- Timber planted in `WinZoneApp`. Debug: `DebugTree`. Release: custom `RingBufferTree` (keeps last 500 lines in memory) + no-op console.
- Log levels: `V` for OCR blocks, `D` for state submissions, `I` for lifecycle, `W` for retries, `E` for failures.
- Never log: passwords, tokens, full email addresses (mask to `u***@d***`).
- `Settings → Open logs` dumps the ring buffer to a shareable text file via FileProvider.

---

## 19. Security checklist

- [ ] RLS verified on `matches` table: user can only `SELECT` rows where they are `player_one_id` or `player_two_id`.
- [ ] `submit_screen_state` rejects non-participants.
- [ ] `start_match` rejects non-participants and non-`matched` status.
- [ ] `app_finalize_result` rejects non-participants and non-`in_progress` status.
- [ ] Publishable key in APK is safe because RLS is on. (Confirmed with backend.)
- [ ] Tokens stored in `EncryptedSharedPreferences`.
- [ ] Network security config enforces TLS 1.2+ and no cleartext.
- [ ] No sensitive data in logs.
- [ ] No `WebView` in the app.
- [ ] No dynamic code loading.
- [ ] `android:allowBackup="false"`.
- [ ] `android:debuggable="false"` in release (default).

---

## 20. Open items to confirm with backend (blocking)

These must be answered before coding §11 (agreement) and §3.2 (`app_join_match` snapshot shape):

1. **`app_join_match` return shape** — does it include `my_side`? If not, how does the app know which side it is?
2. **Opponent state visibility** — does `matches` expose `p1_last_state` / `p2_last_state` (or equivalent) to the client via Realtime? If yes, exact column names and JSON shape. If no, the app cannot do local agreement and the backend must call `start_match` / `app_finalize_result` itself.
3. **Realtime enabled** on the `matches` table for the rows the user can read?
4. **Exact RPC parameter names** — the brief lists positional-looking names; confirm they are named parameters matching §3.2.
5. **`raw_json` accepted type** — `jsonb`? Any size cap?
6. **Rate limit** on `submit_screen_state` — is 0.5 req/s per player acceptable?

Until (1)–(4) are answered, code §1–§10 and §12–§19, and stub §11 behind an interface (`AgreementGate`) with two implementations: `LocalAgreementGate` (if opponent state is visible) and `NoOpAgreementGate` (if backend handles it). Select via a build flag.

---

## 21. Coding order for the agent

Execute in this order. Do not skip ahead.

1. Create Gradle project skeleton per §4 and §15. Confirm `./gradlew :app:assembleDebug` succeeds.
2. Implement `WinZoneApp`, `MainActivity`, Hilt modules, theme.
3. Implement `SessionStore`, `AuthRepository`, `LoginScreen`, `LoginViewModel`. Test login against real Supabase.
4. Implement `SupabaseRpcClient`, `MatchRepository`, `findOpenMatch`, `joinMatch`. Test with a real user.
5. Implement `HomeScreen`, `JoinScreen`, navigation.
6. Implement `CaptureService` skeleton with notification and `startForeground(..., FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)`. No capture yet. Verify on API 34.
7. Implement `MediaProjectionSession`, `FrameSampler`. Dump a frame to disk. Verify.
8. Implement `OcrEngine`, `LayoutClassifier`, `FieldExtractor`, `GameProfile`, `DefaultGameProfile`. Unit test with golden fixtures.
9. Implement `FrameAnalyzer`, `ScreenStatePayload`, `RawJson`.
10. Implement `RetryPolicy`, `StateReporter`. Test against real `submit_screen_state`.
11. Implement `AgreementGate` (stub first, then real per §11 once backend confirms).
12. Implement `CaptureScreen`, `CaptureViewModel`, `TeamNameFallbackDialog`.
13. Implement `PermissionScreen` with exact ordering per §6.
14. Implement `SettingsScreen`, logging.
15. Write all unit tests in §16.1. Iterate until green.
16. Write instrumented tests in §16.2. Run on API 26 and API 34 emulators.
17. Manual test matrix §16.3 on real devices.
18. ProGuard pass: build release, install, run full flow, fix any reflection breakage.
19. Sign release APK per §17.
20. Write `docs/BUILD.md` with build/release steps and the open-items list from §20.

---

## 22. Acceptance criteria (definition of done)

The app is done when **all** of the following are true:

- [ ] A user can log in with their WIN ZONE website credentials.
- [ ] The app finds an open match for that user and calls `app_join_match`.
- [ ] The app requests notifications + MediaProjection, in that order, with correct rationale screens.
- [ ] A persistent, non-dismissible foreground notification appears and stays for the entire capture session.
- [ ] The app captures the screen and runs on-device OCR every 2 seconds.
- [ ] `submit_screen_state` is called every 2 seconds with the correct payload shape.
- [ ] `start_match` is called when both sides agree on kickoff per §11.1.
- [ ] `app_finalize_result` is called when both sides agree on full-time per §11.2.
- [ ] The app never calls any money-moving RPC other than `start_match`.
- [ ] The app never declares a winner.
- [ ] The app never shows a wallet, balance, or stake-entry UI.
- [ ] If notifications are denied, capture does not start.
- [ ] If MediaProjection is denied, capture does not start.
- [ ] If the network drops, submissions retry with exponential backoff and flush on reconnect.
- [ ] If the user force-stops the app, capture stops and no further submissions occur.
- [ ] All unit tests pass.
- [ ] All instrumented tests pass on API 26 and API 34.
- [ ] Manual test matrix §16.3 passes.
- [ ] A signed release APK is produced, verified, and uploaded to the WIN ZONE website.
- [ ] `docs/BUILD.md` is complete and accurate.

---

## 23. Appendix A — Constants (`util/Constants.kt`)

```kotlin
object Constants {
    const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    const val SUPABASE_PUBLISHABLE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    const val CAPTURE_NOTIFICATION_ID = 1001
    const val CAPTURE_CHANNEL_ID = "winzone_capture"
    const val CAPTURE_CHANNEL_NAME = "Screen capture"

    const val SUBMIT_INTERVAL_MS = 2000L
    const val SESSION_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
    const val OCR_TIMEOUT_MS = 3000L

    const val OCR_CONFIDENCE_FALLBACK_THRESHOLD = 0.60f
    const val OCR_CONFIDENCE_KICKOFF_THRESHOLD = 0.70f
    const val OCR_CONFIDENCE_FINALIZE_THRESHOLD = 0.65f
    const val LAYOUT_CONFIDENCE_THRESHOLD = 0.70f

    const val KICKOFF_CLOCK_MAX_SECONDS = 120

    const val MAX_QUEUED_FRAMES = 30
    const val MAX_RAW_JSON_BLOCKS = 40

    const val WAKE_LOCK_TAG = "winzone:capture"
    const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L

    const val PROFILE_ID_DEFAULT = "default-v1"
}
```

---

## 24. Appendix B — Example `profiles.json` (assets)

```json
{
  "profiles": [
    {
      "id": "default-v1",
      "displayName": "Default 1v1 landscape",
      "regions": {
        "teamA": [0.02, 0.03, 0.30, 0.12],
        "teamB": [0.70, 0.03, 0.98, 0.12],
        "scoreA": [0.30, 0.03, 0.45, 0.12],
        "scoreB": [0.55, 0.03, 0.70, 0.12],
        "clock": [0.45, 0.03, 0.55, 0.12],
        "penA": null,
        "penB": null
      },
      "layoutMarkers": {
        "fullTimeKeywords": ["FULL TIME", "FT", "MATCH ENDED", "FULL-TIME"],
        "disconnectedKeywords": ["OPPONENT DISCONNECTED", "OPPONENT LEFT", "CONNECTION LOST"],
        "concededKeywords": ["OPPONENT CONCEDED", "SURRENDERED", "FORFEIT"],
        "inPlayIndicators": ["PAUSE", "II"]
      },
      "clockFormat": "MM_SS",
      "scoreRegex": "^\\d{1,2}$",
      "teamNameRegex": null
    }
  ]
}
```

Regions are `[left, top, right, bottom]` normalized `0f..1f`.

---

## 25. Appendix C — Example `FrameAnalysis.toPayload()`

```kotlin
fun FrameAnalysis.toPayload(matchId: String, side: Side): ScreenStatePayload {
    // side == A means this device IS team_a; side == B means this device IS team_b.
    // Backend expects fields always from the (a, b) canonical view.
    return ScreenStatePayload(
        pMatchId = matchId,
        pLayout = layout.wireName,
        pTeamA = teamA,
        pTeamB = teamB,
        pScoreA = scoreA,
        pScoreB = scoreB,
        pPenA = penA,
        pPenB = penB,
        pClockSeconds = clockSeconds,
        pRawJson = RawJson(
            ocrText = rawText,
            ocrBlocks = rawBlocks.take(Constants.MAX_RAW_JSON_BLOCKS),
            layoutConfidence = layoutConfidence,
            ocrConfidence = ocrConfidence,
            frameTsMs = capturedAtMs,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            profileId = Constants.PROFILE_ID_DEFAULT,
            appVersion = BuildConfig.VERSION_NAME
        )
    )
}
```

`Side` enum: `A`, `B`. `layout.wireName`: `"in_play" | "full_time" | "opponent_disconnected" | "opponent_conceded" | "unknown"`.

---

## 26. Appendix D — Notification factory

```kotlin
object NotificationFactory {
    fun buildCaptureNotification(context: Context, shortMatchId: String): Notification {
        val stopIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_FROM_NOTIFICATION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, Constants.CAPTURE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_capture_notification)
            .setContentTitle("WIN ZONE — capture active")
            .setContentText("Match $shortMatchId — reporting every 2s")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
```

Channel created in `WinZoneApp.onCreate` with `IMPORTANCE_LOW`, no sound, no vibration, no badge.

---

## 27. Appendix E — Failure modes and exact responses

| Failure | Detection | Response |
|---|---|---|
| No network at start | `findOpenMatch` throws `UnknownHostException` | Show "No connection" screen with retry |
| Auth expired | `session` emits null or RPC returns 401 | Route to Login; stop capture if running |
| Notification denied | Permission result `false` | Block capture; show settings deep-link |
| MediaProjection denied | `RESULT_CANCELED` | Block capture; show "Return to website" |
| Service killed by OEM | `onDestroy` called without user stop | Notification disappears; no restart; user must reopen |
| MediaProjection revoked | `MediaProjection.Callback.onStop` | Stop capture; notification "Capture stopped by system" |
| OCR timeout | `withTimeout(3000)` throws | Send `layout = unknown`, all fields null |
| OCR low confidence at kickoff | `ocrConfidence < 0.60` and layout `in_play` | Show `TeamNameFallbackDialog` |
| `submit_screen_state` 5xx | HTTP status | Retry with backoff; queue in memory |
| `submit_screen_state` 429 | HTTP status | Respect `Retry-After`; retry |
| `submit_screen_state` 403 | HTTP status | Log, drop frame, notify "Sync error" |
| `start_match` fails after retries | `Result.failure` after 6 attempts | High-priority notification "Action required" |
| `app_finalize_result` fails after retries | Same | Same |
| Duplicate frame | Same layout + scores + clock ±1s | Skip network call |
| User force-stops | `onDestroy` not called, no callbacks | Nothing; backend stalls; admin handles |

---

## 28. Appendix F — Glossary

- **Canonical view** — the (team_a, team_b) perspective. `my_side` tells the app whether it is A or B; all payloads are sent in canonical view.
- **Agreement** — both players' reported states satisfy the rule in §11.
- **Kickoff** — the moment `start_match` succeeds; money moves here.
- **Full-time** — the moment `app_finalize_result` succeeds; result decided here.
- **Layout** — one of `in_play | full_time | opponent_disconnected | opponent_conceded | unknown`.
- **Frame** — a single screenshot captured via MediaProjection.
- **Profile** — a `GameProfile` describing OCR regions and layout keywords for a specific game.
- **FGS** — Foreground Service.

---

**End of specification.**