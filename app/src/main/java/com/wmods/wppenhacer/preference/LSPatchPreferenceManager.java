package com.wmods.wppenhacer.preference;

import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.wmods.wppenhacer.xposed.core.LSPatchCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages preference visibility and availability based on LSPatch compatibility
 */
public class LSPatchPreferenceManager {
    private static final String TAG = "LSPatchPrefManager";
    
    // Preferences that are completely incompatible with LSPatch
    private static final String[] INCOMPATIBLE_PREFERENCES = {
        "bootloader_spoofer",
        "bootloader_spoofer_custom", 
        "bootloader_spoofer_xml",
        "anti_wa_features", // System-level anti-detection
        "custom_privacy_system", // System-level privacy hooks
        "scope_hook_settings", // System server scope hooks
        "android_permissions_settings" // System permissions modification
    };
    
    // Preferences that have limited functionality in LSPatch manager mode
    private static final String[] LIMITED_IN_MANAGER_MODE = {
        "custom_theme_v2",
        "bubble_colors", 
        "custom_view_modifications",
        "resource_hook_settings",
        "theme_customization"
    };
    
    /**
     * Filters preferences based on LSPatch compatibility
     * @param preferenceScreen The preference screen to filter
     * @param context Application context
     */
    public static void filterPreferences(PreferenceScreen preferenceScreen, Context context) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            // All preferences are available in classic Xposed
            return;
        }
        
        Log.i(TAG, "Filtering preferences for LSPatch compatibility");
        
        List<Preference> toRemove = new ArrayList<>();
        List<Preference> toDisable = new ArrayList<>();
        
        // Scan all preferences recursively
        scanPreferences(preferenceScreen, toRemove, toDisable);
        
        // Remove incompatible preferences
        for (Preference pref : toRemove) {
            removePreferenceFromParent(pref, preferenceScreen);
            Log.d(TAG, "Removed incompatible preference: " + pref.getKey());
        }
        
        // Disable limited preferences with explanatory summary
        for (Preference pref : toDisable) {
            pref.setEnabled(false);
            String originalSummary = pref.getSummary() != null ? pref.getSummary().toString() : "";
            pref.setSummary(originalSummary + "\n[LSPatch: Limited functionality in manager mode]");
            Log.d(TAG, "Disabled limited preference: " + pref.getKey());
        }
        
        Log.i(TAG, String.format("LSPatch filtering complete: %d removed, %d disabled", 
                                toRemove.size(), toDisable.size()));
    }
    
    /**
     * Recursively scans preferences to find incompatible ones
     */
    private static void scanPreferences(PreferenceGroup group, List<Preference> toRemove, List<Preference> toDisable) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            String key = pref.getKey();
            
            if (key != null) {
                // Check for completely incompatible preferences
                if (isIncompatiblePreference(key)) {
                    toRemove.add(pref);
                    continue;
                }
                
                // Check for limited functionality in manager mode
                if (LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER 
                    && isLimitedInManagerMode(key)) {
                    toDisable.add(pref);
                }
            }
            
            // Recursively check nested preference groups
            if (pref instanceof PreferenceGroup) {
                scanPreferences((PreferenceGroup) pref, toRemove, toDisable);
            }
        }
    }
    
    /**
     * Checks if a preference is completely incompatible with LSPatch
     */
    private static boolean isIncompatiblePreference(String key) {
        if (key == null) return false;
        
        for (String incompatible : INCOMPATIBLE_PREFERENCES) {
            if (key.equals(incompatible) || key.contains(incompatible)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a preference has limited functionality in LSPatch manager mode
     */
    private static boolean isLimitedInManagerMode(String key) {
        if (key == null) return false;
        
        for (String limited : LIMITED_IN_MANAGER_MODE) {
            if (key.equals(limited) || key.contains(limited)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Removes a preference from its parent group
     */
    private static void removePreferenceFromParent(Preference preference, PreferenceGroup root) {
        if (removePreferenceRecursive(root, preference)) {
            return;
        }
        
        // If not found, try to remove from root directly
        root.removePreference(preference);
    }
    
    /**
     * Recursively searches and removes a preference from preference groups
     */
    private static boolean removePreferenceRecursive(PreferenceGroup group, Preference preference) {
        // Try to remove directly from this group
        if (group.removePreference(preference)) {
            return true;
        }
        
        // Search in nested groups
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                if (removePreferenceRecursive((PreferenceGroup) pref, preference)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Shows a notification about LSPatch limitations
     */
    public static void showLSPatchLimitations(Context context) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return;
        }
        
        StringBuilder message = new StringBuilder();
        message.append("LSPatch Mode Detected\n\n");
        
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        message.append("Current Mode: ").append(mode).append("\n\n");
        
        message.append("Limitations:\n");
        message.append("• System server hooks disabled\n");
        message.append("• Bootloader spoofer unavailable\n");
        message.append("• Some anti-detection features limited\n");
        
        if (mode == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            message.append("• Resource hooks limited\n");
            message.append("• Theme customization reduced\n");
        }
        
        message.append("\nIncompatible features have been hidden from settings.");
        
        Log.i(TAG, message.toString());
    }
    
    /**
     * Checks if a specific feature is available in current environment
     */
    public static boolean isFeatureAvailable(String featureKey) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true;
        }
        
        return !isIncompatiblePreference(featureKey) && 
               (!isLimitedInManagerMode(featureKey) || 
                LSPatchCompat.getCurrentMode() != LSPatchCompat.LSPatchMode.LSPATCH_MANAGER);
    }
    
    /**
     * Gets feature compatibility information
     */
    public static String getFeatureCompatibilityInfo(String featureKey) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return "Full functionality available";
        }
        
        if (isIncompatiblePreference(featureKey)) {
            return "Not available in LSPatch";
        }
        
        if (isLimitedInManagerMode(featureKey) && 
            LSPatchCompat.getCurrentMode() == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            return "Limited functionality in LSPatch manager mode";
        }
        
        return "Available";
    }
}
