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

    // LSPatch mode flag for features that support it
    protected boolean isLSPatchMode = false;

    public Feature(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        this.classLoader = classLoader;
        this.prefs = preferences;
        this.lspatchPrefs = new LSPatchPreferences(preferences);
        
        // Detect and set LSPatch mode
        this.isLSPatchMode = LSPatchCompat.isLSPatchEnvironment();
        
        // Apply global optimizations if not already done
        if (!sLSPatchOptimized && isLSPatchMode) {
            LSPatchCompat.optimizeForLSPatch();
            sLSPatchOptimized = true;
        }
    }

    public Feature(@NonNull ClassLoader classLoader, @NonNull Context context) {
        this.classLoader = classLoader;
        this.lspatchPrefs = new LSPatchPreferences(context);
        this.prefs = lspatchPrefs.getXSharedPreferences();
        
        // Detect and set LSPatch mode
        this.isLSPatchMode = LSPatchCompat.isLSPatchEnvironment();
        
        // Apply global optimizations if not already done
        if (!sLSPatchOptimized && isLSPatchMode) {
            LSPatchCompat.optimizeForLSPatch();
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
     * Checks if a specific LSPatch feature is available for this module
     */
    protected boolean isLSPatchFeatureAvailable(String feature) {
        return LSPatchCompat.isFeatureAvailable(feature);
    }

    /**
     * Hook a method with LSPatch compatibility
     */
    protected de.robv.android.xposed.XC_MethodHook.Unhook hookMethodCompat(Class<?> clazz, String methodName, 
                                                                           Object[] parameterTypes, de.robv.android.xposed.XC_MethodHook callback) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookMethod(clazz, methodName, parameterTypes, callback);
        } else {
            return de.robv.android.xposed.XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes[0], callback);
        }
    }

    /**
     * Replace a method with LSPatch compatibility
     */
    protected de.robv.android.xposed.XC_MethodHook.Unhook replaceMethodCompat(Class<?> clazz, String methodName, 
                                                                              Object[] parameterTypes, de.robv.android.xposed.XC_MethodReplacement replacement) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.replaceMethod(clazz, methodName, parameterTypes, replacement);
        } else {
            return de.robv.android.xposed.XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypes[0], replacement);
        }
    }

    /**
     * Hook all methods with LSPatch compatibility
     */
    protected java.util.Set<de.robv.android.xposed.XC_MethodHook.Unhook> hookAllMethodsCompat(Class<?> clazz, String methodName, 
                                                                                              de.robv.android.xposed.XC_MethodHook callback) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookAllMethods(clazz, methodName, callback);
        } else {
            return de.robv.android.xposed.XposedBridge.hookAllMethods(clazz, methodName, callback);
        }
    }

    /**
     * Hook constructor with LSPatch compatibility
     */
    protected de.robv.android.xposed.XC_MethodHook.Unhook hookConstructorCompat(Class<?> clazz, Object[] parameterTypes, 
                                                                                de.robv.android.xposed.XC_MethodHook callback) {
        if (isLSPatchMode) {
            return LSPatchHookWrapper.hookConstructor(clazz, parameterTypes, callback);
        } else {
            return de.robv.android.xposed.XposedHelpers.findAndHookConstructor(clazz, parameterTypes[0], callback);
        }
    }

    /**
     * Checks if running in LSPatch mode
     */
    protected boolean isLSPatchMode() {
        return isLSPatchMode;
    }

    /**
     * Gets the current LSPatch mode
     */
    protected LSPatchCompat.LSPatchMode getLSPatchMode() {
        return LSPatchCompat.getCurrentMode();
    }

    /**
     * Log information about LSPatch compatibility for this feature
     */
    protected void logLSPatchInfo() {
        if (isLSPatchMode) {
            logDebug("Feature " + getPluginName() + " running in LSPatch mode: " + getLSPatchMode());
        }
    }

    /**
     * Check if the current feature is supported in LSPatch mode
     * @return true if feature is fully supported, false if limited or unsupported
     */
    protected boolean checkLSPatchSupport() {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // All features supported in classic Xposed
        }
        
        String featureName = getPluginName();
        
        // Features completely incompatible with LSPatch
        String[] incompatibleFeatures = {
            "AntiDetector", // AntiWa - requires system server access
            "ScopeHook", // Requires system server hooks
            "AndroidPermissions", // Requires system server hooks
            "HookBL" // Bootloader spoofer requires system-level access
        };
        
        for (String incompatible : incompatibleFeatures) {
            if (featureName.equals(incompatible)) {
                logDebug("Feature " + featureName + " is not compatible with LSPatch");
                return false;
            }
        }
        
        // Features with limited functionality in LSPatch manager mode
        if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            String[] limitedInManager = {
                "CustomThemeV2", // Limited resource hook capabilities
                "CustomView", // Limited resource modifications
                "BubbleColors", // Limited styling capabilities
                "CustomToolbar", // May have resource limitations
            };
            
            for (String limited : limitedInManager) {
                if (featureName.equals(limited)) {
                    logDebug("Feature " + featureName + " has limited functionality in LSPatch manager mode");
                }
            }
        }
        
        return true;
    }

    /**
     * Should this feature be hidden from UI when running in LSPatch?
     * Override this method in features that should be hidden from LSPatch users
     */
    protected boolean shouldHideInLSPatch() {
        return !checkLSPatchSupport();
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
