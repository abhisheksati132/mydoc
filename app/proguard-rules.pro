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

# Media3 ExoPlayer keep rules
-keep class androidx.media3.** { *; }

# Print Framework programmatic adapter callbacks
-keep class android.print.OpenLayoutResultCallback { *; }
-keep class android.print.OpenWriteResultCallback { *; }

# PDFBox Android (vector PDF forms, watermarks, stamps & flattening)
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn java.awt.**

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
