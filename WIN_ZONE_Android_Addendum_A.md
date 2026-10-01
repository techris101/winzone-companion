# WIN ZONE — Android Companion App
## Addendum A — Missing Specifications (Phase D, v1.0.0)

**Purpose:** This addendum supplements the main specification. Where a
conflict exists, this addendum wins. It covers items the main spec
omitted: onboarding, deep links, anti-cheat, network hardening, capture
edge cases, match-lifecycle edge cases, in-app updates, telemetry,
performance budgets, CI/CD, and the dependency on the backend contract.

---

## A1. Onboarding and first-run experience

### A1.1 Splash screen
- Use `androidx.core:core-splashscreen:1.0.1`.
- Show app icon centered on `#0B0F14` background.
- Duration: system-controlled (do not add artificial delay).
- Theme: `Theme.WinZone.Splash` set as `android:theme` on `MainActivity`,
  swapped to `Theme.WinZone` in `onCreate` via `setTheme()`.

### A1.2 First-run walkthrough (3 pages, shown once)
Stored flag: `DataStore` key `onboarding_complete_v1`.
Pages:
1. **"This app watches your match screen."** — explains capture.
2. **"It needs a visible notification to run."** — Android policy.
3. **"It never handles money."** — reassures user.
Each page: illustration, title, body, page indicator, Skip / Next / Done.
On Done → route to Login.

### A1.3 Legal acceptance
- A single checkbox at the bottom of LoginScreen: *"I agree to the
  Terms of Service and Privacy Policy."*
- Links open in browser: `https://winzone.example/terms` and `/privacy`.
- Login button disabled until checked.
- Flag stored: `DataStore` key `tos_accepted_v1` (with timestamp).
- If ToS version changes (constant `TOS_VERSION` in `Constants.kt`),
  re-prompt.

### A1.4 Age gate
- If the product is gambling-adjacent (stakes are involved), require
  an explicit 18+ confirmation before login.
- Checkbox: *"I confirm I am 18 years or older."*
- Combined with the ToS checkbox on LoginScreen. Both required.
- Flag: `DataStore` key `age_confirmed_v1`.

### A1.5 Permission rationale screens (exact copy)
Before each runtime permission dialog, show an in-app explanation card:

| Permission | Title | Body | Primary button |
|---|---|---|---|
| POST_NOTIFICATIONS | "Allow notifications" | "Capture runs as a foreground service, which Android requires to show a notification. Without it, the app cannot capture your screen." | "Allow" |
| MediaProjection | "Allow screen capture" | "WIN ZONE needs to see your screen to read the match. It does not record audio, and it does not upload your screen — only the parsed match state is sent." | "Continue" |
| Battery exemption (optional) | "Keep capture alive" | "Some phones stop background apps aggressively. Allow WIN ZONE to run without battery restrictions to avoid interruptions." | "Allow" / "Skip" |
| REQUEST_INSTALL_PACKAGES (for updates) | "Enable in-app updates" | "To install updates from inside the app, allow WIN ZONE to install unknown apps. You can revoke this anytime." | "Allow" / "Skip" |

Each rationale uses a consistent card component `PermissionCard`.

---

## A2. Deep links, app links, and shortcuts

### A2.1 Deep link scheme
Two entry points:
- **Custom scheme:** `winzone://join?match_id={uuid}`
- **App link (HTTPS):** `https://winzone.example/app/join?match_id={uuid}`

Both must be declared in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="winzone" android:host="join" />
</intent-filter>

