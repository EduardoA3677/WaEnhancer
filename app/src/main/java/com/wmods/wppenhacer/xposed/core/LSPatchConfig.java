package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * LSPatch Configuration Handler
 * 
 * This class handles reading and processing LSPatch configuration files
 * and provides LSPatch-specific settings management.
 */
public class LSPatchConfig {
    private static final String TAG = "WaEnhancer-LSPatchConfig";
    private static final String CONFIG_PATH = "assets/lspatch/config.json";
    
    private static JSONObject sConfig = null;
    private static boolean sConfigLoaded = false;
    
    /**
     * Load LSPatch configuration from assets
     * @param context Application context
     * @return true if config was loaded successfully
     */
    public static boolean loadConfig(Context context) {
        if (sConfigLoaded) {
            return sConfig != null;
        }
        
        sConfigLoaded = true;
        
        try {
            InputStream is = context.getAssets().open("lspatch/config.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            
            String configJson = new String(buffer, StandardCharsets.UTF_8);
            sConfig = new JSONObject(configJson);
            
            Log.i(TAG, "LSPatch config loaded successfully");
            return true;
            
        } catch (Exception e) {
            Log.d(TAG, "Could not load LSPatch config: " + e.getMessage());
            
            // Create default config for non-LSPatch environments
            try {
                sConfig = new JSONObject();
                sConfig.put("useManager", false);
                sConfig.put("debuggable", false);
                sConfig.put("sigBypassLevel", 0);
                sConfig.put("appComponentFactory", "");
                
                Log.d(TAG, "Using default LSPatch config");
                return true;
                
            } catch (Exception ex) {
                Log.e(TAG, "Failed to create default config: " + ex.getMessage());
                return false;
            }
        }
    }
    
    /**
     * Gets a boolean value from LSPatch config
     * @param key Config key
     * @param defaultValue Default value
     * @return Config value or default
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        if (sConfig == null) {
            return defaultValue;
        }
        
        try {
            return sConfig.optBoolean(key, defaultValue);
        } catch (Exception e) {
            Log.w(TAG, "Error reading boolean config " + key + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Gets an integer value from LSPatch config
     * @param key Config key
     * @param defaultValue Default value
     * @return Config value or default
     */
    public static int getInt(String key, int defaultValue) {
        if (sConfig == null) {
            return defaultValue;
        }
        
        try {
            return sConfig.optInt(key, defaultValue);
        } catch (Exception e) {
            Log.w(TAG, "Error reading int config " + key + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Gets a string value from LSPatch config
     * @param key Config key
     * @param defaultValue Default value
     * @return Config value or default
     */
    public static String getString(String key, String defaultValue) {
        if (sConfig == null) {
            return defaultValue;
        }
        
        try {
            return sConfig.optString(key, defaultValue);
        } catch (Exception e) {
            Log.w(TAG, "Error reading string config " + key + ": " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Checks if the app is configured to use LSPatch manager
     * @return true if using manager mode
     */
    public static boolean isUsingManager() {
        return getBoolean("useManager", false);
    }
    
    /**
     * Gets the signature bypass level
     * @return Bypass level (0=disabled, 1=pm, 2=pm+openat)
     */
    public static int getSignatureBypassLevel() {
        return getInt("sigBypassLevel", 0);
    }
    
    /**
     * Checks if the app is configured as debuggable
     * @return true if debuggable
     */
    public static boolean isDebuggable() {
        return getBoolean("debuggable", false);
    }
    
    /**
     * Gets the original app component factory class name
     * @return App component factory class name or empty string
     */
    public static String getAppComponentFactory() {
        return getString("appComponentFactory", "");
    }
    
    /**
     * Gets the raw config object
     * @return JSONObject config or null
     */
    public static JSONObject getRawConfig() {
        return sConfig;
    }
    
    /**
     * Checks if config is loaded
     * @return true if config was loaded
     */
    public static boolean isConfigLoaded() {
        return sConfigLoaded && sConfig != null;
    }
    
    /**
     * Logs current configuration
     */
    public static void logConfig() {
        if (sConfig == null) {
            Log.i(TAG, "No LSPatch config available");
            return;
        }
        
        Log.i(TAG, "=== LSPatch Configuration ===");
        Log.i(TAG, "Use Manager: " + isUsingManager());
        Log.i(TAG, "Debuggable: " + isDebuggable());
        Log.i(TAG, "Signature Bypass Level: " + getSignatureBypassLevel());
        Log.i(TAG, "App Component Factory: " + getAppComponentFactory());
        Log.i(TAG, "============================");
    }
}
