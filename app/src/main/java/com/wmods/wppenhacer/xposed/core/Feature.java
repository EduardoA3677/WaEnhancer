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
