# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# SecureOps - Keep domain models for serialization
-keep class com.secureops.app.domain.model.** { *; }
-keepclassmembers class com.secureops.app.domain.model.** { *; }

# SecureOps - Keep DTOs for JSON serialization
-keep class com.secureops.app.data.remote.dto.** { *; }
-keepclassmembers class com.secureops.app.data.remote.dto.** { *; }

# SecureOps - Keep API service interfaces
-keep interface com.secureops.app.data.remote.api.** { *; }

# SecureOps - Keep Room entities
-keep class com.secureops.app.data.local.entity.** { *; }
-keepclassmembers class com.secureops.app.data.local.entity.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.secureops.app.**$$serializer { *; }
-keepclassmembers class com.secureops.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.secureops.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# RunAnywhere SDK (if using)
-keep class com.runanywhere.** { *; }
-dontwarn com.runanywhere.**
