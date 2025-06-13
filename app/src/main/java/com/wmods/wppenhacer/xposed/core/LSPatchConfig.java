package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * LSPatch Configuration Handler
 * 
 * This class handles reading and processing LSPatch configuration files
 * and provides LSPatch-specific settings management.
 */
public class LSPatchConfig {
    private static final String TAG = "WaEnhancer-LSPatchConfig";
    
    // LSPatch configuration file paths
    private static final String LSPATCH_CONFIG_ASSET = "assets/lspatch/config.json";
    private static final String LSPATCH_CONFIG_ASSET_ALT = "lspatch/config.json";
    
    private static LSPatchConfig sInstance;
    private JSONObject mConfig;
    private Map<String, Object> mConfigCache;
    private boolean mInitialized = false;
    
    private LSPatchConfig() {
        mConfigCache = new HashMap<>();
    }
    
    public static synchronized LSPatchConfig getInstance() {
        if (sInstance == null) {
            sInstance = new LSPatchConfig();
        }
        return sInstance;
    }
    
    /**
     * Initialize configuration from context
     */
    public boolean initialize(Context context) {
        if (mInitialized) {
            return true;
        }
        
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            Log.d(TAG, "Not in LSPatch environment, skipping config initialization");
            return false;
        }
        
        try {
            // Try to load LSPatch configuration
            InputStream configStream = null;
            
            // First try the standard location
            try {
                configStream = context.getAssets().open(LSPATCH_CONFIG_ASSET_ALT);
            } catch (Exception e) {
                Log.d(TAG, "Config not found in standard location, trying alternative");
            }
            
            // Try alternative location
            if (configStream == null) {
                try {
                    ClassLoader cl = context.getClassLoader();
                    configStream = cl.getResourceAsStream(LSPATCH_CONFIG_ASSET);
                } catch (Exception e) {
                    Log.d(TAG, "Config not found in alternative location");
                }
            }
            
            if (configStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(configStream, StandardCharsets.UTF_8));
                StringBuilder configJson = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    configJson.append(line);
                }
                
                mConfig = new JSONObject(configJson.toString());
                mInitialized = true;
                
                Log.i(TAG, "LSPatch configuration loaded successfully");
                logConfigInfo();
                
                configStream.close();
                return true;
            } else {
                Log.w(TAG, "LSPatch configuration file not found");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LSPatch configuration: " + e.getMessage());
        }
        
        // Create default configuration if loading failed
        createDefaultConfig();
        return mInitialized;
    }
    
    /**
     * Create default configuration for LSPatch
     */
    private void createDefaultConfig() {
        try {
            mConfig = new JSONObject();
            
            // Default LSPatch settings
            mConfig.put("useManager", false);
            mConfig.put("debuggable", false);
            mConfig.put("sigBypassLevel", 0);
            mConfig.put("version", "1.0");
            mConfig.put("waenhancer_version", "1.0.0");
            
            mInitialized = true;
            Log.i(TAG, "Created default LSPatch configuration");
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create default configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get configuration value
     */
    public Object getConfig(String key, Object defaultValue) {
        if (!mInitialized || mConfig == null) {
            return defaultValue;
        }
        
        // Check cache first
        if (mConfigCache.containsKey(key)) {
            return mConfigCache.get(key);
        }
        
        try {
            Object value = mConfig.opt(key);
            if (value != null) {
                mConfigCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting config value for key: " + key);
        }
        
        return defaultValue;
    }
    
    /**
     * Get string configuration value
     */
    public String getString(String key, String defaultValue) {
        Object value = getConfig(key, defaultValue);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * Get boolean configuration value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = getConfig(key, defaultValue);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    /**
     * Get integer configuration value
     */
    public int getInt(String key, int defaultValue) {
        Object value = getConfig(key, defaultValue);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid integer value for key: " + key);
            }
        }
        return defaultValue;
    }
    
    /**
     * Check if using LSPatch manager
     */
    public boolean isUsingManager() {
        return getBoolean("useManager", false);
    }
    
    /**
     * Check if debugging is enabled
     */
    public boolean isDebuggable() {
        return getBoolean("debuggable", false);
    }
    
    /**
     * Get signature bypass level
     */
    public int getSignatureBypassLevel() {
        return getInt("sigBypassLevel", 0);
    }
    
    /**
     * Get LSPatch version
     */
    public String getLSPatchVersion() {
        return getString("version", "unknown");
    }
    
    /**
     * Get WaEnhancer version from config
     */
    public String getWaEnhancerVersion() {
        return getString("waenhancer_version", "unknown");
    }
    
    /**
     * Get original signature if available
     */
    public String getOriginalSignature() {
        return getString("originalSignature", null);
    }
    
    /**
     * Get app component factory if available
     */
    public String getAppComponentFactory() {
        return getString("appComponentFactory", null);
    }
    
    /**
     * Check if configuration is initialized
     */
    public boolean isInitialized() {
        return mInitialized;
    }
    
    /**
     * Get raw configuration JSON
     */
    public JSONObject getRawConfig() {
        return mConfig;
    }
    
    /**
     * Log configuration information
     */
    private void logConfigInfo() {
        if (mConfig == null) return;
        
        Log.i(TAG, "=== LSPatch Configuration ===");
        Log.i(TAG, "Use Manager: " + isUsingManager());
        Log.i(TAG, "Debuggable: " + isDebuggable());
        Log.i(TAG, "Signature Bypass Level: " + getSignatureBypassLevel());
        Log.i(TAG, "LSPatch Version: " + getLSPatchVersion());
        Log.i(TAG, "WaEnhancer Version: " + getWaEnhancerVersion());
        
        String originalSig = getOriginalSignature();
        if (originalSig != null) {
            Log.i(TAG, "Original Signature: " + originalSig.substring(0, Math.min(32, originalSig.length())) + "...");
        }
        
        String componentFactory = getAppComponentFactory();
        if (componentFactory != null) {
            Log.i(TAG, "App Component Factory: " + componentFactory);
        }
        
        Log.i(TAG, "============================");
    }
    
    /**
     * Clear configuration cache
     */
    public void clearCache() {
        mConfigCache.clear();
    }
    
    /**
     * Reload configuration
     */
    public void reload(Context context) {
        mInitialized = false;
        mConfig = null;
        clearCache();
        initialize(context);
    }
}
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
