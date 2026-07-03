# ── InstaDrop R8 / ProGuard rules ─────────────────────────────────────────────
# OkHttp, Okio, Coil and Room all ship consumer rules, so most of the work is
# done for us. The rules below cover the few app-specific edge cases.

# Keep the line numbers so release crash reports stay readable, but hide the
# original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room generates an implementation of the @Database class by name at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }

# Our domain models / Room entities are plain data holders — keep their members
# so reflection-based mapping and any future (de)serialization stays intact.
-keep class com.instadrop.app.domain.model.** { *; }
-keep class com.instadrop.app.data.history.DownloadEntity { *; }

# OkHttp references optional Conscrypt/BouncyCastle providers via reflection.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Kotlin coroutines internals occasionally referenced reflectively.
-dontwarn kotlinx.coroutines.**
