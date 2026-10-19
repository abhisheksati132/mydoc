package org.libreoffice.kit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.File;
import java.nio.ByteBuffer;

public final class LibreOfficeKit {
    private static final String TAG = "LibreOfficeKit";
    private static AssetManager assetManager;
    private static boolean isInitialized = false;

    private LibreOfficeKit() {}

    public static native boolean initializeNative(String dataDir, String cacheDir, String apkFile, AssetManager assetManager);
    public static native ByteBuffer getLibreOfficeKitHandle();
    public static native void redirectStdio(boolean state);

    public static synchronized boolean isAvailable() {
        return isInitialized || NativeLibLoader.isLoaded();
    }

    public static synchronized boolean isInitialized() {
        return isInitialized;
    }

    public static synchronized boolean init(Activity activity) {
        return init((Context) activity, null);
    }

    public static synchronized boolean init(Context context, File engineDir) {
        if (isInitialized) return true;

        boolean nativeLoaded = false;
        if (engineDir != null && engineDir.exists()) {
            File libDir = new File(engineDir, "lib");
            if (!libDir.exists()) {
                libDir = engineDir;
            }
            nativeLoaded = NativeLibLoader.loadFromDirectory(libDir);
        }
        if (!nativeLoaded) {
            nativeLoaded = NativeLibLoader.load();
        }
        if (!nativeLoaded) return false;

        try {
            assetManager = context.getResources().getAssets();
            ApplicationInfo appInfo = context.getApplicationInfo();
            String dataDir = (engineDir != null && engineDir.exists()) ? engineDir.getAbsolutePath() : appInfo.dataDir;
            String cacheDir = context.getCacheDir().getAbsolutePath();
            File engineApk = (engineDir != null) ? new File(engineDir, "libreoffice_core.apk") : null;
            String apkFile = (engineApk != null && engineApk.exists()) ? engineApk.getAbsolutePath() : context.getPackageResourcePath();

            redirectStdio(true);
            isInitialized = initializeNative(dataDir, cacheDir, apkFile, assetManager);
            return isInitialized;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize LibreOfficeKit native engine", t);
            return false;
        }
    }

    public static synchronized void reset() {
        isInitialized = false;
        NativeLibLoader.reset();
    }
}

class NativeLibLoader {
    private static final String TAG = "NativeLibLoader";
    private static boolean loaded = false;
    private static boolean loadAttempted = false;

    protected static synchronized boolean isLoaded() {
        if (!loadAttempted) {
            load();
        }
        return loaded;
    }

    protected static synchronized void reset() {
        loaded = false;
        loadAttempted = false;
    }

    protected static synchronized boolean loadFromDirectory(File libDir) {
        if (loaded) return true;
        if (libDir == null || !libDir.exists() || !libDir.isDirectory()) {
            return false;
        }
        loadAttempted = true;

        File searchDir = libDir;
        if (android.os.Build.SUPPORTED_ABIS != null) {
            for (String abi : android.os.Build.SUPPORTED_ABIS) {
                File sub = new File(libDir, abi);
                if (sub.exists() && new File(sub, "liblo-native-code.so").exists()) {
                    searchDir = sub;
                    break;
                }
            }
        }

        String[] libs = new String[] {
            "libc++_shared.so",
            "libnspr4.so", "libplds4.so", "libplc4.so", "libnssutil3.so",
            "libfreebl3.so", "libsqlite3.so", "libsoftokn3.so", "libnss3.so",
            "libnssckbi.so", "libnssdbm3.so", "libsmime3.so", "libssl3.so",
            "liblo-native-code.so"
        };

        boolean loLoaded = false;
        try {
            for (String libName : libs) {
                File libFile = new File(searchDir, libName);
                if (!libFile.exists()) {
                    libFile = new File(libDir, libName);
                }
                if (libFile.exists()) {
                    try {
                        System.load(libFile.getAbsolutePath());
                        if (libName.contains("lo-native-code")) {
                            loLoaded = true;
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, "Non-fatal error loading library " + libName + ": " + t.getMessage());
                    }
                }
            }
            loaded = loLoaded;
            return loaded;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load libraries from directory " + libDir.getAbsolutePath(), t);
            loaded = false;
            return false;
        }
    }

    protected static synchronized boolean load() {
        if (loaded) return true;
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("nspr4");
            System.loadLibrary("plds4");
            System.loadLibrary("plc4");
            System.loadLibrary("nssutil3");
            System.loadLibrary("freebl3");
            System.loadLibrary("sqlite3");
            System.loadLibrary("softokn3");
            System.loadLibrary("nss3");
            System.loadLibrary("nssckbi");
            System.loadLibrary("nssdbm3");
            System.loadLibrary("smime3");
            System.loadLibrary("ssl3");
            System.loadLibrary("lo-native-code");
            loaded = true;
            return true;
        } catch (Throwable t) {
            loaded = false;
            return false;
        }
    }
}
