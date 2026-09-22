# SplitMate R8 / ProGuard Configuration
# 1. Google Tink & AndroidX Security Crypto (EncryptedSharedPreferences AES256-GCM reflection keep rules)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class androidx.security.crypto.** { *; }

# 2. Jetpack Room SQLite Entities & DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 3. Kotlin Coroutines & Serialization / JSON
-dontwarn kotlinx.coroutines.**
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
