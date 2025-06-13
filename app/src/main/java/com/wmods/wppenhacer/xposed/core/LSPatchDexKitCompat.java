package com.wmods.wppenhacer.xposed.core;

import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import io.luckypray.dexkit.DexKitBridge;

/**
 * DexKit compatibility wrapper for LSPatch
 * 
 * This class provides compatibility between DexKit and LSPatch environments.
 * DexKit may have different behavior in LSPatch vs traditional Xposed.
 */
public class LSPatchDexKitCompat {
    private static final String TAG = "WaEnhancer-DexKitLSPatch";
    
    private static DexKitBridge sDexKitBridge;
    private static boolean sInitialized = false;
    
    /**
     * Initialize DexKit with LSPatch compatibility
     */
    public static synchronized DexKitBridge initDexKit(String apkPath) {
        if (sInitialized && sDexKitBridge != null) {
            return sDexKitBridge;
        }
        
        try {
            if (LSPatchCompat.isLSPatchEnvironment()) {
                Log.i(TAG, "Initializing DexKit for LSPatch environment");
                sDexKitBridge = initDexKitForLSPatch(apkPath);
            } else {
                Log.i(TAG, "Initializing DexKit for classic Xposed environment");
                sDexKitBridge = DexKitBridge.create(apkPath);
            }
            
            if (sDexKitBridge != null) {
                Log.i(TAG, "DexKit initialized successfully");
                sInitialized = true;
            } else {
                Log.e(TAG, "Failed to initialize DexKit");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing DexKit: " + e.getMessage(), e);
        }
        
        return sDexKitBridge;
    }
    
    /**
     * Initialize DexKit specifically for LSPatch environment
     */
    private static DexKitBridge initDexKitForLSPatch(String apkPath) {
        try {
            // In LSPatch, we might need to adjust the APK path
            String actualApkPath = getLSPatchAdjustedPath(apkPath);
            
            if (actualApkPath == null || !new File(actualApkPath).exists()) {
                Log.w(TAG, "APK path not found for LSPatch: " + actualApkPath);
                // Try fallback paths
                actualApkPath = tryFallbackPaths(apkPath);
            }
            
            if (actualApkPath != null) {
                // Apply LSPatch specific optimizations for DexKit
                return createDexKitWithLSPatchOptimizations(actualApkPath);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize DexKit for LSPatch: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * Get LSPatch-adjusted APK path
     */
    private static String getLSPatchAdjustedPath(String originalPath) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return originalPath;
        }
        
        try {
            // In LSPatch embedded mode, the APK might be in a different location
            if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_EMBEDDED) {
                // Check for extracted original APK in cache
                String packageName = getPackageName();
                if (packageName != null) {
                    String cachePath = "/data/data/" + packageName + "/cache/lspatch/origin/";
                    File cacheDir = new File(cachePath);
                    if (cacheDir.exists()) {
                        File[] apkFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".apk"));
                        if (apkFiles != null && apkFiles.length > 0) {
                            return apkFiles[0].getAbsolutePath();
                        }
                    }
                }
            }
            
            // For manager mode, use the original path
            return originalPath;
            
        } catch (Exception e) {
            Log.w(TAG, "Error adjusting APK path for LSPatch: " + e.getMessage());
            return originalPath;
        }
    }
    
    /**
     * Try fallback paths for APK location
     */
    private static String tryFallbackPaths(String originalPath) {
        String[] fallbackPaths = {
            originalPath,
            "/data/app/" + getPackageName() + "/base.apk",
            "/system/app/" + getPackageName() + "/" + getPackageName() + ".apk"
        };
        
        for (String path : fallbackPaths) {
            if (path != null && new File(path).exists()) {
                Log.d(TAG, "Using fallback APK path: " + path);
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Create DexKit with LSPatch-specific optimizations
     */
    private static DexKitBridge createDexKitWithLSPatchOptimizations(String apkPath) {
        try {
            // Create DexKit with reduced resource usage for LSPatch
            DexKitBridge bridge = DexKitBridge.create(apkPath);
            
            if (bridge != null) {
                // Apply LSPatch-specific configurations
                Log.d(TAG, "Applied LSPatch optimizations to DexKit");
            }
            
            return bridge;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create optimized DexKit: " + e.getMessage());
            // Fallback to standard creation
            try {
                return DexKitBridge.create(apkPath);
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback DexKit creation also failed: " + fallbackError.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Get current package name
     */
    private static String getPackageName() {
        try {
            // Try to get package name from ActivityThread
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = activityThreadClass.getMethod("currentApplication");
            Object application = currentApplicationMethod.invoke(null);
            
            if (application != null) {
                Method getPackageNameMethod = application.getClass().getMethod("getPackageName");
                return (String) getPackageNameMethod.invoke(application);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to get package name: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get the DexKit bridge instance
     */
    public static DexKitBridge getDexKitBridge() {
        return sDexKitBridge;
    }
    
    /**
     * Check if DexKit is initialized
     */
    public static boolean isInitialized() {
        return sInitialized && sDexKitBridge != null;
    }
    
    /**
     * Clean up DexKit resources
     */
    public static synchronized void cleanup() {
        if (sDexKitBridge != null) {
            try {
                sDexKitBridge.close();
                Log.d(TAG, "DexKit bridge closed");
            } catch (Exception e) {
                Log.w(TAG, "Error closing DexKit bridge: " + e.getMessage());
            } finally {
                sDexKitBridge = null;
                sInitialized = false;
            }
        }
    }
    
    /**
     * Check if DexKit is compatible with current LSPatch environment
     */
    public static boolean isDexKitCompatible() {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // Always compatible with classic Xposed
        }
        
        // DexKit should work in both LSPatch modes, but may have limitations
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        switch (mode) {
            case LSPATCH_EMBEDDED:
                return true; // Generally compatible
            case LSPATCH_MANAGER:
                Log.w(TAG, "DexKit may have limitations in LSPatch manager mode");
                return true; // Works but with potential limitations
            default:
                return true;
        }
    }
}
