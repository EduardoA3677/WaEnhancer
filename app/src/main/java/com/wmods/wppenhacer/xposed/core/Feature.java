package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public abstract class Feature {

    public final ClassLoader classLoader;
    public final XSharedPreferences prefs;
    public final LSPatchPreferences lspatchPrefs;
    public static boolean DEBUG = false;
    private static boolean sLSPatchOptimized = false;
    
    // LSPatch compatibility flags
    protected boolean isLSPatchMode = false;
    protected boolean hasLimitedFeatures = false;

    public Feature(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        this.classLoader = classLoader;
        this.prefs = preferences;
        this.lspatchPrefs = new LSPatchPreferences(preferences);
        
        // Initialize LSPatch compatibility
        initializeLSPatchCompatibility();
    }

    public Feature(@NonNull ClassLoader classLoader, @NonNull Context context) {
        this.classLoader = classLoader;
        this.lspatchPrefs = new LSPatchPreferences(context);
        this.prefs = lspatchPrefs.getXSharedPreferences();
        
        // Initialize LSPatch compatibility
        initializeLSPatchCompatibility();
    }
    
    /**
     * Initialize LSPatch compatibility for this feature
     */
    private void initializeLSPatchCompatibility() {
        isLSPatchMode = LSPatchCompat.isLSPatchEnvironment();
        
        if (isLSPatchMode) {
            // Apply LSPatch optimizations once per session
            if (!sLSPatchOptimized) {
                LSPatchCompat.optimizeForLSPatch();
                LSPatchCompat.logCompatibilityInfo();
                sLSPatchOptimized = true;
            }
            
            // Check if this feature has limitations in current LSPatch mode
            String featureName = getPluginName();
            checkFeatureLimitations(featureName);
            
            Log.d("WaEnhancer-Feature", "Feature " + featureName + " initialized in LSPatch mode" +
                  (hasLimitedFeatures ? " with limitations" : ""));
        }
    }
    
    /**
     * Check if this feature has limitations in current LSPatch environment
     */
    private void checkFeatureLimitations(String featureName) {
        if (!isLSPatchMode) {
            return;
        }
        
        // Resource-related features have limitations in manager mode
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            String[] limitedFeatures = {
                "CustomThemeV2", "CustomView", "BubbleColors", 
                "FilterGroups", "IGStatus", "HideSeenView"
            };
            
            for (String limited : limitedFeatures) {
                if (featureName.equals(limited)) {
                    hasLimitedFeatures = true;
                    Log.w("WaEnhancer-Feature", "Feature " + featureName + 
                          " has limited functionality in LSPatch manager mode");
                    break;
                }
            }
        }
        
        // Features that require system server hooks are not supported
        String[] unsupportedFeatures = {
            "ScopeHook", "AndroidPermissions", "HookBL"
        };
        
        for (String unsupported : unsupportedFeatures) {
            if (featureName.contains(unsupported)) {
                Log.w("WaEnhancer-Feature", "Feature " + featureName + 
                      " is not supported in LSPatch environment");
                break;
            }
        }
    }

    public abstract void doHook() throws Throwable;

    @NonNull
    public abstract String getPluginName();
    
    /**
     * Hook a method with LSPatch compatibility
     */
    protected XC_MethodHook.Unhook hookMethod(Class<?> clazz, String methodName, Object... parameterTypes) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookMethod(clazz, methodName, parameterTypes);
        } else {
            return XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes);
        }
    }
    
    /**
     * Hook all methods with LSPatch compatibility
     */
    protected java.util.Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookAllMethods(clazz, methodName, callback);
        } else {
            return XposedBridge.hookAllMethods(clazz, methodName, callback);
        }
    }
    
    /**
     * Hook all constructors with LSPatch compatibility
     */
    protected java.util.Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> clazz, XC_MethodHook callback) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookAllConstructors(clazz, callback);
        } else {
            return XposedBridge.hookAllConstructors(clazz, callback);
        }
    }
    
    /**
     * Check if a specific feature is available in current environment
     */
    protected boolean isFeatureAvailable(String feature) {
        return LSPatchCompat.isFeatureAvailable(feature);
    }
    
    /**
     * Get preferences with LSPatch compatibility
     */
    protected SharedPreferences getCompatiblePreferences() {
        if (isLSPatchMode && lspatchPrefs != null) {
            return lspatchPrefs;
        }
        return prefs;
    }
    
    /**
     * Log a message with appropriate tag for LSPatch
     */
    protected void log(String message) {
        String tag = "WaEnhancer-" + getPluginName() + (isLSPatchMode ? "-LSPatch" : "");
        XposedBridge.log(tag + ": " + message);
    }
    
    /**
     * Log an error with appropriate tag for LSPatch
     */
    protected void logError(String message, Throwable throwable) {
        String tag = "WaEnhancer-" + getPluginName() + (isLSPatchMode ? "-LSPatch" : "");
        XposedBridge.log(tag + " ERROR: " + message);
        if (throwable != null) {
            XposedBridge.log(throwable);
        }
    }
    
    /**
     * Check if this feature should be skipped in LSPatch
     */
    protected boolean shouldSkipInLSPatch() {
        if (!isLSPatchMode) {
            return false;
        }
        
        String featureName = getPluginName();
        
        // System server features should be skipped
        String[] skipFeatures = {
            "ScopeHook", "AndroidPermissions", "HookBL"
        };
        
        for (String skip : skipFeatures) {
            if (featureName.contains(skip)) {
                log("Skipping feature in LSPatch environment: " + featureName);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get LSPatch mode information
     */
    protected LSPatchCompat.LSPatchMode getLSPatchMode() {
        return LSPatchCompat.getCurrentMode();
    }

    public void log(String tag, String msg) {
        if (DEBUG) {
            XposedBridge.log("[" + tag + "] " + msg);
        }
    }

    public void logDebug(Object obj) {
        if (DEBUG) {
            XposedBridge.log(obj);
        }
    }

}

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