<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="winzone.example" android:pathPrefix="/app/join" />
</intent-filter>
```

Requires `/.well-known/assetlinks.json` hosted on `winzone.example`
(maintained by the website team, not Antigravity).

### A2.2 Deep link handling
- `MainActivity.onNewIntent` and `onCreate` parse `match_id`.
- If user not logged in → queue the `match_id` in `SavedStateHandle` /
  DataStore, route to Login, then auto-navigate to Join after login.
- If logged in → go straight to Join for that match (after verifying
  the user is a participant via `app_join_match`; if not, show error
  "You are not part of this match.").

### A2.3 App shortcuts (`shortcuts.xml`)
- Shortcut 1: **"Join current match"** → deep link to Home, auto-refresh.
- Shortcut 2: **"Open logs"** → deep link to Settings → Logs.
Declared in `res/xml/shortcuts.xml`, referenced in Manifest.

### A2.4 Back-stack rules
- Login is the root when logged out.
- Home is the root when logged in.
- Back from Capture → confirm stop → Home.
- Back from Join → Home.
- Back from Permissions → Join.
- Back from Settings → Home.
- System back from Home → exit (do not re-show splash).

---

## A3. Accessibility and localization

### A3.1 Accessibility
- Every icon button has `contentDescription` (externalized string).
- Every image has `contentDescription` or `null` + `importantForAccessibility="no"`.
- Minimum touch target 48×48 dp (enforced by Material 3 defaults; do not shrink).
- Color contrast: all text passes WCAG AA (4.5:1 body, 3:1 large).
- TalkBack traversal: logical top-to-bottom, left-to-right; test with TalkBack on.
- Focus order in dialogs: Submit is first, Cancel second.
- Announcements: when capture starts, `liveRegion` announces "Capture started."
- Font scaling: layouts must remain usable at 200% system font scale. Use `sp` for text, `dp` for everything else.

### A3.2 Localization
- All user-facing strings in `res/values/strings.xml`.
- No hardcoded strings in Kotlin/Compose.
- `android:supportsRtl="true"`.
- Provide `res/values-fr/strings.xml` and `res/values-es/strings.xml`
  as stubs (English fallback). Actual translations are a later phase.
- Numbers and timestamps formatted via `java.time` + locale-aware formatters.
- Do not concatenate strings; use `<string name="x">%1$s — %2$s</string>`.

---

## A4. Anti-cheat, integrity, and account safety

### A4.1 Play Integrity API
- Integrate `com.google.android.play:integrity:1.3.0`.
- On app start (and every 6 hours while capture runs), request a
  standard integrity token.
- Send the token to the backend via a **new RPC**:
  `submit_integrity_token(match_id, token)` — **must be added by backend.**
- If integrity fails (rooted / emulator / tampered), the app shows a
  blocking screen: *"This device cannot participate in matches."*
- **Decision required:** is integrity enforcement on or off for v1.0.0?
  Recommended: **on, soft-fail** (warn but allow) for v1, then **on,
  hard-fail** for v1.1 once false-positive rate is measured.

### A4.2 Root and emulator detection
- Use `com.scottyab:rootbeer:0.1.0` for root detection.
- Emulator detection via `Build.FINGERPRINT`, `Build.MODEL`, `Build.HARDWARE`.
- Debug build detection via `ApplicationInfo.FLAG_DEBUGGABLE`.
- Result is a tri-state `IntegrityLevel { CLEAN, SUSPICIOUS, COMPROMISED }`.
- Sent with each `submit_screen_state` in `raw_json` as
  `integrity_level`.
- **Policy:** `SUSPICIOUS` → warn, `COMPROMISED` → block capture.

### A4.3 Device fingerprint
- Compute a stable hash: `SHA-256(ANDROID_ID + Build.FINGERPRINT + app signing cert)`.
- Send in `raw_json` as `device_fingerprint`.
- Backend can use this to flag multi-account abuse.
- Do **not** send raw `ANDROID_ID`.

### A4.4 Multi-device login
- Supabase Auth allows concurrent sessions by default.
- **Policy (locked): last-login-wins.** If the same user logs in on a
  second device, the first device's session is invalidated on the next
  `submit_screen_state` (401). The app then stops capture and shows
  *"This account is active on another device."*
- Backend must enforce; app just reacts to 401.

### A4.5 APK signature check
- At startup, verify the app's signing certificate matches the expected
  SHA-256 (hardcoded in `Constants.kt`).
- If mismatch → block everything with *"This app has been modified."*
- Uses `PackageManager.getPackageInfo(..., GET_SIGNING_CERTIFICATES)`.

### A4.6 Screenshot / recording detection during capture
- **No action.** The app is already recording the screen; users
  screenshotting is irrelevant to match integrity.
- Do not block FLAG_SECURE on our own UI — our UI is what the opponent
  sees anyway if both devices are showing the same game.

---

## A5. Network hardening

### A5.1 Certificate pinning
- Pin `jfniylmbogodgozcczln.supabase.co` to its current leaf + intermediate
  CA public key hashes using OkHttp `CertificatePinner`.
- Since Supabase rotates certs, pin **both the leaf and the SPKI of the
  issuing CA** and provide a remote kill-switch: if the app cannot
  connect after 3 attempts, fall back to unpinned for that session and
  log a warning.
- Configuration in `network_security_config.xml` + `CertificatePinner`.

### A5.2 API version header
- Every request adds header `X-WinZone-Client-Version: 1.0.0`.
- Backend may reject old clients; app shows a blocking update prompt if
  it receives HTTP 426 (Upgrade Required).

### A5.3 Request deduplication
- `StateReporter` tracks an in-flight flag; if a `submit_screen_state`
  call is still in flight when the next tick fires, **skip** the tick.
- Never have more than 1 in-flight `submit_screen_state` per match.

### A5.4 Client-side rate limiting
- Hard cap: **1 request per 1.5 seconds** averaged over 10 seconds.
- If the app is backing off (retry loop), the sampler pauses.

### A5.5 Offline queue persistence
- Main spec said the queue is in-memory (max 30). Change: persist the
  queue to disk using Room (single table `pending_state`).
- On app restart, if a match is still open, flush the queue first, then
  resume live.
- Cap: 60 entries / 24 hours old; older entries are dropped.

### A5.6 Compression
- Enable gzip request bodies (Ktor does this by default with
  `ContentEncoding`).
- `raw_json` is capped at 10 KB after serialization; if over, truncate
  `ocr_blocks` further.

### A5.7 Data-usage estimate and warning
- Compute bytes sent per hour. Show in Settings.
- If projected monthly usage > 500 MB, show a warning.

---

## A6. Capture edge cases (explicit policies)

| Situation | Policy |
|---|---|
| Incoming call during capture | Capture continues; frames may be black or show the call UI. OCR yields `unknown`; submission still occurs. |
| User opens WIN ZONE app during capture | Capture continues. Activity is not the source of frames — service is. |
| User opens another app | Capture continues; OCR sees the other app → `unknown`. |
| Split-screen / multi-window | **Block:** detect via `Activity.isInMultiWindowMode` and via screen size mismatch. Show *"Please use WIN ZONE in full-screen."* |
| Screen rotation | Capture continues; `VirtualDisplay` follows current orientation. Profile regions are normalized so they adapt. |
| Screen off | Capture continues; frames are black; OCR yields `unknown`. Wake lock keeps CPU alive but display can sleep. |
| Notification channel disabled by user | `CaptureService` checks channel importance; if `IMPORTANCE_NONE`, stop capture and notify *"Notification channel disabled — enable it to resume."* |
| User swipes notification away | Android may or may not stop the service. To be safe, monitor via a periodic check that our notification is still posted (`NotificationManager.getActiveNotifications`); if missing, stop capture. |
| Audio capture | **Not used.** Do not request `RECORD_AUDIO`. |
| USB debugging enabled | Log warning, allow. |
| Accessibility service active | No effect on us. |

---

## A7. Match lifecycle edge cases

| Situation | Policy |
|---|---|
| Match cancelled by admin | Backend updates `matches.status` to `cancelled`. App subscribed via Realtime receives it → stop capture → show *"Match was cancelled."* |
| Opponent never joins | App polls match status every 30s; after 15 minutes with no opponent activity, show *"Opponent did not join."* Allow user to exit. Backend decides refund. |
| Opponent never reaches kickoff | Same as above, 10-minute timeout. |
| Opponent never reaches full-time | After 3 hours of capture, app stops and shows *"Match timed out."* Backend decides. |
| Match ends but app doesn't detect full-time | Safety timeout: 3 hours after kickoff, stop capture. |
| Match ends, app detects, `app_finalize_result` succeeds | Stop capture within 5 seconds. Notification transitions to *"Match complete."* Auto-dismiss after 10 seconds. |
| App crash mid-match | On relaunch, if `findOpenMatch` returns a match in `in_progress`, offer *"Resume capture"* which restarts the permission flow (MediaProjection needs re-consent). |
| App force-stopped mid-match | Same as crash, but only when user reopens. |
| Multiple matches returned by `findOpenMatch` | Assert impossibility; if it happens, show the most recent and log an error. |
| User logs out while capture runs | Capture stops immediately; notification cleared; user routed to Login. |
| User's token expires and refresh fails | Capture stops after 3 failed refreshes; notification *"Session expired — reopen WIN ZONE."* |

---

## A8. Service lifecycle details

### A8.1 Restart policy
- `onStartCommand` returns `START_NOT_STICKY`.
- Rationale: MediaProjection consent is per-session and cannot be
  re-obtained in the background. Restart would produce a broken service.
- If the OS kills the service, the app shows a recovery notification
  *"Capture stopped — tap to resume"* (post from a lightweight
  `WorkManager` job that monitors service liveness).

### A8.2 Wake lock
- `PARTIAL_WAKE_LOCK`, tag `winzone:capture`.
- Acquired in `onStartCommand`, released in `onDestroy`.
- Renewed every 30 minutes via coroutine (timeout is 4 hours; renewal
  resets the timer).

### A8.3 Notification channels
Two channels:
- `winzone_capture` — LOW importance, no sound, no vibration, ongoing.
- `winzone_alerts` — HIGH importance, sound + vibration, for
  *"Action required"* events (session expired, integrity failure,
  match cancelled).

Channels created once in `WinZoneApp.onCreate`. Never delete.

### A8.4 Foreground service type
- `mediaProjection` type required on API 34+.
- Passed to `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)`.

### A8.5 Process death vs service death
- Process death kills everything; user must reopen.
- Service-only death (rare) → recovery notification via WorkManager.
- `WorkManager` job: `CaptureWatchdogWorker`, scheduled every 15 minutes
  while a match is `in_progress`; if the service is not running and the
  match is still live, post the recovery notification.

---

## A9. In-app update mechanism (sideload)

### A9.1 Version check
- Static JSON at `https://winzone.example/app/version.json`:
  ```json
  {
    "latest_version_code": 2,
    "latest_version_name": "1.0.1",
    "min_supported_version_code": 1,
    "apk_url": "https://winzone.example/app/winzone-1.0.1.apk",
    "sha256": "...",
    "release_notes": "..."
  }
  ```
