package com.winzone.companion.data.remote

import com.winzone.companion.data.state.ScreenStatePayload
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface SupabaseRpcClient {
    suspend fun submitScreenState(payload: ScreenStatePayload): Result<Unit>
    suspend fun startMatch(matchId: String): Result<Unit>
    suspend fun finalizeResult(matchId: String): Result<Unit>
    suspend fun submitIntegrityToken(matchId: String, token: String): Result<Unit>
}

@Singleton
class SupabaseRpcClientImpl @Inject constructor(
    private val supabase: SupabaseClient
) : SupabaseRpcClient {

    override suspend fun submitScreenState(payload: ScreenStatePayload): Result<Unit> = try {
        supabase.postgrest.rpc("submit_screen_state", payload)
        Timber.d("Submitted screen state: layout=%s, score=%d-%d, clock=%s",
            payload.pLayout, payload.pScoreA, payload.pScoreB, payload.pClockSeconds)
        Result.success(Unit)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.w(error, "Failed to submit screen state for match: %s", payload.pMatchId)
        Result.failure(error)
    }

    override suspend fun startMatch(matchId: String): Result<Unit> = try {
        Timber.i("Invoking start_match RPC for match: %s", matchId)
        val params = buildJsonObject {
            put("match_id", matchId)
        }
        supabase.postgrest.rpc("start_match", params)
        Timber.i("start_match RPC completed successfully")
        Result.success(Unit)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.e(error, "start_match RPC failed for match: %s", matchId)
        Result.failure(error)
    }

    override suspend fun finalizeResult(matchId: String): Result<Unit> = try {
        Timber.i("Invoking app_finalize_result RPC for match: %s", matchId)
        val params = buildJsonObject {
            put("match_id", matchId)
        }
        supabase.postgrest.rpc("app_finalize_result", params)
        Timber.i("app_finalize_result RPC completed successfully")
        Result.success(Unit)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.e(error, "app_finalize_result RPC failed for match: %s", matchId)
        Result.failure(error)
    }

    override suspend fun submitIntegrityToken(matchId: String, token: String): Result<Unit> = try {
        Timber.d("Submitting Play Integrity token for match: %s", matchId)
        val params = buildJsonObject {
            put("match_id", matchId)
            put("token", token)
        }
        supabase.postgrest.rpc("submit_integrity_token", params)
        Result.success(Unit)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.w(error, "submit_integrity_token RPC failed (soft-fail policy)")
        Result.failure(error)
    }
}
