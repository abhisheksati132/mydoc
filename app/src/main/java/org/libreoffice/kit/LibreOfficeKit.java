package org.libreoffice.kit;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;
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
        return NativeLibLoader.isLoaded();
    }

    public static synchronized boolean init(Activity activity) {
        if (isInitialized) return true;
        if (!NativeLibLoader.load()) return false;

        try {
            assetManager = activity.getResources().getAssets();
            ApplicationInfo appInfo = activity.getApplicationInfo();
            String dataDir = appInfo.dataDir;
            String cacheDir = activity.getApplication().getCacheDir().getAbsolutePath();
            String apkFile = activity.getApplication().getPackageResourcePath();

            redirectStdio(true);
            isInitialized = initializeNative(dataDir, cacheDir, apkFile, assetManager);
            return isInitialized;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to initialize LibreOfficeKit native engine", t);
            return false;
        }
    }
}

class NativeLibLoader {
    private static boolean loaded = false;

    protected static synchronized boolean isLoaded() {
        return loaded;
    }

    protected static synchronized boolean load() {
        if (loaded) return true;
        try {
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
            System.loadLibrary("c++_shared");
            System.loadLibrary("lo-native-code");
            loaded = true;
            return true;
        } catch (Throwable t) {
            loaded = false;
            return false;
        }
    }
}