- Checked on app start and once every 24 hours.
- No backend RPC needed — static file.

### A9.2 Update UX
- If `latest_version_code > current` and `min_supported_version_code <= current`:
  show a **soft** update dialog (Later / Update).
- If `min_supported_version_code > current`: show a **hard** update dialog
  (Update only, cannot dismiss).
- Update flow:
  1. Download APK to `getExternalFilesDir("updates")`.
  2. Verify SHA-256.
  3. Launch install intent via `FileProvider` + `ACTION_VIEW` with MIME
     `application/vnd.android.package-archive`.
  4. Requires `REQUEST_INSTALL_PACKAGES` permission; if not granted,
     deep-link to Settings → Install unknown apps.

### A9.3 Permissions
- Add `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`.

### A9.4 Rollback
- Do not auto-downgrade. If `min_supported_version_code` drops, the app
  just stops nagging.

---

## A10. Telemetry, logging, and support

### A10.1 Crash logging
- **Decision (locked): Firebase Crashlytics** for v1.0.0.
- If the client objects to Google analytics, swap for Sentry later.
- PII scrubbing: Crashlytics auto-scrubs emails; verify custom keys
  never contain tokens.

### A10.2 Ring-buffer logs
- In-memory ring buffer of the last 500 log lines.
- Exposed via Settings → Logs.
- Shareable via FileProvider.

