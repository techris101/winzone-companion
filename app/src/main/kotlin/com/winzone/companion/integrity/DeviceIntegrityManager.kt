package com.winzone.companion.integrity

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.scottyab.rootbeer.RootBeer
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

enum class IntegrityLevel {
    CLEAN,
    SUSPICIOUS,
    COMPROMISED
}

@Singleton
class DeviceIntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun checkIntegrity(): IntegrityLevel {
        try {
            val rootBeer = RootBeer(context)
            if (rootBeer.isRooted) {
                Timber.w("Root detected via RootBeer")
                return IntegrityLevel.COMPROMISED
            }

            if (isEmulator()) {
                Timber.w("Emulator environment detected")
                return IntegrityLevel.SUSPICIOUS
            }

            return IntegrityLevel.CLEAN
        } catch (e: Exception) {
            Timber.e(e, "Error checking device integrity")
            return IntegrityLevel.CLEAN
        }
    }

    fun getDeviceFingerprint(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
            val combined = "$androidId:${Build.FINGERPRINT}:${Build.MANUFACTURER}:${Build.MODEL}"
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(combined.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error generating device fingerprint")
            "unknown-fingerprint"
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }
}
