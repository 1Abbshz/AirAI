# AirAI ProGuard Rules
-keepattributes *Annotation*
-keep class com.zen.airai.data.db.entity.** { *; }
-keep class com.zen.airai.core.ai.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
