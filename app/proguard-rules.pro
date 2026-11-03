# Add project specific ProGuard rules here.
# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase

# MuPDF Fitz JNI native bindings
-keep class com.artifex.mupdf.fitz.** { *; }
-keepclassmembers class com.artifex.mupdf.fitz.** { *; }

# Domain models
-keep class com.abhisheksati.mydoc.domain.model.** { *; }

# Media3 ExoPlayer — keep only the public player API and renderer factories accessed via reflection
-keep class androidx.media3.exoplayer.ExoPlayer { *; }
-keep class androidx.media3.exoplayer.ExoPlayer$Builder { *; }
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.Player { *; }
-keep class androidx.media3.exoplayer.source.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.exoplayer.trackselection.** { *; }
-keep class androidx.media3.ui.PlayerView { *; }
-dontwarn androidx.media3.**

# Print Framework programmatic adapter callbacks
-keep class android.print.OpenLayoutResultCallback { *; }
-keep class android.print.OpenWriteResultCallback { *; }

# PDFBox Android — keep entry points and classes loaded via reflection/ServiceLoader
-keep class com.tom_roush.pdfbox.pdmodel.PDDocument { *; }
-keep class com.tom_roush.pdfbox.pdmodel.PDPage { *; }
-keep class com.tom_roush.pdfbox.pdmodel.PDPageContentStream { *; }
-keep class com.tom_roush.pdfbox.rendering.PDFRenderer { *; }
-keep class com.tom_roush.pdfbox.util.** { *; }
-keep class com.tom_roush.pdfbox.cos.** { *; }
-keep class com.tom_roush.pdfbox.io.** { *; }
-keep class com.tom_roush.pdfbox.pdfparser.** { *; }
-keep class com.tom_roush.pdfbox.pdmodel.font.** { *; }
-keep class com.tom_roush.pdfbox.pdmodel.graphics.** { *; }
-keep class com.tom_roush.pdfbox.pdmodel.encryption.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**

# Google MLKit component discovery and text recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep all native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Release APK optimization: strip verbose and debug logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
