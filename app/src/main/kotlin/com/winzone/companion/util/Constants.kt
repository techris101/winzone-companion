package com.winzone.companion.util

import com.winzone.companion.BuildConfig

object Constants {
    const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    const val SUPABASE_PUBLISHABLE_KEY = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    const val CLIENT_VERSION = "1.0.0"
    const val CLIENT_VERSION_HEADER = "X-WinZone-Client-Version"

    const val CAPTURE_NOTIFICATION_ID = 1001
    const val ALERTS_NOTIFICATION_ID = 1002
    const val CAPTURE_CHANNEL_ID = "winzone_capture"
    const val ALERTS_CHANNEL_ID = "winzone_alerts"

    const val SUBMIT_INTERVAL_MS = 2000L
    const val MIN_SUBMIT_INTERVAL_MS = 1500L
    const val SESSION_REFRESH_INTERVAL_MS = 10 * 60 * 1000L
    const val OCR_TIMEOUT_MS = 3000L

    const val OCR_CONFIDENCE_FALLBACK_THRESHOLD = 0.60f
    const val OCR_CONFIDENCE_KICKOFF_THRESHOLD = 0.70f
    const val OCR_CONFIDENCE_FINALIZE_THRESHOLD = 0.65f
    const val LAYOUT_CONFIDENCE_THRESHOLD = 0.70f

    const val KICKOFF_CLOCK_MAX_SECONDS = 120

    const val MAX_QUEUED_FRAMES = 60
    const val MAX_RAW_JSON_BLOCKS = 40

    const val WAKE_LOCK_TAG = "winzone:capture"
    const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L

    const val PROFILE_ID_DEFAULT = "default-v1"

    const val TOS_VERSION = 1
    const val PREFS_ONBOARDING_COMPLETE = "onboarding_complete_v1"
    const val PREFS_TOS_ACCEPTED = "tos_accepted_v1"
    const val PREFS_AGE_CONFIRMED = "age_confirmed_v1"

    const val ACTION_START_CAPTURE = "com.winzone.companion.action.START_CAPTURE"
    const val ACTION_STOP_CAPTURE = "com.winzone.companion.action.STOP_CAPTURE"
    const val ACTION_OPEN_FROM_NOTIFICATION = "com.winzone.companion.action.OPEN_FROM_NOTIFICATION"

    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    const val EXTRA_MATCH_ID = "extra_match_id"
    const val EXTRA_MY_SIDE = "extra_my_side"

    const val UPDATE_VERSION_URL = "https://winzone.example/app/version.json"
}