### A10.3 Diagnostic screen (Settings → Diagnostics)
Shows:
- App version + build number
- Device model, Android version, API level
- Session status (logged in / email masked)
- Current match ID (if any)
- Last 10 submission timestamps + status codes
- Network state
- Integrity level
- Data used in this session

### A10.4 Analytics events (optional, opt-in)
If enabled:
- `capture_started`, `capture_stopped`, `kickoff_triggered`,
  `finalize_triggered`, `ocr_low_confidence`, `integrity_failed`.
- No user identifiers, no screen content.
- Opt-in via Settings, default off.

### A10.5 Remote log opt-in
- Settings toggle: *"Send diagnostic logs to WIN ZONE."*
- If enabled, on crash the ring buffer is uploaded to a backend
  endpoint (**new RPC needed**: `submit_diagnostic_log`).

---

## A11. Performance budgets

| Metric | Budget | Measurement |
|---|---|---|
| CPU (average during capture) | < 5% | Android Studio Profiler |
| CPU (peak per frame) | < 25% for < 300 ms | Systrace |
| Battery | < 10%/hour | Battery Historian |
| Memory (RSS) | < 150 MB | Profiler |
| Data | < 20 MB/hour | Settings → Data usage |
| APK size | < 40 MB | `assembleRelease` output |
| Cold start | < 2 s on Pixel 6 | `adb shell am start -W` |
| Frame-to-submit latency | < 3 s (OCR + network) | Custom metric in `raw_json` |

