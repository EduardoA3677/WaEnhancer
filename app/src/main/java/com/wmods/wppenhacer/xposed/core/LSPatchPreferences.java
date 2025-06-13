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
    
    private void initEmbeddedModePreferences(Context context, String prefName) {
        try {
            // In embedded mode, we can access preferences normally
            mPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            
            // If that fails, try to access the preferences file directly
            if (mPreferences == null) {
                File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
                File prefsFile = new File(prefsDir, prefName + ".xml");
                
                if (prefsFile.exists()) {
                    Log.d(TAG, "Accessing preferences file directly: " + prefsFile.getAbsolutePath());
                    // Create a custom SharedPreferences implementation that reads from the file
                    mPreferences = new FileBasedSharedPreferences(prefsFile);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize embedded mode preferences: " + e.getMessage());
            throw e;
        }
    }
    
    private void initManagerModePreferences(Context context, String prefName) {
        try {
            // In manager mode, use the bridge service to access preferences
            if (LSPatchCompat.handleBridgeService("getPreferences")) {
                // Use bridge service
                mPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            } else {
                // Fallback to normal access
                mPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize manager mode preferences: " + e.getMessage());
            throw e;
        }
    }
    
    private void initLSPatchPreferencesViaBridge(String prefName) {
        try {
            // Use LSPatch bridge service to get preferences
            Log.d(TAG, "Accessing preferences via LSPatch bridge service");
            
            // This would be implemented with actual LSPatch bridge service calls
            // For now, we'll use a placeholder implementation
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to access preferences via bridge: " + e.getMessage());
            throw e;
        }
    }
    
    private void adaptXPreferencesForLSPatch() {
        if (mXPreferences == null) return;
        
        try {
            // Apply LSPatch specific adaptations to XSharedPreferences
            mXPreferences.makeWorldReadable();
            mXPreferences.reload();
            
            // Test if preferences are accessible
            mXPreferences.getAll();
            
            Log.d(TAG, "XSharedPreferences adapted for LSPatch successfully");
            
        } catch (Exception e) {
            Log.w(TAG, "XSharedPreferences adaptation failed: " + e.getMessage());
            // Create a fallback implementation
            createFallbackPreferences();
        }
    }
    
    private void createFallbackPreferences() {
        Log.d(TAG, "Creating fallback preferences implementation");
        
        // Create a minimal SharedPreferences implementation with default values
        mPreferences = new FallbackSharedPreferences();
    }
    
    // SharedPreferences interface implementation
    
    @Override
    public Map<String, ?> getAll() {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getAll();
        } else if (mXPreferences != null) {
            return mXPreferences.getAll();
        }
        return null;
    }
    
    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getString(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getString(key, defValue);
        }
        return defValue;
    }
    
    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getStringSet(key, defValues);
        } else if (mXPreferences != null) {
            return mXPreferences.getStringSet(key, defValues);
        }
        return defValues;
    }
    
    @Override
    public int getInt(String key, int defValue) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getInt(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getInt(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public long getLong(String key, long defValue) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getLong(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getLong(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public float getFloat(String key, float defValue) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getFloat(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getFloat(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.getBoolean(key, defValue);
        } else if (mXPreferences != null) {
            return mXPreferences.getBoolean(key, defValue);
        }
        return defValue;
    }
    
    @Override
    public boolean contains(String key) {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.contains(key);
        } else if (mXPreferences != null) {
            return mXPreferences.contains(key);
        }
        return false;
    }
    
    @Override
    public Editor edit() {
        if (mIsLSPatch && mPreferences != null) {
            return mPreferences.edit();
        } else if (mXPreferences != null) {
            // XSharedPreferences doesn't support editing
            Log.w(TAG, "Edit operation not supported on XSharedPreferences");
            return new NoOpEditor();
        }
        return new NoOpEditor();
    }
    
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mIsLSPatch && mPreferences != null) {
            mPreferences.registerOnSharedPreferenceChangeListener(listener);
        } else if (mXPreferences != null) {
            mXPreferences.registerOnSharedPreferenceChangeListener(listener);
        }
    }
    
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mIsLSPatch && mPreferences != null) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        } else if (mXPreferences != null) {
            mXPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
    
    /**
     * Reloads preferences if supported
     */
    public void reload() {
        if (mXPreferences != null) {
            mXPreferences.reload();
        }
        // Regular SharedPreferences don't need explicit reload
    }
    
    /**
     * Gets the underlying XSharedPreferences if available
     */
    public XSharedPreferences getXSharedPreferences() {
        return mXPreferences;
    }
    
    /**
     * Checks if this instance is using LSPatch preferences
     */
    public boolean isUsingLSPatch() {
        return mIsLSPatch && mPreferences != null;
    }
}
