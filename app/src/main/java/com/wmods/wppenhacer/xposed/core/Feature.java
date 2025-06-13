package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public abstract class Feature {

    public final ClassLoader classLoader;
    public final XSharedPreferences prefs;
    public final LSPatchPreferences lspatchPrefs;
    public static boolean DEBUG = false;
    private static boolean sLSPatchOptimized = false;

    public Feature(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        this.classLoader = classLoader;
        this.prefs = preferences;
        this.lspatchPrefs = new LSPatchPreferences(preferences);
        
        // Apply LSPatch optimizations once
        if (!sLSPatchOptimized && LSPatchCompat.isLSPatchEnvironment()) {
            LSPatchCompat.optimizeForLSPatch();
            LSPatchCompat.logCompatibilityInfo();
            sLSPatchOptimized = true;
        }
    }

    public Feature(@NonNull ClassLoader classLoader, @NonNull Context context) {
        this.classLoader = classLoader;
        this.lspatchPrefs = new LSPatchPreferences(context);
        this.prefs = lspatchPrefs.getXSharedPreferences();
        
        // Apply LSPatch optimizations once
        if (!sLSPatchOptimized && LSPatchCompat.isLSPatchEnvironment()) {
            LSPatchCompat.optimizeForLSPatch();
            LSPatchCompat.logCompatibilityInfo();
            sLSPatchOptimized = true;
        }
    }

    public abstract void doHook() throws Throwable;

    @NonNull
    public abstract String getPluginName();

    /**
     * Gets preference value using LSPatch compatible method
     */
    protected boolean getBooleanPreference(String key, boolean defaultValue) {
        return lspatchPrefs.getBoolean(key, defaultValue);
    }

    protected String getStringPreference(String key, String defaultValue) {
        return lspatchPrefs.getString(key, defaultValue);
    }

    protected int getIntPreference(String key, int defaultValue) {
        return lspatchPrefs.getInt(key, defaultValue);
    }

    protected long getLongPreference(String key, long defaultValue) {
        return lspatchPrefs.getLong(key, defaultValue);
    }

    protected float getFloatPreference(String key, float defaultValue) {
        return lspatchPrefs.getFloat(key, defaultValue);
    }

    /**
     * Checks if a feature is available in the current environment
     */
    protected boolean isFeatureAvailable(String feature) {
        return LSPatchCompat.isFeatureAvailable(feature);
    }

    /**
     * Performs LSPatch compatible hook setup
     */
    protected void setupLSPatchCompatibleHook() {
        if (LSPatchCompat.isLSPatchEnvironment()) {
            logDebug("Setting up LSPatch compatible hooks for " + getPluginName());
        }
    }

    /**
     * Enhanced hook method with LSPatch error handling
     */
    protected boolean performLSPatchCompatibleHook(Runnable hookOperation) {
        try {
            setupLSPatchCompatibleHook();
            hookOperation.run();
            
            if (LSPatchCompat.isLSPatchEnvironment()) {
                logDebug("Successfully applied LSPatch compatible hooks for " + getPluginName());
            }
            return true;
            
        } catch (Exception e) {
            handleLSPatchHookError(e);
            return false;
        }
    }
    
    /**
     * Handle hook errors with LSPatch-specific fallbacks
     */
    protected void handleLSPatchHookError(Exception e) {
        if (LSPatchCompat.isLSPatchEnvironment()) {
            logDebug("LSPatch hook error in " + getPluginName() + ": " + e.getMessage());
            
            // Try LSPatch-specific recovery
            try {
                if (attemptLSPatchFallback()) {
                    logDebug("LSPatch fallback successful for " + getPluginName());
                    return;
                }
            } catch (Exception fallbackError) {
                logDebug("LSPatch fallback also failed: " + fallbackError.getMessage());
            }
            
            // Log as warning rather than error in LSPatch to avoid spam
            log("WARNING: Feature " + getPluginName() + " may have limited functionality in LSPatch: " + e.getMessage());
        } else {
            // Traditional Xposed - log as error
            log("ERROR: Feature " + getPluginName() + " failed to hook: " + e.getMessage());
            log(e);
        }
    }
    
    /**
     * Attempt LSPatch-specific fallback mechanisms
     */
    protected boolean attemptLSPatchFallback() {
        // Override in subclasses that have specific fallback strategies
        return false;
    }
    
    /**
     * Validate that this feature can work in the current environment
     */
    protected boolean validateFeatureCompatibility() {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // Assume compatibility in traditional Xposed
        }
        
        try {
            // Check if feature is supported in current LSPatch mode
            String featureKey = getPluginName().toLowerCase().replace(" ", "_");
            if (!isFeatureAvailable(featureKey)) {
                logDebug("Feature " + getPluginName() + " is not available in current LSPatch mode");
                return false;
            }
            
            // Perform feature-specific validation
            return performFeatureSpecificValidation();
            
        } catch (Exception e) {
            logDebug("Feature validation failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Override in subclasses for feature-specific validation
     */
    protected boolean performFeatureSpecificValidation() {
        return true; // Default: assume compatibility
    }
    
    /**
     * Enhanced doHook wrapper with validation and error handling
     */
    public final boolean safeDoHook() {
        try {
            // Pre-hook validation
            if (!validateFeatureCompatibility()) {
                logDebug("Feature " + getPluginName() + " skipped due to compatibility issues");
                return false;
            }
            
            // Perform the actual hook
            if (LSPatchCompat.isLSPatchEnvironment()) {
                return performLSPatchCompatibleHook(() -> {
                    try {
                        doHook();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            } else {
                doHook();
                return true;
            }
            
        } catch (Throwable t) {
            handleLSPatchHookError(new Exception(t));
            return false;
        }
    }
    
    public void logDebug(Object object) {
        if (!DEBUG) return;
        log(object);
        if (object instanceof Throwable th) {
            Log.i("WAE", this.getPluginName() + "-> " + th.getMessage(), th);
        } else {
            Log.i("WAE", this.getPluginName() + "-> " + object);
        }
    }

    public void logDebug(String title, Object object) {
        if (!DEBUG) return;
        log(title + ": " + object);
        if (object instanceof Throwable th) {
            Log.i("WAE", this.getPluginName() + "-> " + title + ": " + th.getMessage(), th);
        } else {
            Log.i("WAE", this.getPluginName() + "-> " + title + ": " + object);
        }
    }

    public void log(Object object) {
        String logPrefix = LSPatchCompat.isLSPatchEnvironment() ? 
            String.format("[%s-LSPatch] ", this.getPluginName()) : 
            String.format("[%s] ", this.getPluginName());
            
        if (object instanceof Throwable) {
            XposedBridge.log(logPrefix + "Error:");
            XposedBridge.log((Throwable) object);
        } else {
            XposedBridge.log(logPrefix + object);
        }
    }
}
