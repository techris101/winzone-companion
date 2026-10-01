# WIN ZONE Companion App — Open Items & Decisions Log

## 1. Architectural Decisions (v1.0.0 Defaults Applied)

### Play Integrity
- **Decision:** Soft-fail in v1.0.0.
- **Rationale:** Root and tamper detection are checked via RootBeer and basic heuristics. Play Integrity errors or absence on non-GMS devices do not hard-crash the app during initial rollout.

### Crash Reporting
- **Decision:** In-memory ring buffer (500 lines) with exportable diagnostics file.
- **Rationale:** Prevents build breaks in CI pipelines when proprietary Firebase configuration files (`google-services.json`) are omitted. Sentry or Crashlytics can be configured in a subsequent update.

### Age Gate
- **Decision:** Mandatory 18+ confirmation checkbox on LoginScreen per §A1.4.

### Game Profile Tuning
- **Decision:** Shipped with single default landscape profile (`default-v1`).
- **Next Steps:** Region tuning should be performed against the specific production mobile title once final frame captures are recorded.

---

## 2. Backend Contract Confirmations (§20 / §A13)
- `matches.my_side`: Authoritative server-side assignment returned from `app_join_match`.
- `submit_screen_state`: Parameter names strictly matched (`p_match_id`, `p_layout`, `p_team_a`, `p_team_b`, `p_score_a`, `p_score_b`, `p_pen_a`, `p_pen_b`, `p_clock_seconds`, `p_raw_json`).
- `start_match`: Triggered once when both sides reach agreement on kickoff.
- `app_finalize_result`: Triggered once when both sides reach agreement on full-time.
