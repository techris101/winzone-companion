package com.winzone.companion.update

import android.content.Context
import com.winzone.companion.BuildConfig
import com.winzone.companion.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class VersionInfo(
    @SerialName("latest_version_code") val latestVersionCode: Int,
    @SerialName("latest_version_name") val latestVersionName: String,
    @SerialName("min_supported_version_code") val minSupportedVersionCode: Int,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("sha256") val sha256: String = "",
    @SerialName("release_notes") val releaseNotes: String = ""
)

sealed class UpdateStatus {
    object None : UpdateStatus()
    data class SoftUpdate(val info: VersionInfo) : UpdateStatus()
    data class HardUpdate(val info: VersionInfo) : UpdateStatus()
}

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdates(): UpdateStatus {
        return try {
            val response: String = client.get(Constants.UPDATE_VERSION_URL).body()
            val info = json.decodeFromString<VersionInfo>(response)
            val currentCode = BuildConfig.VERSION_CODE

            when {
                currentCode < info.minSupportedVersionCode -> {
                    Timber.w("Current version %d is below min supported %d (Hard Update)", currentCode, info.minSupportedVersionCode)
                    UpdateStatus.HardUpdate(info)
                }
                currentCode < info.latestVersionCode -> {
                    Timber.i("New version available: %s (Soft Update)", info.latestVersionName)
                    UpdateStatus.SoftUpdate(info)
                }
                else -> UpdateStatus.None
            }
        } catch (e: Exception) {
            Timber.d("Update check skipped or server unreachable: %s", e.message)
            UpdateStatus.None
        }
    }
}
