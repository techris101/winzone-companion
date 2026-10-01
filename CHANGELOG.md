# Changelog

All notable changes to the WIN ZONE Companion App will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-01

### Added
- Greenfield Android client implementation matching WIN ZONE Spec & Addendum A.
- Onboarding walkthrough explaining capture, mandatory notification, and zero-money handling.
- Supabase GoTrue authentication with `EncryptedSharedPreferences` token persistence.
- Real-time match discovery via PostgREST and `app_join_match` RPC.
- Screen capture via `MediaProjection` with continuous foreground service notification (`CATEGORY_SERVICE`).
- On-device Latin Text Recognition v2 with ML Kit (offline, bundled).
- Deterministic layout classifier and field extractor with bounding-box mapping.
- Local agreement engine evaluating kickoff and full-time conditions with atomic RPC triggers.
- Persistent offline frame queue via Room database (`pending_state`).
- Rate limiting (min 1.5s interval), duplicate suppression, and exponential backoff retry policy.
- RootBeer root detection, emulator detection, and SHA-256 device fingerprinting.
- Sideload in-app update mechanism checking `version.json` via FileProvider package installer.
- Full unit test suite covering agreement rules, layout classifier, field extraction, retry policy, and serialization.
- GitHub Actions CI/CD workflow for automated test, build, and release packaging.
