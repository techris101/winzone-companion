# WIN ZONE Companion App — Testing Protocol

## 1. Automated Unit Tests

Unit tests validate all pure domain logic, agreement rules, text parsing, retry math, and payload serialization without needing Android hardware:

Run all unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

### Test Suites:
- `AgreementRulesTest`: Covers all kickoff and full-time permutations, ensuring R1, R2, R3, and R4 are never violated.
- `LayoutClassifierTest`: Validates deterministic keyword classification for in_play, full_time, opponent_disconnected, opponent_conceded, and unknown.
- `FieldExtractorTest`: Tests bounding box coordinates against the `default-v1` landscape profile.
- `RetryPolicyTest`: Confirms exponential backoff calculation, jitter ranges, and 5xx / 429 status code handling.
- `ClockParserTest`: Tests MM:SS, apostrophe formats, and second conversions.
- `PayloadSerializationTest`: Asserts JSON schema conformity for `submit_screen_state` payloads and `raw_json` telemetry blocks.

---

## 2. Android Lint Checks

Run static analysis:
```bash
./gradlew :app:lintDebug
```

---

## 3. Manual Device Matrix (§16.3)

| Device Profile | Android Version | Primary Verification Target |
|---|---|---|
| Pixel 6 / 7 | Android 14 (API 34) | Foreground Service type enforcement & per-session MediaProjection consent |
| Samsung Galaxy A-Series | Android 14 (OneUI 6) | OEM background killer exemption & wake lock retention |
| Xiaomi Redmi Note | Android 13 (MIUI) | Notification channel persistence and app standby restrictions |
| API 26 Emulator | Android 8.0 (Min SDK) | Minimum SDK backward compatibility sanity |