If any budget is exceeded on the test matrix in §16.3, address before release.

### A11.1 Frame-drop policy
- If OCR takes > 2 seconds, skip the next tick (never queue frames).
- If 5 consecutive frames fail OCR, show *"Having trouble reading the screen."*

### A11.2 Memory management
- Reuse two `Bitmap` instances in `FrameSampler` (double buffering).
- Never allocate a `Bitmap` per frame.
- Call `image.close()` in a `finally` block.

---

## A12. CI/CD

### A12.1 Pipeline (GitHub Actions)
- **On PR:** `./gradlew testDebugUnitTest lintDebug`.
- **On push to `main`:** same + `./gradlew assembleDebug`, upload artifact.
- **On tag `v*.*.*`:** `./gradlew assembleRelease`, sign with keystore
  from GitHub Secrets, upload APK + SHA-256 as GitHub Release assets.
- **Manual deploy:** owner copies release APK to website.

### A12.2 Secrets
- `WINZONE_KEYSTORE_BASE64`, `WINZONE_KEYSTORE_PASSWORD`,
  `WINZONE_KEY_ALIAS`, `WINZONE_KEY_PASSWORD`.
- Also `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` (from `local.properties`
  pattern — inject via `-P` flags in CI).

### A12.3 Versioning
- Semantic: `MAJOR.MINOR.PATCH` in `versionName`, monotonic int in
  `versionCode`.
- Bump versionCode on every release. Never reuse.
- Changelog in `CHANGELOG.md`.

### A12.4 Rollback plan
- If v1.0.1 is broken, the website reverts the download link to v1.0.0.
- Do not force-update. Users on the broken version get a soft prompt
  when they next check (which uses the corrected `version.json`).

---

## A13. Backend contract — additional items the app needs

This is not a request to build the backend; it is the list of things the
app **cannot be completed without**. The human owner must confirm each
with the backend team.

