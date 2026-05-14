# ProGuard rules for Wear OS module
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <methods>;
    @dagger.* <methods>;
    <init>();
}
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityRetainedComponentManager { *; }

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
