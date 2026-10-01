package com.winzone.companion.data.match

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface MatchRepository {
    suspend fun findOpenMatch(userId: String): MatchSnapshot?
    suspend fun joinMatch(matchId: String): Result<MatchSnapshot>
}

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : MatchRepository {

    override suspend fun findOpenMatch(userId: String): MatchSnapshot? = try {
        Timber.d("Querying open matches for user: %s", userId)
        val result = supabase.postgrest["matches"]
            .select {
                filter {
                    or {
                        eq("player_one_id", userId)
                        eq("player_two_id", userId)
                    }
                    isIn("status", listOf("matched", "in_progress"))
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<MatchRow>()

        val matchRow = result.firstOrNull()
        matchRow?.toSnapshot(userId)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.e(error, "Error querying open match")
        null
    }

    override suspend fun joinMatch(matchId: String): Result<MatchSnapshot> = try {
        Timber.d("Calling app_join_match for match: %s", matchId)
        val params = buildJsonObject {
            put("match_id", matchId)
        }
        val response = supabase.postgrest.rpc("app_join_match", params).decodeSingle<MatchSnapshot>()
        Timber.i("Joined match successfully: %s, assigned side: %s", response.matchId, response.mySide)
        Result.success(response)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        Timber.w(error, "app_join_match RPC failed, falling back to direct match row query")
        try {
            val matchRow = supabase.postgrest["matches"]
                .select {
                    filter { eq("id", matchId) }
                    limit(1)
                }
                .decodeList<MatchRow>()
                .firstOrNull()
                ?: throw IllegalStateException("Match $matchId not found")

            Result.success(matchRow.toSnapshot(matchRow.playerOneId))
        } catch (inner: Exception) {
            if (inner is CancellationException) throw inner
            Timber.e(inner, "Fallback direct match query failed")
            Result.failure(inner)
        }
    }
}