### A13.1 New RPCs / endpoints required
1. `submit_integrity_token(match_id uuid, token text) -> void` — §A4.1.
2. `submit_diagnostic_log(match_id uuid, log text) -> void` — §A10.5 (optional).
3. A way to receive **admin cancellation** events on the `matches` row
   (Realtime UPDATE with `status = 'cancelled'`) — §A7.

### A13.2 Schema columns the app must read
- `matches.id`, `matches.status`, `matches.player_one_id`,
  `matches.player_two_id`, `matches.created_at`, `matches.kickoff_at`,
  `matches.p1_last_state` (jsonb), `matches.p2_last_state` (jsonb),
  `matches.my_side` (returned by `app_join_match`, not necessarily a column).
- Confirm exact names.

### A13.3 Realtime configuration
- `matches` table must be in the Realtime publication.
- RLS must allow the client to subscribe to rows it can SELECT.
- The app subscribes to `UPDATE` events on `matches` filtered by
  `id = matchId`.

### A13.4 Auth configuration
- Email confirmation: on or off? App's UX differs.
- Password reset: handled on website, or in-app? Assume website.
- MFA: not supported in v1.0.0.

### A13.5 Rate limits and error shapes
- Supabase default rate limit per user per second? Confirm.
- Error response shape for each RPC (JSON body for `message`, `code`).

### A13.6 Static files (hosted on website, not Supabase)
- `https://winzone.example/app/version.json`
- `https://winzone.example/app/winzone-1.0.1.apk`
- `https://winzone.example/.well-known/assetlinks.json`

---

## A14. Out-of-scope (explicitly, to prevent scope creep)

The following are **not** part of v1.0.0 and Antigravity must not implement them:

- Website changes.
- Backend changes (Supabase RPCs beyond the six the app calls).
- Admin panel.
- Matchmaking.
- Chat.
- Any wallet/balance UI.
- Any results entered by hand.
- Any second game profile (only `default-v1` ships).
- Audio capture.
- Video recording.
- Cloud upload of screenshots.
- WebView.
- Push notifications from FCM (Supabase Realtime is used instead).
- Wear OS, TV, tablet-specific layouts.
- Multi-language beyond stubs.
- In-app purchases.
- Play Store metadata.

---

## A15. Final coding order (revised, supersedes §21)

Antigravity must execute in this order:

1. Gradle skeleton, Hilt, theme, splash.
2. Onboarding walkthrough, ToS + age gate.
3. Auth (login, session store, refresh).
4. Match repository + `findOpenMatch`.
5. Home, Join screens + navigation.
6. Deep links + app links + shortcuts.
7. `CaptureService` skeleton with notification, no capture.
8. `MediaProjectionSession` + `FrameSampler`, dump frame to disk.
9. OCR engine + classifier + extractor + profile, unit tests.
10. `FrameAnalyzer` + payload + `RetryPolicy` + `StateReporter`.
11. Offline queue (Room), rate limiting, dedup, backoff.
12. Certificate pinning, API version header, gzip.
13. Agreement gate (stub first).
14. `CaptureScreen`, `CaptureViewModel`, fallback dialog.
15. Permission flow (exact order, rationale cards).
16. Settings, diagnostics, logs, ring buffer.
17. Integrity, root detection, device fingerprint, signature check.
18. In-app update flow (version.json check, download, install).
19. Accessibility pass (TalkBack, contrast, touch targets).
20. Localization stubs.
21. Telemetry (Crashlytics, optional analytics).
22. Performance profiling; fix any budget violations.
23. Full test matrix run (§16.3 + new cases in this addendum).
24. CI/CD pipeline.
25. Release: sign, verify, upload, document.

---

## A16. Additional acceptance criteria (append to §22)

