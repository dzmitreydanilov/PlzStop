# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve all classes annotated with @Serializable
-keep @kotlinx.serialization.Serializable class ** { *; }

# Preserve the serializer methods generated for all @Serializable classes
-keepclassmembers class ** {
    public static kotlinx.serialization.KSerializer serializer(...);
}

# Preserve internal serialization-related classes and metadata
-keepnames class kotlinx.serialization.internal.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class **$$serializer { *; }

# Keep all KSerializer implementations
-keep class * implements kotlinx.serialization.KSerializer { *; }

# Keep the generated serialization companion and constructor
-keepnames class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
