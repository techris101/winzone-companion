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

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# RootBeer
-keep class com.scottyab.rootbeer.** { *; }