- [ ] Splash screen renders per Android 12+ spec.
- [ ] Onboarding shows once, never again.
- [ ] ToS + age gate enforced before login.
- [ ] Deep link from WhatsApp opens the app and routes to Join.
- [ ] App link verified (assetlinks.json works on a clean device).
- [ ] App shortcut "Join current match" works.
- [ ] TalkBack traverses every screen logically.
- [ ] App usable at 200% font scale.
- [ ] All strings externalized.
- [ ] Play Integrity token requested and sent per §A4.1 (if enabled).
- [ ] Rooted / tampered device behavior matches §A4.2.
- [ ] Second-device login invalidates the first within one tick.
- [ ] Certificate pinning active; kill-switch verified.
- [ ] Offline queue survives app restart.
- [ ] Rate limiting enforced; no more than 1 req / 1.5s.
- [ ] Update check runs; soft and hard dialogs behave correctly.
- [ ] Diagnostics screen shows all listed fields.
- [ ] Crashlytics receives a test crash in release mode.
- [ ] All performance budgets in §A11 are met on Pixel 6.
- [ ] CI pipeline produces a signed APK on tag push.
- [ ] CHANGELOG.md exists and is updated.

---

**End of Addendum A.**
```

---

## Summary of what was missing and is now covered

For your review, here is the itemized gap list against my first document:

| # | Gap | Added in |
|---|---|---|
| 1 | Splash screen | §A1.1 |
| 2 | First-run onboarding | §A1.2 |
| 3 | ToS + age acceptance | §A1.3, §A1.4 |
| 4 | Permission rationale copy | §A1.5 |
| 5 | Deep links / app links | §A2.1, §A2.2 |
| 6 | App shortcuts | §A2.3 |
| 7 | Back-stack rules | §A2.4 |
| 8 | Accessibility | §A3.1 |
| 9 | Localization | §A3.2 |
| 10 | Play Integrity | §A4.1 |
| 11 | Root / emulator detection | §A4.2 |
| 12 | Device fingerprint | §A4.3 |
| 13 | Multi-device login | §A4.4 |
| 14 | APK signature check | §A4.5 |
| 15 | Screenshot-during-capture policy | §A4.6 |
| 16 | Certificate pinning | §A5.1 |
| 17 | API version header | §A5.2 |
| 18 | Request dedup | §A5.3 |
| 19 | Client rate limiting | §A5.4 |
| 20 | Persistent offline queue | §A5.5 |
| 21 | Compression | §A5.6 |
| 22 | Data-usage warning | §A5.7 |
| 23 | Capture edge cases (call, split-screen, screen-off, notification swipe, audio off) | §A6 |
| 24 | Match lifecycle edge cases (cancel, timeouts, crash recovery) | §A7 |
| 25 | Service restart policy / watchdog | §A8.1, §A8.5 |
| 26 | Wake lock renewal | §A8.2 |
| 27 | Two notification channels | §A8.3 |
| 28 | In-app update (sideload) | §A9 |
| 29 | Crash logging | §A10.1 |
| 30 | Diagnostics screen | §A10.3 |
| 31 | Optional analytics | §A10.4 |
| 32 | Remote log upload | §A10.5 |
| 33 | Performance budgets | §A11 |
| 34 | Frame-drop / memory policy | §A11.1, §A11.2 |
| 35 | CI/CD | §A12 |
| 36 | Versioning + rollback | §A12.3, §A12.4 |
| 37 | Backend contract additions (new RPCs, Realtime, static files) | §A13 |
| 38 | Explicit out-of-scope list | §A14 |
| 39 | Revised coding order | §A15 |
| 40 | Additional acceptance criteria | §A16 |

## What you still must decide before Antigravity starts

1. **Is Play Integrity on or off for v1.0.0?** (soft vs hard)
2. **Is Crashlytics acceptable**, or do you want Sentry / self-hosted?
3. **Is the app 18+ only?** (affects §A1.4)
4. **Backend contract confirmations in §20 + §A13** — without these, Antigravity can code up to §11 and then must stop.
5. **Is there only one game, or several?** If several, you need more `GameProfile` entries and a UI to select the active profile.