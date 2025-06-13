package com.wmods.wppenhacer.xposed.core;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * File-based SharedPreferences implementation for LSPatch compatibility
 */
class FileBasedSharedPreferences implements SharedPreferences {
    private static final String TAG = "FileBasedPrefs";
    
    private Map<String, Object> mValues = new HashMap<>();
    private File mFile;
    
    public FileBasedSharedPreferences(File file) {
        mFile = file;
        loadFromFile();
    }
    
    private void loadFromFile() {
        try {
            if (!mFile.exists()) {
                Log.d(TAG, "Preferences file does not exist: " + mFile.getAbsolutePath());
                return;
            }
            
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(mFile)) {
                props.loadFromXML(fis);
            }
            
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                // Try to parse the value as different types
                mValues.put(key, parseValue(value));
            }
            
            Log.d(TAG, "Loaded " + mValues.size() + " preferences from file");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load preferences from file: " + e.getMessage());
        }
    }
    
    private Object parseValue(String value) {
        if (value == null) return null;
        
        // Try boolean
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // Try integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        
        // Try long
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {}
        
        // Try float
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {}
        
        // Default to string
        return value;
    }
    
    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(mValues);
    }
    
    @Override
    public String getString(String key, String defValue) {
        Object value = mValues.get(key);
        return value instanceof String ? (String) value : defValue;
    }
    
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = mValues.get(key);
        if (value instanceof Set) {
            try {
                @SuppressWarnings("unchecked")
                Set<String> result = (Set<String>) value;
                return result;
            } catch (ClassCastException ignored) {}
        }
        return defValues;
    }
    
    @Override
    public int getInt(String key, int defValue) {
        Object value = mValues.get(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }
    
    @Override
    public long getLong(String key, long defValue) {
        Object value = mValues.get(key);
        return value instanceof Long ? (Long) value : defValue;
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        Object value = mValues.get(key);
        return value instanceof Float ? (Float) value : defValue;
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = mValues.get(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }
    
    @Override
    public boolean contains(String key) {
        return mValues.containsKey(key);
    }
    
    @Override
    public Editor edit() {
        return new NoOpEditor();
    }
    
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not supported for file-based preferences
    }
    
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not supported for file-based preferences
    }
}

/**
 * Fallback SharedPreferences implementation with default values
 */
class FallbackSharedPreferences implements SharedPreferences {
    private static final String TAG = "FallbackPrefs";
    
    private Map<String, Object> mDefaults = new HashMap<>();
    
    public FallbackSharedPreferences() {
        // Initialize with common WaEnhancer default values
        initializeDefaults();
        Log.w(TAG, "Using fallback preferences with default values");
    }
    
    private void initializeDefaults() {
        // Add common WaEnhancer preference defaults
        mDefaults.put("xposed_enabled", true);
        mDefaults.put("show_online", false);
        mDefaults.put("hide_seen", false);
        mDefaults.put("anti_revoke", false);
        mDefaults.put("download_profile", false);
        mDefaults.put("custom_theme", false);
        mDefaults.put("filter_groups", false);
        mDefaults.put("media_quality", false);
        mDefaults.put("status_download", false);
        mDefaults.put("view_once", false);
        mDefaults.put("call_privacy", false);
        mDefaults.put("custom_privacy", false);
        mDefaults.put("dnd_mode", false);
        mDefaults.put("freeze_lastseen", false);
        mDefaults.put("hide_receipt", false);
        mDefaults.put("hide_chat", false);
        mDefaults.put("typing_privacy", false);
        mDefaults.put("bootloader_spoofer", false);
        mDefaults.put("proximity_audios", false);
        mDefaults.put("disable_sensor_proximity", false);
        mDefaults.put("old_status", false);
        mDefaults.put("status_style", 0);
        mDefaults.put("filter_chats", false);
        mDefaults.put("separategroups", false);
        mDefaults.put("copystatus", false);
        mDefaults.put("toast_viewed_status", false);
        mDefaults.put("toast_viewed_message", false);
        mDefaults.put("calltype", false);
        mDefaults.put("admin_grp", false);
        mDefaults.put("new_chat", false);
        mDefaults.put("pinned_limit", false);
        mDefaults.put("share_limit", false);
        mDefaults.put("show_edit_message", false);
        mDefaults.put("tasker", false);
        mDefaults.put("others_igstatus", false);
        mDefaults.put("bubble_colors", false);
        mDefaults.put("custom_time", false);
        mDefaults.put("custom_toolbar", false);
        mDefaults.put("custom_view", false);
        mDefaults.put("hide_seen_view", false);
        mDefaults.put("hide_tabs", false);
    }
    
    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(mDefaults);
    }
    
    @Override
    public String getString(String key, String defValue) {
        Object value = mDefaults.get(key);
        return value instanceof String ? (String) value : defValue;
    }
    
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = mDefaults.get(key);
        if (value instanceof Set) {
            try {
                @SuppressWarnings("unchecked")
                Set<String> result = (Set<String>) value;
                return result;
            } catch (ClassCastException ignored) {}
        }
        return defValues != null ? defValues : new HashSet<>();
    }
    
    @Override
    public int getInt(String key, int defValue) {
        Object value = mDefaults.get(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }
    
    @Override
    public long getLong(String key, long defValue) {
        Object value = mDefaults.get(key);
        return value instanceof Long ? (Long) value : defValue;
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        Object value = mDefaults.get(key);
        return value instanceof Float ? (Float) value : defValue;
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = mDefaults.get(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }
    
    @Override
    public boolean contains(String key) {
        return mDefaults.containsKey(key);
    }
    
    @Override
    public Editor edit() {
        return new NoOpEditor();
    }
    
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not supported
    }
    
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // Not supported
    }
}

/**
 * No-op Editor implementation for read-only preferences
 */
class NoOpEditor implements SharedPreferences.Editor {
    private static final String TAG = "NoOpEditor";
    
    @Override
    public SharedPreferences.Editor putString(String key, String value) {
        Log.w(TAG, "Edit operation not supported: putString(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
        Log.w(TAG, "Edit operation not supported: putStringSet(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor putInt(String key, int value) {
        Log.w(TAG, "Edit operation not supported: putInt(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor putLong(String key, long value) {
        Log.w(TAG, "Edit operation not supported: putLong(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor putFloat(String key, float value) {
        Log.w(TAG, "Edit operation not supported: putFloat(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor putBoolean(String key, boolean value) {
        Log.w(TAG, "Edit operation not supported: putBoolean(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor remove(String key) {
        Log.w(TAG, "Edit operation not supported: remove(" + key + ")");
        return this;
    }
    
    @Override
    public SharedPreferences.Editor clear() {
        Log.w(TAG, "Edit operation not supported: clear()");
        return this;
    }
    
    @Override
    public boolean commit() {
        Log.w(TAG, "Edit operation not supported: commit()");
        return false;
    }
    
    @Override
    public void apply() {
        Log.w(TAG, "Edit operation not supported: apply()");
    }
}
