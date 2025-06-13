package com.wmods.wppenhacer.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import com.wmods.wppenhacer.xposed.core.LSPatchCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing UI preferences based on LSPatch compatibility
 */
public class LSPatchUIHelper {
    
    // Preferences that should be hidden in LSPatch environment
    private static final String[] LSPATCH_INCOMPATIBLE_PREFERENCES = {
        "bootloader_spoofer", // AntiWa feature
        "scope_hook_enabled", // System server hooks
        "android_permissions", // System permissions modification
    };
    
    // Preferences that have limited functionality in LSPatch manager mode
    private static final String[] LSPATCH_MANAGER_LIMITED_PREFERENCES = {
        "custom_themes", // Limited resource hooks
        "custom_view_mods", // Limited resource modifications
        "bubble_colors", // Limited styling capabilities
    };
    
    /**
     * Filters preferences based on LSPatch compatibility
     * @param preferenceGroup The preference group to filter
     * @param context Application context
     */
    public static void filterLSPatchPreferences(PreferenceGroup preferenceGroup, Context context) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return; // No filtering needed in classic Xposed
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<Preference> preferencesToRemove = new ArrayList<>();
        
        // Check each preference in the group
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            String key = preference.getKey();
            
            if (key != null) {
                // Check if preference should be hidden
                if (shouldHidePreference(key)) {
                    preferencesToRemove.add(preference);
                } else if (shouldMarkAsLimited(key)) {
                    // Mark as limited functionality
                    String summary = preference.getSummary() != null ? 
                        preference.getSummary().toString() : "";
                    preference.setSummary(summary + " (Limited in LSPatch)");
                }
            }
            
            // Recursively filter sub-groups
            if (preference instanceof PreferenceGroup) {
                filterLSPatchPreferences((PreferenceGroup) preference, context);
            }
        }
        
        // Remove incompatible preferences
        for (Preference pref : preferencesToRemove) {
            preferenceGroup.removePreference(pref);
        }
    }
    
    /**
     * Checks if a preference should be hidden in LSPatch
     * @param key Preference key
     * @return true if preference should be hidden
     */
    private static boolean shouldHidePreference(String key) {
        // Check against incompatible preferences
        for (String incompatible : LSPATCH_INCOMPATIBLE_PREFERENCES) {
            if (key.equals(incompatible)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a preference has limited functionality in current LSPatch mode
     * @param key Preference key
     * @return true if preference has limitations
     */
    private static boolean shouldMarkAsLimited(String key) {
        if (LSPatchCompat.getCurrentMode() != LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
            return false;
        }
        
        // Check against limited preferences in manager mode
        for (String limited : LSPATCH_MANAGER_LIMITED_PREFERENCES) {
            if (key.equals(limited)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets a user-friendly status message for current LSPatch mode
     * @param context Application context
     * @return Status message
     */
    public static String getLSPatchStatusMessage(Context context) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return "Classic Xposed - All features available";
        }
        
        LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
        switch (mode) {
            case LSPATCH_EMBEDDED:
                return "LSPatch Embedded Mode - Most features available";
            case LSPATCH_MANAGER:
                return "LSPatch Manager Mode - Some limitations apply";
            default:
                return "Unknown LSPatch mode";
        }
    }
    
    /**
     * Gets detailed capability information for current environment
     * @return Array of capability descriptions
     */
    public static String[] getLSPatchCapabilities() {
        List<String> capabilities = new ArrayList<>();
        
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            capabilities.add("• Full Xposed API support");
            capabilities.add("• System server hooks available");
            capabilities.add("• Resource hooks available");
            capabilities.add("• Bridge service available");
        } else {
            // LSPatch capabilities
            if (LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS")) {
                capabilities.add("• Resource hooks available");
            } else {
                capabilities.add("• Resource hooks limited");
            }
            
            if (LSPatchCompat.isFeatureAvailable("SYSTEM_SERVER_HOOKS")) {
                capabilities.add("• System server hooks available");
            } else {
                capabilities.add("• System server hooks unavailable");
            }
            
            if (LSPatchCompat.isFeatureAvailable("SIGNATURE_BYPASS")) {
                capabilities.add("• Signature bypass enabled");
            }
            
            if (LSPatchCompat.isFeatureAvailable("BRIDGE_SERVICE")) {
                capabilities.add("• Bridge service available");
            } else {
                capabilities.add("• Bridge service unavailable");
            }
            
            LSPatchCompat.LSPatchMode mode = LSPatchCompat.getCurrentMode();
            if (mode == LSPatchCompat.LSPatchMode.LSPATCH_MANAGER) {
                capabilities.add("• Manager mode limitations apply");
            }
        }
        
        return capabilities.toArray(new String[0]);
    }
    
    /**
     * Checks if a specific feature is available in current environment
     * @param featureName Feature name
     * @return true if feature is available
     */
    public static boolean isFeatureAvailable(String featureName) {
        if (!LSPatchCompat.isLSPatchEnvironment()) {
            return true; // All features available in classic Xposed
        }
        
        // Map feature names to LSPatch features
        switch (featureName) {
            case "bootloader_spoofer":
            case "anti_detection":
                return false; // These require system-level access
            case "resource_hooks":
                return LSPatchCompat.isFeatureAvailable("RESOURCE_HOOKS");
            case "system_hooks":
                return LSPatchCompat.isFeatureAvailable("SYSTEM_SERVER_HOOKS");
            default:
                return true; // Most features are compatible
        }
    }
}
