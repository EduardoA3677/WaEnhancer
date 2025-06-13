package com.wmods.wppenhacer.xposed.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.wmods.wppenhacer.BuildConfig;

import java.io.File;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

/**
 * LSPatch Compatible Preferences Wrapper
 * 
 * This class provides a compatibility layer for shared preferences that works
 * with both traditional Xposed and LSPatch environments. LSPatch doesn't support
 * XSharedPreferences in the same way as traditional Xposed, so we need to adapt.
 */
public class LSPatchPreferences implements SharedPreferences {
    private static final String TAG = "WaEnhancer-LSPatchPrefs";
    
    private SharedPreferences mPreferences;
    private XSharedPreferences mXPreferences;
    private boolean mIsLSPatch;
    
    public LSPatchPreferences(Context context) {
        mIsLSPatch = LSPatchCompat.isLSPatchEnvironment();
        
        if (mIsLSPatch) {
            Log.d(TAG, "Initializing preferences for LSPatch environment");
            initLSPatchPreferences(context);
        } else {
            Log.d(TAG, "Initializing preferences for classic Xposed environment");
            initXposedPreferences();
        }
    }
    
    public LSPatchPreferences(XSharedPreferences xPrefs) {
        mIsLSPatch = LSPatchCompat.isLSPatchEnvironment();
        
        if (mIsLSPatch && xPrefs != null) {
            Log.d(TAG, "Converting XSharedPreferences for LSPatch compatibility");
            // Try to get context and convert to regular SharedPreferences
            try {
                // In LSPatch, we need to access preferences differently
                String packageName = BuildConfig.APPLICATION_ID;
                String prefName = packageName + "_preferences";
                
                // Try to access via LSPatch service if available
                if (LSPatchCompat.handleBridgeService("getPreferences")) {
                    // Use bridge service to access preferences
                    initLSPatchPreferencesViaBridge(prefName);
                } else {
                    // Fallback to XSharedPreferences but with LSPatch adaptations
                    mXPreferences = xPrefs;
                    adaptXPreferencesForLSPatch();
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to convert XSharedPreferences for LSPatch, using fallback: " + e.getMessage());
                mXPreferences = xPrefs;
            }
        } else {
            mXPreferences = xPrefs;
        }
    }
    
    private void initLSPatchPreferences(Context context) {
        try {
            // In LSPatch environment, use regular SharedPreferences
            String prefName = BuildConfig.APPLICATION_ID + "_preferences";
            
            // Try different access methods based on LSPatch mode
            LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
            
            switch (mode) {
                case LSPATCH_EMBEDDED:
                    // For embedded mode, try to access preferences via file system
                    initEmbeddedModePreferences(context, prefName);
                    break;
                    
                case LSPATCH_MANAGER:
                    // For manager mode, use bridge service
                    initManagerModePreferences(context, prefName);
                    break;
                    
                default:
                    // Fallback to standard method
                    mPreferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LSPatch preferences: " + e.getMessage());
            throw new RuntimeException("Could not initialize preferences for LSPatch", e);
        }
    }
    
    private void initXposedPreferences() {
        try {
            mXPreferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences");
            mXPreferences.makeWorldReadable();
            mXPreferences.reload();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Xposed preferences: " + e.getMessage());
            throw new RuntimeException("Could not initialize XSharedPreferences", e);
        }
    }
    
    /**
     * Initialize preferences for embedded LSPatch mode
     */
    private void initEmbeddedModePreferences(Context context, String prefName) {
        try {
            // In embedded mode, preferences are usually accessible normally
            mPreferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
            
            // Verify access works
            mPreferences.getAll();
            
            Log.d(TAG, "Embedded mode preferences initialized successfully");
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to use standard preferences in embedded mode, trying file-based: " + e.getMessage());
            
            // Fallback to file-based preferences
            try {
                String prefsPath = LSPatchService.getPreferencesPath(BuildConfig.APPLICATION_ID);
                if (prefsPath != null) {
                    mPreferences = new LSPatchPreferencesImpl(prefsPath + "/" + prefName + ".xml");
                } else {
                    // Ultimate fallback
                    String dataDir = context.getApplicationInfo().dataDir;
                    String prefsFile = dataDir + "/shared_prefs/" + prefName + ".xml";
                    mPreferences = new LSPatchPreferencesImpl(prefsFile);
                }
                
                Log.d(TAG, "File-based preferences initialized for embedded mode");
                
            } catch (Exception e2) {
                throw new RuntimeException("Could not initialize preferences for LSPatch embedded mode", e2);
            }
        }
    }
    
    /**
     * Initialize preferences for manager LSPatch mode
     */
    private void initManagerModePreferences(Context context, String prefName) {
        try {
            // Try to use LSPatch bridge service for preferences
            if (LSPatchBridge.isInitialized()) {
                String prefsPath = LSPatchBridge.getPreferencesPath(BuildConfig.APPLICATION_ID);
                if (prefsPath != null) {
                    String prefsFile = prefsPath + "/" + prefName + ".xml";
                    mPreferences = new LSPatchPreferencesImpl(prefsFile);
                    
                    Log.d(TAG, "Manager mode preferences initialized via bridge service");
                    return;
                }
            }
            
            // Fallback to LSPatch service
            String prefsPath = LSPatchService.getPreferencesPath(BuildConfig.APPLICATION_ID);
            if (prefsPath != null) {
                String prefsFile = prefsPath + "/" + prefName + ".xml";
                mPreferences = new LSPatchPreferencesImpl(prefsFile);
                
                Log.d(TAG, "Manager mode preferences initialized via LSPatch service");
                return;
            }
            
            // Final fallback
            mPreferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
            
            Log.d(TAG, "Manager mode preferences initialized with standard method (fallback)");
            
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize preferences for LSPatch manager mode", e);
        }
    }
    
    /**
     * Initialize preferences via LSPatch bridge service
     */
    private void initLSPatchPreferencesViaBridge(String prefName) {
        try {
            if (LSPatchBridge.isInitialized()) {
                String prefsPath = LSPatchBridge.getPreferencesPath(BuildConfig.APPLICATION_ID);
                if (prefsPath != null) {
                    String prefsFile = prefsPath + "/" + prefName + ".xml";
                    mPreferences = new LSPatchPreferencesImpl(prefsFile);
                    
                    Log.d(TAG, "Preferences initialized via LSPatch bridge");
                    return;
                }
            }
            
            throw new Exception("LSPatch bridge not available or preferences path not found");
            
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize preferences via LSPatch bridge", e);
        }
    }
    
    /**
     * Adapt XSharedPreferences for LSPatch compatibility
     */
    private void adaptXPreferencesForLSPatch() {
        Log.d(TAG, "Adapting XSharedPreferences for LSPatch compatibility");
        
        try {
            // In LSPatch, XSharedPreferences might not work properly
            // Try to reload and check if it's functional
            if (mXPreferences != null) {
                mXPreferences.reload();
                
                // Test if we can read preferences
                mXPreferences.getAll();
                
                Log.d(TAG, "XSharedPreferences appears to be functional in LSPatch");
            }
            
        } catch (Exception e) {
            Log.w(TAG, "XSharedPreferences not functional in LSPatch, needs conversion: " + e.getMessage());
            
            // Try to convert to regular SharedPreferences
            try {
                Context context = getCurrentContext();
                if (context != null) {
                    String prefName = BuildConfig.APPLICATION_ID + "_preferences";
                    mPreferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
                    mXPreferences = null; // Clear non-functional XSharedPreferences
                    
                    Log.d(TAG, "Successfully converted XSharedPreferences to regular SharedPreferences");
                }
            } catch (Exception e2) {
                Log.e(TAG, "Failed to convert XSharedPreferences: " + e2.getMessage());
            }
        }
    }
    
    /**
     * Get current context for preferences operations
     */
    private Context getCurrentContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThreadClass.getMethod("getApplication").invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
    }
    
    // =============================================================================
    // SharedPreferences interface implementation
    // =============================================================================
    
    @Override
    public Map<String, ?> getAll() {
        if (mPreferences != null) {
            return mPreferences.getAll();
        } else if (mXPreferences != null) {
            return mXPreferences.getAll();
        }
        return new java.util.HashMap<>();
    }
    
    @Override
    public String getString(String key, String defValue) {
        if (mPreferences != null) {
            return mPreferences.getString(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getString(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        if (mPreferences != null) {
            return mPreferences.getStringSet(key, defValues);
        } else if (mXPreferences != null) {
            return mXPreferences.getStringSet(key, defValues);
        }
        return defValues;
    }
    
    @Override
    public int getInt(String key, int defValue) {
        if (mPreferences != null) {
            return mPreferences.getInt(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getInt(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public long getLong(String key, long defValue) {
        if (mPreferences != null) {
            return mPreferences.getLong(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getLong(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        if (mPreferences != null) {
            return mPreferences.getFloat(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getFloat(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mPreferences != null) {
            return mPreferences.getBoolean(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getBoolean(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public boolean contains(String key) {
        if (mPreferences != null) {
            return mPreferences.contains(key);
        } else if (mXPreferences != null) {
            return mXPreferences.contains(key);
        }
        return false;
    }
    
    @Override
    public Editor edit() {
        if (mPreferences != null) {
            return mPreferences.edit();
        } else {
            Log.w(TAG, "Edit not supported on XSharedPreferences in LSPatch mode");
            return new NoOpEditor();
        }
    }
    
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mPreferences != null) {
            mPreferences.registerOnSharedPreferenceChangeListener(listener);
        } else {
            Log.w(TAG, "Change listeners not supported on XSharedPreferences in LSPatch mode");
        }
    }
    
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mPreferences != null) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
    
    /**
     * Check if this preferences instance is functional
     */
    public boolean isFunctional() {
        try {
            getAll();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get underlying XSharedPreferences (for compatibility)
     */
    public XSharedPreferences getXSharedPreferences() {
        return mXPreferences;
    }
    
    /**
     * Force reload preferences (useful for XSharedPreferences)
     */
    public void reload() {
        if (mXPreferences != null) {
            mXPreferences.reload();
        }
    }
    
    /**
     * No-op editor for cases where editing is not supported
     */
    private static class NoOpEditor implements Editor {
        @Override
        public Editor putString(String key, String value) { return this; }
        
        @Override
        public Editor putStringSet(String key, Set<String> values) { return this; }
        
        @Override
        public Editor putInt(String key, int value) { return this; }
        
        @Override
        public Editor putLong(String key, long value) { return this; }
        
        @Override
        public Editor putFloat(String key, float value) { return this; }
        
        @Override
        public Editor putBoolean(String key, boolean value) { return this; }
        
        @Override
        public Editor remove(String key) { return this; }
        
        @Override
        public Editor clear() { return this; }
        
        @Override
        public boolean commit() { return false; }
        
        @Override
        public void apply() { }
    }
}
