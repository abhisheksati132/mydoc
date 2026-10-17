# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\abhis\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase

# MuPDF Fitz JNI native bindings
-keep class com.artifex.mupdf.fitz.** { *; }
-keepclassmembers class com.artifex.mupdf.fitz.** { *; }

# LibreOfficeKit JNI native bindings
-keep class org.libreoffice.kit.** { *; }
-keepclassmembers class org.libreoffice.kit.** { *; }

# Domain models
-keep class com.abhisheksati.mydoc.domain.model.** { *; }

